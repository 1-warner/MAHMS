'use strict';

// /api/emergencies
//
// The SOS button posts here. An alert is accepted even when the phone had no GPS fix, because
// losing the alert would be worse than losing the position.

const express = require('express');
const db = require('../db');
const refs = require('../refs');
const { isStaff, ownPatientRow, requireRole } = require('../auth');

const router = express.Router();

const STATUSES = ['Active', 'In Progress', 'Resolved'];

const SELECT = `
  SELECT e.emergency_id,
         e.client_ref,
         e.patient_id,
         p.client_ref AS patient_ref,
         p.first_name || ' ' || p.last_name AS patient_name,
         p.phone      AS patient_phone,
         e.location,
         e.latitude,
         e.longitude,
         e.status,
         e.description,
         e.raised_at,
         e.resolved_at
  FROM emergencies e
  JOIN patients p ON p.patient_id = e.patient_id`;

/**
 * GET /api/emergencies?status=active
 *
 * The responder dashboard calls this. A patient sees only their own alerts.
 */
router.get('/', async (req, res, next) => {
  try {
    const where = [];
    const params = [];

    if (!isStaff(req.user)) {
      const mine = await ownPatientRow(req.user);
      if (!mine) return res.json([]);
      params.push(mine.patient_id);
      where.push(`e.patient_id = $${params.length}`);
    } else if (req.query.patientId) {
      const patient = await refs.findPatient(req.query.patientId);
      if (!patient) return res.status(404).json({ error: 'No such patient' });
      params.push(patient.patient_id);
      where.push(`e.patient_id = $${params.length}`);
    }

    if (req.query.status) {
      // "active" is accepted in any case so ?status=active reads naturally.
      const wanted = STATUSES.find(
        (s) => s.toLowerCase() === String(req.query.status).toLowerCase(),
      );
      if (!wanted) {
        return res.status(400).json({ error: `status must be one of ${STATUSES.join(', ')}` });
      }
      params.push(wanted);
      where.push(`e.status = $${params.length}`);
    }

    const sql = `${SELECT}
      ${where.length ? `WHERE ${where.join(' AND ')}` : ''}
      ORDER BY e.raised_at DESC
      LIMIT 500`;
    res.json(await db.many(sql, params));
  } catch (err) {
    next(err);
  }
});

/**
 * POST /api/emergencies
 * { patientId?, location?, latitude?, longitude?, description?, clientRef? }
 *
 * A patient raises the alert for themselves, so patientId is ignored for patient accounts.
 */
router.post('/', async (req, res, next) => {
  try {
    const body = req.body || {};

    let patient;
    if (isStaff(req.user)) {
      patient = await refs.findPatient(body.patientId);
      if (!patient) return res.status(400).json({ error: 'patientId is required and must exist' });
    } else {
      patient = await ownPatientRow(req.user);
      if (!patient) return res.status(409).json({ error: 'This account has no patient record yet' });
    }

    const latitude = body.latitude === undefined || body.latitude === null ? null : Number(body.latitude);
    const longitude = body.longitude === undefined || body.longitude === null ? null : Number(body.longitude);

    if (latitude !== null && (!Number.isFinite(latitude) || latitude < -90 || latitude > 90)) {
      return res.status(400).json({ error: 'latitude must be between -90 and 90' });
    }
    if (longitude !== null && (!Number.isFinite(longitude) || longitude < -180 || longitude > 180)) {
      return res.status(400).json({ error: 'longitude must be between -180 and 180' });
    }

    const inserted = await db.one(
      `INSERT INTO emergencies
         (client_ref, patient_id, location, latitude, longitude, status, description)
       VALUES ($1, $2, $3, $4, $5, 'Active', $6)
       ON CONFLICT (client_ref) DO UPDATE SET
         location    = EXCLUDED.location,
         latitude    = EXCLUDED.latitude,
         longitude   = EXCLUDED.longitude,
         description = EXCLUDED.description
       RETURNING emergency_id`,
      [
        body.clientRef ? String(body.clientRef) : null,
        patient.patient_id,
        body.location || 'Location unavailable',
        latitude,
        longitude,
        body.description || '',
      ],
    );

    const row = await db.one(`${SELECT} WHERE e.emergency_id = $1`, [inserted.emergency_id]);
    console.log(`[emergencies] SOS from ${row.patient_name} at ${row.location}`);
    res.status(201).json(row);
  } catch (err) {
    next(err);
  }
});

/**
 * PATCH /api/emergencies/:id  { status }
 * The responder moves an alert to In Progress, then Resolved.
 */
router.patch('/:id', requireRole('Responder', 'Nurse', 'Doctor', 'Admin'), async (req, res, next) => {
  try {
    const existing = await refs.findEmergency(req.params.id);
    if (!existing) return res.status(404).json({ error: 'No such emergency' });

    const status = req.body && req.body.status;
    if (!STATUSES.includes(status)) {
      return res.status(400).json({ error: `status must be one of ${STATUSES.join(', ')}` });
    }

    await db.query(
      `UPDATE emergencies SET
         status      = $2,
         resolved_at = CASE WHEN $2 = 'Resolved' THEN COALESCE(resolved_at, now()) ELSE NULL END
       WHERE emergency_id = $1`,
      [existing.emergency_id, status],
    );

    const row = await db.one(`${SELECT} WHERE e.emergency_id = $1`, [existing.emergency_id]);
    console.log(`[emergencies] ${req.user.email} set ${row.emergency_id} to ${status}`);
    res.json(row);
  } catch (err) {
    next(err);
  }
});

module.exports = router;
