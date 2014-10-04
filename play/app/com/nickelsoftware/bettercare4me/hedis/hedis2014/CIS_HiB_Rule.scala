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

object CIS_HiB {

  val name = "CIS-HiB-C-HEDIS-2014"

  val HiBVaccine = "H Influenza Type B Vaccine"

  /**
   * CPT codes for H Influenza Type B vaccination
   */
  val cptA = List("90645-90648", "90698", "90721", "90748")
  val cptAS = cptA.toSet
}

/**
 * H Influenza Type B Vaccine
 *
 * H Influenza Type B (HiB) Vaccine indicates whether a child, who turned 2 years old during the measurement year, received three
 * (3) H. influenzae type B vaccinations. This excludes children who had a previous adverse reaction to a vaccine, as well as those
 * with a vaccine contraindication such as immunodeficiency syndrome, HIV, lymphoreticular or histiocytic tissue cancer, multiple
 * myeloma, or leukemia.
 *
 * NUMERATOR:
 * Identifies children, who turned 2 years old during the measurement year, and received three (3) H Influenza Type B vaccinations with
 * different dates of service on or before the child's 2nd birthday. Evidence of the antigen or vaccine counted in the numerator.
 *
 */
class CIS_HiB_Rule(config: RuleConfig, hedisDate: DateTime) extends CIS_RuleBase(config, hedisDate) {

  val name = CIS_HiB.name
  val fullName = "Hepatitis B Vaccine"
  val description = "H Influenza Type B (HiB) Vaccine indicates whether a child, who turned 2 years old during the measurement year, received three " +
    "(3) H. influenzae type B vaccinations. This excludes children who had a previous adverse reaction to a vaccine, as well as those " +
    " with a vaccine contraindication such as immunodeficiency syndrome, HIV, lymphoreticular or histiocytic tissue cancer, multiple " +
    "myeloma, or leukemia."

  import CIS_HiB._
  override def generateMeetMeasureClaims(pl: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = {

    // after 42 days after birth and before 2 years of age
    val base = patient.dob.plusDays(42)
    val interval = new Interval(base, patient.dob.plusMonths(20).plusDays(1))
    val days = interval.toDuration().getStandardDays().toInt
    val dos1 = base.plusDays(Random.nextInt(days))
    val dos2 = dos1.plusDays(30)
    val dos3 = dos2.plusDays(30)

    /* 3 vaccinations received on different dates
     * of service (anytime prior to the child's 2nd birthday) */
    pickOne(List(

      // Possible set: CPT
      () => List(
        pl.createMedClaim(patient.patientID, provider.providerID, dos1, dos1, cpt = pickOne(cptA)),
        pl.createMedClaim(patient.patientID, provider.providerID, dos2, dos2, cpt = pickOne(cptA)),
        pl.createMedClaim(patient.patientID, provider.providerID, dos3, dos3, cpt = pickOne(cptA)))))()
  }

  override def scorePatientMeetMeasure(scorecard: Scorecard, patient: Patient, ph: PatientHistory): Scorecard = {

    // after 42 days after birth and before 2 years of age
    val measurementInterval = new Interval(patient.dob.plusDays(42), patient.dob.plusMonths(24).plusDays(1))

    def rules = List[(Scorecard) => Scorecard](

      // Check for patient has CPT
      (s: Scorecard) => {
        val claims = filterClaims(ph.cpt, cptAS, { claim: MedClaim => measurementInterval.contains(claim.dos) })

        // need to have 3 claims with different dates
        if (hasDifferentDates(3, claims)) s.addScore(name, HEDISRule.meetMeasure, HiBVaccine, claims)
        else s
      })

    applyRules(scorecard, rules)
  }
}
