/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package models

import org.joda.time.LocalDate

case class Claim(
  val uuid: String, val patientUuid: String, val providerUuid: String,
  val dos: LocalDate,

  // ICD-9 / ICD-10 CM (diagnostic codes)
  val icd_d_pri: String, val icd_d: Set[String],

  // ICD-9 / ICD-10 PCS (procedure codes)
  val icd_p: Set[String],

  // HCFA Form 1500 POS (Point of Service),
  // UB Revenue (billing code), CPT (procedure), HCPCS (medical goods and services)
  val hcfaPOS: String, val ubRevenue: String, val cpt: String, val hcpcs: String) {

  def toList: List[String] = {

    List.concat(
      List(uuid, patientUuid, providerUuid, dos.toString, icd_d_pri),
      icd_d.toList,
      for (i <- icd_d.size to 9) yield "",
      icd_p.toList,
      for (i <- icd_p.size to 9) yield "",
      List(hcfaPOS, ubRevenue, cpt, hcpcs))

  }
}



