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
      if (l.size < 46) throw NickelException("ClaimParser.fromList - MedClaim must have a least 46 elements, have " + l.size)

      MedClaim(
        l(1), // claimID claim ID
        l(2), // patientID Patient ID 
        l(3), // patientFirstName Patient first name 
        l(4), // patientLastName Patient last name 
        l(5), // providerID Provider ID
        l(6), // providerFirstName provider first name 
        l(7), // providerLastName provider last name 
        LocalDate.parse(l(8)).toDateTimeAtStartOfDay(), // dos Date of Service
        LocalDate.parse(l(9)).toDateTimeAtStartOfDay(), // dosThru DOS Thru
        MHead(
          l(10), // claimStatus Claim status
          l(11), // pcpFlag PCP Flag
          l(12), // specialtyCde
          l(13), // hcfaPOS HCFA Form 1500 POS (Point of Service),
          l(14), // dischargeStatus Discharge Status (2 chars)
          l(15).toInt, // daysDenied Nbr of days denied for in-patient claims
          l(16)), // roomBoardFlag Room & Board Flag ("Y" indicates in-patient discharged claim) - optional
        MCodes(
          l(17), // icdDPri ICD Primary Diagnostic
          toSet(l.slice(18, 28)), // icdD Secondary Diagnostic codes (up to 10)
          toSet(l.slice(28, 38)), // icdP ICD Procedure codes (up to 10)
          l(38), // drg Diagnosis Related Group
          l(39), // cpt CPT (procedure procedure)
          l(40), // cptMod1 CPT Modifier 1 (2 chars)
          l(41)), // cptMod2 CPT Modifier 1 (2 chars)
        MBill(
          l(42), // tob Type of Bill (3 chars)
          l(43), // ubRevenue UB Revenue (billing code) 
          l(44), // hcpcs HCPCS (medical goods and services)
          l(45))) // hcpcsMod HCPCS Modifier code (2 chars)
    }

    def mkRxClaim(l: List[String]): Claim = {
      if (l.size < 14) throw NickelException("ClaimParser.fromList - RxClaim must have a least 14 elements, have " + l.size)

      RxClaim(
        l(1), // claimID claim ID
        l(2), // patientID Patient ID 
        l(3), // patientFirstName Patient first name 
        l(4), // patientLastName Patient last name 
        l(5), // providerID Provider ID
        l(6), // providerFirstName provider first name 
        l(7), // providerLastName provider last name 
        LocalDate.parse(l(8)).toDateTimeAtStartOfDay(), // fill date
        l(9), // claimStatus Claim status
        l(10), // ndc
        l(11).toInt, // days of supply
        l(12).toInt, // quantity
        l(13)) // supply flag
    }

    def mkLabClaim(l: List[String]): Claim = {
      if (l.size < 14) throw NickelException("ClaimParser.fromList - RxClaim must have a least 14 elements, have " + l.size)

      LabClaim(
        l(1), // claimID claim ID
        l(2), // patientID Patient ID 
        l(3), // patientFirstName Patient first name 
        l(4), // patientLastName Patient last name 
        l(5), // providerID Provider ID
        l(6), // providerFirstName provider first name 
        l(7), // providerLastName provider last name 
        LocalDate.parse(l(8)).toDateTimeAtStartOfDay(), // date of service
        l(9), // claimStatus Claim status
        l(10), // cpt
        l(11), // LOINC
        BigDecimal(l(12)), // result
        l(13)) // positive/negative result
    }

    if (l(0) == "MD") mkMedClaim(l)
    else if (l(0) == "RX") mkRxClaim(l)
    else if (l(0) == "LC") mkLabClaim(l)
    else throw NickelException("ClaimParser.fromList - Unknown claim type (1st attribute) should be 'MD', 'RX', or 'LC', have " + l(0))
  }

  def toList(claim: Claim): List[String] = claim.toList
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

  val claimType: String
  val claimID: String
  def date: DateTime

  val patientID: String
  val patientFirstName: String
  val patientLastName: String

  val providerID: String
  val providerFirstName: String
  val providerLastName: String

  def toList: List[String]
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
 *  - cpt CPT (procedure code)
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
 *  - patientFirstName patient first name
 *  - patientLastName patient last name
 *  - providerID Provider ID
 *  - providerFirstName provider first name
 *  - providerLastName provider last name
 *  - dos Date of Service (Claim.date)
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
case class MedClaim(claimID: String,
  patientID: String, patientFirstName: String, patientLastName: String,
  providerID: String, providerFirstName: String, providerLastName: String,
  dos: DateTime, dosThru: DateTime,
  mHead: MHead = MHead(), mCodes: MCodes = MCodes(), mBill: MBill = MBill()) extends Claim {

  val claimType = "MD"
  def date = dos

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

  def toList: List[String] = List.concat(List("MD", claimID,
    patientID, patientFirstName, patientLastName, providerID, providerFirstName, providerLastName,
    dos.toLocalDate().toString, dosThru.toLocalDate().toString), mHead.toList, mCodes.toList, mBill.toList)

  /**
   * @param dia set of diasgnotics to check against
   * @returns true if this claim has any of the diagnostics passed as argument
   */
  def hasDiagnostic(dia: Set[String]): Boolean = {
    if (dia.isEmpty) false
    else {
      val d = dia.head
      if (mCodes.icdDPri == d || mCodes.icdD.contains(d)) true
      else hasDiagnostic(dia.tail)
    }
  }
}

