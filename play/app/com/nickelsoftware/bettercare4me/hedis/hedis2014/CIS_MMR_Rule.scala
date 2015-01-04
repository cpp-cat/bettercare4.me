/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package com.nickelsoftware.bettercare4me.hedis.hedis2014

import scala.util.Random

import org.joda.time.DateTime
import org.joda.time.Interval

import com.nickelsoftware.bettercare4me.hedis.HEDISRule
import com.nickelsoftware.bettercare4me.hedis.Scorecard
import com.nickelsoftware.bettercare4me.models.Claim
import com.nickelsoftware.bettercare4me.models.LabClaim
import com.nickelsoftware.bettercare4me.models.MedClaim
import com.nickelsoftware.bettercare4me.models.Patient
import com.nickelsoftware.bettercare4me.models.PatientHistory
import com.nickelsoftware.bettercare4me.models.PersistenceLayer
import com.nickelsoftware.bettercare4me.models.Provider
import com.nickelsoftware.bettercare4me.models.RuleConfig
import com.nickelsoftware.bettercare4me.utils.Utils

object CIS_MMR {

  val name = "CIS-MMR-C-HEDIS-2014"

  val MMRVaccine = "Measles/Mumps/Rubella Vaccine"

  /**
   * CPT codes for Measles vaccination
   */
  val cptA = List("90705", "90707", "90708", "90710")
  val cptAS = cptA.toSet

  /**
   * ICD P codes for Measles vaccination
   */
  val icdPA = List("99.45", "99.48")
  val icdPAS = icdPA.toSet

  /**
   * ICD D codes for Measles vaccination
   */
  val icdDA = List("055*")
  val icdDAS = icdDA.toSet

  /**
   * CPT codes for Mumps vaccination
   */
  val cptB = List("90704", "90707", "90710")
  val cptBS = cptB.toSet

  /**
   * ICD P codes for Mumps vaccination
   */
  val icdPB = List("99.46", "99.48")
  val icdPBS = icdPB.toSet

  /**
   * ICD D codes for Mumps vaccination
   */
  val icdDB = List("072*")
  val icdDBS = icdDB.toSet

  /**
   * CPT codes for Rubella vaccination
   */
  val cptC = List("90706", "90707", "90708", "90710")
  val cptCS = cptC.toSet

  /**
   * ICD P codes for Rubella vaccination
   */
  val icdPC = List("99.47", "99.48")
  val icdPCS = icdPC.toSet

  /**
   * ICD D codes for Rubella vaccination
   */
  val icdDC = List("056*")
  val icdDCS = icdDC.toSet
}

/**
 * Measles-Mumps-Rubella (MMR) Vaccine
 *
 * Measles-Mumps-Rubella (MMR) Vaccine indicates whether a child, who turned 2 years old during the measurement year,
 * received one measles/mumps/rubella (MMR) vaccination. This excludes children who had a previous adverse reaction to a
 * vaccine, as well as those with a vaccine contraindication such as immunodeficiency syndrome, HIV, lymphoreticular or histiocytic
 * tissue cancer, multiple myeloma, or leukemia.
 *
 * NUMERATOR:
 * Identifies children who turned 2 years old during the measurement year and received at least one measles/mumps/rubella (MMR)
* vaccination (or separate measles, mumps, and rubella vaccinations) occurring on or before the child's 2nd birthday. Evidence of
* all 3 antigens or the combination vaccine, a documented history of the illness, or a seropositive test result are all counted in the
* numerator.
 *
 */
class CIS_MMR_Rule(config: RuleConfig, hedisDate: DateTime) extends CIS_RuleBase(config, hedisDate) {

  val name = CIS_MMR.name
  val fullName = "Measles-Mumps-Rubella (MMR) Vaccine"
  val description = "Measles-Mumps-Rubella (MMR) Vaccine indicates whether a child, who turned 2 years old during the measurement year, " +
    "received one measles/mumps/rubella (MMR) vaccination. This excludes children who had a previous adverse reaction to a " +
    "vaccine, as well as those with a vaccine contraindication such as immunodeficiency syndrome, HIV, lymphoreticular or histiocytic " +
    "tissue cancer, multiple myeloma, or leukemia."

  import CIS_MMR._
  override def generateMeetMeasureClaims(pl: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = {

    // after 42 days after birth and before 2 years of age
    val days = Utils.daysBetween(patient.dob.plusDays(42), patient.dob.plusMonths(20))
    val dos1 = patient.dob.plusDays(42 + Random.nextInt(days))
    val dos2 = dos1.plusDays(30)
    val dos3 = dos2.plusDays(30)

    // received at least one measles/mumps/rubella (MMR) vaccination (or separate measles, mumps, and rubella vaccinations)
    List(
        pickOne(List(
            () => pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos1, dos1, cpt = pickOne(cptA)),
            () => pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos1, dos1, icdP = Set(pickOne(icdPA))),
            () => pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos1, dos1, icdDPri = pickOne(icdDA))
            ) ) (),
        pickOne(List(
            () => pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos2, dos2, cpt = pickOne(cptB)),
            () => pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos2, dos2, icdP = Set(pickOne(icdPB))),
            () => pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos2, dos2, icdDPri = pickOne(icdDB))
            ))(),
        pickOne(List(
            () => pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos3, dos3, cpt = pickOne(cptC)),
            () => pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos3, dos3, icdP = Set(pickOne(icdPC))),
            () => pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos3, dos3, icdDPri = pickOne(icdDC))
            ))() )
  }

  override def scorePatientMeetMeasure(scorecard: Scorecard, patient: Patient, ph: PatientHistory): Scorecard = {

    // after 42 days after birth and before 2 years of age
    val measurementInterval = new Interval(patient.dob.plusDays(42), patient.dob.plusMonths(24).plusDays(1))

    def rules = List[(Scorecard) => Scorecard](

      // Check for patient has CPT
      (s: Scorecard) => {
        val claims1A = filterClaims(ph.cpt, cptAS, { claim: MedClaim => measurementInterval.contains(claim.dos) })
        val claims2A = filterClaims(ph.icdP, icdPAS, { claim: MedClaim => measurementInterval.contains(claim.dos) })
        val claims3A = filterClaims(ph.icdD, icdDAS, { claim: MedClaim => measurementInterval.contains(claim.dos) })
        val claimsA = List.concat(claims1A, claims2A, claims3A)

        val claims1B = filterClaims(ph.cpt, cptBS, { claim: MedClaim => measurementInterval.contains(claim.dos) })
        val claims2B = filterClaims(ph.icdP, icdPBS, { claim: MedClaim => measurementInterval.contains(claim.dos) })
        val claims3B = filterClaims(ph.icdD, icdDBS, { claim: MedClaim => measurementInterval.contains(claim.dos) })
        val claimsB = List.concat(claims1B, claims2B, claims3B)

        val claims1C = filterClaims(ph.cpt, cptCS, { claim: MedClaim => measurementInterval.contains(claim.dos) })
        val claims2C = filterClaims(ph.icdP, icdPCS, { claim: MedClaim => measurementInterval.contains(claim.dos) })
        val claims3C = filterClaims(ph.icdD, icdDCS, { claim: MedClaim => measurementInterval.contains(claim.dos) })
        val claimsC = List.concat(claims1C, claims2C, claims3C)

        // need to have all 3 vaccines
        if (claimsA.isEmpty || claimsB.isEmpty || claimsC.isEmpty) s
        else s.addScore(name, fullName, HEDISRule.meetMeasure, MMRVaccine, List.concat(claimsA, claimsB, claimsC))
      })

    applyRules(scorecard, rules)
  }
}
