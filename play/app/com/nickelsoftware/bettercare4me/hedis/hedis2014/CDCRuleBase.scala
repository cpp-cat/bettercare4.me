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
import com.nickelsoftware.bettercare4me.models.RxClaim
import com.nickelsoftware.bettercare4me.models.Patient
import com.nickelsoftware.bettercare4me.models.PatientHistory
import com.nickelsoftware.bettercare4me.models.PersistenceLayer
import com.nickelsoftware.bettercare4me.models.Provider
import com.nickelsoftware.bettercare4me.models.RuleConfig
import com.github.tototoshi.csv.CSVReader
import java.io.File
import com.nickelsoftware.bettercare4me.utils.NickelException

/**
 * Comprehensive Diabetes Control Base Rule
 *
 * The base rule implements rules that are common to all CDC rules, such the determination if the
 * patient has diabetes.
 *
 * DENOMINATOR:
 * Identifies the unique count of patients, aged 18 to 75 years, with type 1 or type 2 diabetes. It excludes patients with a previous
 * diagnosis of polycystic ovaries, gestational diabetes, or steroid-induced diabetes.
 *
 * EXCLUSION:
 * Excludes from the eligible population all patients with a history of polycystic ovaries (based on claims included in the database),
 * gestational diabetes, or steroid-induced diabetes, and who did not have a face-to-face encounter with the diagnosis of diabetes in
 * any setting during the measurement year or the year prior.
 */
abstract class CDCRuleBase(config: RuleConfig, hedisDate: DateTime) extends HEDISRuleBase(config, hedisDate) {

  import CDC._

  //
  // isPatientMeetDemographic
  //
  override def isPatientMeetDemographic(patient: Patient): Boolean = {
    val age = patient.age(hedisDate)
    age >= 18 && age <= 75
  }

