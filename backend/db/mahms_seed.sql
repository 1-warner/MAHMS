-- MAHMS API seed data (PostgreSQL). Run after mahms_schema.sql.
--
-- The rows mirror the Android app's DatabaseSeeder so the same demo data appears in the
-- app and in the API database. client_ref holds the Room row id the app uses for the same
-- record (patients and doctors 1..10), which is what lets the two line up in the demo.
--
-- Every table gets at least ten rows, as the brief asks.
-- Foreign keys are resolved by sub-query on a natural key, so this file can be re-run after
-- the schema without knowing any generated UUID.

-- ---------------------------------------------------------------- departments (10)
INSERT INTO departments (name, description) VALUES
    ('Cardiology',    'Heart and circulatory care'),
    ('Outpatient',    'General consultations and walk-in clinic'),
    ('Maternity',     'Antenatal, delivery and postnatal care'),
    ('Paediatrics',   'Care for children under 13'),
    ('Orthopaedics',  'Bones, joints and trauma'),
    ('Dermatology',   'Skin conditions'),
    ('Neurology',     'Brain and nervous system'),
    ('Mental Health', 'Psychiatry and counselling'),
    ('Radiology',     'Imaging and diagnostics'),
    ('Pharmacy',      'Dispensary and medicine stock');

-- ---------------------------------------------------------------- users (10)
-- No passwords here: Firebase Authentication holds the credential. These rows map a
-- verified e-mail onto a role. firebase_uid is filled in the first time the account calls
-- the API with a valid token.
INSERT INTO users (email, full_name, phone, role) VALUES
    ('patient@mpilo.co.zw',     'Thabo Moyo',        '+263772345670', 'Patient'),
    ('doctor@mpilo.co.zw',      'Dr Sibusiso Moyo',  '+263772345671', 'Doctor'),
    ('nurse@mpilo.co.zw',       'Sr Lindiwe Ndlovu', '+263772345672', 'Nurse'),
    ('admin@mpilo.co.zw',       'Admin Office',      '+263772345673', 'Admin'),
    ('pharmacist@mpilo.co.zw',  'Pharmacy 03',       '+263772345674', 'Pharmacist'),
    ('responder@mpilo.co.zw',   'Casualty Desk',     '+263772345675', 'Responder'),
    ('naledi@mail.co.zw',       'Naledi Ncube',      '+263772345676', 'Patient'),
    ('tsibanda@mpilo.co.zw',    'Dr Tariro Sibanda', '+263772345677', 'Doctor'),
    ('gmutasa@mpilo.co.zw',     'Sr Grace Mutasa',   '+263772345678', 'Nurse'),
    ('pharmacist2@mpilo.co.zw', 'Pharmacy 07',       '+263772345679', 'Pharmacist');

-- ---------------------------------------------------------------- doctors (10)
INSERT INTO doctors (client_ref, department_id, first_name, last_name, specialization, department, phone, email) VALUES
    ('1',  (SELECT department_id FROM departments WHERE name = 'Cardiology'),    'Sibusiso', 'Moyo',    'Cardiologist',          'Cardiology',    '+263772000001', 'smoyo@mpilo.co.zw'),
    ('2',  (SELECT department_id FROM departments WHERE name = 'Outpatient'),    'Tariro',   'Sibanda', 'General Practitioner',  'Outpatient',    '+263772000002', 'tsibanda@mpilo.co.zw'),
    ('3',  (SELECT department_id FROM departments WHERE name = 'Maternity'),     'Anesu',    'Ndlovu',  'Obstetrician',          'Maternity',     '+263772000003', 'andlovu@mpilo.co.zw'),
    ('4',  (SELECT department_id FROM departments WHERE name = 'Paediatrics'),   'Farai',    'Chuma',   'Paediatrician',         'Paediatrics',   '+263772000004', 'fchuma@mpilo.co.zw'),
    ('5',  (SELECT department_id FROM departments WHERE name = 'Orthopaedics'),  'Nomsa',    'Khanye',  'Orthopaedic Surgeon',   'Orthopaedics',  '+263772000005', 'nkhanye@mpilo.co.zw'),
    ('6',  (SELECT department_id FROM departments WHERE name = 'Dermatology'),   'Tapiwa',   'Mhlanga', 'Dermatologist',         'Dermatology',   '+263772000006', 'tmhlanga@mpilo.co.zw'),
    ('7',  (SELECT department_id FROM departments WHERE name = 'Neurology'),     'Lerato',   'Mathe',   'Neurologist',           'Neurology',     '+263772000007', 'lmathe@mpilo.co.zw'),
    ('8',  (SELECT department_id FROM departments WHERE name = 'Outpatient'),    'Kudzai',   'Marufu',  'General Practitioner',  'Outpatient',    '+263772000008', 'kmarufu@mpilo.co.zw'),
    ('9',  (SELECT department_id FROM departments WHERE name = 'Mental Health'), 'Zodwa',    'Tshuma',  'Psychiatrist',          'Mental Health', '+263772000009', 'ztshuma@mpilo.co.zw'),
    ('10', (SELECT department_id FROM departments WHERE name = 'Radiology'),     'Blessing', 'Gumbo',   'Radiologist',           'Radiology',     '+263772000010', 'bgumbo@mpilo.co.zw');

