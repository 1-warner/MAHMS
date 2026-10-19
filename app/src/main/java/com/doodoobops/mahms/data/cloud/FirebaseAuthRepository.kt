package com.doodoobops.mahms.data.cloud

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.doodoobops.mahms.data.entity.UserEntity
import com.doodoobops.mahms.data.repository.UserRepository
import com.doodoobops.mahms.domain.model.Role
import com.doodoobops.mahms.utils.AppLog
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/** Result of a register / login attempt, shown directly to the user on failure. */
sealed class AuthOutcome {
    data class Success(val user: UserEntity, val offline: Boolean = false) : AuthOutcome()
    data class Failure(val message: String) : AuthOutcome()
}

/**
 * Authentication backed by Firebase (online-hosted auth service).
 *
 *  - Email/password: Firebase stores only a salted hash of the password server-side.
 *  - Google sign-in (SSO): Credential Manager -> Google ID token -> Firebase credential.
 *  - Profile + role live in Firestore `users/{uid}`; a copy is cached in Room so the rest
 *    of the app (session, role routing) keeps working unchanged and offline.
 *  - Demo accounts seeded locally are migrated into Firebase on their first sign-in.
 */
class FirebaseAuthRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val users: UserRepository,
    private val cloud: CloudSync,
) {

    // ---------------------------------------------------------------- register

    suspend fun register(
        fullName: String,
        email: String,
        phone: String,
        password: String,
        role: String,
    ): AuthOutcome {
        val cleanEmail = email.trim().lowercase()
        return try {
            val created = auth.createUserWithEmailAndPassword(cleanEmail, password).await()
            val uid = created.user?.uid ?: return AuthOutcome.Failure("Could not create the account.")
            // Await so the write is sent while we are still authenticated (before sign-out).
            writeProfile(uid, fullName, cleanEmail, phone, role)
            val local = users.upsert(fullName, cleanEmail, phone, password, role)
            auth.signOut() // user is sent to the Login screen after registering
            AppLog.i("Auth", "Registered a new $role account")
            AuthOutcome.Success(local)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AuthOutcome.Failure(friendly(e))
        }
    }

    // ------------------------------------------------------------------- login

    suspend fun login(email: String, password: String): AuthOutcome {
        val cleanEmail = email.trim().lowercase()

        val failure: AuthOutcome.Failure? = try {
            auth.signInWithEmailAndPassword(cleanEmail, password).await()
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseNetworkException) {
            AppLog.w("Auth", "No network, trying the offline sign-in copy")
            return offlineLogin(cleanEmail, password)
        } catch (e: FirebaseAuthInvalidUserException) {
            migrateLocalAccount(cleanEmail, password)
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            // With email-enumeration protection Firebase reports "no such user" and
            // "wrong password" identically, so try the local-account migration too.
            migrateLocalAccount(cleanEmail, password)
        } catch (e: Exception) {
            AuthOutcome.Failure(friendly(e))
        }

        if (failure != null) {
            AppLog.w("Auth", "Email sign-in failed: ${failure.message}")
            return failure
        }
        return finishLogin(cleanEmail, password)
    }

    // ------------------------------------------------------- Google SSO sign-in

    /**
     * @param activityContext must be an Activity context (Credential Manager shows UI).
     * @param webClientId the OAuth *web* client id (R.string.default_web_client_id).
     */
    suspend fun signInWithGoogle(activityContext: Context, webClientId: String): AuthOutcome {
        return try {
            val option = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .build()
            val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
            val response = CredentialManager.create(activityContext)
                .getCredential(activityContext, request)

            val credential = response.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
                AppLog.i("Auth", "Google single sign-on accepted by Firebase")
                val email = auth.currentUser?.email?.lowercase()
                    ?: return AuthOutcome.Failure("Your Google account has no email address.")
                finishLogin(email, password = "")
            } else {
                AuthOutcome.Failure("Unsupported sign-in credential.")
            }
        } catch (e: GetCredentialCancellationException) {
            AuthOutcome.Failure("Google sign-in was cancelled.")
        } catch (e: NoCredentialException) {
            AuthOutcome.Failure("No Google account found on this device. Add one in Settings and retry.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AuthOutcome.Failure(friendly(e))
        }
    }

    fun signOut() = auth.signOut()

    /** False for Google-only accounts, which have no password to change. */
    fun hasPassword(): Boolean =
        auth.currentUser?.providerData?.any { it.providerId == EmailAuthProvider.PROVIDER_ID } ?: true

    /**
     * Changes the signed-in user's password in Firebase (after re-authenticating with the
     * current one) and refreshes the offline cache. Returns null on success, else a message.
     * When no Firebase session exists (offline login) it only updates the local cache.
     */
    suspend fun changePassword(userId: Long, current: String, newPassword: String): String? {
        val fb = auth.currentUser
        val email = fb?.email
        if (fb == null || email == null) {
            if (!users.matchesPassword(userId, current)) return "Current password is incorrect"
            users.updatePassword(userId, newPassword)
            return null
        }
        return try {
            fb.reauthenticate(EmailAuthProvider.getCredential(email, current)).await()
            fb.updatePassword(newPassword).await()
            users.updatePassword(userId, newPassword)
            AppLog.i("Auth", "Password changed")
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseAuthWeakPasswordException) {
            "That password is too weak."
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            "Current password is incorrect"
        } catch (e: Exception) {
            friendly(e)
        }
    }

    /** Downloads and re-uploads all data with Cloud Firestore (manual "Sync now"). */
    fun syncNow(role: String?): Boolean {
        if (auth.currentUser == null) return false
        AppLog.i("Sync", "Manual sync started")
        cloud.syncAll(includeStaffData = role != Role.PATIENT.label)
        return true
    }

    // ---------------------------------------------------------------- internals

    /** A locally-seeded demo account signing in for the first time: create it in Firebase. */
    private suspend fun migrateLocalAccount(email: String, password: String): AuthOutcome.Failure? {
        val local = users.login(email, password)
            ?: return AuthOutcome.Failure("Incorrect email or password")
        return try {
            val created = auth.createUserWithEmailAndPassword(email, password).await()
            created.user?.uid?.let { writeProfile(it, local.fullName, email, local.phone, local.role) }
            null // signed in
        } catch (e: FirebaseAuthUserCollisionException) {
            // The account already exists online, so the password really was wrong.
            AuthOutcome.Failure("Incorrect email or password")
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseNetworkException) {
            AuthOutcome.Failure(friendly(e))
        } catch (e: Exception) {
            AuthOutcome.Failure(friendly(e))
        }
    }

    private suspend fun finishLogin(email: String, password: String): AuthOutcome {
        val fb = auth.currentUser ?: return AuthOutcome.Failure("Sign-in failed. Please try again.")
        val existingLocal = users.findByEmail(email)

        val name: String
        val phone: String
        val role: String
        when (val result = fetchProfile(fb.uid)) {
            is ProfileResult.Found -> {
                name = result.fullName ?: fb.displayName ?: email.substringBefore('@')
                phone = result.phone ?: ""
                role = result.role ?: existingLocal?.role ?: Role.PATIENT.label
            }
            ProfileResult.Missing -> {
                // First ever sign-in for this account (e.g. new Google user): default to Patient.
                name = existingLocal?.fullName ?: fb.displayName ?: email.substringBefore('@')
                phone = existingLocal?.phone ?: ""
                role = existingLocal?.role ?: Role.PATIENT.label
                writeProfile(fb.uid, name, email, phone, role)
            }
            ProfileResult.Unavailable -> {
                // Could not reach Firestore: never overwrite the cloud profile, use the cache.
                name = existingLocal?.fullName ?: fb.displayName ?: email.substringBefore('@')
                phone = existingLocal?.phone ?: ""
                role = existingLocal?.role ?: Role.PATIENT.label
            }
        }

        val local = users.upsert(name, email, phone, password, role)
        cloud.syncAll(includeStaffData = role != Role.PATIENT.label)
        AppLog.i("Auth", "Signed in as $role")
        return AuthOutcome.Success(local)
    }

    private suspend fun offlineLogin(email: String, password: String): AuthOutcome {
        val local = users.login(email, password)
        return if (local != null) {
            AuthOutcome.Success(local, offline = true)
        } else {
            AuthOutcome.Failure("No internet connection, and these credentials are not saved on this device yet.")
        }
    }

    private suspend fun writeProfile(uid: String, name: String, email: String, phone: String, role: String) {
        try {
            withTimeoutOrNull(10_000) {
                firestore.collection("users").document(uid).set(
                    mapOf(
                        "fullName" to name,
                        "email" to email,
                        "phone" to phone,
                        "role" to role,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                    SetOptions.merge(),
                ).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Non-fatal: the local cache still has the profile.
        }
    }

    private sealed interface ProfileResult {
        data class Found(val fullName: String?, val phone: String?, val role: String?) : ProfileResult
        data object Missing : ProfileResult
        data object Unavailable : ProfileResult
    }

    private suspend fun fetchProfile(uid: String): ProfileResult {
        return try {
            val snap = withTimeoutOrNull(8_000) {
                firestore.collection("users").document(uid).get().await()
            } ?: return ProfileResult.Unavailable
            if (snap.exists()) {
                ProfileResult.Found(
                    fullName = snap.getString("fullName"),
                    phone = snap.getString("phone"),
                    role = snap.getString("role"),
                )
            } else {
                ProfileResult.Missing
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            ProfileResult.Unavailable
        }
    }

    private fun friendly(e: Exception): String = when (e) {
        is FirebaseNetworkException -> "No internet connection. Check your network and try again."
        // Weak-password is a subclass of invalid-credentials, so it must be checked first.
        is FirebaseAuthWeakPasswordException -> "That password is too weak."
        is FirebaseAuthUserCollisionException -> "An account with this email already exists."
        is FirebaseAuthInvalidCredentialsException,
        is FirebaseAuthInvalidUserException -> "Incorrect email or password"
        is FirebaseTooManyRequestsException -> "Too many attempts. Please try again later."
        else -> e.localizedMessage ?: "Something went wrong. Please try again."
    }
}
