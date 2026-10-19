'use strict';

// /api/appointments
//
// A patient sees and books only their own appointments. Staff see any patient's.

const express = require('express');
const db = require('../db');
const refs = require('../refs');
const { isStaff, ownPatientRow } = require('../auth');

const router = express.Router();

const STATUSES = ['Pending', 'Confirmed', 'Completed', 'Cancelled'];

// Shape sent back to the clients. client_ref is echoed so the app can match the row to its
// own Room record.
const SELECT = `
  SELECT a.appointment_id,
         a.client_ref,
         a.patient_id,
         p.client_ref        AS patient_ref,
         a.doctor_id,
         d.client_ref        AS doctor_ref,
         d.first_name || ' ' || d.last_name AS doctor_name,
         d.specialization,
         d.department,
         to_char(a.appointment_date, 'YYYY-MM-DD') AS appointment_date,
         a.appointment_time,
         a.status,
         a.reason,
         a.created_at
  FROM appointments a
  JOIN patients p ON p.patient_id = a.patient_id
  JOIN doctors  d ON d.doctor_id  = a.doctor_id`;

/**
 * GET /api/appointments?patientId=&doctorId=&date=&status=
 *
 * patientId and doctorId accept a UUID or the app's client_ref. A Patient is always limited
 * to their own record whatever they ask for.
 */
router.get('/', async (req, res, next) => {
  try {
    const where = [];
    const params = [];

    if (!isStaff(req.user)) {
      const mine = await ownPatientRow(req.user);
      if (!mine) return res.json([]);
      params.push(mine.patient_id);
      where.push(`a.patient_id = $${params.length}`);
    } else if (req.query.patientId) {
      const patient = await refs.findPatient(req.query.patientId);
      if (!patient) return res.status(404).json({ error: 'No such patient' });
      params.push(patient.patient_id);
      where.push(`a.patient_id = $${params.length}`);
    }

    if (req.query.doctorId) {
      const doctor = await refs.findDoctor(req.query.doctorId);
      if (!doctor) return res.status(404).json({ error: 'No such doctor' });
      params.push(doctor.doctor_id);
      where.push(`a.doctor_id = $${params.length}`);
    }
    if (req.query.date) {
      params.push(req.query.date);
      where.push(`a.appointment_date = $${params.length}::date`);
    }
    if (req.query.status) {
      params.push(req.query.status);
      where.push(`a.status = $${params.length}`);
    }

    const sql = `${SELECT}
      ${where.length ? `WHERE ${where.join(' AND ')}` : ''}
      ORDER BY a.appointment_date DESC, a.appointment_time DESC
      LIMIT 500`;

    res.json(await db.many(sql, params));
  } catch (err) {
    next(err);
  }
});

/**
 * POST /api/appointments
 * { patientId, doctorId, appointmentDate, appointmentTime, reason, status?, clientRef? }
 *
 * Booking the same clientRef twice updates the first row instead of creating a duplicate, so
 * the app can safely retry after a dropped connection.
 */
router.post('/', async (req, res, next) => {
  try {
    const body = req.body || {};
    const { appointmentDate, appointmentTime, reason, clientRef } = body;
    const status = body.status || 'Pending';

    if (!appointmentDate || !appointmentTime) {
      return res.status(400).json({ error: 'appointmentDate and appointmentTime are required' });
    }
    if (!/^\d{4}-\d{2}-\d{2}$/.test(String(appointmentDate))) {
      return res.status(400).json({ error: 'appointmentDate must be yyyy-MM-dd' });
    }
    if (!STATUSES.includes(status)) {
      return res.status(400).json({ error: `status must be one of ${STATUSES.join(', ')}` });
    }

    // A patient may only book for themselves; staff book for the patient they name.
    let patient;
    if (isStaff(req.user)) {
      patient = await refs.findPatient(body.patientId);
      if (!patient) return res.status(400).json({ error: 'patientId is required and must exist' });
    } else {
      patient = await ownPatientRow(req.user);
      if (!patient) {
        return res.status(409).json({
          error: 'This account has no patient record yet',
          hint: 'POST /api/patients/me first, or ask reception to create the record',
        });
      }
    }

    const doctor = await refs.findDoctor(body.doctorId);
    if (!doctor) return res.status(400).json({ error: 'doctorId is required and must exist' });

    const inserted = await db.one(
      `INSERT INTO appointments
         (client_ref, patient_id, doctor_id, appointment_date, appointment_time, status, reason)
       VALUES ($1, $2, $3, $4::date, $5, $6, $7)
       ON CONFLICT (client_ref) DO UPDATE SET
         appointment_date = EXCLUDED.appointment_date,
         appointment_time = EXCLUDED.appointment_time,
         status           = EXCLUDED.status,
         reason           = EXCLUDED.reason
       RETURNING appointment_id`,
      [
        clientRef ? String(clientRef) : null,
        patient.patient_id,
        doctor.doctor_id,
        appointmentDate,
        String(appointmentTime),
        status,
        reason || '',
      ],
    );

    const row = await db.one(`${SELECT} WHERE a.appointment_id = $1`, [inserted.appointment_id]);
    console.log(`[appointments] ${req.user.email} booked ${row.appointment_id}`);
    res.status(201).json(row);
  } catch (err) {
    next(err);
  }
});

/**
 * PATCH /api/appointments/:id
 * { status?, appointmentDate?, appointmentTime?, reason? }
 *
 * A patient may only cancel their own appointment. Staff may set any status.
 */
router.patch('/:id', async (req, res, next) => {
  try {
    const existing = await refs.findAppointment(req.params.id);
    if (!existing) return res.status(404).json({ error: 'No such appointment' });

    if (!isStaff(req.user)) {
      const mine = await ownPatientRow(req.user);
      if (!mine || mine.patient_id !== existing.patient_id) {
        return res.status(403).json({ error: 'This is not your appointment' });
      }
      if (req.body && req.body.status && req.body.status !== 'Cancelled') {
        return res.status(403).json({ error: 'A patient may only cancel an appointment' });
      }
    }

    const body = req.body || {};
    if (body.status && !STATUSES.includes(body.status)) {
      return res.status(400).json({ error: `status must be one of ${STATUSES.join(', ')}` });
    }
    if (body.appointmentDate && !/^\d{4}-\d{2}-\d{2}$/.test(String(body.appointmentDate))) {
      return res.status(400).json({ error: 'appointmentDate must be yyyy-MM-dd' });
    }

    const updated = await db.one(
      `UPDATE appointments SET
         status           = COALESCE($2, status),
         appointment_date = COALESCE($3::date, appointment_date),
         appointment_time = COALESCE($4, appointment_time),
         reason           = COALESCE($5, reason)
       WHERE appointment_id = $1
       RETURNING appointment_id`,
      [
        existing.appointment_id,
        body.status || null,
        body.appointmentDate || null,
        body.appointmentTime || null,
        body.reason === undefined ? null : body.reason,
      ],
    );

    res.json(await db.one(`${SELECT} WHERE a.appointment_id = $1`, [updated.appointment_id]));
  } catch (err) {
    next(err);
  }
});

module.exports = router;