-- Link the two doctor accounts to their doctor record.
UPDATE doctors SET user_id = (SELECT user_id FROM users WHERE email = 'doctor@mpilo.co.zw')   WHERE client_ref = '1';
UPDATE doctors SET user_id = (SELECT user_id FROM users WHERE email = 'tsibanda@mpilo.co.zw') WHERE client_ref = '2';

-- ---------------------------------------------------------------- patients (10)
INSERT INTO patients (client_ref, first_name, last_name, date_of_birth, gender, phone, email, address, emergency_contact) VALUES
    ('1',  'Thabo',    'Moyo',    '1987-03-12', 'Male',   '+263772345678', 'thabo@mail.co.zw',    '14 Fife St, Bulawayo',            '+263772999111'),
    ('2',  'Naledi',   'Ncube',   '1995-07-22', 'Female', '+263773111222', 'naledi@mail.co.zw',   '8 Josiah Tongogara, Bulawayo',    '+263772999222'),
    ('3',  'Tendai',   'Dube',    '1979-11-02', 'Male',   '+263774222333', 'tendai@mail.co.zw',   '22 Leopold Takawira, Bulawayo',   '+263772999333'),
    ('4',  'Sipho',    'Sibanda', '2001-01-30', 'Male',   '+263775333444', 'sipho@mail.co.zw',    '5 Robert Mugabe Way, Bulawayo',   '+263772999444'),
    ('5',  'Rutendo',  'Khumalo', '1990-09-15', 'Female', '+263776444555', 'rutendo@mail.co.zw',  '31 George Silundika, Bulawayo',   '+263772999555'),
    ('6',  'Themba',   'Nkomo',   '1983-05-19', 'Male',   '+263777555666', 'themba@mail.co.zw',   '17 Jason Moyo St, Bulawayo',      '+263772999666'),
    ('7',  'Chipo',    'Mutasa',  '1998-12-08', 'Female', '+263778666777', 'chipo@mail.co.zw',    '3 Main St, Bulawayo',             '+263772999777'),
    ('8',  'Mandla',   'Ncube',   '1965-02-27', 'Male',   '+263779777888', 'mandla@mail.co.zw',   '40 Lobengula St, Bulawayo',       '+263772999888'),
    ('9',  'Precious', 'Ndebele', '2010-06-11', 'Female', '+263771888999', 'precious@mail.co.zw', '9 Fort St, Bulawayo',             '+263772999999'),
    ('10', 'Joseph',   'Mpofu',   '1972-08-04', 'Male',   '+263772999000', 'joseph@mail.co.zw',   '26 Selborne Ave, Bulawayo',       '+263772999000');

-- Link the two patient accounts to their patient record.
UPDATE patients SET user_id = (SELECT user_id FROM users WHERE email = 'patient@mpilo.co.zw') WHERE client_ref = '1';
UPDATE patients SET user_id = (SELECT user_id FROM users WHERE email = 'naledi@mail.co.zw')   WHERE client_ref = '2';

