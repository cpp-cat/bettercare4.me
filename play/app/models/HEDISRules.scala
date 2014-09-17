/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package models

import org.joda.time.LocalDate
import scala.util.Random
import org.joda.time.Interval
import org.joda.time.DateTime

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

  def generateClaims(pl: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = {

    // Check if the patient is to be considered for the rule
    if (isPatientMeetDemographic(patient)) {

      // Check if patient is eligible
      if (Random.nextInt(100) < eligibleRate) {

        // Generate the claims to make the patient eligible
        val claims = generateEligibleClaims(pl, patient, provider)

        // Check if the patient will meet the exclusion criteria
        if (Random.nextInt(100) < exclusionRate) {

          // Generate the claim to meet the exclusion criteria
          List.concat(claims, generateExclusionClaims(pl, patient, provider))

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
   * Utility that returns \c true as soon as the first function in the \c rules list returns \c true
   *
   * Returns \c false if the \c rules list is empty
   */
  def isAnyRuleMatch(rules: List[() => Boolean]): Boolean = {
    if (rules.isEmpty) false
    else if ((rules.head)()) true
    else isAnyRuleMatch(rules.tail)
  }

  /**
   * Utility that evaluate the function \c f for each element in \c l
   * and returns \c true as soon as one element of \c l makes \c f to evaluate to \c true
   */
  def firstTrue[A](l: List[A], f: A => Boolean): Boolean = {
    if (l.isEmpty) false
    else if (f(l.head)) true
    else firstTrue(l.tail, f)
  }

  def isPatientEligible(patient: Patient, patientHistory: PatientHistory): Boolean = isPatientMeetDemographic(patient)
  def isPatientInDenominator(patient: Patient, patientHistory: PatientHistory): Boolean = isPatientEligible(patient, patientHistory) && !isPatientExcluded(patient, patientHistory)

  /**
   * Utility method to pick randomly one item from the list
   */
  def pickOne[A](items: List[A]): A = items(Random.nextInt(items.size))

  /**
   * Utility method to get an \c Interval from the \c hedisDate to the nbr of specified days prior to it.
   *
   * This interval exclude the hedisDate
   */
  def getInterval(nbrDays: Int): Interval = new Interval(hedisDate.minusDays(nbrDays), hedisDate)
}

object HEDISRules {

  val createRuleByName: Map[String, (RuleConfig, DateTime) => HEDISRule] = Map(
    "TEST" -> { (c, d) => new TestRule(c, d) },
    "BCS" -> { (c, d) => new BCSRule(c, d) })

}

// define all rules

/**
 * Breast Cancer Screening Rule
 */
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

/**
 * Breast Cancer Screening Rule
 */
class BCSRule(config: RuleConfig, hedisDate: DateTime) extends HEDISRuleBase(config, hedisDate) {

  def name = "BCS"
  def fullName = "Breast Cancer Screening"
  def description = "Breast Cancer Screening indicates whether a woman member, aged 42 to 69 years, had a mammogram done during the " +
    "measurement year or the year prior to the measurement year. This excludes women who had a bilateral mastectomy or two " +
    "unilateral mastectomies."

  override def isPatientMeetDemographic(patient: Patient): Boolean = {
    val age = patient.age(hedisDate)
    patient.gender == "F" && age > 41 && age < 70
  }

  // This rule has 100% eligibility when the demographics are meet
  override def eligibleRate: Int = 100

  override def generateExclusionClaims(pl: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = {

    val days = new Interval(hedisDate.minusYears(2), hedisDate).toDuration().getStandardDays().toInt
    val dos1 = hedisDate.minusDays(Random.nextInt(days))
    pickOne(List(
      // One possible set of claims based on ICD Procedure code
       () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos1, dos1, icdP = Set(pickOne(List("85.42", "85.44", "85.46", "85.48"))))),
      // Another possible set of claims based on CPT codes and modifier being 50
       () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos1, dos1, cpt = pickOne(List("19180", "19200", "19220", "19240", "19303", "19304", "19305", "19306", "19307")), cptMod1 = "50")),
       () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos1, dos1, cpt = pickOne(List("19180", "19200", "19220", "19240", "19303", "19304", "19305", "19306", "19307")), cptMod2 = "50")),
      // Another possible set of claims based on 2 claims have CPT codes and each have one of the modifier RT and LT
      {() =>
        val dos2 = hedisDate.minusDays(Random.nextInt(days))
        List(
          pl.createMedClaim(patient.patientID, provider.providerID, dos1, dos1, cpt = pickOne(List("19180", "19200", "19220", "19240", "19303", "19304", "19305", "19306", "19307")), cptMod1 = "RT"),
          pl.createMedClaim(patient.patientID, provider.providerID, dos2, dos2, cpt = pickOne(List("19180", "19200", "19220", "19240", "19303", "19304", "19305", "19306", "19307")), cptMod2 = "LT"))
      }))()
  }

  override def isPatientExcluded(patient: Patient, ph: PatientHistory): Boolean = {

    def hasCPTwithMod(mod: String): Boolean = {
      firstTrue(List("19180", "19200", "19220", "19240", "19303", "19304", "19305", "19306", "19307"), { cpt: String =>
        firstTrue(ph.claims4CPT(cpt), { claim: MedClaim =>
          claim.dos.isBefore(hedisDate) && (claim.cptMod1 == mod || claim.cptMod2 == mod)
        })
      })
    }

    def rules = List[() => Boolean](
      // Check if patient had Bilateral Mastectomy (anytime prior to or during the measurement year)
      () => firstTrue(List("85.42", "85.44", "85.46", "85.48"), { icdP: String =>
        firstTrue(ph.claims4ICDP(icdP), { claim: MedClaim =>
          claim.dos.isBefore(hedisDate)
        })
      }),

      // Check if patient had a Unilateral Mastectomy with bilateral modifier (anytime prior to or during the measurement year)
      () => hasCPTwithMod("50"),

      // Check if patient had a previous right unilateral mastectomy and a previous
      // left unilateral mastectomy (anytime prior to or during the measurement year)
      () => hasCPTwithMod("RT") && hasCPTwithMod("LT"))

    isAnyRuleMatch(rules)
  }

  override def generateMeetMeasureClaims(pl: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = {

    val days = new Interval(hedisDate.minusYears(2), hedisDate).toDuration().getStandardDays().toInt
    val dos = hedisDate.minusDays(Random.nextInt(days))
    pickOne(List(
      // One possible set of claims based on cpt
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos, dos, cpt = pickOne(List("76083", "76090", "76091", "76092")))),
      // Another possible set of claims based on hcpcs
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos, dos, hcpcs = pickOne(List("G0202", "G0204", "G0206")))),
      // Another possible set of claims based on ICD Procedure codes
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos, dos, icdP = Set(pickOne(List("87.36", "87.37"))))),
      // Another possible set of claims based on UB Revenue Procedure codes
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos, dos, ubRevenue = pickOne(List("0401", "0403"))))))()
  }

  override def isPatientMeetMeasure(patient: Patient, ph: PatientHistory): Boolean = {

    val measurementInterval = new Interval(hedisDate.minusYears(2), hedisDate)
    def rules = List[() => Boolean](
      // Check if patient had a Mamogram performed (during the measurement year or the year before)
      () => firstTrue(List("76083", "76090", "76091", "76092"), { cpt: String =>
        firstTrue(ph.claims4CPT(cpt), { claim: MedClaim => measurementInterval.contains(claim.dos) })
      }),

      () => firstTrue(List("G0202", "G0204", "G0206"), { hcpcs: String =>
        firstTrue(ph.claims4HCPCS(hcpcs), { claim: MedClaim => measurementInterval.contains(claim.dos) })
      }),

      () => firstTrue(List("87.36", "87.37"), { icdP: String =>
        firstTrue(ph.claims4ICDP(icdP), { claim: MedClaim => measurementInterval.contains(claim.dos) })
      }),

      () => firstTrue(List("0401", "0403"), { ubRevevue: String =>
        firstTrue(ph.claims4UBRev(ubRevevue), { claim: MedClaim => measurementInterval.contains(claim.dos) })
      }))

    isAnyRuleMatch(rules)
  }

}
