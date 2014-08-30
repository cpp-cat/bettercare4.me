/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package models

import org.joda.time.DateTime

case class Claim(
  val uuid: String, val patientUuid: String, val providerUuid: String,
  val dos: DateTime,

  // ICD-9 / ICD-10 CM (diagnostic codes)
  val icd_d_pri: String, val icd_d: Set[String],

  // ICD-9 / ICD-10 PCS (procedure codes)
  val icd_p: Set[String],

  // HCFA Form 1500 POS (Point of Service),
  // UB Revenue (billing code), CPT (procedure), HCPCS (medical goods and services)
  val hcfaPOS: String, val ubRevenue: String, val cpt: String, val hcpcs: String)



