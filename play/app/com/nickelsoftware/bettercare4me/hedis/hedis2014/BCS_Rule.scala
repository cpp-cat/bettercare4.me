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

object BCS {

  val name = "BCS-HEDIS-2014"

  val bilateralMastectomy = "Bilateral Mastectomy"

  val unilateralMastectomy50 = "Unilateral Mastectomy with bilateral modifier"
  val unilateralMastectomy2 = "Two Unilateral Mastectomy"
  val unilateralMastectomyLR = "LT and RT Unilateral Mastectomy"

  val mammogramCPT = "Mammogram (CPT)"
  val mammogramHCPCS = "Mammogram (HCPCS)"
  val mammogramICDP = "Mammogram (ICDP)"
  val mammogramUB = "Mammogram (UB)"

  // exclusions - unilateral mastectomy (must have modifier 50)
  val cptA = List("19180", "19200", "19220", "19240", "19303", "19304", "19305", "19306", "19307")
  val cptAS = cptA.toSet

  // exclusion - unilateral mastectomy (must have 2 on different dates)
  val icdPC = List("85.41", "85.43", "85.45", "85.47")
  val icdPCS = icdPC.toSet

  // exclusion - bilateral mastectomy
  val icdPA = List("85.42", "85.44", "85.46", "85.48")
  val icdPAS = icdPA.toSet

  // meet criteria - mammogram performed (according to bcbstx.com/mammography_2014.pdf and internet search)
  val cptB = List("77055", "77056", "77057")
  val cptBS = cptB.toSet

  // meet criteria - screening / diagnosis mammography
  val hcpcsA = List("G0202", "G0204", "G0206")
  val hcpcsAS = hcpcsA.toSet

  // meet criteria - screening / diagnosis mammography
  val icdPB = List("87.36", "87.37")
  val icdPBS = icdPB.toSet

  // meet criteria - screening / diagnosis mammography
  val ubA = List("0401", "0403")
  val ubAS = ubA.toSet
}
/**
 * Breast Cancer Screening Rule
 *
 * Breast Cancer Screening indicates whether a woman member, aged 42 to 69 years, had a mammogram done during the
 * measurement year or the year prior to the measurement year. This excludes women who had a bilateral mastectomy or two
 * unilateral mastectomies.
 *
 * DENOMINATOR:
 * Identifies women members, aged 42 to 69 years. Because this measure looks back 24 months for a mammogram, the eligible
 * population includes women who could have been 40 years of age at the time of the mammogram. This measure excludes
 * women who had a bilateral mastectomy or 2 unilateral mastectomies.
 *
 * EXCLUSIONS:
 * Excludes from the eligible population those women who had a bilateral mastectomy or a right and left unilateral mastectomy
 * anytime prior to or during the measurement year (based on claims included in the database). Note: NCQA specifies that the
 * mastectomy exclusion criteria should only be applied if a woman has not had a mammogram. This initial check for a
 * mammogram has not been implemented.
 *
 * NUMERATOR:
 * Identifies women members, aged 42 to 69 years, who had a mammogram done during the measurement year or the year prior to
 * the measurement year.
 *
 */
class BCSRule(config: RuleConfig, hedisDate: DateTime) extends HEDISRuleBase(config, hedisDate) {

  val name = BCS.name
  val fullName = "Breast Cancer Screening"
  val description = "Breast Cancer Screening indicates whether a woman member, aged 42 to 69 years, had a mammogram done during the " +
    "measurement year or the year prior to the measurement year. This excludes women who had a bilateral mastectomy or two " +
    "unilateral mastectomies."

  override def isPatientMeetDemographic(patient: Patient): Boolean = {
    val age = patient.age(hedisDate)
    patient.gender == "F" && age >= 42 && age <= 69
  }

  // This rule has 100% eligibility when the demographics are meet
  override val eligibleRate: Int = 100