/**
 * RxClaim class
 *
 * The attributes are:
 * 	- claimID Claim ID
 *  - patientID Patient ID
 *  - patientFirstName patient first name
 *  - patientLastName patient last name
 *  - providerID Provider ID
 *  - providerFirstName provider first name
 *  - providerLastName provider last name
 *  - fillD Rx fill date (Claim.date)
 *  - claimStatus Claim status:
 *    - "A" Adjustment to original claim
 *    - "D" Denied claims
 *    - "I" Initial Paid Claim
 *    - "P" Pended for adjudication
 *    - "R" Reversal to original claim
 *  - ndc drug NDC
 *  - daysSupply Nbr of days supply: Int >= 1
 *  - qty Qty quantity dispensed: Int >= 0
 *  - supplyF: Supply Flag, "Y" if DME rather than drugs
 */
case class RxClaim(claimID: String,
  patientID: String, patientFirstName: String, patientLastName: String,
  providerID: String, providerFirstName: String, providerLastName: String,
  fillD: DateTime, claimStatus: String = "",
  ndc: String = "", daysSupply: Int = 1, qty: Int = 0, supplyF: String = "N") extends Claim {

  val claimType = "RX"
  def date = fillD

  def toList: List[String] = List("RX", claimID,
    patientID, patientFirstName, patientLastName, providerID, providerFirstName, providerLastName,
    fillD.toLocalDate().toString, claimStatus, ndc, daysSupply.toString(), qty.toString(), supplyF)
}

/**
 * LabClaim class
 *
 * The attributes are:
 * 	- claimID Claim ID
 *  - patientID Patient ID
 *  - patientFirstName patient first name
 *  - patientLastName patient last name
 *  - providerID Provider ID
 *  - providerFirstName provider first name
 *  - providerLastName provider last name
 *  - dos date of service (Claim.date)
 *  - claimStatus Claim status:
 *    - "A" Adjustment to original claim
 *    - "D" Denied claims
 *    - "I" Initial Paid Claim
 *    - "P" Pended for adjudication
 *    - "R" Reversal to original claim
 *  - cpt CPT (procedure procedure)
 *  - loinc LOINC code
 *  - result test result: BigDecimal
 *  - posNegResult: for binary result (as opposed to numeric) "1" is positive, "0" is negative, "" for N/A (numeric)
 */
case class LabClaim(claimID: String,
  patientID: String, patientFirstName: String, patientLastName: String,
  providerID: String, providerFirstName: String, providerLastName: String,
  dos: DateTime, claimStatus: String = "",
  cpt: String = "", loinc: String = "", result: BigDecimal = 0.0, posNegResult: String = "") extends Claim {

  val claimType = "LC"
  def date = dos

  def toList: List[String] = List("LC", claimID,
    patientID, patientFirstName, patientLastName, providerID, providerFirstName, providerLastName,
    dos.toLocalDate().toString, claimStatus, cpt, loinc, result.toString(), posNegResult)
}
