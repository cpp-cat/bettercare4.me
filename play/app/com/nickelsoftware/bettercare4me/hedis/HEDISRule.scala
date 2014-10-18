/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package com.nickelsoftware.bettercare4me.hedis

import com.nickelsoftware.bettercare4me.models.Claim
import com.nickelsoftware.bettercare4me.models.Patient
import com.nickelsoftware.bettercare4me.models.PatientHistory
import com.nickelsoftware.bettercare4me.models.PersistenceLayer
import com.nickelsoftware.bettercare4me.models.Provider
import com.nickelsoftware.bettercare4me.models.RuleConfig

/**
 * Singleton to avoid creating empty object over and over again and other useful names
 */
object HEDISRule {

  val meetDemographic: String = "meetDemographic"
  val eligible: String = "eligible"
  val excluded: String = "excluded"
  val meetMeasure: String = "meetMeasure"

  val emptyMeetDemographicCriteriaScore = RuleCriteriaScore(name = meetDemographic)
  val emptyEligibleCriteriaScore = RuleCriteriaScore(name = eligible)
  val emptyExcludedCriteriaScore = RuleCriteriaScore(name = excluded)
  val emptyMeetMeasureCriteriaScore = RuleCriteriaScore(name = meetMeasure)
  val emptyRuleScore = RuleScore()
  val emptyScorecard = Scorecard()
}

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
  val name: String

  /**
   * Indicate the full name of the rule (human readable)
   */
  val fullName: String

  /**
   * Indicate the rule description (human readable)
   */
  val description: String

  /**
   * Configuration object for the rule
   */
  val config: RuleConfig
  
  /**
   * Indicates the rate at which the patients are eligible to the  measure.
   *
   * To be eligible, the patient must first meet the demographic requirements.
   * Example, an  `eligibleRate` of 25 for CDC H1C, means that 25% of patient of age between 18 - 75
   * will have diabetes. Note that all CDC measure should have the same `eligibleRate`
   */
  val eligibleRate: Int

  /**
   * Indicates the rate at which the patients meet the measure, in %
   *
   * (patient in numerator, i.e., meet measure) / (patient in denominator, i.e., not excluded from measure) * 100
   *
   * This rate does not apply to exclusions (patients excluded from measure).
   *
   */
  val meetMeasureRate: Int

  /**
   * Indicates the rate at which patients are excluded from measure, in %
   *
   * Fraction of eligible patients that meet the exclusion criteria:
   * (excluded patients) / (eligible patients)
   */
  val exclusionRate: Int

  /**
   * Generate the claims for the patient to be in the denominator and possibly in the numerator as well.
   *
   * The patient is randomly in the numerator based on the `targetCompliance` rate.
   */
  def generateClaims(persistenceLayer: PersistenceLayer, patient: Patient, provider: Provider, eligibleSimScore: Int, excludedSimScore: Int, meetCriteriaSimScore: Int): List[Claim]

  /**
   * Verify if patient meet the demographic of the measure
   */
  def isPatientMeetDemographic(patient: Patient): Boolean

  /**
   * Verify if patient meet the demographic of the measure (must be called after `scorePatientMeetDemographic)
   *
   * This is a utility method
   */
  def isPatientMeetDemographic(scorecard: Scorecard): Boolean

  /**
   * Verify if the measure is applicable to the patient based on patient's
   * demographics only.
   *
   * The patient may still not be eligible to the measure if the clinical criteria are not met.
   */
  def scorePatientMeetDemographic(scorecard: Scorecard, patient: Patient): Scorecard

  /**
   * Verify if patient meet the eligibility of the measure (must be called after `scorePatientEligible)
   */
  def isPatientEligible(scorecard: Scorecard): Boolean

  /**
   * Verify if patient is eligible to the measure
   *
   * Patient may be eligible to the measure but excluded if meet the exclusion criteria.
   */
  def scorePatientEligible(scorecard: Scorecard, patient: Patient, patientHistory: PatientHistory): Scorecard

  /**
   * Verify if patient meet the exclusion condition of the measure (must be called after `scorePatientExcluded
   */
  def isPatientExcluded(scorecard: Scorecard): Boolean

  /**
   * Verify if patient meet the exclusion condition of the measure
   *
   * Does not verify if patient is eligible, but simply the exclusion criteria
   */
  def scorePatientExcluded(scorecard: Scorecard, patient: Patient, patientHistory: PatientHistory): Scorecard

  /**
   * Verify if the patient is in the numerator of the rule, i.e., meets the measure. (must be called after `scorePatientMeetMeasure
   */
  def isPatientMeetMeasure(scorecard: Scorecard): Boolean

  /**
   * Verify if the patient is in the numerator of the rule, i.e., meets the measure.
   */
  def scorePatientMeetMeasure(scorecard: Scorecard, patient: Patient, patientHistory: PatientHistory): Scorecard

  /**
   * Verify if the patient is in the denominator of the rule, i.e., eligible to the measure and not excluded.
   */
  def isPatientInDenominator(scorecard: Scorecard): Boolean

  /**
   * Apply the rule criteria to patient
   *
   * Apply the following scoring methods:
   * - scorePatientMeetDemographic
   * - scorePatientEligible
   *
   * Then the following methods, conditionally:
   * - scorePatientExcluded (if isPatientEligible is true)
   * - scorePatientMeetMeasure (if isPatientExcluded is false)
   *
   * @param scorecard to be updated with the criteria of this rule
   * @param patient to evaluate on this rule
   * @param ph is the patient claim's history
   * @returns the updated scorecard
   */
  def scoreRule(scorecard: Scorecard, patient: Patient, ph: PatientHistory): Scorecard
}
