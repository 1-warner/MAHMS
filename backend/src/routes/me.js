'use strict';

// /api/me and /api/patients/me
//
// The app calls /api/me straight after sign-in to learn its role and which patient or doctor
// record belongs to the account. That is the server-side version of AppContainer's
// startLinkedSession.

const express = require('express');
const db = require('../db');

const router = express.Router();

/** GET /api/me — the signed-in account, its role and its linked hospital record. */
router.get('/me', async (req, res, next) => {
  try {
    const user = req.user;

    const patient = await db.one(
      `SELECT patient_id, client_ref, first_name, last_name, phone, email, address
       FROM patients WHERE user_id = $1`,
      [user.user_id],
    );
    const doctor = await db.one(
      `SELECT doctor_id, client_ref, first_name, last_name, specialization, department
       FROM doctors WHERE user_id = $1`,
      [user.user_id],
    );

    res.json({
      userId: user.user_id,
      email: user.email,
      fullName: user.full_name,
      phone: user.phone,
      role: user.role,
      createdAt: user.created_at,
      patient,
      doctor,
    });
  } catch (err) {
    next(err);
  }
});

/**
 * POST /api/patients/me
 * { firstName?, lastName?, dateOfBirth?, gender?, phone?, address?, emergencyContact?, clientRef? }
 *
 * Creates or updates the patient record of the signed-in account. A new patient account calls
 * this once so it has a record to book against. The server decides which row that is, so one
 * patient can never write over another's details.
 */
router.post('/patients/me', async (req, res, next) => {
  try {
    const user = req.user;
    if (user.role !== 'Patient') {
      return res.status(403).json({ error: 'Only a patient account has a patient record' });
    }

    const body = req.body || {};
    const names = String(body.firstName || user.full_name || 'Patient').trim().split(/\s+/);

    const existing = await db.one('SELECT * FROM patients WHERE user_id = $1', [user.user_id]);

    // An unlinked row seeded with the same e-mail is adopted rather than duplicated.
    const byEmail = existing
      ? null
      : await db.one('SELECT * FROM patients WHERE lower(email) = $1 AND user_id IS NULL', [
          user.email,
        ]);

    const target = existing || byEmail;

    if (target) {
      const updated = await db.one(
        `UPDATE patients SET
           user_id           = $2,
           first_name        = COALESCE($3, first_name),
           last_name         = COALESCE($4, last_name),
           date_of_birth     = COALESCE($5, date_of_birth),
           gender            = COALESCE($6, gender),
           phone             = COALESCE($7, phone),
           address           = COALESCE($8, address),
           emergency_contact = COALESCE($9, emergency_contact),
           client_ref        = COALESCE(client_ref, $10)
         WHERE patient_id = $1
         RETURNING *`,
        [
          target.patient_id,
          user.user_id,
          body.firstName || null,
          body.lastName || null,
          body.dateOfBirth || null,
          body.gender || null,
          body.phone || null,
          body.address || null,
          body.emergencyContact || null,
          body.clientRef ? String(body.clientRef) : null,
        ],
      );
      return res.json(updated);
    }

    const created = await db.one(
      `INSERT INTO patients
         (client_ref, user_id, first_name, last_name, date_of_birth, gender,
          phone, email, address, emergency_contact)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10)
       RETURNING *`,
      [
        body.clientRef ? String(body.clientRef) : null,
        user.user_id,
        body.firstName || names[0] || 'Patient',
        body.lastName || names.slice(1).join(' ') || '',
        body.dateOfBirth || '',
        body.gender || '',
        body.phone || user.phone || '',
        user.email,
        body.address || '',
        body.emergencyContact || '',
      ],
    );
    console.log(`[patients] record created for ${user.email}`);
    res.status(201).json(created);
  } catch (err) {
    next(err);
  }
});

module.exports = router;
