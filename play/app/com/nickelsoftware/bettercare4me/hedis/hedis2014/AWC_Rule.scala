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

object AWC {

  val name = "AWC-HEDIS-2014"

  val wellCareVisit = "Adolescent Well-Care Visits"

  // CPT for well child visit
  val cptA = List("99383", "99384", "99385", "99393", "99394", "99395")
  val cptAS = cptA.toSet

  // ICD D for well child visit
  val icdDA = List("V20.2", "V70.0", "V70.3", "V70.5", "V70.6", "V70.8", "V70.9")
  val icdDAS = icdDA.toSet
}
/**
 * Adolescent Well-Care Visits
 *
 * The percentage of enrolled members 12–21 years of age who had at least one comprehensive well-care visit
 * with a PCP or an OB/GYN practitioner during the measurement year.
 *
 * NUMERATOR:
 * At least one comprehensive well-care visit with a PCP or an OB/GYN practitioner
 * during the measurement year. The PCP does not have to be assigned to the member.
 *
 */
class AWC_Rule(config: RuleConfig, hedisDate: DateTime) extends HEDISRuleBase(config, hedisDate) {

  val name = AWC.name
  val fullName = "Adolescent Well-Care Visits"
  val description = "The percentage of enrolled members 12–21 years of age who had at least one comprehensive well-care visit " +
    "with a PCP or an OB/GYN practitioner during the measurement year."

  override def isPatientMeetDemographic(patient: Patient): Boolean = {
    val age = patient.age(hedisDate)
    age >= 12 && age <= 21
  }

  import AWC._

  // This rule has 100% eligibility when the demographics are meet
  override val eligibleRate: Int = 100

  // This rule has 0% exclusion when the demographics are meet
  override val exclusionRate: Int = 0

  override def scorePatientExcluded(scorecard: Scorecard, patient: Patient, ph: PatientHistory): Scorecard = scorecard.addScore(name, HEDISRule.excluded, false)

  override def generateMeetMeasureClaims(pl: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = {

    val days = Utils.daysBetween(hedisDate.minusYears(1), hedisDate)
    val dos = hedisDate.minusDays(Random.nextInt(days))

    pickOne(List(

      // One possible set of claims based on cpt
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos, dos, cpt = pickOne(cptA))),

      // Another possible set of claims based on ICD D
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos, dos, icdDPri = pickOne(icdDA)))))()
  }

  override def scorePatientMeetMeasure(scorecard: Scorecard, patient: Patient, ph: PatientHistory): Scorecard = {

    val measurementInterval = getIntervalFromYears(1)

    def rules = List[(Scorecard) => Scorecard](

      (s: Scorecard) => {
        val claims1 = filterClaims(ph.cpt, cptAS, { claim: MedClaim => measurementInterval.contains(claim.dos) })
        val claims2 = filterClaims(ph.icdD, icdDAS, { claim: MedClaim => measurementInterval.contains(claim.dos) })
        val claims = List.concat(claims1, claims2)
        s.addScore(name, HEDISRule.meetMeasure, wellCareVisit, claims)
      })

    applyRules(scorecard, rules)
  }

}
