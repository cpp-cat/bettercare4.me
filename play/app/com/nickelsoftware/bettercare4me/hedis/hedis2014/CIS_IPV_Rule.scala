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

object CIS_IPV {

  val name = "CIS-IPV-C-HEDIS-2014"

  val ipvVaccine = "Polio Vaccine"

  /**
   * CPT codes for polio vaccination
   */
  val cptA = List("90698", "90713", "90723")
  val cptAS = cptA.toSet

  /**
   * ICD P for polio vaccination
   */
  val icdPA = List("99.41")
  val icdPAS = icdPA.toSet
}

/**
 * Polio (IPV) Vaccine
 *
 * Polio (IPV) Vaccine indicates whether a child, who turned 2 years old during the measurement year, received three (3) polio
 * vaccinations. This excludes children who had a previous adverse reaction to a vaccine, as well as those with a vaccine
 * contraindication such as immunodeficiency syndrome, HIV, lymphoreticular or histiocytic tissue cancer, multiple myeloma, or
 * leukemia.
 *
 * NUMERATOR:
 * Identifies children who turned 2 years old during the measurement year and received at least three (3) polio vaccinations (IPV)
 * with different dates of service on or before the child's 2nd birthday. A vaccination administered before 42 days after birth is not counted.
 *
 */
class CIS_IPV_Rule(config: RuleConfig, hedisDate: DateTime) extends CIS_RuleBase(config, hedisDate) {

  val name = CIS_IPV.name
  val fullName = "Polio (IPV) Vaccine"
  val description = "Polio (IPV) Vaccine indicates whether a child, who turned 2 years old during the measurement year, received three (3) polio " +
    "vaccinations. This excludes children who had a previous adverse reaction to a vaccine, as well as those with a vaccine " +
    "contraindication such as immunodeficiency syndrome, HIV, lymphoreticular or histiocytic tissue cancer, multiple myeloma, or leukemia."

  import CIS_IPV._
  override def generateMeetMeasureClaims(pl: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = {

    // after 42 days after birth and before 2 years of age
    val days = Utils.daysBetween(patient.dob.plusDays(42), patient.dob.plusMonths(20))
    val dos1 = patient.dob.plusDays(42 + Random.nextInt(days))
    val dos2 = dos1.plusDays(30)
    val dos3 = dos2.plusDays(30)

    /* 3 hepatitis B vaccinations received on different dates
     * of service (anytime prior to the child's 2nd birthday),
     * or a history of the disease */
    pickOne(List(

      // Possible set: CPT
      () => List(
        pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos1, dos1, cpt = pickOne(cptA)),
        pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos2, dos2, cpt = pickOne(cptA)),
        pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos3, dos3, cpt = pickOne(cptA))),

      // Another possible set: HCPCS
      () => List(
        pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos1, dos1, icdP = Set(pickOne(icdPA))),
        pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos2, dos2, icdP = Set(pickOne(icdPA))),
        pl.createMedClaim(patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos3, dos3, icdP = Set(pickOne(icdPA))))))()
  }

  override def scorePatientMeetMeasure(scorecard: Scorecard, patient: Patient, ph: PatientHistory): Scorecard = {

    // after 42 days after birth and before 2 years of age
    val measurementInterval = new Interval(patient.dob.plusDays(42), patient.dob.plusMonths(24).plusDays(1))

    def rules = List[(Scorecard) => Scorecard](

      // Check for patient has CPT
      (s: Scorecard) => {
        val claims1 = filterClaims(ph.cpt, cptAS, { claim: MedClaim => measurementInterval.contains(claim.dos) })
        val claims2 = filterClaims(ph.icdP, icdPAS, { claim: MedClaim => measurementInterval.contains(claim.dos) })
        val claims = List.concat(claims1, claims2)

        // need to have 3 claims with different dates
        if (hasDifferentDates(3, claims)) s.addScore(name, HEDISRule.meetMeasure, ipvVaccine, claims)
        else s
      })

    applyRules(scorecard, rules)
  }
}
