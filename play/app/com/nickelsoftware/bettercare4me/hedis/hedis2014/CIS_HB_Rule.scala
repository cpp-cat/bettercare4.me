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

object CIS_HB {

  val name = "CIS-HB-C-HEDIS-2014"

  val hepBVaccine = "Hepatitis B Vaccine"
  val hepBHistory = "Hepatitis B History"
  /**
   * CPT codes for Hep B vaccination
   */
  val cptA = List("90723", "90740", "90744", "90747", "90748")
  val cptAS = cptA.toSet

  /**
   * HCPCS for Hep B vaccination
   */
  val hcpcsA = List("G0010")
  val hcpcsAS = hcpcsA.toSet

  /**
   * ICD Diagnosis codes for Hep B
   */
  val icdDA = List("070.2*", "070.3*", "V02.61")
  val icdDAS = icdDA.toSet
}

/**
 * Hepatitis B Vaccine
 *
 * Hepatitis B Vaccine indicates whether a child, who turned 2 years old during the measurement year, received three (3) hepatitis
 * B vaccinations. This excludes children who had a previous adverse reaction to a vaccine, as well as those with a vaccine
 * contraindication such as immunodeficiency syndrome, HIV, lymphoreticular or histiocytic tissue cancer, multiple myeloma, or
 * leukemia.
 *
 * NUMERATOR:
 * Identifies children, who turned 2 years old during the measurement year, and received three (3) hepatitis B vaccinations with
 * different dates of service on or before the child's 2nd birthday. Evidence of the antigen or vaccine, a documented history of the
 * illness, or a seropositive test result are counted in the numerator.
 *
 */
class CIS_HB_Rule(config: RuleConfig, hedisDate: DateTime) extends CIS_RuleBase(config, hedisDate) {

  val name = CIS_HB.name
  val fullName = "Hepatitis B Vaccine"
  val description = "Hepatitis B Vaccine indicates whether a child, who turned 2 years old during the measurement year, received three (3) hepatitis " +
    "B vaccinations. This excludes children who had a previous adverse reaction to a vaccine, as well as those with a vaccine " +
    "contraindication such as immunodeficiency syndrome, HIV, lymphoreticular or histiocytic tissue cancer, multiple myeloma, or " +
    "leukemia."

  import CIS_HB._
  override def generateMeetMeasureClaims(pl: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = {

    // after 42 days after birth and before 2 years of age
    val base = patient.dob.plusDays(42)
    val interval = new Interval(base, patient.dob.plusMonths(20).plusDays(1))
    val days = interval.toDuration().getStandardDays().toInt
    val dos1 = base.plusDays(Random.nextInt(days))
    val dos2 = dos1.plusDays(30)
    val dos3 = dos2.plusDays(30)

    /* 3 hepatitis B vaccinations received on different dates
     * of service (anytime prior to the child's 2nd birthday),
     * or a history of the disease */
    pickOne(List(

      // Possible set: CPT
      () => List(
        pl.createMedClaim(patient.patientID, provider.providerID, dos1, dos1, cpt = pickOne(cptA)),
        pl.createMedClaim(patient.patientID, provider.providerID, dos2, dos2, cpt = pickOne(cptA)),
        pl.createMedClaim(patient.patientID, provider.providerID, dos3, dos3, cpt = pickOne(cptA))),

      // Another possible set: HCPCS
      () => List(
        pl.createMedClaim(patient.patientID, provider.providerID, dos1, dos1, hcpcs = pickOne(hcpcsA)),
        pl.createMedClaim(patient.patientID, provider.providerID, dos2, dos2, hcpcs = pickOne(hcpcsA)),
        pl.createMedClaim(patient.patientID, provider.providerID, dos3, dos3, hcpcs = pickOne(hcpcsA))),

      // Another possible set: ICD D
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos1, dos1, icdDPri = pickOne(icdDA)))))()
  }

  override def scorePatientMeetMeasure(scorecard: Scorecard, patient: Patient, ph: PatientHistory): Scorecard = {

    // after 42 days after birth and before 2 years of age
    val measurementInterval = new Interval(patient.dob.plusDays(42), patient.dob.plusMonths(24).plusDays(1))

    def rules = List[(Scorecard) => Scorecard](

      // Check for patient has CPT
      (s: Scorecard) => {
        val claims1 = filterClaims(ph.cpt, cptAS, { claim: MedClaim => measurementInterval.contains(claim.dos) })
        val claims2 = filterClaims(ph.hcpcs, hcpcsAS, { claim: MedClaim => measurementInterval.contains(claim.dos) })
        val claims = List.concat(claims1, claims2)

        // need to have 3 claims with different dates
        if (hasDifferentDates(3, claims)) s.addScore(name, HEDISRule.meetMeasure, hepBVaccine, claims)
        else s
      },

      // Check for patient has ICD D (History of disease)
      (s: Scorecard) => {
        val claims = filterClaims(ph.icdD, icdDAS, { claim: MedClaim => !claim.dos.isAfter(hedisDate) })
        s.addScore(name, HEDISRule.meetMeasure, hepBHistory, claims)
      })

    applyRules(scorecard, rules)
  }
}
