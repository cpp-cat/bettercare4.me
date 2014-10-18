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
import com.nickelsoftware.bettercare4me.models.LabClaim
import com.nickelsoftware.bettercare4me.models.Patient
import com.nickelsoftware.bettercare4me.models.PatientHistory
import com.nickelsoftware.bettercare4me.models.PersistenceLayer
import com.nickelsoftware.bettercare4me.models.Provider
import com.nickelsoftware.bettercare4me.models.RuleConfig
import com.nickelsoftware.bettercare4me.utils.Utils
import com.nickelsoftware.bettercare4me.models.RxClaim

object MPM {

  // Inpatient admission (acute, non-acute, or long term care)
  // ---------
  // POS
  val posA = List("21", "25", "31", "32", "34", "51", "54", "55", "56", "61")
  val posAS = posA.toSet

  // Bill Type
  //@TODO expand codes
  val tobA = List("11*", "12*", "18*", "21*", "22*", "41*", "81*", "82*", "84*")
  val tobAS = tobA.toSet

  // UB Revenue
  //@TODO expand codes
  val ubA = List("0115", "0118", "0125", "0128", "0135", "0138", "0145", "0148", "0155", "0158", "019*", "0650", "0655", "0656", "0658", "0659", "1001", "1002")
  val ubAS = ubA.toSet

  // HCPCS
  //@TODO expand codes
  val hcpcsA = List("H0017-H0019", "T2048")
  val hcpcsAS = hcpcsA.toSet

  // CPT - Patients who had at all 3 tests (serum potassium, serum creatinine and blood urea nitrogen (BUN) tests)
  val cptB = List("80047", "80048", "80050", "80053", "80069")
  val cptBS = cptB.toSet

  // CPT - Potassium:
  val cptC = List("80051", "84132")
  val cptCS = cptC.toSet

  // CPT - Creatinine::
  val cptD = List("82565", "82575")

  // CPT - BUN:
  val cptE = List("84520", "84525")
  val cptDES = List.concat(cptD, cptE).toSet
}

/**
 * Annual Monitoring for Patients on Persistent Medications (MPM) Rule
 *
 * Annual Monitoring of Patients on Persistent Medications indicates whether a patient, aged 18 years and older, who received at least 180 days
 * of Persistent Medications medication therapy during the measurement year, had at least one therapeutic monitoring event
 * during that same period. This excludes patients who had an inpatient stay during the measurement year.
 *
 * EXCLUSIONS:
 * Excludes from the eligible population all patients who had an inpatient stay anytime during the measurement year. This includes
 * admissions to hospitals, hospices, skilled nursing facilities, intermediate care facilities, rehabilitation facilities, and residential
 * psychiatric or substance abuse facilities.
 *
 * NUMERATOR (for ACE/ARB, Digoxin, and Diuretics):
 * Identifies patients, aged 18 years and older, who had at least one serum potassium test and either a serum creatinine or a blood
 * urea nitrogen (BUN) test during the measurement year.
 *
 * NUMERATOR (for anticonvulsants):
 * Identifies patients, aged 18 years and older, who had at least one drug serum concentration monitoring test for the anticonvulsant
 * during the measurement year.
 *
 */
abstract class MPM_RuleBase(val name: String, tag: String, ndcA: List[String], ndcAS: Set[String], config: RuleConfig, hedisDate: DateTime) extends HEDISRuleBase(config, hedisDate) {

  val fullName = "Annual Monitoring for Patients on " + tag + "."
  val description = "Annual Monitoring of " + tag + " indicates whether a patient, aged 18 years and older, who received at least 180 days " +
    "of " + tag + " therapy during the measurement year, had at least one therapeutic monitoring event and/or lab tests during that same period. " +
    "This excludes patients who had an inpatient stay during the measurement year."

  import MPM._

  override def isPatientMeetDemographic(patient: Patient): Boolean = {
    val age = patient.age(hedisDate)
    age >= 18
  }

