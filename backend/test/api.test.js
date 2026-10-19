'use strict';

// API tests. Run with:  npm test
//
// They need a real PostgreSQL database, because the routes are mostly SQL and a mock would
// not prove much. DATABASE_URL must point at a throw-away database: the suite reloads the
// schema and the seed data before it starts, which drops every table first.
//
// MAHMS_AUTH_MODE=test lets the tests send "Bearer test:<email>" instead of a real Firebase
// token, so no service account is needed. See src/auth.js.

process.env.MAHMS_AUTH_MODE = 'test';
process.env.NODE_ENV = 'test';

const { test, before, after } = require('node:test');
const assert = require('node:assert');
const fs = require('node:fs');
const path = require('node:path');
const request = require('supertest');

if (!process.env.DATABASE_URL) {
  throw new Error('DATABASE_URL must be set to a throw-away database before running the tests');
}

const { createApp } = require('../src/app');
const db = require('../src/db');

const app = createApp();

// Sign-in helpers for the seeded demo accounts.
const asPatient = () => 'Bearer test:patient@mpilo.co.zw';
const asOtherPatient = () => 'Bearer test:naledi@mail.co.zw';
const asDoctor = () => 'Bearer test:doctor@mpilo.co.zw';
const asResponder = () => 'Bearer test:responder@mpilo.co.zw';
const asNewUser = () => 'Bearer test:brand.new@mail.co.zw';

/** Reloads schema + seed so each run starts from the same known data. */
async function resetDatabase() {
  const dir = path.join(__dirname, '..', 'db');
  for (const file of ['mahms_schema.sql', 'mahms_seed.sql']) {
    const sql = fs.readFileSync(path.join(dir, file), 'utf8');
    await db.pool.query(sql);
  }
}

before(async () => {
  await resetDatabase();
});

after(async () => {
  await db.pool.end();
});

// ----------------------------------------------------------------- health

test('GET /health reports the service and the database', async () => {
  const res = await request(app).get('/health').expect(200);
  assert.equal(res.body.status, 'ok');
  assert.equal(res.body.service, 'mahms-api');
  assert.equal(res.body.database, 'connected');
});

test('GET /health needs no token', async () => {
  await request(app).get('/health').expect(200);
});

// ----------------------------------------------------------------- authentication

test('a protected route refuses a request with no token', async () => {
  const res = await request(app).get('/api/me').expect(401);
  assert.match(res.body.error, /Authorization/);
});

test('a protected route refuses an empty stub token', async () => {
  const res = await request(app).get('/api/me').set('Authorization', 'Bearer test:').expect(401);
  assert.match(res.body.error, /test token/);
});

test('a token that is not a stub is sent to Firebase for verification', async () => {
  // The tests run without a service account, so verification cannot happen and the server
  // says so with 503 instead of letting the request through. On a deployed server, where
  // credentials are configured, the same request would be rejected with 401.
  const res = await request(app)
    .get('/api/me')
    .set('Authorization', 'Bearer not-a-real-token')
    .expect(503);
  assert.match(res.body.error, /not configured/);
});

test('GET /api/me returns the role and the linked patient record', async () => {
  const res = await request(app).get('/api/me').set('Authorization', asPatient()).expect(200);
  assert.equal(res.body.email, 'patient@mpilo.co.zw');
  assert.equal(res.body.role, 'Patient');
  assert.ok(res.body.patient, 'the demo patient account should be linked to a patient record');
  assert.equal(res.body.patient.client_ref, '1');
  assert.equal(res.body.doctor, null);
});

test('GET /api/me returns the linked doctor record for a doctor account', async () => {
  const res = await request(app).get('/api/me').set('Authorization', asDoctor()).expect(200);
  assert.equal(res.body.role, 'Doctor');
  assert.ok(res.body.doctor);
  assert.equal(res.body.doctor.specialization, 'Cardiologist');
});

test('an unknown e-mail becomes a Patient, never a staff role', async () => {
  const res = await request(app).get('/api/me').set('Authorization', asNewUser()).expect(200);
  assert.equal(res.body.role, 'Patient');
  assert.equal(res.body.patient, null, 'a brand new account has no patient record yet');
});

// ----------------------------------------------------------------- reference data

