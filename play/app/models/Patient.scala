/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package models

import org.joda.time.DateTime

case class Patient(val uuid: String, val firstName: String, val lastName: String, val gender: String, val dob: DateTime)

case class PatientHistory(
  val icd_d_pri: Map[String, DateTime], val icd_d: Map[String, DateTime],

  // ICD-9 / ICD-10 PCS (procedure codes)
  val icd_p: Map[String, DateTime],

  // HCFA Form 1500 POS (Point of Service),
  // UB Revenue (billing code), CPT (procedure), HCPCS (medical goods and services)
  val hcfaPOS: Map[String, DateTime], val ubRevenue: Map[String, DateTime],
  val cpt: Map[String, DateTime], val hcpcs: Map[String, DateTime])

