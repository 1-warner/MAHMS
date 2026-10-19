'use strict';

// /api/payments and /api/receipts
//
// The gateway is still simulated (no money moves), but the record and the receipt number are
// real and are allocated here rather than on the phone. A database sequence issues the
// number, so two phones paying at the same moment cannot be given the same receipt number.

const express = require('express');
const db = require('../db');
const refs = require('../refs');
const { isStaff, ownPatientRow } = require('../auth');

const router = express.Router();

const METHODS = ['EcoCash', 'Card', 'Cash'];
const STATUSES = ['Pending', 'Completed', 'Failed'];

const SELECT = `
  SELECT pay.payment_id,
         pay.client_ref,
         pay.appointment_id,
         a.client_ref  AS appointment_ref,
         a.patient_id,
         p.client_ref  AS patient_ref,
         p.first_name || ' ' || p.last_name AS patient_name,
         pay.amount::float8 AS amount,
         pay.payment_method,
         to_char(pay.payment_date, 'YYYY-MM-DD') AS payment_date,
         pay.status,
         r.receipt_no,
         r.issued_at
  FROM payments pay
  JOIN appointments a ON a.appointment_id = pay.appointment_id
  JOIN patients     p ON p.patient_id     = a.patient_id
  LEFT JOIN receipts r ON r.payment_id    = pay.payment_id`;

/** GET /api/payments?patientId=&status= */
router.get('/payments', async (req, res, next) => {
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

    if (req.query.status) {
      params.push(req.query.status);
      where.push(`pay.status = $${params.length}`);
    }

    const sql = `${SELECT}
      ${where.length ? `WHERE ${where.join(' AND ')}` : ''}
      ORDER BY pay.payment_date DESC, pay.created_at DESC
      LIMIT 500`;
    res.json(await db.many(sql, params));
  } catch (err) {
    next(err);
  }
});

/**
 * POST /api/payments
 * { appointmentId, amount, paymentMethod, status?, paymentDate?, clientRef? }
 *
 * A completed payment also gets a receipt, in the same transaction, so a payment can never
 * end up recorded without its receipt.
 */
router.post('/payments', async (req, res, next) => {
  try {
    const body = req.body || {};
    const status = body.status || 'Completed';
    const amount = Number(body.amount);

    if (!Number.isFinite(amount) || amount < 0) {
      return res.status(400).json({ error: 'amount must be a number of 0 or more' });
    }
    if (!METHODS.includes(body.paymentMethod)) {
      return res.status(400).json({ error: `paymentMethod must be one of ${METHODS.join(', ')}` });
    }
    if (!STATUSES.includes(status)) {
      return res.status(400).json({ error: `status must be one of ${STATUSES.join(', ')}` });
    }

    const appointment = await refs.findAppointment(body.appointmentId);
    if (!appointment) {
      return res.status(400).json({ error: 'appointmentId is required and must exist' });
    }

    // A patient may only pay for their own visit.
    if (!isStaff(req.user)) {
      const mine = await ownPatientRow(req.user);
      if (!mine || mine.patient_id !== appointment.patient_id) {
        return res.status(403).json({ error: 'This is not your appointment' });
      }
    }

    const paymentId = await db.transaction(async (client) => {
      const { rows } = await client.query(
        `INSERT INTO payments
           (client_ref, appointment_id, amount, payment_method, payment_date, status)
         VALUES ($1, $2, $3, $4, COALESCE($5::date, CURRENT_DATE), $6)
         ON CONFLICT (client_ref) DO UPDATE SET
           amount         = EXCLUDED.amount,
           payment_method = EXCLUDED.payment_method,
           status         = EXCLUDED.status
         RETURNING payment_id, to_char(payment_date, 'YYYY') AS year`,
        [
          body.clientRef ? String(body.clientRef) : null,
          appointment.appointment_id,
          amount,
          body.paymentMethod,
          body.paymentDate || null,
          status,
        ],
      );
      const payment = rows[0];

      if (status === 'Completed') {
        // One receipt per payment. A retried request keeps the number already issued.
        await client.query(
          `INSERT INTO receipts (payment_id, receipt_no)
           VALUES ($1, 'RCP-' || $2 || '-' || lpad(nextval('receipt_serial')::text, 5, '0'))
           ON CONFLICT (payment_id) DO NOTHING`,
          [payment.payment_id, payment.year],
        );
      }
      return payment.payment_id;
    });

    const row = await db.one(`${SELECT} WHERE pay.payment_id = $1`, [paymentId]);
    console.log(`[payments] ${req.user.email} paid ${row.amount} (${row.receipt_no || 'no receipt'})`);
    res.status(201).json(row);
  } catch (err) {
    next(err);
  }
});

/** GET /api/receipts/:paymentId — the receipt for one payment. */
router.get('/receipts/:paymentId', async (req, res, next) => {
  try {
    const payment = await refs.findPayment(req.params.paymentId);
    if (!payment) return res.status(404).json({ error: 'No such payment' });

    const row = await db.one(`${SELECT} WHERE pay.payment_id = $1`, [payment.payment_id]);

    if (!isStaff(req.user)) {
      const mine = await ownPatientRow(req.user);
      if (!mine || mine.patient_id !== row.patient_id) {
        return res.status(403).json({ error: 'This is not your receipt' });
      }
    }
    if (!row.receipt_no) {
      return res.status(404).json({
        error: 'No receipt for this payment',
        hint: 'A receipt is issued only once the payment is Completed',
      });
    }

    res.json({
      receiptNo: row.receipt_no,
      issuedAt: row.issued_at,
      paymentId: row.payment_id,
      appointmentId: row.appointment_id,
      patientName: row.patient_name,
      amount: row.amount,
      paymentMethod: row.payment_method,
      paymentDate: row.payment_date,
      status: row.status,
      hospital: 'Mpilo Central Hospital',
    });
  } catch (err) {
    next(err);
  }
});

module.exports = router;