-- ---------------------------------------------------------------- staff (10)
INSERT INTO staff (client_ref, department_id, first_name, last_name, role, department, phone, email) VALUES
    ('1',  (SELECT department_id FROM departments WHERE name = 'Outpatient'),    'Lindiwe',  'Ndlovu',  'Nurse',      'Outpatient',    '+263772100001', 'nurse@mpilo.co.zw'),
    ('2',  (SELECT department_id FROM departments WHERE name = 'Pharmacy'),      'Pharmacy', '03',      'Pharmacist', 'Pharmacy',      '+263772100002', 'pharmacist@mpilo.co.zw'),
    ('3',  (SELECT department_id FROM departments WHERE name = 'Outpatient'),    'Casualty', 'Desk',    'Responder',  'Casualty',      '+263772100003', 'responder@mpilo.co.zw'),
    ('4',  (SELECT department_id FROM departments WHERE name = 'Outpatient'),    'Admin',    'Office',  'Admin',      'Administration','+263772100004', 'admin@mpilo.co.zw'),
    ('5',  (SELECT department_id FROM departments WHERE name = 'Maternity'),     'Grace',    'Mutasa',  'Nurse',      'Maternity',     '+263772100005', 'gmutasa@mpilo.co.zw'),
    ('6',  (SELECT department_id FROM departments WHERE name = 'Pharmacy'),      'Pharmacy', '07',      'Pharmacist', 'Pharmacy',      '+263772100006', 'pharmacist2@mpilo.co.zw'),
    ('7',  (SELECT department_id FROM departments WHERE name = 'Paediatrics'),   'Memory',   'Zulu',    'Nurse',      'Paediatrics',   '+263772100007', 'mzulu@mpilo.co.zw'),
    ('8',  (SELECT department_id FROM departments WHERE name = 'Radiology'),     'Peter',    'Banda',   'Radiographer','Radiology',    '+263772100008', 'pbanda@mpilo.co.zw'),
    ('9',  (SELECT department_id FROM departments WHERE name = 'Orthopaedics'),  'Sarah',    'Moyo',    'Nurse',      'Orthopaedics',  '+263772100009', 'samoyo@mpilo.co.zw'),
    ('10', (SELECT department_id FROM departments WHERE name = 'Mental Health'), 'Daniel',   'Chirwa',  'Counsellor', 'Mental Health', '+263772100010', 'dchirwa@mpilo.co.zw');

UPDATE staff SET user_id = (SELECT user_id FROM users WHERE email = staff.email)
WHERE email IN (SELECT email FROM users);

-- ---------------------------------------------------------------- patient_vitals (10)
INSERT INTO patient_vitals (patient_id, recorded_by, blood_pressure, temperature, pulse, weight, notes, recorded_at)
SELECT p.patient_id,
       (SELECT user_id FROM users WHERE email = 'nurse@mpilo.co.zw'),
       v.bp, v.temp, v.pulse, v.weight, v.notes,
       now() - (v.days_ago || ' days')::interval
FROM (VALUES
    ('1',  '128/82', 36.8, 78,  82.4, 'Routine check before cardiology review', 2),
    ('2',  '118/76', 36.6, 84,  68.1, 'Antenatal visit, 28 weeks',              0),
    ('3',  '142/90', 37.1, 92,  91.0, 'Reports chest tightness on exertion',    0),
    ('4',  '120/78', 36.5, 70,  74.3, 'Ankle still swollen',                    3),
    ('5',  '115/72', 36.7, 68,  61.5, 'Hay fever, otherwise well',             10),
    ('6',  '126/80', 36.9, 75,  88.2, 'Knee pain after football',               7),
    ('7',  '122/79', 37.4, 88,  57.9, 'Skin rash on both forearms',             5),
    ('8',  '150/95', 36.6, 80,  95.6, 'Blood pressure high, review booked',     1),
    ('9',  '105/68', 36.8, 96,  32.1, 'Child, due for vaccination',             4),
    ('10', '134/86', 36.7, 74,  79.8, 'Migraine frequency reducing',           12)
) AS v(client_ref, bp, temp, pulse, weight, notes, days_ago)
JOIN patients p ON p.client_ref = v.client_ref;

-- ---------------------------------------------------------------- appointments (14)
INSERT INTO appointments (client_ref, patient_id, doctor_id, appointment_date, appointment_time, status, reason)
SELECT a.client_ref,
       p.patient_id,
       d.doctor_id,
       CURRENT_DATE + a.day_offset,
       a.at_time, a.status, a.reason
