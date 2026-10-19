'use strict';

// Helpers for turning an id in a request into a database row.
//
// Server ids are UUIDs, but the Android app knows its own Room row ids (plain numbers), which
// it sends in client_ref. Every lookup here accepts either form, so the app does not have to
// store a second set of ids to talk to the API.

const db = require('./db');

const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

function isUuid(value) {
  return typeof value === 'string' && UUID_RE.test(value.trim());
}

/**
 * Finds one row in [table] by its UUID primary key [pk], or by client_ref.
 * Returns null when nothing matches.
 */
async function findByIdOrRef(table, pk, value) {
  if (value === undefined || value === null || value === '') return null;
  const key = String(value).trim();
  if (isUuid(key)) {
    return db.one(`SELECT * FROM ${table} WHERE ${pk} = $1`, [key]);
  }
  return db.one(`SELECT * FROM ${table} WHERE client_ref = $1`, [key]);
}

const findPatient = (value) => findByIdOrRef('patients', 'patient_id', value);
const findDoctor = (value) => findByIdOrRef('doctors', 'doctor_id', value);
const findAppointment = (value) => findByIdOrRef('appointments', 'appointment_id', value);
const findPayment = (value) => findByIdOrRef('payments', 'payment_id', value);
const findEmergency = (value) => findByIdOrRef('emergencies', 'emergency_id', value);

module.exports = {
  isUuid,
  findByIdOrRef,
  findPatient,
  findDoctor,
  findAppointment,
  findPayment,
  findEmergency,
};