test('GET /api/doctors lists the hospital doctors', async () => {
  const res = await request(app).get('/api/doctors').set('Authorization', asPatient()).expect(200);
  assert.equal(res.body.length, 10);
  assert.ok(res.body[0].full_name);
});

test('GET /api/doctors?department= filters by department', async () => {
  const res = await request(app)
    .get('/api/doctors?department=Cardiology')
    .set('Authorization', asPatient())
    .expect(200);
  assert.equal(res.body.length, 1);
  assert.equal(res.body[0].specialization, 'Cardiologist');
});

test('GET /api/departments lists the departments', async () => {
  const res = await request(app).get('/api/departments').set('Authorization', asPatient()).expect(200);
  assert.equal(res.body.length, 10);
});

// ----------------------------------------------------------------- appointments

test('a patient sees only their own appointments', async () => {
  const res = await request(app)
    .get('/api/appointments')
    .set('Authorization', asPatient())
    .expect(200);
  assert.ok(res.body.length > 0);
  const refs = new Set(res.body.map((a) => a.patient_ref));
  assert.deepEqual([...refs], ['1'], 'every row should belong to patient 1');
});

test('a patient cannot read another patient by passing patientId', async () => {
  const res = await request(app)
    .get('/api/appointments?patientId=2')
    .set('Authorization', asPatient())
    .expect(200);
  const refs = new Set(res.body.map((a) => a.patient_ref));
  assert.deepEqual([...refs], ['1'], 'the patientId in the query must be ignored for a patient');
});

test('staff can read a named patient', async () => {
  const res = await request(app)
    .get('/api/appointments?patientId=2')
    .set('Authorization', asDoctor())
    .expect(200);
  assert.ok(res.body.length > 0);
  for (const row of res.body) assert.equal(row.patient_ref, '2');
});

test('POST /api/appointments books a visit for the signed-in patient', async () => {
  const res = await request(app)
    .post('/api/appointments')
    .set('Authorization', asPatient())
    .send({
      doctorId: '2',
      appointmentDate: '2026-12-01',
      appointmentTime: '10:15',
      reason: 'Booked from the API test',
      clientRef: '9001',
    })
    .expect(201);

  assert.equal(res.body.patient_ref, '1');
  assert.equal(res.body.doctor_ref, '2');
  assert.equal(res.body.status, 'Pending');
  assert.equal(res.body.appointment_date, '2026-12-01');

  // The row really is in the database.
  const row = await db.one('SELECT * FROM appointments WHERE client_ref = $1', ['9001']);
  assert.ok(row);
});

test('booking the same clientRef twice updates one row instead of making two', async () => {
  const body = {
    doctorId: '2',
    appointmentDate: '2026-12-02',
    appointmentTime: '11:00',
    reason: 'Retry test',
    clientRef: '9002',
  };
  await request(app).post('/api/appointments').set('Authorization', asPatient()).send(body).expect(201);
  await request(app)
    .post('/api/appointments')
    .set('Authorization', asPatient())
    .send({ ...body, appointmentTime: '12:00' })
    .expect(201);

  const rows = await db.many('SELECT * FROM appointments WHERE client_ref = $1', ['9002']);
  assert.equal(rows.length, 1);
  assert.equal(rows[0].appointment_time, '12:00');
});

test('POST /api/appointments rejects a bad date and a missing doctor', async () => {
  await request(app)
    .post('/api/appointments')
    .set('Authorization', asPatient())
    .send({ doctorId: '2', appointmentDate: '01-12-2026', appointmentTime: '10:00' })
    .expect(400);

  await request(app)
    .post('/api/appointments')
    .set('Authorization', asPatient())
    .send({ doctorId: '999', appointmentDate: '2026-12-03', appointmentTime: '10:00' })
    .expect(400);
});

test('a patient may cancel their own appointment but not confirm it', async () => {
  await request(app)
    .patch('/api/appointments/9001')
    .set('Authorization', asPatient())
    .send({ status: 'Confirmed' })
    .expect(403);

  const res = await request(app)
    .patch('/api/appointments/9001')
    .set('Authorization', asPatient())
    .send({ status: 'Cancelled' })
    .expect(200);
  assert.equal(res.body.status, 'Cancelled');
});

