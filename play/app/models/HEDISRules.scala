/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package models

/**
 * Trait to define an HEDIS rule.
 *
 * Implementation of this trait are able to generate claims for a patient meeting the rule criteria.
 * The generated claims will be compliant to the rule randomly based on targetCompliance rate (percentage)
 */
trait HEDISRule {

  /**
   * Indicates the rate at which the patients are eligible to the  measure.
   *
   * To be eligible, the patient must first meet the demographic requirements.
   * Example, an \c eligibleRate of 25 for CDC H1C, means that 25% of patient of age between 18 - 75
   * will have diabetes. Note that all CDC measure should have the same \c eligibleRate
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
   * The patient is randomly in the numerator based on the \c targetCompliance rate.
   */
  def generateClaims(patient: Patient, provider: Provider): List[Claim]

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

class HEDISRuleBase(eligRate: Int, meetMeasRate: Int, exclRate: Int) extends HEDISRule {

  def eligibleRate: Int = eligRate
  def meetMeasureRate: Int = meetMeasRate
  def exclusionRate: Int = exclRate

  def generateClaims(patient: Patient, provider: Provider): List[Claim] = List.empty
  def isPatientMeetDemographic(patient: Patient): Boolean = true
  def isPatientEligible(patient: Patient, patientHistory: PatientHistory): Boolean = isPatientMeetDemographic(patient)
  def isPatientExcluded(patient: Patient, patientHistory: PatientHistory): Boolean = false
  def isPatientMeetMeasure(patient: Patient, patientHistory: PatientHistory): Boolean = true
  def isPatientInDenominator(patient: Patient, patientHistory: PatientHistory): Boolean = isPatientEligible(patient, patientHistory) && !isPatientExcluded(patient, patientHistory)
}