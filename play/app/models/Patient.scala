/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package models

import org.joda.time.LocalDate
import org.joda.time.Years
import org.joda.time.DateTime

object PatientParser {
  
  def fromList(l: List[String]): Patient = Patient(l(0), l(1), l(2), l(3), LocalDate.parse(l(4)).toDateTimeAtStartOfDay())
}


case class Patient(patientID: String, firstName: String, lastName: String, gender: String, dob: DateTime) {
  
  def age(asOf: DateTime): Int = Years.yearsBetween(dob, asOf).getYears()
  
  def toList = List(patientID, firstName, lastName, gender, dob.toLocalDate().toString)
}

case class PatientHistory(
  icd_d_pri: Map[String, LocalDate], icd_d: Map[String, LocalDate],

  // ICD-9 / ICD-10 PCS (procedure codes)
  icd_p: Map[String, LocalDate],

  // HCFA Form 1500 POS (Point of Service),
  // UB Revenue (billing code), CPT (procedure), HCPCS (medical goods and services)
  hcfaPOS: Map[String, LocalDate], ubRevenue: Map[String, LocalDate],
  cpt: Map[String, LocalDate], hcpcs: Map[String, LocalDate])

