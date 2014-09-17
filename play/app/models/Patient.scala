/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package models

import org.joda.time.LocalDate
import org.joda.time.Years
import org.joda.time.DateTime
import org.joda.time.Months
import utils.NickelException

object PatientParser {

  def fromList(l: List[String]): Patient = Patient(l(0), l(1), l(2), l(3), LocalDate.parse(l(4)).toDateTimeAtStartOfDay())
}

/**
 * Class representing a patient and his/her demographics.
 */
case class Patient(patientID: String, firstName: String, lastName: String, gender: String, dob: DateTime) {

  def age(asOf: DateTime): Int = Years.yearsBetween(dob, asOf).getYears()
  def ageInMonths(asOf: DateTime): Int = Months.monthsBetween(dob, asOf).getMonths()

  def toList = List(patientID, firstName, lastName, gender, dob.toLocalDate().toString)
}

object PatientHistoryFactory {

  def createPatientHistory(patient: Patient, claims: List[Claim]): PatientHistory = {

    def add2Map[C](s: String, c: C, map: Map[String, List[C]]): Map[String, List[C]] = {
      val l = map.getOrElse(s, List())
      map + (s -> (c :: l))
    }

    claims.foldLeft(PatientHistory(Map(), Map(), Map(), Map(), Map(), Map(), Map(), Map())) { (ph, claim) =>

      // Medical Claim
      def medClaim2Map(c: MedClaim): (Map[String, List[MedClaim]], Map[String, List[MedClaim]], Map[String, List[MedClaim]], Map[String, List[MedClaim]], Map[String, List[MedClaim]], Map[String, List[MedClaim]]) = {

        // ICD Primary Diagnostic
        val icdD1 = if (c.icdDPri.length() > 0) add2Map(c.icdDPri, c, ph.icdD); else ph.icdD

        // ICD (Secondary) Diagnostics
        val icdD = c.icdD.foldLeft(icdD1)((m, s) => add2Map(s, c, m))

        // ICD Procedures
        val icdP = c.icdP.foldLeft(ph.icdP)((m, s) => add2Map(s, c, m))

        // UB Revenue code
        val ubRevenue = if (c.ubRevenue.length() > 0) add2Map(c.ubRevenue, c, ph.ubRevenue); else ph.ubRevenue

        // CPT code
        val cpt = if (c.cpt.length() > 0) add2Map(c.cpt, c, ph.cpt); else ph.cpt

        // HCPCS code
        val hcpcs = if (c.hcpcs.length() > 0) add2Map(c.hcpcs, c, ph.hcpcs); else ph.hcpcs

        (icdD1, icdD, icdP, ubRevenue, cpt, hcpcs)
      }

      // Rx Claim
      def rxClaim2Map(c: RxClaim): Map[String, List[RxClaim]] = {

        if (c.ndc.length() > 0) add2Map(c.ndc, c, ph.ndc)
        else ph.ndc
      }

      // Lab Claim
      def labClaim2Map(c: LabClaim): (Map[String, List[LabClaim]], Map[String, List[LabClaim]]) = {

        val cptLab = if (c.cpt.length() > 0) add2Map(c.cpt, c, ph.cptLab); else ph.cptLab
        val loinc = if (c.loinc.length() > 0) add2Map(c.loinc, c, ph.loinc); else ph.loinc

        (cptLab, loinc)
      }

      claim match {
        case medClaim: MedClaim =>
          val (icdD1, icdD, icdP, ubRevenue, cpt, hcpcs) = medClaim2Map(medClaim)
          PatientHistory(icdD, icdP, ubRevenue, cpt, hcpcs, ph.ndc, ph.cptLab, ph.loinc)

        case rxClaim: RxClaim => PatientHistory(ph.icdD, ph.icdP, ph.ubRevenue, ph.cpt, ph.hcpcs, rxClaim2Map(rxClaim), ph.cptLab, ph.loinc)

        case labClaim: LabClaim =>
          val (cptLab, loinc) = labClaim2Map(labClaim)
          PatientHistory(ph.icdD, ph.icdP, ph.ubRevenue, ph.cpt, ph.hcpcs, ph.ndc, cptLab, loinc)

        case _ => throw NickelException("PatientHistoryFactory.createPatientHistory - Unknown claim type")
      }
    }
  }
}

/**
 * Class representing the clinical history of a patient.
 *
 * It ties key clinical codes back to claims.
 */
case class PatientHistory(
  icdD: Map[String, List[MedClaim]],
  icdP: Map[String, List[MedClaim]],
  ubRevenue: Map[String, List[MedClaim]],
  cpt: Map[String, List[MedClaim]],
  hcpcs: Map[String, List[MedClaim]],
  ndc: Map[String, List[RxClaim]],
  cptLab: Map[String, List[LabClaim]],
  loinc: Map[String, List[LabClaim]]) {

  //
  // Access methods for making the code more readable
  //
  def claims4ICDD(icdDCode: String): List[MedClaim] = icdD.getOrElse(icdDCode, List())
  def claims4ICDP(icdPCode: String): List[MedClaim] = icdP.getOrElse(icdPCode, List())
  def claims4CPT(cptCode: String): List[MedClaim] = cpt.getOrElse(cptCode, List())
  def claims4UBRev(ubRevenueCode: String): List[MedClaim] = ubRevenue.getOrElse(ubRevenueCode, List())
  def claims4HCPCS(hcpcsCode: String): List[MedClaim] = hcpcs.getOrElse(hcpcsCode, List())
  def claims4NDC(ndcCode: String): List[RxClaim] = ndc.getOrElse(ndcCode, List())
  def claims4CPTLab(cptCode: String): List[LabClaim] = cptLab.getOrElse(cptCode, List())
  def claims4LOINC(loincCode: String): List[LabClaim] = loinc.getOrElse(loincCode, List())
}

