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

object CIS_DTaP {

  val name = "CIS-DTaP-C-HEDIS-2014"

  val DTaPVacc = "Diphtheria-Tetanus-Pertussis Vaccination"

  /**
   * CPT codes for Diphtheria-Tetanus-Pertussis vaccination
   */
  val cptA = List("90698", "90700", "90721", "90723")
  val cptAS = cptA.toSet

  /**
   * ICD Procedure codes for vaccination
   */
  val icdPA = List("99.39")
  val icdPAS = icdPA.toSet
}

/**
 * Diphtheria-Tetanus-Pertussis (DTaP) Vaccine
 *
 * Diphtheria-Tetanus-Pertussis (DTaP) Vaccine indicates whether a child, who turned 2 years old during the measurement year, received three (3)
 * Diphtheria-Tetanus-Pertussis (DTaP) vaccinations. This excludes children who had a previous adverse reaction to a vaccine, as well
 * as those with a vaccine contraindication such as immunodeficiency syndrome, HIV, lymphoreticular or histiocytic tissue cancer,
 * multiple myeloma, or leukemia.
 *
 * NUMERATOR:
 * Identifies children, who turned 2 years old during the measurement year, and received at least three (3) Diphtheria-Tetanus-Pertussis vaccination with
 * a date of service occurring on or before the child's 2nd birthday. Evidence of the antigen or vaccine, a documented history of the
 * illness, or a seropositive test result are counted in the numerator.
 *
 */
class CIS_DTaP_Rule(config: RuleConfig, hedisDate: DateTime) extends CIS_RuleBase(config, hedisDate) {

  val name = CIS_DTaP.name
  val fullName = "Diphtheria-Tetanus-Pertussis (DTaP) Vaccine"
  val description = "Diphtheria-Tetanus-Pertussis (DTaP) Vaccine indicates whether a child, who turned 2 years old during the measurement year, received three (3)" +
    "Diphtheria-Tetanus-Pertussis (DTaP) vaccinations. This excludes children who had a previous adverse reaction to a vaccine, as well" +
    "as those with a vaccine contraindication such as immunodeficiency syndrome, HIV, lymphoreticular or histiocytic tissue cancer," +
    "multiple myeloma, or leukemia."

  import CIS_DTaP._
  override def generateMeetMeasureClaims(pl: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = {

    // after 42 days after birth and before 2 years of age
    val base = patient.dob.plusDays(42)
    val interval = new Interval(base, patient.dob.plusMonths(20).plusDays(1))
    val days = interval.toDuration().getStandardDays().toInt
    val dos1 = base.plusDays(Random.nextInt(days))
    val dos2 = dos1.plusDays(30)
    val dos3 = dos2.plusDays(30)

    // At least one received at least 3 Diphtheria-Tetanus-Pertussis vaccination
    pickOne(List(

      // Possible set: CPT
      () => List(
          pl.createMedClaim(patient.patientID, provider.providerID, dos1, dos1, cpt = pickOne(cptA)),
          pl.createMedClaim(patient.patientID, provider.providerID, dos2, dos2, cpt = pickOne(cptA)),
          pl.createMedClaim(patient.patientID, provider.providerID, dos3, dos3, cpt = pickOne(cptA))
          ),

      // Another possible set: ICD P
      () => List(
          pl.createMedClaim(patient.patientID, provider.providerID, dos1, dos1, icdP = Set(pickOne(icdPA))),
          pl.createMedClaim(patient.patientID, provider.providerID, dos2, dos2, icdP = Set(pickOne(icdPA))),
          pl.createMedClaim(patient.patientID, provider.providerID, dos3, dos3, icdP = Set(pickOne(icdPA)))
          )
          ))()
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
        // need to have 3 with different dates
        if(hasDifferentDates(3, claims)) s.addScore(name, HEDISRule.meetMeasure, DTaPVacc, claims)
        else s
      })

    applyRules(scorecard, rules)
  }
}