  override def generateExclusionClaims(pl: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = {

    val days = new Interval(hedisDate.minusYears(2), hedisDate).toDuration().getStandardDays().toInt
    val dos1 = hedisDate.minusDays(Random.nextInt(days))
    val dos2 = dos1.minusDays(Random.nextInt(180) + 1) // to make sure it's not on the same day
    pickOne(List(

      // One possible set of claims based on ICD Procedure code
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos1, dos1, icdP = Set(pickOne(BCS.icdPA)))),

      // Another possible set of claims based on CPT codes and modifier being 50
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos1, dos1, cpt = pickOne(BCS.cptA), cptMod1 = "50")),
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos1, dos1, cpt = pickOne(BCS.cptA), cptMod2 = "50")),

      // Another possible set of claims based on 2 claims have CPT codes and each have one of the modifier RT and LT
      () => List(
        pl.createMedClaim(patient.patientID, provider.providerID, dos1, dos1, cpt = pickOne(BCS.cptA), cptMod1 = "RT"),
        pl.createMedClaim(patient.patientID, provider.providerID, dos2, dos2, cpt = pickOne(BCS.cptA), cptMod2 = "LT")),

      // Another possible set of claims based on 2 claims on different day with unilateral mastectomy using icdP
      () => List(
        pl.createMedClaim(patient.patientID, provider.providerID, dos1, dos1, icdP = Set(pickOne(BCS.icdPC))),
        pl.createMedClaim(patient.patientID, provider.providerID, dos2, dos2, icdP = Set(pickOne(BCS.icdPC))))))()
  }

  override def scorePatientExcluded(scorecard: Scorecard, patient: Patient, ph: PatientHistory): Scorecard = {

    def claimWithMod(claim: MedClaim, mod: String): Boolean = claim.dos.isBefore(hedisDate) && (claim.cptMod1 == mod || claim.cptMod2 == mod)

    def rules = List[(Scorecard) => Scorecard](

      // Check if patient had Bilateral Mastectomy (anytime prior to or during the measurement year)
      (s: Scorecard) => {
        val claims = filterClaims(ph.icdP, BCS.icdPAS, { claim: MedClaim => claim.dos.isBefore(hedisDate) })
        s.addScore(name, HEDISRule.excluded, BCS.bilateralMastectomy, claims)
      },

      // Check if patient had a Unilateral Mastectomy with bilateral modifier (anytime prior to or during the measurement year)
      (s: Scorecard) => {
        val claims = filterClaims(ph.cpt, BCS.cptAS, { claim: MedClaim => claimWithMod(claim, "50") })
        s.addScore(name, HEDISRule.excluded, BCS.unilateralMastectomy50, claims)
      },

      // Check if patient had a previous right unilateral mastectomy and a previous
      // left unilateral mastectomy (anytime prior to or during the measurement year)
      (s: Scorecard) => {
        val claimsRT = filterClaims(ph.cpt, BCS.cptAS, { claim: MedClaim => claimWithMod(claim, "RT") })
        val claimsLT = filterClaims(ph.cpt, BCS.cptAS, { claim: MedClaim => claimWithMod(claim, "LT") })
        if (claimsRT.isEmpty || claimsLT.isEmpty) s
        else s.addScore(name, HEDISRule.excluded, BCS.unilateralMastectomyLR, List.concat(claimsRT, claimsLT))
      },

      // Check if patient had 2 previous unilateral mastectomy on different days based on icdP (anytime prior to or during the measurement year)
      (s: Scorecard) => {
        val claims = filterClaims(ph.icdP, BCS.icdPCS, { claim: MedClaim => claim.dos.isBefore(hedisDate) })
        if (Claim.twoDifferentDOS(claims)) {
          s.addScore(name, HEDISRule.excluded, BCS.unilateralMastectomy2, claims)
        } else s
      })

    applyRules(scorecard, rules)
  }

  override def generateMeetMeasureClaims(pl: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = {

    val days = new Interval(hedisDate.minusYears(1), hedisDate).toDuration().getStandardDays().toInt
    val dos = hedisDate.minusDays(Random.nextInt(days)).minusDays(180)
    pickOne(List(

      // One possible set of claims based on cpt
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos, dos, cpt = pickOne(BCS.cptB))),

      // Another possible set of claims based on hcpcs
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos, dos, hcpcs = pickOne(BCS.hcpcsA))),

      // Another possible set of claims based on ICD Procedure codes
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos, dos, icdP = Set(pickOne(BCS.icdPB)))),

      // Another possible set of claims based on UB Revenue Procedure codes
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos, dos, ubRevenue = pickOne(BCS.ubA)))))()
  }

  override def scorePatientMeetMeasure(scorecard: Scorecard, patient: Patient, ph: PatientHistory): Scorecard = {

    val measurementInterval = new Interval(hedisDate.minusYears(2), hedisDate)

    def rules = List[(Scorecard) => Scorecard](

      // Check if patient had Bilateral Mastectomy (anytime prior to or during the measurement year)
      (s: Scorecard) => {
        val claims = filterClaims(ph.cpt, BCS.cptBS, { claim: MedClaim => measurementInterval.contains(claim.dos) })
        s.addScore(name, HEDISRule.meetMeasure, BCS.mammogramCPT, claims)
      },

      (s: Scorecard) => {
        val claims = filterClaims(ph.hcpcs, BCS.hcpcsAS, { claim: MedClaim => measurementInterval.contains(claim.dos) })
        s.addScore(name, HEDISRule.meetMeasure, BCS.mammogramHCPCS, claims)
      },

      (s: Scorecard) => {
        val claims = filterClaims(ph.icdP, BCS.icdPBS, { claim: MedClaim => measurementInterval.contains(claim.dos) })
        s.addScore(name, HEDISRule.meetMeasure, BCS.mammogramICDP, claims)
      },

      (s: Scorecard) => {
        val claims = filterClaims(ph.ubRevenue, BCS.ubAS, { claim: MedClaim => measurementInterval.contains(claim.dos) })
        s.addScore(name, HEDISRule.meetMeasure, BCS.mammogramUB, claims)
      })

    applyRules(scorecard, rules)
  }

}
