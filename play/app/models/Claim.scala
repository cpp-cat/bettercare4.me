/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package models

import org.joda.time.LocalDate
import utils.NickelException
import org.joda.time.DateTime

object ClaimParser {

  def toSet(l: List[String]): Set[String] = { (for (s <- l if s.length > 0) yield s).toSet }

  def fromList(l: List[String]): Claim = {

    def mkMedClaim(l: List[String]): Claim = {
      if (l.size < 41) throw NickelException("ClaimParser.fromList - MedClaim must have a least 41 elements, have " + l.size)

      MedClaim(
        l(1), //1 claimID claim ID
        l(2), //2 patientID Patient ID 
        l(3), //3 providerID Provider ID
        LocalDate.parse(l(4)).toDateTimeAtStartOfDay(), //4 dos Date of Service
        LocalDate.parse(l(5)).toDateTimeAtStartOfDay(), //5 dosThru DOS Thru
        l(6), //6 claimStatus Claim status
        l(7), //7 pcpFlag PCP Flag
        l(8), //8 icdDPri ICD Primary Diagnostic
        toSet(l.slice(9, 19)), //9-18 icdD Secondary Diagnostic codes (up to 10)
        toSet(l.slice(19, 29)), //19-28 icdP ICD Procedure codes (up to 10)
        l(29), //29 hcfaPOS HCFA Form 1500 POS (Point of Service),
        l(30), //30 drg Diagnosis Related Group
        l(31), //31 tob Type of Bill (3 chars)
        l(32), //32 ubRevenue UB Revenue (billing code) 
        l(33), //33 cpt CPT (procedure procedure)
        l(34), //34 cptMod1 CPT Modifier 1 (2 chars)
        l(35), //35 cptMod2 CPT Modifier 1 (2 chars)
        l(36), //36 hcpcs HCPCS (medical goods and services)
        l(37), //37 hcpcsMod HCPCS Modifier code (2 chars)
        l(38), //38 dischargeStatus Discharge Status (2 chars)
        l(39).toInt, //39 daysDenied Nbr of days denied for in-patient claims
        l(40) //40 roomBoardFlag Room & Board Flag ("Y" indicates in-patient discharged claim) - optional
        )
    }

    def mkRxClaim(l: List[String]): Claim = {
      if (l.size < 10) throw NickelException("ClaimParser.fromList - RxClaim must have a least 10 elements, have " + l.size)

      RxClaim(
        l(1), //1 claimID claim ID
        l(2), //2 patientID Patient ID 
        l(3), //3 providerID Provider ID
        LocalDate.parse(l(4)).toDateTimeAtStartOfDay(), //4 fill date
        l(5), //5 claimStatus Claim status
        l(6), //6 ndc
        l(7).toInt, //7 days of supply
        l(8).toInt, //8 quantity
        l(9) //9 supply flag
        )
    }

    def mkLabClaim(l: List[String]): Claim = {
      if (l.size < 10) throw NickelException("ClaimParser.fromList - RxClaim must have a least 10 elements, have " + l.size)

      LabClaim(
        l(1), //1 claimID claim ID
        l(2), //2 patientID Patient ID 
        l(3), //3 providerID Provider ID
        LocalDate.parse(l(4)).toDateTimeAtStartOfDay(), //4 date of service
        l(5), //5 claimStatus Claim status
        l(6), //6 cpt
        l(7), //7 LOINC
        l(8).toDouble, //8 result
        l(9) //9 positive/negative result
        )
    }

    if (l(0) == "MD") mkMedClaim(l)
    else if (l(0) == "RX") mkRxClaim(l)
    else if (l(0) == "LC") mkLabClaim(l)
    else throw NickelException("ClaimParser.fromList - Unknown claim type (1st attribute) should be 'MD', 'RX', or 'LC', have " + l(0))
  }
  
  def toList(claim: Claim): List[String] = {
    claim match {
      case medClaim: MedClaim => medClaim.toList
      case rxClaim: RxClaim => rxClaim.toList
      case labClaim: LabClaim => labClaim.toList
      case _ => throw NickelException("ClaimParser.toList - Unknown claim type")
    }
  }
}

/**
 * Defining trait for all claim type
 */
trait Claim {

  def claimType: String

}

/**
 * Represent Institutional and Professional Claims
 *
 * The parameters to the class are:
 * 	- claimID Claim ID
 *  - patientID Patient ID
 *  - providerID Provider ID
 *  - dos Date of Service
 *  - dosThru DOS Thru - same as DOS for single day service, same as discharge date for in-patient claim
 *  - claimStatus Claim status:
 * 		"A" Adjustment to original claim
 * 		"D" Denied claims
 * 		"I" Initial Paid Claim
 * 		"P" Pended for adjudication
 * 		"R" Reversal to original claim
 *  - pcpFlag PCP Flag - relationship of provider with health plan ("Y" / "N")
 * 	- icdDPri ICD Primary Diagnostic
 *  - icdD Secondary Diagnostic codes (up to 10)
 *  - icdP ICD Procedure codes (up to 10)
 *  - drg Diagnosis Related Group
 *  - tob Type of Bill (3 chars)
 *  - ubRevenue UB Revenue (billing code)
 *  - cpt CPT (procedure procedure)
 *  - cptMod1 CPT Modifier 1 (2 chars)
 *  - cptMod2 CPT Modifier 2 (2 chars)
 *  - hcpcs HCPCS (medical goods and services)
 *  - hcpcsMod HCPCS Modifier code (2 chars)
 *  - dischargeStatus Discharge Status (2 chars)
 *  - daysDenied Nbr of days denied for in-patient claims
 *  - roomBoardFlag Room & Board Flag ("Y" indicates in-patient discharged claim) - optional
 */