  //
  // generateEligibleAndExclusionClaims
  //
  override def generateEligibleAndExclusionClaims(pl: PersistenceLayer, patient: Patient, provider: Provider, isExcluded: Boolean): List[Claim] = {

    val days = getIntervalFromYears(1).toDuration().getStandardDays().toInt
    val dos1 = hedisDate.minusDays(Random.nextInt(days))
    val dos2 = dos1.minusDays(Random.nextInt(180)+2)

    def generateDiabetesRx: List[Claim] = {
      val ndc1 = pickOne(ndcA)
      List(
        pl.createRxClaim(patient.patientID, provider.providerID, dos1, ndc = ndc1, daysSupply = 90, qty = 90),
        pl.createRxClaim(patient.patientID, provider.providerID, dos1.minusDays(80 + Random.nextInt(20)), ndc = ndc1, daysSupply = 90, qty = 90),
        pl.createRxClaim(patient.patientID, provider.providerID, dos1.minusDays(170 + Random.nextInt(30)), ndc = ndc1, daysSupply = 90, qty = 90))
    }

    if (isExcluded) {

      // the only eligible claims are those with diabetes drugs
      List.concat(generateDiabetesRx, generateExclusionClaims(pl, patient, provider))

    } else {
      pickOne(List(

        // One possible set of claims based on NDC code - patient taking diabetes drug
        () => generateDiabetesRx,

        // Another possible set of claims based on 2 face-to-face diagnosis
        () => {
          List(
            pickOne(List(
              pl.createMedClaim(patient.patientID, provider.providerID, dos1, dos1, icdDPri = pickOne(icd9DAT), cpt = pickOne(cptA), hcfaPOS = pickOne(posAT)),
              pl.createMedClaim(patient.patientID, provider.providerID, dos1, dos1, icdDPri = pickOne(icd9DAT), ubRevenue = pickOne(ubAT), hcfaPOS = pickOne(posAT)))),
            pickOne(List(
              pl.createMedClaim(patient.patientID, provider.providerID, dos2, dos2, icdDPri = pickOne(icd9DAT), ubRevenue = pickOne(ubAT), hcfaPOS = pickOne(posAT)),
              pl.createMedClaim(patient.patientID, provider.providerID, dos2, dos2, icdDPri = pickOne(icd9DAT), cpt = pickOne(cptA), hcfaPOS = pickOne(posAT)))))
        },

        // Another possible set of claims based on 1 face-to-face in ER
        () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos1, dos1, icdDPri = pickOne(icd9DAT), cpt = pickOne(cptB), hcfaPOS = pickOne(posB))),
        () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos1, dos1, icdDPri = pickOne(icd9DAT), ubRevenue = pickOne(ubBT), hcfaPOS = pickOne(posB)))))()
    }
  }

  //
  // isPatientEligible
  //
  override def scorePatientEligible(scorecard: Scorecard, patient: Patient, ph: PatientHistory): Scorecard = {

    val measurementInterval = getIntervalFromYears(2)

    def rules = List[(Scorecard) => Scorecard](

      // Check if patient is taking diabetes drugs
      (s: Scorecard) => {
        val claims = filterClaims(ph.ndc, ndcAS, { claim: RxClaim => measurementInterval.contains(claim.fillD) })
        s.addScore(name, HEDISRule.eligible, diabetesDrugs, claims)
      },

      // check if patient has 2 face-2-face Diagnosis on different dates
      (s: Scorecard) => {
        val claims = filterClaims(ph.icdD, icd9DAS, { claim: MedClaim =>
          measurementInterval.contains(claim.dos) &&
            posAS.contains(claim.hcfaPOS) &&
            (cptAS.contains(claim.cpt) || ubAS.contains(claim.ubRevenue))
        })
        
        // need to check we have 2 claims with different DOS
        if (Claim.twoDifferentDOS(claims)) {
          s.addScore(name, HEDISRule.eligible, twoF2FDiabetesICD, claims)
        } else s
      },

      // check if patient has 1 face-2-face in ER
      (s: Scorecard) => {
        val claims = filterClaims(ph.icdD, icd9DAS, { claim: MedClaim =>
          measurementInterval.contains(claim.dos) &&
            posBS.contains(claim.hcfaPOS) &&
            (cptBS.contains(claim.cpt) || (ubBS.contains(claim.ubRevenue)))
        })
        s.addScore(name, HEDISRule.eligible, oneF2FDiabetesICD, claims)
      })

    if (!isPatientMeetDemographic(patient)) scorecard.addScore(name, HEDISRule.eligible, false)
    else applyRules(scorecard, rules)
  }

  //
  // generateExclusionClaims
  //
  override def generateExclusionClaims(pl: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = {

    val days = getIntervalFromYears(2).toDuration().getStandardDays().toInt
    val dos1 = hedisDate.minusDays(Random.nextInt(days))

    // Exclusion based on ICD Diagnostic (anytime prior to or during the measurement year)
    List(pl.createMedClaim(patient.patientID, provider.providerID, dos1, dos1, icdDPri = pickOne(icd9DB)))
  }

  //
  // isPatientExcluded
  //
  override def scorePatientExcluded(scorecard: Scorecard, patient: Patient, ph: PatientHistory): Scorecard = {

    val measurementInterval = getIntervalFromYears(2)

    // Patient is excluded if has diagnosis from ICD9DB and no face-2-face visit (identified from ICD9DA) - regardless of POS
    // --
    // all claims w/ exclusion diagnosis
    val exClaims = filterClaims(ph.icdD, icd9DBS, { _: Claim => true })

    if (exClaims.isEmpty) scorecard
    else {

      // see if have any claims w/ face-2-face diagnosis
      val claims = filterClaims(ph.icdD, icd9DAS, { _: Claim => true })
      if (claims.isEmpty) scorecard.addScore(name, HEDISRule.excluded, exclutionDiabetesICD, exClaims)
      else scorecard
    }
  }
}

