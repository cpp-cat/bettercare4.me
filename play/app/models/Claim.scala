/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package models

import org.joda.time.LocalDate

case class Claim(
  uuid: String, patientUuid: String, providerUuid: String,
  dos: LocalDate,

  // ICD-9 / ICD-10 CM (diagnostic codes)
  icd_d_pri: String, icd_d: Set[String],

  // ICD-9 / ICD-10 PCS (procedure codes)
  icd_p: Set[String],

  // HCFA Form 1500 POS (Point of Service),
  // UB Revenue (billing code), CPT (procedure), HCPCS (medical goods and services)
  hcfaPOS: String, ubRevenue: String, cpt: String, hcpcs: String) {

  def toList: List[String] = {

    List.concat(
      List(uuid, patientUuid, providerUuid, dos.toString, icd_d_pri),
      icd_d.toList,
      List.fill(10-icd_d.size)(""),
      icd_p.toList,
      List.fill(10-icd_p.size)(""),
      List(hcfaPOS, ubRevenue, cpt, hcpcs))

  }
}



