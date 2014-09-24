/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package com.nickelsoftware.bettercare4me.models

import org.joda.time.DateTime
import org.joda.time.LocalDate
import org.joda.time.Months
import org.joda.time.Years

import com.nickelsoftware.bettercare4me.utils.NickelException

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

    claims.foldLeft(PatientHistory(MedMap(), RxMap(), LabMap())) { (ph, claim) =>

      /*
       * Mapping Medical Claim
 * - medMap: MedMap is the mapping of the codes / info back to medical claims
 * 		- specialtyCde: Map[String, List[MedClaim]],
 * 		- hcfaPOS: Map[String, List[MedClaim]],
 * 		- icdD: Map[String, List[MedClaim]],
 * 		- icdP: Map[String, List[MedClaim]],
 * 		- cpt: Map[String, List[MedClaim]],
 * 		- tob: Map[String, List[MedClaim]],
 * 		- ubRevenue: Map[String, List[MedClaim]],
 * 		- hcpcs: Map[String, List[MedClaim]])
       */
      def medClaim2Map(c: MedClaim): MedMap = {

        // Provider specialty
        val specialtyCde = if (c.specialtyCde.length() > 0) add2Map(c.specialtyCde, c, ph.specialtyCde); else ph.specialtyCde
        
        // HCFA POS place of service
        val hcfaPOS = if (c.hcfaPOS.length() > 0) add2Map(c.hcfaPOS, c, ph.hcfaPOS); else ph.hcfaPOS
        
        // ICD primary & secondary Diagnostics
        val icdD1 = if (c.icdDPri.length() > 0) add2Map(c.icdDPri, c, ph.icdD); else ph.icdD
        val icdD = c.icdD.foldLeft(icdD1)((m, s) => add2Map(s, c, m))

        // ICD Procedures
        val icdP = c.icdP.foldLeft(ph.icdP)((m, s) => add2Map(s, c, m))

        // CPT code
        val cpt = if (c.cpt.length() > 0) add2Map(c.cpt, c, ph.cpt); else ph.cpt

        // TOB type of bill code
        val tob = if (c.tob.length() > 0) add2Map(c.tob, c, ph.tob); else ph.tob

        // UB Revenue code
        val ubRevenue = if (c.ubRevenue.length() > 0) add2Map(c.ubRevenue, c, ph.ubRevenue); else ph.ubRevenue

        // HCPCS code
        val hcpcs = if (c.hcpcs.length() > 0) add2Map(c.hcpcs, c, ph.hcpcs); else ph.hcpcs

        MedMap(specialtyCde, hcfaPOS, icdD, icdP, cpt, tob, ubRevenue, hcpcs)
      }

      // Rx Claim
      def rxClaim2Map(c: RxClaim): RxMap = {

        if (c.ndc.length() > 0) RxMap(add2Map(c.ndc, c, ph.ndc))
        else ph.rxMap
      }

      // Lab Claim
      def labClaim2Map(c: LabClaim): LabMap = {

        val cptLab = if (c.cpt.length() > 0) add2Map(c.cpt, c, ph.cptLab); else ph.cptLab
        val loinc = if (c.loinc.length() > 0) add2Map(c.loinc, c, ph.loinc); else ph.loinc

        LabMap(cptLab, loinc)
      }

      claim match {
        case medClaim: MedClaim => PatientHistory(medClaim2Map(medClaim), ph.rxMap, ph.labMap)

        case rxClaim: RxClaim => PatientHistory(ph.medMap, rxClaim2Map(rxClaim), ph.labMap)

        case labClaim: LabClaim => PatientHistory(ph.medMap, ph.rxMap, labClaim2Map(labClaim))

        case _ => throw NickelException("PatientHistoryFactory.createPatientHistory - Unknown claim type")
      }
    }
  }
}

/**
 * Representing the mapping of the codes / info back to medical claims
 */
