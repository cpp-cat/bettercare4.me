/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package com.nickelsoftware.bettercare4me.hedis

import scala.util.Random

import org.joda.time.DateTime
import org.joda.time.Interval
import org.joda.time.LocalDate

import com.nickelsoftware.bettercare4me.hedis.hedis2014.BCSRule
import com.nickelsoftware.bettercare4me.models.Claim
import com.nickelsoftware.bettercare4me.models.Patient
import com.nickelsoftware.bettercare4me.models.PatientHistory
import com.nickelsoftware.bettercare4me.models.PersistenceLayer
import com.nickelsoftware.bettercare4me.models.Provider
import com.nickelsoftware.bettercare4me.models.RuleConfig

/**
 * Trait to define an HEDIS rule.
 *
 * Implementation of this trait are able to generate claims for a patient meeting the rule criteria.
 * The generated claims will be compliant to the rule randomly based on targetCompliance rate (percentage)
 */
trait HEDISRule {

  /**
   * Indicate the name of the rule for configuration and reporting purpose
   */
  def name: String

  /**
   * Indicate the full name of the rule (human readable)
   */
  def fullName: String

  /**
   * Indicate the rule description (human readable)
   */
  def description: String

  /**
   * Indicates the rate at which the patients are eligible to the  measure.
   *
   * To be eligible, the patient must first meet the demographic requirements.
   * Example, an  `eligibleRate` of 25 for CDC H1C, means that 25% of patient of age between 18 - 75
   * will have diabetes. Note that all CDC measure should have the same `eligibleRate`
   */
  def eligibleRate: Int

  /**
   * Indicates the rate at which the patients meet the measure, in %
   *
   * (patient in numerator, i.e., meet measure) / (patient in denominator, i.e., not excluded from measure) * 100
   *
   * This rate does not apply to exclusions (patients excluded from measure).
   *
   */
  def meetMeasureRate: Int

  /**
   * Indicates the rate at which patients are excluded from measure, in %
   *
   * Fraction of eligible patients that meet the exclusion criteria:
   * (excluded patients) / (eligible patients)
   */
  def exclusionRate: Int

  /**
   * Generate the claims for the patient to be in the denominator and possibly in the numerator as well.
   *
   * The patient is randomly in the numerator based on the `targetCompliance` rate.
   */
  def generateClaims(persistenceLayer: PersistenceLayer, patient: Patient, provider: Provider): List[Claim]

  /**
   * Verify if the measure is applicable to the patient based on patient's
   * demographics only.
   *
   * The patient may still not be eligible to the measure if the clinical criteria are not met.
   */
  def isPatientMeetDemographic(patient: Patient): Boolean

  /**
   * Verify if patient is eligible to the measure
   *
   * Patient may be eligible to the measure but excluded if meet the exclusion criteria.
   */
  def isPatientEligible(patient: Patient, patientHistory: PatientHistory): Boolean

  /**
   * Verify if patient meet the exclusion condition of the measure
   *
   * Does not verify if patient is eligible, but simply the exclusion criteria
   */
  def isPatientExcluded(patient: Patient, patientHistory: PatientHistory): Boolean

  /**
   * Verify if the patient is in the numerator of the rule, i.e., meets the measure.
   */
  def isPatientMeetMeasure(patient: Patient, patientHistory: PatientHistory): Boolean

  /**
   * Verify if the patient is in the denominator of the rule, i.e., eligible to the measure and not excluded.
   */
  def isPatientInDenominator(patient: Patient, patientHistory: PatientHistory): Boolean

}

abstract class HEDISRuleBase(config: RuleConfig, hedisDate: DateTime) extends HEDISRule {

  def eligibleRate: Int = config.eligibleRate
  def meetMeasureRate: Int = config.meetMeasureRate
  def exclusionRate: Int = config.exclusionRate

