/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package models

import org.joda.time.LocalDate
import utils.NickelException

object ClaimParser {

  def toSet(l: List[String]): Set[String] = { (for (s <- l if s.length > 0) yield s).toSet }

  def fromList(l: List[String]): Claim = {
    if (l.size < 29) throw NickelException("ClaimParser.fromList - list must have a least 29 elements, have " + l.size)
    Claim(l(0), l(1), l(2), LocalDate.parse(l(3)),
      l(4), toSet(l.slice(5, 15)),
      toSet(l.slice(15, 25)),
      l(25), l(26), l(27), l(28))
  }
}

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
      List.fill(10 - icd_d.size)(""),
      icd_p.toList,
      List.fill(10 - icd_p.size)(""),
      List(hcfaPOS, ubRevenue, cpt, hcpcs))

  }
}



