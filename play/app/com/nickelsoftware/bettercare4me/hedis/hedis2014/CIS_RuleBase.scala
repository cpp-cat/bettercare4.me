/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package com.nickelsoftware.bettercare4me.hedis.hedis2014

import scala.util.Random

import org.joda.time.DateTime
import org.joda.time.Interval

import com.nickelsoftware.bettercare4me.hedis.HEDISRule
import com.nickelsoftware.bettercare4me.hedis.HEDISRuleBase
import com.nickelsoftware.bettercare4me.hedis.Scorecard
import com.nickelsoftware.bettercare4me.models.Claim
import com.nickelsoftware.bettercare4me.models.MedClaim
import com.nickelsoftware.bettercare4me.models.Patient
import com.nickelsoftware.bettercare4me.models.PatientHistory
import com.nickelsoftware.bettercare4me.models.PersistenceLayer
import com.nickelsoftware.bettercare4me.models.Provider
import com.nickelsoftware.bettercare4me.models.RuleConfig
import com.nickelsoftware.bettercare4me.utils.Utils

object CIS {

  val anaphylactic = "Anaphylactic"
  val encephalopathy = "Encephalopathy"
  val immunodeficiency = "Immunodeficiency"

  // exclusions - Anaphylactic reaction to the vaccine or its components (ICD D)
  val icdDA = List("999.4", "999.42")
  val icdDAS = icdDA.toSet

  // exclusions - Encephalopathy (ICD D)
  val icdDB = List("323.51")
  val icdDBS = icdDB.toSet

  // exclusions - Encephalopathy (ICD D)
  val icdDC = List("E948.4", "E948.5", "E948.6")
  val icdDCS = icdDC.toSet

  // exclusion - Immunodeficiency syndromes, HIV disease,
  //		asymptomatic HIV, cancer of lymphoreticular or
  //		histiocytic tissue, multiple myeloma, or leukemia (ICD D)
  //@TODO expand codes
  val icdDD = List("042", "200.00-208.91", "279*", "V08")
  val icdDDS = icdDD.toSet
}
/**
 * Childhood Immunization Status Base Rule
 *
 * EXCLUSIONS:
 * Excludes from the eligible population all children who had a previous adverse reaction to a vaccine, as well as those with a
 * vaccine contraindication such as immunodeficiency syndrome, HIV, lymphoreticular or histiocytic tissue cancer, multiple
 * myeloma, or leukemia. Children who have a contraindication for one vaccine are excluded from the denominator for all vaccine
 * rates and combination rates, since the denominator for all rates must be the same. Contraindications are checked as far back as
 * possible in the patient's history, but must have occurred by their 2nd birthday.
 *
 */
abstract class CIS_RuleBase(config: RuleConfig, hedisDate: DateTime) extends HEDISRuleBase(config, hedisDate) {

  import CIS._

  override def isPatientMeetDemographic(patient: Patient): Boolean = {
    val age = patient.age(hedisDate)
    age == 2
  }

  // This rule has 100% eligibility when the demographics are meet
  override val eligibleRate: Int = 100

  override def generateExclusionClaims(pl: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = {

    val days = Utils.daysBetween(hedisDate.minusMonths(20), hedisDate)
    val dos = hedisDate.minusDays(Random.nextInt(days))

    pickOne(List(

      // exclusions - Anaphylactic reaction to the vaccine or its components (ICD D)
      () => List(pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos, dos, icdDPri = pickOne(icdDA))),

      // exclusions - Encephalopathy (ICD D)
      () => List(pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos, dos, icdDPri = pickOne(icdDB), icdD = Set(pickOne(icdDC)))),

      // exclusion - Immunodeficiency syndromes, HIV disease. . . (ICD D)
      () => List(pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos, dos, icdDPri = pickOne(icdDD)))))()
  }

  override def scorePatientExcluded(scorecard: Scorecard, patient: Patient, ph: PatientHistory): Scorecard = {

    def rules = List[(Scorecard) => Scorecard](

      // exclusions - Anaphylactic reaction to the vaccine or its components (ICD D)
      (s: Scorecard) => {
        val claims = filterClaims(ph.icdD, icdDAS, { claim: MedClaim => !claim.dos.isAfter(hedisDate) })
        s.addScore(name, HEDISRule.excluded, anaphylactic, claims)
      },

      // exclusions - Encephalopathy (ICD D)
      (s: Scorecard) => {
        val claims = filterClaims(ph.icdD, icdDBS, { claim: MedClaim => claim.hasDiagnostic(icdDCS) && !claim.dos.isAfter(hedisDate) })
        s.addScore(name, HEDISRule.excluded, encephalopathy, claims)
      },

      // exclusion - Immunodeficiency syndromes, HIV disease. . . (ICD D)
      (s: Scorecard) => {
        val claims = filterClaims(ph.icdD, icdDDS, { claim: MedClaim => !claim.dos.isAfter(hedisDate) })
        s.addScore(name, HEDISRule.excluded, immunodeficiency, claims)
      })

    applyRules(scorecard, rules)
  }
}