case class MedClaim(

  //1 claimID Claim ID
  claimID: String,

  //2 patientID Patient ID
  patientID: String,

  //3 providerID Provider ID
  providerID: String,

  //4 dos Date of Service
  dos: DateTime,

  //5 dosThru DOS Thru - same as DOS for single day service, same as discharge date for in-patient claim
  dosThru: DateTime,

  //6 claimStatus Claim status: 
  //	"A" Adjustment to original claim
  //	"D" Denied claims
  // 	"I" Initial Paid Claim
  // 	"P" Pended for adjudication
  //	"R" Reversal to original claim
  claimStatus: String = "",

  //7 pcpFlag PCP Flag - relationship of provider with health plan ("Y" / "N")
  pcpFlag: String = "",

  // ICD-9 / ICD-10 CM (diagnostic codes)
  //8 icdDPri ICD Primary Diagnostic
  icdDPri: String = "",

  //9-18 icdD Secondary Diagnostic codes (up to 10)
  icdD: Set[String] = Set(),

  // ICD-9 / ICD-10 PCS (procedure codes)
  //19-28 icdP ICD Procedure codes (up to 10)
  icdP: Set[String] = Set(),

  //29 hcfaPOS HCFA Form 1500 POS (Point of Service),
  hcfaPOS: String = "",

  //30 drg Diagnosis Related Group
  drg: String = "",

  //31 tob Type of Bill (3 chars)
  tob: String = "",

  //32 ubRevenue UB Revenue (billing code) 
  ubRevenue: String = "",

  //33 cpt CPT (procedure procedure)
  cpt: String = "",

  //34 cptMod1 CPT Modifier 1 (2 chars)
  cptMod1: String = "",

  //35 cptMod2 CPT Modifier 1 (2 chars)
  cptMod2: String = "",

  //36 hcpcs HCPCS (medical goods and services)
  hcpcs: String = "",

  //37 hcpcsMod HCPCS Modifier code (2 chars)
  hcpcsMod: String = "",

  //38 dischargeStatus Discharge Status (2 chars)
  dischargeStatus: String = "",

  //39 daysDenied Nbr of days denied for in-patient claims
  daysDenied: Int = 0,

  //40 roomBoardFlag Room & Board Flag ("Y" indicates in-patient discharged claim) - optional
  roomBoardFlag: String = "N") extends Claim {

  override def claimType = "MD"

  def toList: List[String] = {

    List.concat(
      List("MD", claimID, patientID, providerID, dos.toLocalDate().toString, dosThru.toLocalDate().toString, claimStatus, pcpFlag, icdDPri),
      icdD.toList,
      List.fill(10 - icdD.size)(""),
      icdP.toList,
      List.fill(10 - icdP.size)(""),
      List(hcfaPOS, drg, tob, ubRevenue, cpt, cptMod1, cptMod2, hcpcs, hcpcsMod, dischargeStatus, daysDenied.toString, roomBoardFlag))

  }
}

/**
 * RxClaim class
 *
 * The attributes are:
 * - claimID
 * - patientID
 * - fillD
 * - claimStatus
 * - ndc
 * - daysSupply: Int >= 1
 * - qty: Int >= 0
 * - supplyF: set to "Y" for DME
 */
case class RxClaim(

  //1 claimID Claim ID
  claimID: String,

  //2 patientID Patient ID
  patientID: String,

  //3 providerID Provider ID
  providerID: String,

  //4 fillD: fill date
  fillD: DateTime,

  //5 claimStatus Claim status: 
  //	"A" Adjustment to original claim
  //	"D" Denied claims
  // 	"I" Initial Paid Claim
  // 	"P" Pended for adjudication
  //	"R" Reversal to original claim
  claimStatus: String = "",

  //6 NDC drug NDC
  ndc: String = "",

  //7 daysSupply Nbr of days supply
  daysSupply: Int = 1,

  //8 Qty quantity dispensed
  qty: Int = 0,

  //9 Supply Flag, "Y" if DME rather than drugs
  supplyF: String = "N") extends Claim {

  override def claimType = "RX"

  def toList: List[String] = List("RX", claimID, patientID, providerID,
    fillD.toLocalDate().toString, claimStatus, ndc, daysSupply.toString(), qty.toString(), supplyF)
}

case class LabClaim(

  //1 claimID Claim ID
  claimID: String,

  //2 patientID Patient ID
  patientID: String,

  //3 providerID Provider ID
  providerID: String,

  //4 dos Date of Service
  dos: DateTime,

  //5 claimStatus Claim status: 
  //	"A" Adjustment to original claim
  //	"D" Denied claims
  // 	"I" Initial Paid Claim
  // 	"P" Pended for adjudication
  //	"R" Reversal to original claim
  claimStatus: String = "",

  //6 cpt CPT (procedure procedure)
  cpt: String = "",

  //7 LOINC
  loinc: String = "",

  //8 result: numeric (Double)
  result: Double = 0.0,

  //9 PosNegResult for binary result (as opposed to numeric) "1" is positive, "0" is negative, "" for N/A (numeric)
  posNegResult: String = "") extends Claim {

  override def claimType = "LC"

  def toList: List[String] = List("LC", claimID, patientID, providerID,
    dos.toLocalDate().toString, claimStatus, cpt, loinc, result.toString(), posNegResult)
}

