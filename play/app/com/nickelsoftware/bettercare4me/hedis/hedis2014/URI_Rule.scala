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
import com.nickelsoftware.bettercare4me.models.LabClaim
import com.nickelsoftware.bettercare4me.models.MedClaim
import com.nickelsoftware.bettercare4me.models.RxClaim
import com.nickelsoftware.bettercare4me.models.Patient
import com.nickelsoftware.bettercare4me.models.PatientHistory
import com.nickelsoftware.bettercare4me.models.PersistenceLayer
import com.nickelsoftware.bettercare4me.models.Provider
import com.nickelsoftware.bettercare4me.models.RuleConfig
import com.nickelsoftware.bettercare4me.utils.Utils

object URI {

  val name = "URI-HEDIS-2014"

  val uriPatient = "URI Diagnosed Patient"
  val antibioticMedication = "Antibiotic Medication Prescribed"
  val confictingDiagnosis = "Conficting Diagnosis"

  // ICD D URI diagnosis 
  val icdDA = List("460", "465")
  val icdDAS = icdDA.toSet

  // ICD D exclusion - competing diagnosis
  //@TODO expand codes
  val icdDF = List("001.0-009.3", "033*", "034.0", "041.9", "078.88", "079.88", "079.98", "088*", "090.0-097.9", "098*", "099*", "131*", "382*", "383*", "461*", "462", "463", "464.10-464.31", "473*", "474*", "478.21-478.24", "478.29", "478.71", "478.79", "478.9", "481-486", "590*", "595*", "599.0", "601*", "614.0-616.9", "681*", "682*", "683", "684", "686*", "706.0", "706.1", "730*", "V01.6", "V02.7", "V02.8")
  val icdDFS = icdDF.toSet
}

/**
 * URI Treatment Without Antibiotics for Children Rule
 *
 * This measure is based on the HEDIS measure Appropriate Treatment for Children With Upper Respiratory Infection (URI).
 *
 * URI Treatment Without Antibiotics for Children indicates whether a child, aged 3 months to 18 years of age, who was seen with a
 * diagnosis of upper respiratory infection (URI), did not have an antibiotic prescribed within 3 days following their URI visit.
 * This excludes children who had an antibiotic prescription within the previous 30 days, or had a competing diagnosis within 3 days
 * of the visit.
 *
 * DENOMINATOR:
 * Identifies the unique count of children who had an outpatient or emergency room visit with a single diagnosis of pharyngitis, aged
 * 2 to 18 years during the intake period, and were prescribed an antibiotic within 3 days of their pharyngitis visit.
 * The intake period starts 6 months before the beginning of the measurement year and ends
 * 6 months before the end of the measurement year. If multiple pharyngitis visits meet the denominator criteria, the first one is
 * used. This excludes children who had an antibiotic prescription within the previous 30 days.
 *
 * EXCLUSIONS:
 * Excludes from the eligible population those who had an antibiotic prescription within the previous 30 days.
 * No antibiotic medication prescribed or refilled within 30 days prior to the pharyngitis visit or still active on
 * the date of the visit (claims are checked up to 90 days prior to the visit date)
 * A prescription is still active if the prescription was filled more than 30 days prior to the pharyngitis visit
 * date and the Days Supply is >= the number of date between the prescription fill date and the pharyngitis visit date.
 *
 * NUMERATOR:
 * Identifies patients who had an outpatient visit with a single diagnosis of pharyngitis, aged 2 to 18 years, and were prescribed an
 * antibiotic within 3 days of their pharyngitis visit, and also received a group A streptococcus test within 3 days before or after that visit.
 *
 * NOTE:
 * 1. In 2013, Added LOINC code 68954-7 to Table CWP-D.
 *
 */
class URI_Rule(config: RuleConfig, hedisDate: DateTime) extends HEDISRuleBase(config, hedisDate) {

  val name = URI.name
  val fullName = "URI Treatment Without Antibiotics for Children"
  val description = "URI Treatment Without Antibiotics for Children indicates whether a child, aged 3 months to 18 years of age, who was seen with a " +
    "diagnosis of upper respiratory infection (URI), did not have an antibiotic prescribed within 3 days following their URI visit. " +
    "This excludes children who had an antibiotic prescription within the previous 30 days, or had a competing diagnosis within 3 days " +
    "of the visit."

  import URI._
  import CWP._

  override def isPatientMeetDemographic(patient: Patient): Boolean = {
    val d = hedisDate.minusMonths(6)
    val age = patient.age(d)
    if (age < 2) {
      val ageM = patient.ageInMonths(d)
      ageM >= 15 && ageM <= 23
    } else age >= 2 && age <= 18
  }

