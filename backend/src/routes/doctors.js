'use strict';

// /api/doctors and /api/departments. Read-only reference data used by the booking screen.

const express = require('express');
const db = require('../db');

const router = express.Router();

/** GET /api/doctors?department= */
router.get('/doctors', async (req, res, next) => {
  try {
    const params = [];
    let where = '';
    if (req.query.department) {
      params.push(req.query.department);
      where = 'WHERE d.department = $1';
    }
    const rows = await db.many(
      `SELECT d.doctor_id,
              d.client_ref,
              d.first_name,
              d.last_name,
              d.first_name || ' ' || d.last_name AS full_name,
              d.specialization,
              d.department,
              d.phone,
              d.email
       FROM doctors d
       ${where}
       ORDER BY d.department, d.last_name`,
      params,
    );
    res.json(rows);
  } catch (err) {
    next(err);
  }
});

/** GET /api/departments */
router.get('/departments', async (_req, res, next) => {
  try {
    res.json(
      await db.many(
        'SELECT department_id, name, description FROM departments ORDER BY name',
      ),
    );
  } catch (err) {
    next(err);
  }
});

module.exports = router;
