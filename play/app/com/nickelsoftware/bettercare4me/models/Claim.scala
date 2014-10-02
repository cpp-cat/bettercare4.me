/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package com.nickelsoftware.bettercare4me.models

import scala.math.BigDecimal.double2bigDecimal

import org.joda.time.DateTime
import org.joda.time.LocalDate

import com.nickelsoftware.bettercare4me.utils.NickelException

object ClaimParser {

  def toSet(l: List[String]): Set[String] = { (for (s <- l if s.length > 0) yield s).toSet }

  def fromList(l: List[String]): Claim = {

    def mkMedClaim(l: List[String]): Claim = {
      if (l.size < 42) throw NickelException("ClaimParser.fromList - MedClaim must have a least 42 elements, have " + l.size)

      MedClaim(
        l(1), // claimID claim ID
        l(2), // patientID Patient ID 
        l(3), // providerID Provider ID
        LocalDate.parse(l(4)).toDateTimeAtStartOfDay(), // dos Date of Service
        LocalDate.parse(l(5)).toDateTimeAtStartOfDay(), // dosThru DOS Thru
        MHead(
          l(6), // claimStatus Claim status
          l(7), // pcpFlag PCP Flag
          l(8), // specialtyCde
          l(9), // hcfaPOS HCFA Form 1500 POS (Point of Service),
          l(10), // dischargeStatus Discharge Status (2 chars)
          l(11).toInt, // daysDenied Nbr of days denied for in-patient claims
          l(12)), // roomBoardFlag Room & Board Flag ("Y" indicates in-patient discharged claim) - optional
        MCodes(
          l(13), // icdDPri ICD Primary Diagnostic
          toSet(l.slice(14, 24)), // icdD Secondary Diagnostic codes (up to 10)
          toSet(l.slice(24, 34)), // icdP ICD Procedure codes (up to 10)
          l(34), // drg Diagnosis Related Group
          l(35), // cpt CPT (procedure procedure)
          l(36), // cptMod1 CPT Modifier 1 (2 chars)
          l(37)), // cptMod2 CPT Modifier 1 (2 chars)
        MBill(
          l(38), // tob Type of Bill (3 chars)
          l(39), // ubRevenue UB Revenue (billing code) 
          l(40), // hcpcs HCPCS (medical goods and services)
          l(41))) // hcpcsMod HCPCS Modifier code (2 chars)
    }

    def mkRxClaim(l: List[String]): Claim = {
      if (l.size < 10) throw NickelException("ClaimParser.fromList - RxClaim must have a least 10 elements, have " + l.size)

      RxClaim(
        l(1), // claimID claim ID
        l(2), // patientID Patient ID 
        l(3), // providerID Provider ID
        LocalDate.parse(l(4)).toDateTimeAtStartOfDay(), // fill date
        l(5), // claimStatus Claim status
        l(6), // ndc
        l(7).toInt, // days of supply
        l(8).toInt, // quantity
        l(9)) // supply flag
    }

    def mkLabClaim(l: List[String]): Claim = {
      if (l.size < 10) throw NickelException("ClaimParser.fromList - RxClaim must have a least 10 elements, have " + l.size)

      LabClaim(
        l(1), // claimID claim ID
        l(2), // patientID Patient ID 
        l(3), // providerID Provider ID
        LocalDate.parse(l(4)).toDateTimeAtStartOfDay(), // date of service
        l(5), // claimStatus Claim status
        l(6), // cpt
        l(7), // LOINC
        BigDecimal(l(8)), // result
        l(9)) // positive/negative result
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
 * Define utility methods
 */
object Claim {

  /**
   * @returns true if there is at least 2 claims with different DOS
   */
  def twoDifferentDOS(claims: List[MedClaim]): Boolean = {
    
    if (claims.size < 2) false
    else {
      val dos = claims.head.dos
      claims.tail.foldLeft(false)({ (b, c) => if (!b && dos == c.dos) false else true })
    }
  }
}

/**
 * Defining trait for all claim type
 */
trait Claim {

  def claimType: String
  def date: DateTime

}

/**
 * Represents the additional head parameters for Medical Claim
 *
 * The parameters are:
 *  - claimStatus Claim status:
 * 		"A" Adjustment to original claim
 * 		"D" Denied claims
 * 		"I" Initial Paid Claim
 * 		"P" Pended for adjudication
 * 		"R" Reversal to original claim
 *  - pcpFlag PCP Flag - relationship of provider with health plan ("Y" / "N")
 *  - specialtyCde - NUCC Provider Specialty
 *  - hcfaPOS HCFA Form 1500 POS (Point of Service),
 *  - dischargeStatus Discharge Status (2 chars)
 *  - daysDenied Nbr of days denied for in-patient claims
 *  - roomBoardFlag Room & Board Flag ("Y" indicates in-patient discharged claim) - optional
 */
case class MHead(claimStatus: String = "", pcpFlag: String = "", specialtyCde: String = "", hcfaPOS: String = "",
  dischargeStatus: String = "", daysDenied: Int = 0, roomBoardFlag: String = "N") {

  def toList = List(claimStatus, pcpFlag, specialtyCde, hcfaPOS, dischargeStatus, daysDenied.toString, roomBoardFlag)
}

/**
 * Represents the clinical codes for Medical Claim
 *
 * The parameters are:
 * 	- icdDPri ICD Primary Diagnostic
 *  - icdD Secondary Diagnostic codes (up to 10)
 *  - icdP ICD Procedure codes (up to 10)
 *  - drg Diagnosis Related Group
 *  - cpt CPT (procedure procedure)
 *  - cptMod1 CPT Modifier 1 (2 chars)
 *  - cptMod2 CPT Modifier 2 (2 chars)
 */
case class MCodes(icdDPri: String = "", icdD: Set[String] = Set(), icdP: Set[String] = Set(), drg: String = "",
  cpt: String = "", cptMod1: String = "", cptMod2: String = "") {

  def toList = List.concat(List(icdDPri), icdD.toList, List.fill(10 - icdD.size)(""), icdP.toList, List.fill(10 - icdP.size)(""), List(drg, cpt, cptMod1, cptMod2))
}

/**
 * Represents the billing codes and info
 *
 * The parameters are:
 *  - tob Type of Bill (3 chars)
 *  - ubRevenue UB Revenue (billing code)
 *  - hcpcs HCPCS (medical goods and services)
 *  - hcpcsMod HCPCS Modifier code (2 chars)
 */
case class MBill(tob: String = "", ubRevenue: String = "", hcpcs: String = "", hcpcsMod: String = "") {

  def toList = List(tob, ubRevenue, hcpcs, hcpcsMod)
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
 *  - mHead: MHead - Medical Claim additional header parameters
 *  	- claimStatus Claim status
 *  	- pcpFlag PCP Flag - relationship of provider with health plan ("Y" / "N")
 *  	- specialtyCde - NUCC Provider Specialty
 *  	- hcfaPOS HCFA Form 1500 POS (Point of Service),
 *  	- dischargeStatus Discharge Status (2 chars)
 *  	- daysDenied Nbr of days denied for in-patient claims
 *  	- roomBoardFlag Room & Board Flag ("Y" indicates in-patient discharged claim) - optional
 *  - mCodes: MCodes - Medical Claim clinical codes:
 * 		- icdDPri ICD Primary Diagnostic
 *  	- icdD Secondary Diagnostic codes (up to 10)
 *  	- icdP ICD Procedure codes (up to 10)
 *  	- drg Diagnosis Related Group
 *  	- cpt CPT (procedure procedure)
 *  	- cptMod1 CPT Modifier 1 (2 chars)
 *  	- cptMod2 CPT Modifier 2 (2 chars)
 *  - mBill: MBill - Billing codes and info
 *  	- tob Type of Bill (3 chars)
 *  	- ubRevenue UB Revenue (billing code)
 *  	- hcpcs HCPCS (medical goods and services)
 *  	- hcpcsMod HCPCS Modifier code (2 chars)
 */
case class MedClaim(claimID: String, patientID: String, providerID: String, dos: DateTime, dosThru: DateTime,
  mHead: MHead = MHead(), mCodes: MCodes = MCodes(), mBill: MBill = MBill()) extends Claim {

  override def claimType = "MD"
  override def date = dos

  def claimStatus = mHead.claimStatus
  def pcpFlag = mHead.pcpFlag
  def specialtyCde = mHead.specialtyCde
  def hcfaPOS = mHead.hcfaPOS
  def dischargeStatus = mHead.dischargeStatus
  def daysDenied = mHead.daysDenied
  def roomBoardFlag = mHead.roomBoardFlag

  def icdDPri = mCodes.icdDPri
  def icdD = mCodes.icdD
  def icdP = mCodes.icdP
  def drg = mCodes.drg
  def cpt = mCodes.cpt
  def cptMod1 = mCodes.cptMod1
  def cptMod2 = mCodes.cptMod2

  def tob = mBill.tob
  def ubRevenue = mBill.ubRevenue
  def hcpcs = mBill.hcpcs
  def hcpcsMod = mBill.hcpcsMod

  def toList: List[String] = List.concat(List("MD", claimID, patientID, providerID, dos.toLocalDate().toString, dosThru.toLocalDate().toString), mHead.toList, mCodes.toList, mBill.toList)

  /**
   * @param dia set of diasgnotics to check against
   * @returns true if this claim has any of the diagnostics passed as argument
   */
  def hasDiagnostic(dia: Set[String]): Boolean = {
    if(dia.isEmpty) false
    else {
      val d = dia.head
      if(mCodes.icdDPri==d || mCodes.icdD.contains(d)) true
      else hasDiagnostic(dia.tail)
    }
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
  override def date = fillD

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

  //8 result: numeric (BigDecimal)
  result: BigDecimal = 0.0,

  //9 PosNegResult for binary result (as opposed to numeric) "1" is positive, "0" is negative, "" for N/A (numeric)
  posNegResult: String = "") extends Claim {

  override def claimType = "LC"
  override def date = dos

  def toList: List[String] = List("LC", claimID, patientID, providerID,
    dos.toLocalDate().toString, claimStatus, cpt, loinc, result.toString(), posNegResult)
}