test('a patient cannot change another patient\'s appointment', async () => {
  const theirs = await db.one("SELECT client_ref FROM appointments WHERE client_ref = '3'");
  await request(app)
    .patch(`/api/appointments/${theirs.client_ref}`)
    .set('Authorization', asPatient())
    .send({ status: 'Cancelled' })
    .expect(403);
});

test('a doctor may confirm an appointment', async () => {
  const res = await request(app)
    .patch('/api/appointments/2')
    .set('Authorization', asDoctor())
    .send({ status: 'Confirmed' })
    .expect(200);
  assert.equal(res.body.status, 'Confirmed');
});

// ----------------------------------------------------------------- prescriptions

test('a patient sees only their own prescriptions', async () => {
  const res = await request(app)
    .get('/api/prescriptions')
    .set('Authorization', asPatient())
    .expect(200);
  assert.ok(res.body.length > 0);
  for (const row of res.body) assert.equal(row.patient_ref, '1');
});

test('a patient may not write a prescription', async () => {
  const res = await request(app)
    .post('/api/prescriptions')
    .set('Authorization', asPatient())
    .send({ patientId: '1', medicationName: 'Morphine' })
    .expect(403);
  assert.match(res.body.error, /Doctor/);
});

test('a doctor may write a prescription and it is filed under that doctor', async () => {
  const res = await request(app)
    .post('/api/prescriptions')
    .set('Authorization', asDoctor())
    .send({
      patientId: '1',
      medicationName: 'Simvastatin',
      dosage: '20 mg',
      frequency: 'Once daily',
      duration: '30 days',
      clientRef: '9101',
    })
    .expect(201);

  assert.equal(res.body.medication_name, 'Simvastatin');
  assert.equal(res.body.patient_ref, '1');
  // doctor 1 is the account doctor@mpilo.co.zw, taken from the token, not the request body.
  assert.equal(res.body.doctor_ref, '1');
});

test('a pharmacist may mark a prescription dispensed', async () => {
  const res = await request(app)
    .patch('/api/prescriptions/9101')
    .set('Authorization', 'Bearer test:pharmacist@mpilo.co.zw')
    .send({ dispensed: true })
    .expect(200);
  assert.equal(res.body.dispensed, true);
});

// ----------------------------------------------------------------- payments and receipts

test('POST /api/payments records the payment and issues a receipt number', async () => {
  const res = await request(app)
    .post('/api/payments')
    .set('Authorization', asPatient())
    .send({ appointmentId: '1', amount: 20, paymentMethod: 'EcoCash', clientRef: '9201' })
    .expect(201);

  assert.equal(res.body.amount, 20);
  assert.equal(res.body.status, 'Completed');
  assert.match(res.body.receipt_no, /^RCP-\d{4}-\d{5}$/);
});

test('GET /api/receipts/:paymentId returns the receipt for the payer', async () => {
  const res = await request(app)
    .get('/api/receipts/9201')
    .set('Authorization', asPatient())
    .expect(200);
  assert.match(res.body.receiptNo, /^RCP-\d{4}-\d{5}$/);
  assert.equal(res.body.hospital, 'Mpilo Central Hospital');
  assert.equal(res.body.amount, 20);
});

test('another patient cannot read that receipt', async () => {
  await request(app).get('/api/receipts/9201').set('Authorization', asOtherPatient()).expect(403);
});

test('a pending payment gets no receipt', async () => {
  const created = await request(app)
    .post('/api/payments')
    .set('Authorization', asPatient())
    .send({
      appointmentId: '1',
      amount: 20,
      paymentMethod: 'Cash',
      status: 'Pending',
      clientRef: '9202',
    })
    .expect(201);
  assert.equal(created.body.receipt_no, null);

  await request(app).get('/api/receipts/9202').set('Authorization', asPatient()).expect(404);
});

test('POST /api/payments rejects a bad method and a negative amount', async () => {
  await request(app)
    .post('/api/payments')
    .set('Authorization', asPatient())
    .send({ appointmentId: '1', amount: 20, paymentMethod: 'Bitcoin' })
    .expect(400);

  await request(app)
    .post('/api/payments')
    .set('Authorization', asPatient())
    .send({ appointmentId: '1', amount: -5, paymentMethod: 'Cash' })
    .expect(400);
});

test('a patient cannot pay for someone else\'s appointment', async () => {
  await request(app)
    .post('/api/payments')
    .set('Authorization', asPatient())
    .send({ appointmentId: '3', amount: 30, paymentMethod: 'Cash' })
    .expect(403);
});