case class MedMap(
  specialtyCde: Map[String, List[MedClaim]] = Map(),
  hcfaPOS: Map[String, List[MedClaim]] = Map(),
  icdD: Map[String, List[MedClaim]] = Map(),
  icdP: Map[String, List[MedClaim]] = Map(),
  cpt: Map[String, List[MedClaim]] = Map(),
  tob: Map[String, List[MedClaim]] = Map(),
  ubRevenue: Map[String, List[MedClaim]] = Map(),
  hcpcs: Map[String, List[MedClaim]] = Map())

/**
 * Representing the mapping of the codes / info back to pharmacy claims
 */
case class RxMap(ndc: Map[String, List[RxClaim]] = Map())

/**
 * Representing the mapping of the codes / info back to lab claims
 */
case class LabMap(cptLab: Map[String, List[LabClaim]] = Map(), loinc: Map[String, List[LabClaim]] = Map())

/**
 * Class representing the clinical history of a patient.
 *
 * It ties key clinical codes back to claims. The data elements are:
 * - medMap: MedMap is the mapping of the codes / info back to medical claims
 * 		- specialtyCde: Map[String, List[MedClaim]],
 * 		- hcfaPOS: Map[String, List[MedClaim]],
 * 		- icdD: Map[String, List[MedClaim]],
 * 		- icdP: Map[String, List[MedClaim]],
 * 		- cpt: Map[String, List[MedClaim]],
 * 		- tob: Map[String, List[MedClaim]],
 * 		- ubRevenue: Map[String, List[MedClaim]],
 * 		- hcpcs: Map[String, List[MedClaim]])
 * - rxMap: RxMap is the mapping of the ndc / info back to Rx claims
 * 		- ndc: Map[String, List[RxClaim]]
 * - labMap: LabMap is the mapping of the loinc / info back to lab claims
 * 		- cptLab: Map[String, List[LabClaim]]
 *   	- loinc: Map[String, List[LabClaim]]
 *
 * @param medMap is the mapping of the codes / info back to medical claims
 * @param rxMap is the mapping of the ndc / info back to Rx claims
 * @param labMap is the mapping of the loinc / info back to lab claims
 */
case class PatientHistory(medMap: MedMap, rxMap: RxMap, labMap: LabMap) {

  //
  // pass through methods
  //
  def specialtyCde = medMap.specialtyCde
  def hcfaPOS = medMap.hcfaPOS
  def icdD = medMap.icdD
  def icdP = medMap.icdP
  def cpt = medMap.cpt
  def tob = medMap.tob
  def ubRevenue = medMap.ubRevenue
  def hcpcs = medMap.hcpcs
  def ndc = rxMap.ndc
  def cptLab = labMap.cptLab
  def loinc = labMap.loinc

  //
  // Access methods for making the code more readable
  //
  def claims4Specialty(c: String): List[MedClaim] = medMap.specialtyCde.getOrElse(c, List())
  def claims4HCFAPOS(c: String): List[MedClaim] = medMap.hcfaPOS.getOrElse(c, List())
  def claims4ICDD(c: String): List[MedClaim] = medMap.icdD.getOrElse(c, List())
  def claims4ICDP(c: String): List[MedClaim] = medMap.icdP.getOrElse(c, List())
  def claims4CPT(c: String): List[MedClaim] = medMap.cpt.getOrElse(c, List())
  def claims4TOB(c: String): List[MedClaim] = medMap.tob.getOrElse(c, List())
  def claims4UBRev(c: String): List[MedClaim] = medMap.ubRevenue.getOrElse(c, List())
  def claims4HCPCS(c: String): List[MedClaim] = medMap.hcpcs.getOrElse(c, List())
  def claims4NDC(c: String): List[RxClaim] = rxMap.ndc.getOrElse(c, List())
  def claims4CPTLab(c: String): List[LabClaim] = labMap.cptLab.getOrElse(c, List())
  def claims4LOINC(c: String): List[LabClaim] = labMap.loinc.getOrElse(c, List())
}