  def generateEligibleClaims(pl: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = List.empty
  def generateExclusionClaims(pl: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = List.empty
  def generateMeetMeasureClaims(pl: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = List.empty
  
  def generateEligibleAndExclusionClaims(pl: PersistenceLayer, patient: Patient, provider: Provider, isExcluded: Boolean): List[Claim] = {
    
    val claims = generateEligibleClaims(pl, patient, provider)
    
    if(isExcluded) List.concat(claims, generateExclusionClaims(pl, patient, provider))
    else claims
  }

  def generateClaims(pl: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = {

    // Check if the patient is to be considered for the rule
    if (isPatientMeetDemographic(patient)) {

      // Check if patient is eligible
      if (Random.nextInt(100) < eligibleRate) {

        // Generate the claims to make the patient eligible
        val isExcluded = Random.nextInt(100) < exclusionRate
        
        val claims = generateEligibleAndExclusionClaims(pl, patient, provider, isExcluded)

        // Check if the patient meet the excluded criteria, if not check if meet criteria
        if (isExcluded) {

          // Already generated the claim to meet the exclusion criteria
          claims

        } else {

          // Check if the patient is in the measure
          if (Random.nextInt(100) < meetMeasureRate) {

            // Generate the claim to meet the measure
            List.concat(claims, generateMeetMeasureClaims(pl, patient, provider))

          } else {

            // Patient does not meet the measure
            claims
          }
        }

      } else {

        // Patient does not meet eligibility criteria
        List.empty
      }

    } else {

      // Patient does not meet demographics
      List.empty
    }
  }

  /**
   * Utility that returns `true` as soon as the first function in the `rules` list returns `true`
   *
   * Returns `false` if the `rules` list is empty
   */
  def isAnyRuleMatch(rules: List[() => Boolean]): Boolean = {
    if (rules.isEmpty) false
    else if ((rules.head)()) true
    else isAnyRuleMatch(rules.tail)
  }

  /**
   * Utility that evaluate the function `f` for each element in `l`
   * and returns `true` as soon as one element of `l` makes `f` to evaluate to `true`
   */
  def firstTrue[A](l: List[A], f: A => Boolean): Boolean = {
    if (l.isEmpty) false
    else if (f(l.head)) true
    else firstTrue(l.tail, f)
  }

  /**
   * Utility that evaluate the function `f` for each claim that has a key in `m` that
   * is present in the set `s`
   * 
   * @return `true` as soon as one claim makes `f` to evaluate to `true`
   */
  def firstMatch[C](m: Map[String, List[C]], s: Set[String], f: C => Boolean): Boolean = {
    if (m.isEmpty) false
    else {
      val key = m.head._1
      if(s.contains(key) && firstTrue(m.head._2, f)) true
      else firstMatch(m.tail, s, f)
    }
  }

  def isPatientEligible(patient: Patient, patientHistory: PatientHistory): Boolean = isPatientMeetDemographic(patient)
  def isPatientInDenominator(patient: Patient, patientHistory: PatientHistory): Boolean = isPatientEligible(patient, patientHistory) && !isPatientExcluded(patient, patientHistory)

  /**
   * Utility method to pick randomly one item from the list
   */
  def pickOne[A](items: List[A]): A = items(Random.nextInt(items.size))

  /**
   * Utility method to get an `Interval` from the `hedisDate` to the nbr of specified days prior to it.
   *
   * @param nbrDays number of days for the interval, just prior the `hedisDate`
   * @return The calculated interval, excluding the hedisDate
   */
  def getInterval(nbrDays: Int): Interval = new Interval(hedisDate.minusDays(nbrDays), hedisDate)
}

object HEDISRules {

  val createRuleByName: Map[String, (RuleConfig, DateTime) => HEDISRule] = Map(
    "TEST" -> { (c, d) => new TestRule(c, d) },
    "BCS-HEDIS-2014" -> { (c, d) => new BCSRule(c, d) })

}

class TestRule(config: RuleConfig, hedisDate: DateTime) extends HEDISRuleBase(config, hedisDate) {

  def name = "TEST"
  def fullName = "Test Rule"
  def description = "This rule is for testing."
    
  def isPatientMeetDemographic(patient: Patient): Boolean = true
  def isPatientExcluded(patient: Patient, patientHistory: PatientHistory): Boolean = false
  def isPatientMeetMeasure(patient: Patient, patientHistory: PatientHistory): Boolean = true

  override def generateClaims(persistenceLayer: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = {
    val dos = new LocalDate(2014, 9, 5).toDateTimeAtStartOfDay()
    List(
      persistenceLayer.createMedClaim(patient.patientID, provider.providerID, dos, dos,
        icdDPri = "icd 1", icdD = Set("icd 1", "icd 2"), icdP = Set("icd p1"),
        hcfaPOS = "hcfaPOS", ubRevenue = "ubRevenue"))
  }
}
