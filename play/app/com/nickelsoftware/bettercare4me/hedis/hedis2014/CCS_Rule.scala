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

object CCS {

  val name = "CCS-HEDIS-2014"
    
  val hysterectomy = "Hysterectomy"
  val papTest = "PAP Test"

  // exclusions - Previous hysterectomy with no residual cervix (CPT)
  val cptA = List("51925", "56308", "57540", "57545", "57550", "57555", "57556", "58150", "58152", "58200", "58210", "58240", "58260", "58262", "58263", "58267", "58270", "58275", "58280", "58285", "58290", "58571", "58572", "58573", "58294", "58548", "58550", "58571", "58572", "58573", "58554", "58570", "58571", "58572", "58573", "58951", "58953", "58954", "58956", "59135")
  val cptAS = cptA.toSet

  // exclusion - Previous hysterectomy with no residual cervix (ICD P)
  val icdPA = List("68.4", "68.41", "68.49", "68.5", "68.51", "68.59", "68.6", "68.61", "68.69", "68.7", "68.71", "68.79", "68.8")
  val icdPAS = icdPA.toSet

  // exclusion - Previous hysterectomy with no residual cervix (ICD D)
  val icdDA = List("618.5", "752.43", "V67.01", "V76.47", "V88.01", "V88.03")
  val icdDAS = icdDA.toSet

  // meet criteria - At least one Pap test (CPT)
  val cptB = List("88141", "88142", "88143", "88147", "88148", "88150", "88152", "88153", "88154", "88155", "88164", "88165", "88166", "88167", "88174", "88175")
  val cptBS = cptB.toSet

  // meet criteria - At least one Pap test (HCPCS)
  val hcpcsA = List("G0123", "G0124", "G0141", "G0143", "G0144", "G0145", "G0147", "G0148", "P3000", "P3001", "Q0091")
  val hcpcsAS = hcpcsA.toSet

  // meet criteria - At least one Pap test (ICD P)
  val icdPB = List("91.46")
  val icdPBS = icdPB.toSet

  // meet criteria - At least one Pap test (UB)
  val ubA = List("0923")
  val ubAS = ubA.toSet
}
/**
 * Cervical Cancer Screen Rule
 *
 * Cervical Cancer Screen indicates whether a women, aged 24 to 64 years, had a Pap test done during the measurement year or
 * the 2 years prior. This excludes women who had a previous hysterectomy with no residual cervix.
 *
 * DENOMINATOR:
 * Identifies women aged 24 to 64 years. Because this measure looks back 36 months for a Pap test, the eligible population
 * includes women who could have been 21 years of age at the time of the test. This excludes women who had a hysterectomy
 * with no residual cervix.
 *
 * EXCLUSIONS:
 * Excludes from the eligible population those women who had a previous hysterectomy with no residual cervix (based on claims
 * included in the database). Note: NCQA specifies that the Pap test exclusion criteria should only be applied if a woman has not
 * had a Pap test performed.
 *
 * NUMERATOR:
 * Identifies women, aged 24 to 64 years, who had a Pap test done during the measurement year or the 2 years prior.
 *
 */
class CCS_Rule(config: RuleConfig, hedisDate: DateTime) extends HEDISRuleBase(config, hedisDate) {

  val name = BCS.name
  val fullName = "Cervical Cancer Screen"
  val description = "Cervical Cancer Screen indicates whether a women, aged 24 to 64 years, had a Pap test done during the measurement year or the 2 years prior. This excludes women who had a previous hysterectomy with no residual cervix."

  override def isPatientMeetDemographic(patient: Patient): Boolean = {
    val age = patient.age(hedisDate)
    patient.gender == "F" && age >= 24 && age <= 64
  }

  // This rule has 100% eligibility when the demographics are meet
  override val eligibleRate: Int = 100
  import CCS._

  override def generateExclusionClaims(pl: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = {

    val days = Utils.daysBetween(hedisDate.minusYears(15), hedisDate)
    val dos = hedisDate.minusDays(Random.nextInt(days))
    pickOne(List(

      // Previous hysterectomy with no residual cervix (CPT)
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos, dos, cpt = pickOne(cptA))),

      // Previous hysterectomy with no residual cervix (ICD D)
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos, dos, icdDPri = pickOne(icdDA))),

      // Previous hysterectomy with no residual cervix (ICD P)
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos, dos, icdP = Set(pickOne(icdPA))))))()
  }

  override def scorePatientExcluded(scorecard: Scorecard, patient: Patient, ph: PatientHistory): Scorecard = {

    def rules = List[(Scorecard) => Scorecard](

      // Previous hysterectomy with no residual cervix (CPT)
      (s: Scorecard) => {
        val claims = filterClaims(ph.cpt, cptAS, { claim: MedClaim => !claim.dos.isAfter(hedisDate) })
        s.addScore(name, HEDISRule.excluded, hysterectomy, claims)
      },

      // Previous hysterectomy with no residual cervix (ICD D)
      (s: Scorecard) => {
        val claims = filterClaims(ph.icdD, icdDAS, { claim: MedClaim => !claim.dos.isAfter(hedisDate) })
        s.addScore(name, HEDISRule.excluded, hysterectomy, claims)
      },

      // Previous hysterectomy with no residual cervix (ICD P)
      (s: Scorecard) => {
        val claims = filterClaims(ph.icdP, icdPAS, { claim: MedClaim => !claim.dos.isAfter(hedisDate) })
        s.addScore(name, HEDISRule.excluded, hysterectomy, claims)
      })

    applyRules(scorecard, rules)
  }

  override def generateMeetMeasureClaims(pl: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = {

    val days = Utils.daysBetween(hedisDate.minusYears(2), hedisDate)
    val dos = hedisDate.minusDays(Random.nextInt(days)).minusDays(180)
    pickOne(List(

      // One possible set of claims based on cpt
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos, dos, cpt = pickOne(cptB))),

      // Another possible set of claims based on hcpcs
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos, dos, hcpcs = pickOne(hcpcsA))),

      // Another possible set of claims based on ICD Procedure codes
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos, dos, icdP = Set(pickOne(icdPB)))),

      // Another possible set of claims based on UB Revenue Procedure codes
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos, dos, ubRevenue = pickOne(ubA)))))()
  }

  override def scorePatientMeetMeasure(scorecard: Scorecard, patient: Patient, ph: PatientHistory): Scorecard = {

    val measurementInterval = new Interval(hedisDate.minusYears(3), hedisDate)

    def rules = List[(Scorecard) => Scorecard](

      // Check if patient had Bilateral Mastectomy (anytime prior to or during the measurement year)
      (s: Scorecard) => {
        val claims = filterClaims(ph.cpt, cptBS, { claim: MedClaim => measurementInterval.contains(claim.dos) })
        s.addScore(name, HEDISRule.meetMeasure, papTest, claims)
      },

      (s: Scorecard) => {
        val claims = filterClaims(ph.hcpcs, hcpcsAS, { claim: MedClaim => measurementInterval.contains(claim.dos) })
        s.addScore(name, HEDISRule.meetMeasure, papTest, claims)
      },

      (s: Scorecard) => {
        val claims = filterClaims(ph.icdP, icdPBS, { claim: MedClaim => measurementInterval.contains(claim.dos) })
        s.addScore(name, HEDISRule.meetMeasure, papTest, claims)
      },

      (s: Scorecard) => {
        val claims = filterClaims(ph.ubRevenue, ubAS, { claim: MedClaim => measurementInterval.contains(claim.dos) })
        s.addScore(name, HEDISRule.meetMeasure, papTest, claims)
      })

    applyRules(scorecard, rules)
  }
}
