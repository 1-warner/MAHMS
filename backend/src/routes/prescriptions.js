'use strict';

// /api/prescriptions
//
// Reading: a patient sees only their own. Writing: doctors only, which is the rule the
// Firestore-only build could not enforce for certain.

const express = require('express');
const db = require('../db');
const refs = require('../refs');
const { isStaff, ownPatientRow, requireRole } = require('../auth');

const router = express.Router();

const SELECT = `
  SELECT rx.prescription_id,
         rx.client_ref,
         rx.appointment_id,
         rx.patient_id,
         p.client_ref AS patient_ref,
         rx.doctor_id,
         d.client_ref AS doctor_ref,
         d.first_name || ' ' || d.last_name AS doctor_name,
         rx.medication_name,
         rx.dosage,
         rx.frequency,
         rx.duration,
         to_char(rx.issue_date, 'YYYY-MM-DD') AS issue_date,
         rx.dispensed
  FROM prescriptions rx
  JOIN patients p ON p.patient_id = rx.patient_id
  JOIN doctors  d ON d.doctor_id  = rx.doctor_id`;

/** GET /api/prescriptions?patientId=&dispensed= */
router.get('/', async (req, res, next) => {
  try {
    const where = [];
    const params = [];

    if (!isStaff(req.user)) {
      const mine = await ownPatientRow(req.user);
      if (!mine) return res.json([]);
      params.push(mine.patient_id);
      where.push(`rx.patient_id = $${params.length}`);
    } else if (req.query.patientId) {
      const patient = await refs.findPatient(req.query.patientId);
      if (!patient) return res.status(404).json({ error: 'No such patient' });
      params.push(patient.patient_id);
      where.push(`rx.patient_id = $${params.length}`);
    }

    if (req.query.dispensed !== undefined) {
      params.push(req.query.dispensed === 'true');
      where.push(`rx.dispensed = $${params.length}`);
    }

    const sql = `${SELECT}
      ${where.length ? `WHERE ${where.join(' AND ')}` : ''}
      ORDER BY rx.issue_date DESC
      LIMIT 500`;
    res.json(await db.many(sql, params));
  } catch (err) {
    next(err);
  }
});

/**
 * POST /api/prescriptions  (Doctor only)
 * { patientId, medicationName, dosage, frequency, duration, appointmentId?, issueDate?, clientRef? }
 *
 * The prescribing doctor is taken from the signed-in account, not from the request body, so a
 * prescription can never be filed under someone else's name.
 */
router.post('/', requireRole('Doctor'), async (req, res, next) => {
  try {
    const body = req.body || {};
    if (!body.medicationName) {
      return res.status(400).json({ error: 'medicationName is required' });
    }

    const patient = await refs.findPatient(body.patientId);
    if (!patient) return res.status(400).json({ error: 'patientId is required and must exist' });

    const doctor = await db.one('SELECT * FROM doctors WHERE user_id = $1', [req.user.user_id]);
    if (!doctor) {
      return res.status(409).json({
        error: 'This doctor account is not linked to a doctor record',
        hint: 'An administrator must link the account before prescribing',
      });
    }

    let appointmentId = null;
    if (body.appointmentId) {
      const appointment = await refs.findAppointment(body.appointmentId);
      if (!appointment) return res.status(400).json({ error: 'No such appointment' });
      appointmentId = appointment.appointment_id;
    }

    const inserted = await db.one(
      `INSERT INTO prescriptions
         (client_ref, appointment_id, patient_id, doctor_id,
          medication_name, dosage, frequency, duration, issue_date, dispensed)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8, COALESCE($9::date, CURRENT_DATE), $10)
       ON CONFLICT (client_ref) DO UPDATE SET
         medication_name = EXCLUDED.medication_name,
         dosage          = EXCLUDED.dosage,
         frequency       = EXCLUDED.frequency,
         duration        = EXCLUDED.duration,
         dispensed       = EXCLUDED.dispensed
       RETURNING prescription_id`,
      [
        body.clientRef ? String(body.clientRef) : null,
        appointmentId,
        patient.patient_id,
        doctor.doctor_id,
        body.medicationName,
        body.dosage || '',
        body.frequency || '',
        body.duration || '',
        body.issueDate || null,
        body.dispensed === true,
      ],
    );

    const row = await db.one(`${SELECT} WHERE rx.prescription_id = $1`, [inserted.prescription_id]);
    console.log(`[prescriptions] ${req.user.email} issued ${row.prescription_id}`);
    res.status(201).json(row);
  } catch (err) {
    next(err);
  }
});

/** PATCH /api/prescriptions/:id  { dispensed } — the pharmacist marks medicine handed over. */
router.patch('/:id', requireRole('Pharmacist', 'Doctor', 'Admin'), async (req, res, next) => {
  try {
    const existing = await refs.findByIdOrRef('prescriptions', 'prescription_id', req.params.id);
    if (!existing) return res.status(404).json({ error: 'No such prescription' });

    if (req.body === undefined || req.body.dispensed === undefined) {
      return res.status(400).json({ error: 'dispensed (true or false) is required' });
    }

    await db.query('UPDATE prescriptions SET dispensed = $2 WHERE prescription_id = $1', [
      existing.prescription_id,
      req.body.dispensed === true,
    ]);
    res.json(await db.one(`${SELECT} WHERE rx.prescription_id = $1`, [existing.prescription_id]));
  } catch (err) {
    next(err);
  }
});

module.exports = router;