test('receipt numbers are unique across many payments', async () => {
  const numbers = new Set();
  for (let i = 0; i < 5; i += 1) {
    const res = await request(app)
      .post('/api/payments')
      .set('Authorization', asPatient())
      .send({ appointmentId: '1', amount: 10, paymentMethod: 'Cash', clientRef: `9300${i}` })
      .expect(201);
    numbers.add(res.body.receipt_no);
  }
  assert.equal(numbers.size, 5, 'every payment should get its own receipt number');
});

// ----------------------------------------------------------------- emergencies (SOS)

test('POST /api/emergencies accepts an SOS with a GPS position', async () => {
  const res = await request(app)
    .post('/api/emergencies')
    .set('Authorization', asPatient())
    .send({
      location: '14 Fife St, Bulawayo (-20.15280, 28.58190)',
      latitude: -20.1528,
      longitude: 28.5819,
      description: 'Chest pain',
      clientRef: '9401',
    })
    .expect(201);

  assert.equal(res.body.status, 'Active');
  assert.equal(res.body.patient_ref, '1');
  assert.equal(res.body.latitude, -20.1528);
  assert.ok(res.body.patient_phone, 'the responder needs the patient phone number');
});

test('an SOS is still accepted when the phone had no GPS fix', async () => {
  const res = await request(app)
    .post('/api/emergencies')
    .set('Authorization', asPatient())
    .send({ description: 'Permission refused on the phone', clientRef: '9402' })
    .expect(201);
  assert.equal(res.body.latitude, null);
  assert.equal(res.body.location, 'Location unavailable');
});

test('POST /api/emergencies rejects an impossible latitude', async () => {
  await request(app)
    .post('/api/emergencies')
    .set('Authorization', asPatient())
    .send({ latitude: 999, longitude: 0 })
    .expect(400);
});

test('GET /api/emergencies?status=active is what the responder dashboard reads', async () => {
  const res = await request(app)
    .get('/api/emergencies?status=active')
    .set('Authorization', asResponder())
    .expect(200);
  assert.ok(res.body.length > 0);
  for (const row of res.body) assert.equal(row.status, 'Active');
});

test('a responder may resolve an alert, a patient may not', async () => {
  await request(app)
    .patch('/api/emergencies/9401')
    .set('Authorization', asPatient())
    .send({ status: 'Resolved' })
    .expect(403);

  const res = await request(app)
    .patch('/api/emergencies/9401')
    .set('Authorization', asResponder())
    .send({ status: 'Resolved' })
    .expect(200);
  assert.equal(res.body.status, 'Resolved');
  assert.ok(res.body.resolved_at, 'resolving should stamp the time');
});

test('PATCH /api/emergencies rejects an unknown status', async () => {
  await request(app)
    .patch('/api/emergencies/9402')
    .set('Authorization', asResponder())
    .send({ status: 'Maybe' })
    .expect(400);
});

// ----------------------------------------------------------------- patient record

test('a new account can create its own patient record and then book', async () => {
  const created = await request(app)
    .post('/api/patients/me')
    .set('Authorization', asNewUser())
    .send({ firstName: 'Brand', lastName: 'New', phone: '+263770000000', clientRef: '9501' })
    .expect(201);
  assert.equal(created.body.first_name, 'Brand');

  const booked = await request(app)
    .post('/api/appointments')
    .set('Authorization', asNewUser())
    .send({ doctorId: '2', appointmentDate: '2026-12-10', appointmentTime: '08:00' })
    .expect(201);
  assert.equal(booked.body.patient_ref, '9501');
});

test('calling POST /api/patients/me twice does not create a second record', async () => {
  await request(app)
    .post('/api/patients/me')
    .set('Authorization', asNewUser())
    .send({ address: '1 Test Road' })
    .expect(200);

  const rows = await db.many('SELECT * FROM patients WHERE client_ref = $1', ['9501']);
  assert.equal(rows.length, 1);
  assert.equal(rows[0].address, '1 Test Road');
});

// ----------------------------------------------------------------- not found

test('an unknown route answers 404 with a message', async () => {
  const res = await request(app).get('/api/nothing-here').set('Authorization', asPatient()).expect(404);
  assert.match(res.body.error, /No route/);
});
