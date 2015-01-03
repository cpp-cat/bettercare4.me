/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package com.nickelsoftware.bettercare4me.models

import scala.math.BigDecimal.double2bigDecimal

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
   */
  def createMedClaim(
    patientID: String, patientFirstName: String, patientLastName: String,
    providerID: String, providerFirstName: String, providerLastName: String,
    dos: DateTime, dosThru: DateTime,
    claimStatus: String = "", pcpFlag: String = "", specialtyCde: String = "", hcfaPOS: String = "",
    dischargeStatus: String = "", daysDenied: Int = 0, roomBoardFlag: String = "N",
    icdDPri: String = "", icdD: Set[String] = Set(), icdP: Set[String] = Set(), drg: String = "",
    cpt: String = "", cptMod1: String = "", cptMod2: String = "",
    tob: String = "", ubRevenue: String = "", hcpcs: String = "", hcpcsMod: String = ""): MedClaim

  /**
   * create Rx Claim
   */
  def createRxClaim(
    patientID: String, patientFirstName: String, patientLastName: String,
    providerID: String, providerFirstName: String, providerLastName: String,
    fillD: DateTime, claimStatus: String = "",
    ndc: String = "", daysSupply: Int = 1, qty: Int = 0, supplyF: String = "N"): RxClaim

  /**
   * create Lab Claim
   */
  def createLabClaim(
    patientID: String, patientFirstName: String, patientLastName: String,
    providerID: String, providerFirstName: String, providerLastName: String,
    dos: DateTime, claimStatus: String = "",
    cpt: String = "", loinc: String = "", result: Double = 0.0, posNegResult: String = ""): LabClaim
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
    patientID: String, patientFirstName: String, patientLastName: String,
    providerID: String, providerFirstName: String, providerLastName: String,
    dos: DateTime, dosThru: DateTime,
    claimStatus: String, pcpFlag: String, specialtyCde: String, hcfaPOS: String,
    dischargeStatus: String, daysDenied: Int, roomBoardFlag: String,
    icdDPri: String, icdD: Set[String], icdP: Set[String], drg: String,
    cpt: String, cptMod1: String, cptMod2: String,
    tob: String, ubRevenue: String, hcpcs: String, hcpcsMod: String): MedClaim = {

    val k = nextClaimKey
    nextClaimKey = nextClaimKey + 1
    MedClaim(mdClaimKeyPrefix + k,
      patientID, patientFirstName, patientLastName, providerID, providerFirstName, providerLastName,
      dos, dosThru,
      MHead(claimStatus, pcpFlag, specialtyCde, hcfaPOS, dischargeStatus, daysDenied, roomBoardFlag),
      MCodes(icdDPri, icdD, icdP, drg, cpt, cptMod1, cptMod2),
      MBill(tob, ubRevenue, hcpcs, hcpcsMod))
  }

  def createRxClaim(
    patientID: String, patientFirstName: String, patientLastName: String,
    providerID: String, providerFirstName: String, providerLastName: String,
    fillD: DateTime,
    claimStatus: String,
    ndc: String,
    daysSupply: Int,
    qty: Int,
    supplyF: String): RxClaim = {

    val k = nextClaimKey
    nextClaimKey = nextClaimKey + 1
    RxClaim(rxClaimKeyPrefix + k,
      patientID, patientFirstName, patientLastName, providerID, providerFirstName, providerLastName,
      fillD,
      claimStatus,
      ndc,
      daysSupply,
      qty,
      supplyF)
  }

  def createLabClaim(
    patientID: String, patientFirstName: String, patientLastName: String,
    providerID: String, providerFirstName: String, providerLastName: String,
    dos: DateTime,
    claimStatus: String,
    cpt: String,
    loinc: String,
    result: Double,
    posNegResult: String): LabClaim = {

    val k = nextClaimKey
    nextClaimKey = nextClaimKey + 1
    LabClaim(lcClaimKeyPrefix + k,
      patientID, patientFirstName, patientLastName, providerID, providerFirstName, providerLastName,
      dos,
      claimStatus,
      cpt,
      loinc,
      result,
      posNegResult)
  }
}