FROM (VALUES
    ('1',  '1',  '1',   2, '09:00', 'Confirmed', 'Cardiology follow-up and BP review'),
    ('2',  '1',  '2',   5, '11:30', 'Pending',   'Persistent cough for two weeks'),
    ('3',  '2',  '3',   0, '10:00', 'Confirmed', 'Antenatal review, 28 weeks'),
    ('4',  '3',  '1',   0, '08:30', 'Confirmed', 'Chest tightness on exertion'),
    ('5',  '4',  '2',  -3, '14:30', 'Completed', 'Sprained ankle, follow-up'),
    ('6',  '5',  '2', -10, '09:15', 'Completed', 'Hay fever consultation'),
    ('7',  '2',  '1',  -1, '12:00', 'Cancelled', 'Palpitations, rescheduled'),
    ('8',  '1',  '1', -20, '09:00', 'Completed', 'Initial cardiology assessment'),
    ('9',  '6',  '5',  -7, '10:30', 'Completed', 'Knee pain after football'),
    ('10', '7',  '6',  -5, '13:00', 'Completed', 'Persistent skin rash'),
    ('11', '8',  '8',   1, '15:00', 'Confirmed', 'Blood pressure check'),
    ('12', '9',  '4',   3, '09:45', 'Pending',   'Childhood vaccination'),
    ('13', '10', '7', -12, '11:00', 'Completed', 'Recurring migraines'),
    ('14', '4',  '9',   7, '14:00', 'Pending',   'Stress and sleep problems')
) AS a(client_ref, patient_ref, doctor_ref, day_offset, at_time, status, reason)
JOIN patients p ON p.client_ref = a.patient_ref
JOIN doctors  d ON d.client_ref = a.doctor_ref;

-- ---------------------------------------------------------------- prescriptions (12)
INSERT INTO prescriptions (client_ref, appointment_id, patient_id, doctor_id, medication_name, dosage, frequency, duration, issue_date, dispensed)
SELECT r.client_ref,
       ap.appointment_id,
       p.patient_id,
       d.doctor_id,
       r.medication, r.dosage, r.frequency, r.duration,
       CURRENT_DATE + r.day_offset,
       r.dispensed
FROM (VALUES
    ('1',  '8',  '1',  '1',  'Atenolol',              '50 mg',  'Once daily',        'Ongoing', -20, TRUE),
    ('2',  '8',  '1',  '1',  'Amoxicillin',           '500 mg', 'Twice daily',       '7 days',   -2, FALSE),
    ('3',  '5',  '4',  '2',  'Ibuprofen',             '400 mg', 'Three times daily', '5 days',   -3, FALSE),
    ('4',  '6',  '5',  '2',  'Loratadine',            '10 mg',  'Once daily',        '14 days', -10, TRUE),
    ('5',  '9',  '6',  '5',  'Diclofenac',            '50 mg',  'Twice daily',       '7 days',   -7, TRUE),
    ('6',  '10', '7',  '6',  'Hydrocortisone cream',  '1%',     'Twice daily',       '10 days',  -5, FALSE),
    ('7',  '13', '10', '7',  'Sumatriptan',           '50 mg',  'As needed',         '30 days', -12, TRUE),
    ('8',  '13', '10', '7',  'Paracetamol',           '500 mg', 'Three times daily', '5 days',  -12, TRUE),
    ('9',  '5',  '4',  '2',  'Paracetamol',           '500 mg', 'Three times daily', '3 days',   -3, FALSE),
    ('10', '6',  '5',  '2',  'Cetirizine',            '10 mg',  'Once daily',        '14 days', -10, FALSE),
    ('11', '8',  '1',  '1',  'Aspirin',               '75 mg',  'Once daily',        'Ongoing', -20, TRUE),
    ('12', '9',  '6',  '5',  'Codeine',               '30 mg',  'As needed',         '5 days',   -7, FALSE)
) AS r(client_ref, appointment_ref, patient_ref, doctor_ref, medication, dosage, frequency, duration, day_offset, dispensed)
JOIN appointments ap ON ap.client_ref = r.appointment_ref
JOIN patients     p  ON p.client_ref  = r.patient_ref
JOIN doctors      d  ON d.client_ref  = r.doctor_ref;

-- ---------------------------------------------------------------- medication_reminders (10)
INSERT INTO medication_reminders (client_ref, prescription_id, patient_id, medication_name, dosage, reminder_time, is_active)
SELECT m.client_ref, rx.prescription_id, rx.patient_id, rx.medication_name, rx.dosage, m.at_time, m.active
FROM (VALUES
    ('1',  '1',  '08:00', TRUE),
    ('2',  '2',  '08:00', TRUE),
    ('3',  '2',  '20:00', TRUE),
    ('4',  '3',  '07:30', TRUE),
    ('5',  '4',  '09:00', FALSE),
    ('6',  '5',  '08:30', TRUE),
    ('7',  '7',  '21:00', TRUE),
    ('8',  '8',  '12:00', FALSE),
    ('9',  '10', '08:00', TRUE),
    ('10', '11', '08:00', TRUE)
) AS m(client_ref, prescription_ref, at_time, active)
JOIN prescriptions rx ON rx.client_ref = m.prescription_ref;

