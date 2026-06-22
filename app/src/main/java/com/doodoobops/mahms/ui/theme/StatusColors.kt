package com.doodoobops.mahms.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Colour-coded status chip palette (background to foreground):
 *  Pending=Warning, Confirmed=Primary, Completed=Success, Cancelled=Neutral,
 *  Active=Danger, In Progress=Warning, Resolved=Success, Failed=Danger.
 */
fun statusColors(status: String): Pair<Color, Color> = when (status.trim().lowercase()) {
    "pending" -> Warning50 to Warning700
    "confirmed" -> MpiloBlue50 to MpiloBlue600
    "completed" -> Success50 to Success700
    "cancelled", "canceled" -> Neutral100 to Neutral600
    "active" -> Emergency50 to Emergency700
    "in progress" -> Warning50 to Warning700
    "resolved" -> Success50 to Success700
    "failed" -> Emergency50 to Emergency700
    "dispensed" -> Success50 to Success700
    else -> Neutral100 to Neutral600
}
