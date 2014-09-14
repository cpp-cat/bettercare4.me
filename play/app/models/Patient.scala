/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package models

import org.joda.time.LocalDate
import org.joda.time.Years
import org.joda.time.DateTime
import org.joda.time.Months

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
  
    def add2Map(s: String, c: Claim, map: Map[String, List[Claim]]): Map[String, List[Claim]] = {
      val l = map.getOrElse(s, List())
      map + (s -> (c::l))
    }
    
    claims.foldLeft(PatientHistory(Map(), Map(), Map(), Map(), Map())){(ph, claim) =>
      
      // ICD Primary Diagnostic
      val icdD1 = if(claim.icdDPri.length()>0) add2Map(claim.icdDPri, claim, ph.icdD); else ph.icdD
      
      // ICD (Secondary) Diagnostics
      val icdD = claim.icdD.foldLeft(icdD1)((m, s) => add2Map(s, claim, m))
      
      // ICD Procedures
      val icdP = claim.icdP.foldLeft(ph.icdP)((m, s) => add2Map(s, claim, m))
      
      // UB Revenue code
      val ubRevenue = if(claim.ubRevenue.length()>0) add2Map(claim.ubRevenue, claim, ph.ubRevenue); else ph.ubRevenue
      
      // CPT code
      val cpt = if(claim.cpt.length()>0) add2Map(claim.cpt, claim, ph.cpt); else ph.cpt
      
      // HCPCS code
      val hcpcs = if(claim.hcpcs.length()>0) add2Map(claim.hcpcs, claim, ph.hcpcs); else ph.hcpcs

      PatientHistory(icdD, icdP, ubRevenue, cpt, hcpcs)
    }
  }
}

/**
 * Class representing the clinical history of a patient.
 * 
 * It ties key clinical codes back to claims.
 */
case class PatientHistory(
  icdD: Map[String, List[Claim]],
  icdP: Map[String, List[Claim]],
  ubRevenue: Map[String, List[Claim]],
  cpt: Map[String, List[Claim]], 
  hcpcs: Map[String, List[Claim]]) {
  
  //
  // Access methods for making the code more readable
  //
  def claims4ICDD(icdDCode: String): List[Claim] = icdD.getOrElse(icdDCode, List())
  def claims4ICDP(icdPCode: String): List[Claim] = icdP.getOrElse(icdPCode, List())
  def claims4CPT(cptCode: String): List[Claim] = cpt.getOrElse(cptCode, List())
  def claims4UBRev(ubRevenueCode: String): List[Claim] = ubRevenue.getOrElse(ubRevenueCode, List())
  def claims4HCPCS(hcpcsCode: String): List[Claim] = hcpcs.getOrElse(hcpcsCode, List())
}

