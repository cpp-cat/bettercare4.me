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

object CIS_VZV {

  val name = "CIS-VZV-C-HEDIS-2014"

  val chickenPoxVacc = "Chicken Pox Vaccination"
  val chickenPoxHist = "Chicken Pox History"

  /**
   * CPT codes for chicken pox vaccination
   */
  val cptA = List("90710", "90716")
  val cptAS = cptA.toSet

  /**
   * ICD Diagnosis codes for history of the disease
   */
  //@TODO expands codes
  val icdDA = List("052*", "053*")
  val icdDAS = icdDA.toSet
}

/**
 * Chicken Pox (VZV) Vaccine
 *
 * Chicken Pox (VZV) Vaccine indicates whether a child, who turned 2 years old during the measurement year, received one
 * chicken pox (varicella zoster virus) vaccination. This excludes children who had a previous adverse reaction to a vaccine, as well
 * as those with a vaccine contraindication such as immunodeficiency syndrome, HIV, lymphoreticular or histiocytic tissue cancer,
 * multiple myeloma, or leukemia.
 *
 * NUMERATOR:
 * Identifies children, who turned 2 years old during the measurement year, and received at least one chicken pox vaccination with
 * a date of service occurring on or before the child's 2nd birthday. Evidence of the antigen or vaccine, a documented history of the
 * illness, or a seropositive test result are counted in the numerator.
 *
 */
class CIS_VZV_Rule(config: RuleConfig, hedisDate: DateTime) extends CIS_RuleBase(config, hedisDate) {

  val name = CIS_VZV.name
  val fullName = "Chicken Pox (VZV) Vaccine"
  val description = "Chicken Pox (VZV) Vaccine indicates whether a child, who turned 2 years old during the measurement year, received one" +
    "chicken pox (varicella zoster virus) vaccination. This excludes children who had a previous adverse reaction to a vaccine, as well" +
    "as those with a vaccine contraindication such as immunodeficiency syndrome, HIV, lymphoreticular or histiocytic tissue cancer," +
    "multiple myeloma, or leukemia."

  import CIS_VZV._
  override def generateMeetMeasureClaims(pl: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = {

    // after 42 days after birth and before 2 years of age
    val days = Utils.daysBetween(patient.dob.plusDays(42), patient.dob.plusMonths(20))
    val dos = patient.dob.plusDays(42 + Random.nextInt(days))

    // At least one received at least one chicken pox vaccination
    pickOne(List(

      // Possible set: CPT
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos, dos, cpt = pickOne(cptA))),

      // Another possible set: ICD D
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos, dos, icdD = Set(pickOne(icdDA))))))()
  }

  override def scorePatientMeetMeasure(scorecard: Scorecard, patient: Patient, ph: PatientHistory): Scorecard = {

    // after 42 days after birth and before 2 years of age
    val measurementInterval = new Interval(patient.dob.plusDays(42), patient.dob.plusMonths(24).plusDays(1))

    def rules = List[(Scorecard) => Scorecard](

      // Check for patient has CPT
      (s: Scorecard) => {
        val claims = filterClaims(ph.cpt, cptAS, { claim: MedClaim => measurementInterval.contains(claim.dos) })
        s.addScore(name, HEDISRule.meetMeasure, chickenPoxVacc, claims)
      },

      // Check for ICD D
      (s: Scorecard) => {
        val claims = filterClaims(ph.icdD, icdDAS, { claim: MedClaim => measurementInterval.contains(claim.dos) })
        s.addScore(name, HEDISRule.meetMeasure, chickenPoxHist, claims)
      })

    applyRules(scorecard, rules)
  }
}
