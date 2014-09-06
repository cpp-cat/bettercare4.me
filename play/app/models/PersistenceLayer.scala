/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package models

import org.joda.time.LocalDate

/**
 * Simple class to abstract the creation of \c Patients and \c Providers
 *
 * This is to allow different implementations to \c uuid creation
 */
trait PersistenceLayer {

  def createPatient(firstName: String, lastName: String, gender: String, dob: LocalDate): Patient

  def createProvider(firstName: String, lastName: String): Provider

  def createClaim(patientUuid: String, providerUuid: String, dos: LocalDate,
    icd_d_pri: String, icd_d: Set[String], icd_p: Set[String],
    hcfaPOS: String, ubRevenue: String, cpt: String, hcpcs: String): Claim
}

class SimplePersistenceLayer(keyGen: Int) extends PersistenceLayer {
  val patientKeyPrefix = "patient-" + keyGen + "-"
  val providerKeyPrefix = "provider-" + keyGen + "-"
  val claimKeyPrefix = "claim-" + keyGen + "-"

  var nextPatientKey = 0
  var nextProviderKey = 0
  var nextClaimKey = 0

  def createPatient(firstName: String, lastName: String, gender: String, dob: LocalDate): Patient = {
    val k = nextPatientKey
    nextPatientKey = nextPatientKey + 1
    Patient(patientKeyPrefix + k, firstName, lastName, gender, dob)
  }

  def createProvider(firstName: String, lastName: String): Provider = {
    val k = nextProviderKey
    nextProviderKey = nextProviderKey + 1
    Provider(providerKeyPrefix + k, firstName, lastName)
  }

  def createClaim(patientUuid: String, providerUuid: String, dos: LocalDate,
    icd_d_pri: String, icd_d: Set[String], icd_p: Set[String],
    hcfaPOS: String, ubRevenue: String, cpt: String, hcpcs: String): Claim = {
    val k = nextClaimKey
    nextClaimKey = nextClaimKey + 1
    Claim(claimKeyPrefix + k, patientUuid, providerUuid, dos, icd_d_pri, icd_d, icd_p, hcfaPOS, ubRevenue, cpt, hcpcs)
  }
}