-- ---------------------------------------------------------------- emergencies (10)
INSERT INTO emergencies (client_ref, patient_id, location, latitude, longitude, status, description, raised_at, resolved_at)
SELECT e.client_ref, p.patient_id, e.location, e.lat, e.lon, e.status, e.description,
       now() - (e.hours_ago || ' hours')::interval,
       CASE WHEN e.status = 'Resolved' THEN now() - ((e.hours_ago - 1) || ' hours')::interval END
FROM (VALUES
    ('1',  '1',  '14 Fife St, Bulawayo (-20.15280, 28.58190)',          -20.15280, 28.58190, 'Active',      'Severe chest pain',                1),
    ('2',  '3',  '22 Leopold Takawira, Bulawayo (-20.16010, 28.58640)', -20.16010, 28.58640, 'In Progress', 'Collapsed at home, breathing',      3),
    ('3',  '8',  '40 Lobengula St, Bulawayo (-20.15910, 28.57720)',     -20.15910, 28.57720, 'Resolved',    'Very high blood pressure',         28),
    ('4',  '5',  '31 George Silundika, Bulawayo (-20.15440, 28.58330)', -20.15440, 28.58330, 'Resolved',    'Allergic reaction, swelling',      52),
    ('5',  '2',  'Location unavailable (GPS permission refused)',             NULL,     NULL, 'Resolved',    'Early contractions, 28 weeks',     76),
    ('6',  '6',  '17 Jason Moyo St, Bulawayo (-20.15680, 28.58010)',    -20.15680, 28.58010, 'Resolved',    'Fall, suspected fracture',        100),
    ('7',  '10', '26 Selborne Ave, Bulawayo (-20.15120, 28.59010)',     -20.15120, 28.59010, 'Resolved',    'Severe migraine with vomiting',   124),
    ('8',  '4',  '5 Robert Mugabe Way, Bulawayo (-20.15350, 28.58470)', -20.15350, 28.58470, 'Resolved',    'Road accident, leg injury',       148),
    ('9',  '9',  '9 Fort St, Bulawayo (-20.15230, 28.58120)',           -20.15230, 28.58120, 'Resolved',    'Child with high fever',            172),
    ('10', '7',  '3 Main St, Bulawayo (-20.15590, 28.58250)',           -20.15590, 28.58250, 'Resolved',    'Fainted at work',                 196)
) AS e(client_ref, patient_ref, location, lat, lon, status, description, hours_ago)
JOIN patients p ON p.client_ref = e.patient_ref;

-- ---------------------------------------------------------------- payments (10) and receipts (10)
INSERT INTO payments (client_ref, appointment_id, amount, payment_method, payment_date, status)
SELECT y.client_ref, ap.appointment_id, y.amount, y.method, CURRENT_DATE + y.day_offset, y.status
FROM (VALUES
    ('1',  '8',  20.00, 'EcoCash', -20, 'Completed'),
    ('2',  '5',  20.00, 'Cash',     -3, 'Completed'),
    ('3',  '6',  20.00, 'Card',    -10, 'Completed'),
    ('4',  '9',  40.00, 'EcoCash',  -7, 'Completed'),
    ('5',  '10', 30.00, 'Card',     -5, 'Completed'),
    ('6',  '13', 30.00, 'EcoCash', -12, 'Completed'),
    ('7',  '3',  30.00, 'Cash',      0, 'Completed'),
    ('8',  '4',  30.00, 'EcoCash',   0, 'Pending'),
    ('9',  '7',  30.00, 'Card',     -1, 'Failed'),
    ('10', '1',  30.00, 'EcoCash',  -2, 'Completed'),
    ('11', '11', 20.00, 'Cash',     -1, 'Completed'),
    ('12', '2',  20.00, 'Card',     -4, 'Completed')
) AS y(client_ref, appointment_ref, amount, method, day_offset, status)
JOIN appointments ap ON ap.client_ref = y.appointment_ref;

-- A receipt is issued for completed payments only.
INSERT INTO receipts (payment_id, receipt_no, issued_at)
SELECT pay.payment_id,
       'RCP-' || to_char(pay.payment_date, 'YYYY') || '-' ||
           lpad(nextval('receipt_serial')::text, 5, '0'),
       pay.created_at
FROM payments pay
WHERE pay.status = 'Completed'
ORDER BY pay.payment_date;
