/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package models

import org.joda.time.LocalDate

case class Patient(val uuid: String, val firstName: String, val lastName: String, val gender: String, val dob: LocalDate) {
  
  def toList = List(uuid, firstName, lastName, gender, dob.toString)
}

case class PatientHistory(
  val icd_d_pri: Map[String, LocalDate], val icd_d: Map[String, LocalDate],

  // ICD-9 / ICD-10 PCS (procedure codes)
  val icd_p: Map[String, LocalDate],

  // HCFA Form 1500 POS (Point of Service),
  // UB Revenue (billing code), CPT (procedure), HCPCS (medical goods and services)
  val hcfaPOS: Map[String, LocalDate], val ubRevenue: Map[String, LocalDate],
  val cpt: Map[String, LocalDate], val hcpcs: Map[String, LocalDate])

