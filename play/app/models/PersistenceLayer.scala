/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package models

import org.joda.time.DateTime

/**
 * Simple class to abstract the creation of \c Patients and \c Providers
 *
 * This is to allow different implementations to \c uuid creation
 */
trait PersistenceLayer {

  def createPatient(firstName: String, lastName: String, gender: String, dob: DateTime): Patient

  def createProvider(firstName: String, lastName: String): Provider

  def createMedClaim(
    //2 patientID Patient ID
    patientID: String,

    //3 providerID Provider ID
    providerID: String,

    //4 dos Date of Service
    dos: DateTime,

    //5 dosThru DOS Thru - same as DOS for single day service, same as discharge date for in-patient claim
    dosThru: DateTime,

    //6 claimStatus Claim status: 
    claimStatus: String = "",

    //7 pcpFlag PCP Flag - relationship of provider with health plan ("Y" / "N")
    pcpFlag: String = "",

    //8 icdDPri ICD Primary Diagnostic
    icdDPri: String = "",

    //9-18 icdD Secondary Diagnostic codes (up to 10)
    icdD: Set[String] = Set(),

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
    roomBoardFlag: String = "N"): MedClaim

    
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

  def createMedClaim(
    patientID: String,
    providerID: String,
    dos: DateTime,
    dosThru: DateTime,
    claimStatus: String,
    pcpFlag: String,
    icdDPri: String,
    icdD: Set[String],
    icdP: Set[String],
    hcfaPOS: String,
    drg: String,
    tob: String,
    ubRevenue: String,
    cpt: String,
    cptMod1: String,
    cptMod2: String,
    hcpcs: String,
    hcpcsMod: String,
    dischargeStatus: String,
    daysDenied: Int,
    roomBoardFlag: String): MedClaim = {

    val k = nextClaimKey
    nextClaimKey = nextClaimKey + 1
    MedClaim(mdClaimKeyPrefix + k,
      patientID,
      providerID,
      dos,
      dosThru,
      claimStatus,
      pcpFlag,
      icdDPri,
      icdD,
      icdP,
      hcfaPOS,
      drg,
      tob,
      ubRevenue,
      cpt,
      cptMod1,
      cptMod2,
      hcpcs,
      hcpcsMod,
      dischargeStatus,
      daysDenied,
      roomBoardFlag)
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