  override def generateEligibleClaims(pl: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = {

    // generate an eligible episode in the lst 6 months of the intake period
    val intakeEnd = hedisDate.minusMonths(6)
    val days = Utils.daysBetween(intakeEnd.minusMonths(4), intakeEnd)
    val dos1 = intakeEnd.minusDays(10 + Random.nextInt(days)) // provider visit - will be in the last 6 months of the intake period so we can insert excluding claim 
    val dos2 = dos1.plusDays(Random.nextInt(3)) // prescribed antibiotics

    pickOne(List(

      // Visit at ED
      () => List(
        pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos1, dos1, icdDPri = pickOne(URI.icdDA), hcfaPOS = "01", cpt = pickOne(cptA)),
        pl.createRxClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos2, ndc = pickOne(ndcA), daysSupply = 20, qty = 40)),
      () => List(
        pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos1, dos1, icdDPri = pickOne(URI.icdDA), hcfaPOS = "01", ubRevenue = pickOne(ubA), roomBoardFlag = "N"),
        pl.createRxClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos2, ndc = pickOne(ndcA), daysSupply = 20, qty = 40)),

      // Visit at Outpatient facility
      () => List(
        pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos1, dos1, icdDPri = pickOne(URI.icdDA), cpt = pickOne(cptB)),
        pl.createRxClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos2, ndc = pickOne(ndcA), daysSupply = 20, qty = 40)),
      () => List(
        pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos1, dos1, icdDPri = pickOne(URI.icdDA), ubRevenue = pickOne(ubB)),
        pl.createRxClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos2, ndc = pickOne(ndcA), daysSupply = 20, qty = 40))))()
  }

  override def scorePatientEligible(scorecard: Scorecard, patient: Patient, ph: PatientHistory): Scorecard = {

    if (!isPatientMeetDemographic(patient)) scorecard.addScore(name, fullName, HEDISRule.eligible, false)
    else {

      // intake period - find all claim with only pharyngitis (icdDA) is primary diagnosis, no other diagnosis
      val claims = diagnosisClaims(URI.icdDAS, ph, hedisDate)

      if (claims.isEmpty) scorecard
      else scorecard.addScore(name, fullName, HEDISRule.eligible, uriPatient, claims)
    }
  }

  override def generateExclusionClaims(pl: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = {

    // generate an excluding episode within the first 6 months
    val dos1 = hedisDate.minusMonths(16).plusDays(10 + Random.nextInt(60)) // provider visit
    val dos2 = dos1.minusDays(1 + Random.nextInt(28)) // prescribed antibiotics (to make it excluded)
    val dos3 = dos2.minusDays(30) // prescribed antibiotics (to make it excluded, alternate rule)
    val dos4 = dos1.plusDays(Random.nextInt(3)) // competing diagnosis on or up to 3 days following the date of the URI visit

    pickOne(List(

      // Visit at ED
      () => List(
        pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos1, dos1, icdDPri = pickOne(URI.icdDA), hcfaPOS = "01", cpt = pickOne(cptA)),
        pl.createRxClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos2, ndc = pickOne(ndcA), daysSupply = 20, qty = 40)),
      () => List(
        pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos1, dos1, icdDPri = pickOne(URI.icdDA), hcfaPOS = "01", ubRevenue = pickOne(ubA), roomBoardFlag = "N"),
        pl.createRxClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos2, ndc = pickOne(ndcA), daysSupply = 20, qty = 40)),

      // make such that the antibiotics were dispensed more than 30 days prior the episode (diagnosis)
      () => List(
        pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos1, dos1, icdDPri = pickOne(URI.icdDA), hcfaPOS = "01", cpt = pickOne(cptA)),
        pl.createRxClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos3, ndc = pickOne(ndcA), daysSupply = Utils.daysBetween(dos3, dos1), qty = 90)),
      () => List(
        pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos1, dos1, icdDPri = pickOne(URI.icdDA), hcfaPOS = "01", ubRevenue = pickOne(ubA), roomBoardFlag = "N"),
        pl.createRxClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos3, ndc = pickOne(ndcA), daysSupply = Utils.daysBetween(dos3, dos1), qty = 90)),

      // Visit at Outpatient facility
      () => List(
        pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos1, dos1, icdDPri = pickOne(URI.icdDA), cpt = pickOne(cptB)),
        pl.createRxClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos2, ndc = pickOne(ndcA), daysSupply = 20, qty = 40)),
      () => List(
        pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos1, dos1, icdDPri = pickOne(URI.icdDA), ubRevenue = pickOne(ubB)),
        pl.createRxClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos2, ndc = pickOne(ndcA), daysSupply = 20, qty = 40)),

      // make such that the antibiotics were dispensed more than 30 days prior the episode (diagnosis)
      () => List(
        pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos1, dos1, icdDPri = pickOne(URI.icdDA), cpt = pickOne(cptB)),
        pl.createRxClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos3, ndc = pickOne(ndcA), daysSupply = Utils.daysBetween(dos3, dos1), qty = 90)),
      () => List(
        pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos1, dos1, icdDPri = pickOne(URI.icdDA), ubRevenue = pickOne(ubB)),
        pl.createRxClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos3, ndc = pickOne(ndcA), daysSupply = Utils.daysBetween(dos3, dos1), qty = 90)),

      // Competing diagnosis
      () => List(
        pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos1, dos1, icdDPri = pickOne(URI.icdDA), cpt = pickOne(cptB)),
        pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos4, dos4, icdDPri = pickOne(icdDF))),
      () => List(
        pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos1, dos1, icdDPri = pickOne(URI.icdDA), ubRevenue = pickOne(ubB)),
        pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos4, dos4, icdDPri = pickOne(icdDF)))))()
  }

  override def scorePatientExcluded(scorecard: Scorecard, patient: Patient, ph: PatientHistory): Scorecard = {

    // get all claims with Antibiotic Medication prior to pharyngitis diagnosis
    def activeRxPriorDianosis(claim: MedClaim, ph: PatientHistory): List[Claim] = {

      // get all claims with Antibiotic Medication prior to pharyngitis diagnosis
      val prior90 = new Interval(claim.dos.minusMonths(3), claim.dos)
      filterClaims(ph.ndc, ndcAS, { rx: RxClaim => prior90.contains(rx.fillD) && ((Utils.daysBetween(rx.fillD, claim.dos) <= 30) || !rx.fillD.plusDays(rx.daysSupply).isBefore(claim.dos)) })
    }

    // get the episode claims
    val claims = diagnosisClaims(URI.icdDAS, ph, hedisDate)

    def rules = List[(Scorecard) => Scorecard](

      (s: Scorecard) => {

        // get all claims with Antibiotic Medication prior to diagnosis to see if should be excluded
        val rxExclusion = activeRxPriorDianosis(claims.head, ph)
        if (rxExclusion.isEmpty) s
        else s.addScore(name, fullName, HEDISRule.excluded, antibioticMedication, List.concat(claims, rxExclusion))
      },

      (s: Scorecard) => {

        // check for conficting diagnosis
        val claim = claims.head
        val next3 = new Interval(claim.dos, claim.dos.plusDays(4))
        val claims2 = filterClaims(ph.icdD, icdDFS, { c: MedClaim => next3.contains(c.dos) })
        if (claims2.isEmpty) s
        else s.addScore(name, fullName, HEDISRule.excluded, confictingDiagnosis, List.concat(claims, claims2))
      })

    if (claims.isEmpty) scorecard
    else applyRules(scorecard, rules)

  }

  override def generateMeetMeasureClaims(pl: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = {

    // generate meet measure episode within the first 6 months
    val dos1 = hedisDate.minusMonths(16).plusDays(10 + Random.nextInt(60)) // URI diagnosis

    pickOne(List(

      // Visit at ED
      () => List(pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos1, dos1, icdDPri = pickOne(URI.icdDA), hcfaPOS = "01", cpt = pickOne(cptA))),
      () => List(pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos1, dos1, icdDPri = pickOne(URI.icdDA), hcfaPOS = "01", ubRevenue = pickOne(ubA), roomBoardFlag = "N")),

      // Visit at Outpatient facility
      () => List(pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos1, dos1, icdDPri = pickOne(URI.icdDA), cpt = pickOne(cptB))),
      () => List(pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos1, dos1, icdDPri = pickOne(URI.icdDA), ubRevenue = pickOne(ubB)))))()
  }

  override def scorePatientMeetMeasure(scorecard: Scorecard, patient: Patient, ph: PatientHistory): Scorecard = {

    // get the episode claims
    val claims = diagnosisClaims(URI.icdDAS, ph, hedisDate)
    if (claims.isEmpty) scorecard
    else {

      val claim = claims.head

      // check to ensure antibiotic was not prescribed within 3 days of the diagnosis
      val next3 = new Interval(claim.dos, claim.dos.plusDays(4))
      val rxs = filterClaims(ph.ndc, ndcAS, { rx: RxClaim => next3.contains(rx.fillD) })
      if (rxs.isEmpty) scorecard.addScore(name, fullName, HEDISRule.meetMeasure, "Patient Meet Measure", claims)
      else scorecard
    }
  }
}
