/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package com.nickelsoftware.bettercare4me.models

import org.joda.time.DateTime

/**
 * Simple class to abstract the creation of \c Patients and \c Providers
 *
 * This is to allow different implementations to \c uuid creation
 */
trait PersistenceLayer {

  def createPatient(firstName: String, lastName: String, gender: String, dob: DateTime): Patient

  def createProvider(firstName: String, lastName: String): Provider

  /**
   * create Medical Claim
   *
   * The params are:
   *  - patientID Patient ID
   *  - providerID Provider ID
   *  - dos Date of Service
   *  - dosThru DOS Thru - same as DOS for single day service, same as discharge date for in-patient claim
   *  - claimStatus Claim status
   *  - pcpFlag PCP Flag - relationship of provider with health plan ("Y" / "N")
   *  - specialtyCde - NUCC Provider Specialty
   *  - icdDPri ICD Primary Diagnostic
   *  - icdD Secondary Diagnostic codes (up to 10)
   *  - icdP ICD Procedure codes (up to 10)
   *  - drg Diagnosis Related Group
   *  - cpt CPT (procedure procedure)
   *  - cptMod1 CPT Modifier 1 (2 chars)
   *  - cptMod2 CPT Modifier 2 (2 chars)
   *  - tob Type of Bill (3 chars)
   *  - ubRevenue UB Revenue (billing code)
   *  - hcpcs HCPCS (medical goods and services)
   *  - hcpcsMod HCPCS Modifier code (2 chars)
   */
  def createMedClaim(patientID: String, providerID: String, dos: DateTime, dosThru: DateTime,
    claimStatus: String = "", pcpFlag: String = "", specialtyCde: String = "", hcfaPOS: String = "",
    dischargeStatus: String = "", daysDenied: Int = 0, roomBoardFlag: String = "N",
    icdDPri: String = "", icdD: Set[String] = Set(), icdP: Set[String] = Set(), drg: String = "",
    cpt: String = "", cptMod1: String = "", cptMod2: String = "",
    tob: String = "", ubRevenue: String = "", hcpcs: String = "", hcpcsMod: String = ""): MedClaim

  def createRxClaim(

    //2 patientID Patient ID
    patientID: String,

    //3 providerID Provider ID
    providerID: String,

    //4 fillD: fill date
    fillD: DateTime,

    //5 claimStatus Claim status: 
    claimStatus: String = "",

    //6 NDC drug NDC
    ndc: String = "",

    //7 daysSupply Nbr of days supply
    daysSupply: Int = 1,

    //8 Qty quantity dispensed
    qty: Int = 0,

    //9 Supply Flag, "Y" if DME rather than drugs
    supplyF: String = "N"): RxClaim

  def createLabClaim(

    //2 patientID Patient ID
    patientID: String,

    //3 providerID Provider ID
    providerID: String,

    //4 dos Date of Service
    dos: DateTime,

    //5 claimStatus Claim status: 
    claimStatus: String = "",

    //6 cpt CPT (procedure procedure)
    cpt: String = "",

    //7 LOINC
    loinc: String = "",

    //8 result: numeric (Double)
    result: Double = 0.0,

    //9 PosNegResult for binary result (as opposed to numeric) "1" is positive, "0" is negative, "" for N/A (numeric)
    posNegResult: String = ""): LabClaim
}

class SimplePersistenceLayer(keyGen: Int) extends PersistenceLayer {
  val patientKeyPrefix = "patient-" + keyGen + "-"
  val providerKeyPrefix = "provider-" + keyGen + "-"
  val mdClaimKeyPrefix = "c-md-" + keyGen + "-"
  val rxClaimKeyPrefix = "c-rx-" + keyGen + "-"
  val lcClaimKeyPrefix = "c-lc-" + keyGen + "-"

  var nextPatientKey = 0
  var nextProviderKey = 0
  var nextClaimKey = 0

  def createPatient(firstName: String, lastName: String, gender: String, dob: DateTime): Patient = {
    val k = nextPatientKey
    nextPatientKey = nextPatientKey + 1
    Patient(patientKeyPrefix + k, firstName, lastName, gender, dob)
  }

  def createProvider(firstName: String, lastName: String): Provider = {
    val k = nextProviderKey
    nextProviderKey = nextProviderKey + 1
    Provider(providerKeyPrefix + k, firstName, lastName)
  }

  def createMedClaim(patientID: String, providerID: String, dos: DateTime, dosThru: DateTime,
    claimStatus: String, pcpFlag: String, specialtyCde: String, hcfaPOS: String,
    dischargeStatus: String, daysDenied: Int, roomBoardFlag: String,
    icdDPri: String, icdD: Set[String], icdP: Set[String], drg: String,
    cpt: String, cptMod1: String, cptMod2: String,
    tob: String, ubRevenue: String, hcpcs: String, hcpcsMod: String): MedClaim = {

    val k = nextClaimKey
    nextClaimKey = nextClaimKey + 1
    MedClaim(mdClaimKeyPrefix + k, patientID, providerID, dos, dosThru,
        MHead(claimStatus, pcpFlag, specialtyCde, hcfaPOS, dischargeStatus, daysDenied, roomBoardFlag),
        MCodes(icdDPri, icdD, icdP, drg, cpt, cptMod1, cptMod2),
        MBill(tob, ubRevenue, hcpcs, hcpcsMod))
  }

  def createRxClaim(
    patientID: String,
    providerID: String,
    fillD: DateTime,
    claimStatus: String,
    ndc: String,
    daysSupply: Int,
    qty: Int,
    supplyF: String): RxClaim = {

    val k = nextClaimKey
    nextClaimKey = nextClaimKey + 1
    RxClaim(rxClaimKeyPrefix + k,
      patientID,
      providerID,
      fillD,
      claimStatus,
      ndc,
      daysSupply,
      qty,
      supplyF)
  }

  def createLabClaim(
    patientID: String,
    providerID: String,
    dos: DateTime,
    claimStatus: String,
    cpt: String,
    loinc: String,
    result: Double,
    posNegResult: String): LabClaim = {

    val k = nextClaimKey
    nextClaimKey = nextClaimKey + 1
    LabClaim(lcClaimKeyPrefix + k,
      patientID,
      providerID,
      dos,
      claimStatus,
      cpt,
      loinc,
      result,
      posNegResult)
  }
}