  override def generateEligibleClaims(pl: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = {

    val dos1 = hedisDate.minusYears(1).plusDays(30 + Random.nextInt(60))
    val dos2 = dos1.plusDays(30 + Random.nextInt(30))
    val dos3 = dos2.plusDays(30 + Random.nextInt(30))

    // Ensure patient has 180 days supply within the current measurement year
    List(
      pl.createRxClaim(patient.patientID, provider.providerID, dos1, ndc = pickOne(ndcA), daysSupply = 60, qty = 60),
      pl.createRxClaim(patient.patientID, provider.providerID, dos2, ndc = pickOne(ndcA), daysSupply = 60, qty = 60),
      pl.createRxClaim(patient.patientID, provider.providerID, dos3, ndc = pickOne(ndcA), daysSupply = 60, qty = 60))
  }

  override def scorePatientEligible(scorecard: Scorecard, patient: Patient, ph: PatientHistory): Scorecard = {

    if (!isPatientMeetDemographic(patient)) scorecard.addScore(name, HEDISRule.eligible, false)
    else {
      val measurementInterval = getIntervalFromYears(1)

      val claims = filterClaims(ph.ndc, ndcAS, { claim: RxClaim => measurementInterval.contains(claim.fillD) })
      val daysSupply = claims.foldLeft(0)({ (daysSupply: Int, claim: RxClaim) =>
        {
          val d = claim.fillD.plusDays(claim.daysSupply)
          if (d.isAfter(hedisDate)) daysSupply + Utils.daysBetween(hedisDate, d)
          else daysSupply + claim.daysSupply
        }
      })

      if (daysSupply >= 180) scorecard.addScore(name, HEDISRule.eligible, "Patient Eligible", claims)
      else scorecard
    }
  }

  override def generateExclusionClaims(pl: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = {

    val days = Utils.daysBetween(hedisDate.minusYears(1), hedisDate)
    val dos = hedisDate.minusDays(Random.nextInt(days))

    pickOne(List(

      // Inpatient admission (acute, non-acute, or long term care)
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos, dos, hcfaPOS = pickOne(posA))),
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos, dos, tob = pickOne(tobA))),
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos, dos, ubRevenue = pickOne(ubA))),
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos, dos, hcpcs = pickOne(hcpcsA)))))()
  }

  override def scorePatientExcluded(scorecard: Scorecard, patient: Patient, ph: PatientHistory): Scorecard = {

    val measurementInterval = getIntervalFromYears(1)

    // Inpatient admission (acute, non-acute, or long term care)
    val claims = List.concat(
      filterClaims(ph.hcfaPOS, posAS, { claim: MedClaim => measurementInterval.contains(claim.dos) }),
      filterClaims(ph.tob, tobAS, { claim: MedClaim => measurementInterval.contains(claim.dos) }),
      filterClaims(ph.ubRevenue, ubAS, { claim: MedClaim => measurementInterval.contains(claim.dos) }),
      filterClaims(ph.hcpcs, hcpcsAS, { claim: MedClaim => measurementInterval.contains(claim.dos) }))

    if (claims.isEmpty) scorecard
    else scorecard.addScore(name, HEDISRule.excluded, "Patient Excluded", claims)
  }

  def generateMeetMeasureClaimsADD(pl: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = {

    val days = Utils.daysBetween(hedisDate.minusYears(1), hedisDate)
    val dos1 = hedisDate.minusDays(Random.nextInt(days))
    val dos2 = hedisDate.minusDays(Random.nextInt(days))

    pickOne(List(

      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos1, dos1, cpt = pickOne(cptB))),
      () => List(
        pl.createMedClaim(patient.patientID, provider.providerID, dos1, dos1, cpt = pickOne(cptC)),
        pickOne(List(
          pl.createMedClaim(patient.patientID, provider.providerID, dos2, dos2, cpt = pickOne(cptD)),
          pl.createMedClaim(patient.patientID, provider.providerID, dos2, dos2, cpt = pickOne(cptE)))))))()
  }

  def scorePatientMeetMeasureADD(scorecard: Scorecard, patient: Patient, ph: PatientHistory): Scorecard = {

    val measurementInterval = getIntervalFromYears(1)

    def rules = List[(Scorecard) => Scorecard](

      (s: Scorecard) => {
        val claims = filterClaims(ph.cpt, cptBS, { claim: MedClaim => measurementInterval.contains(claim.dos) })
        s.addScore(name, HEDISRule.meetMeasure, "All 3 tests", claims)
      },

      (s: Scorecard) => {
        val claims1 = filterClaims(ph.cpt, cptCS, { claim: MedClaim => measurementInterval.contains(claim.dos) })
        val claims2 = filterClaims(ph.cpt, cptDES, { claim: MedClaim => measurementInterval.contains(claim.dos) })
        if (claims1.isEmpty || claims2.isEmpty) s
        else s.addScore(name, HEDISRule.meetMeasure, "Potassium and Creatinine/BUN", List.concat(claims1, claims2))
      })

    applyRules(scorecard, rules)
  }

  def generateMeetMeasureClaimsA(cptA: List[String], pl: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = {

    val days = Utils.daysBetween(hedisDate.minusYears(1), hedisDate)
    val dos = hedisDate.minusDays(Random.nextInt(days))

    List(pl.createMedClaim(patient.patientID, provider.providerID, dos, dos, cpt = pickOne(cptA)))
  }

  def scorePatientMeetMeasureA(cptAS: Set[String], scorecard: Scorecard, patient: Patient, ph: PatientHistory): Scorecard = {

    val measurementInterval = getIntervalFromYears(1)
    val claims = filterClaims(ph.cpt, cptAS, { claim: MedClaim => measurementInterval.contains(claim.dos) })

    if (claims.isEmpty) scorecard
    else scorecard.addScore(name, HEDISRule.meetMeasure, "All 3 tests", claims)
  }
}

