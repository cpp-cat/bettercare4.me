/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package models

import org.joda.time.LocalDate

case class Patient(uuid: String, firstName: String, lastName: String, gender: String, dob: LocalDate) {
  
  def toList = List(uuid, firstName, lastName, gender, dob.toString)
}

case class PatientHistory(
  icd_d_pri: Map[String, LocalDate], icd_d: Map[String, LocalDate],

  // ICD-9 / ICD-10 PCS (procedure codes)
  icd_p: Map[String, LocalDate],

  // HCFA Form 1500 POS (Point of Service),
  // UB Revenue (billing code), CPT (procedure), HCPCS (medical goods and services)
  hcfaPOS: Map[String, LocalDate], ubRevenue: Map[String, LocalDate],
  cpt: Map[String, LocalDate], hcpcs: Map[String, LocalDate])

