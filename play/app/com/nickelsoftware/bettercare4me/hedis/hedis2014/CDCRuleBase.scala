/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package com.nickelsoftware.bettercare4me.hedis.hedis2014

import scala.util.Random
import org.joda.time.DateTime
import org.joda.time.Interval
import com.nickelsoftware.bettercare4me.hedis.HEDISRuleBase
import com.nickelsoftware.bettercare4me.models.Claim
import com.nickelsoftware.bettercare4me.models.MedClaim
import com.nickelsoftware.bettercare4me.models.RxClaim
import com.nickelsoftware.bettercare4me.models.Patient
import com.nickelsoftware.bettercare4me.models.PatientHistory
import com.nickelsoftware.bettercare4me.models.PersistenceLayer
import com.nickelsoftware.bettercare4me.models.Provider
import com.nickelsoftware.bettercare4me.models.RuleConfig
import com.github.tototoshi.csv.CSVReader
import java.io.File
import com.nickelsoftware.bettercare4me.utils.NickelException

object CDC {
  val ndcAKey = "ndc.cdc.a.fname"
  
  //*
  val ndcA: List[String] = CSVReader.open(new File("./data/cdc.ndc.a.csv")).all().flatten
  val ndcAS = ndcA.toSet

  /**
   * ICD Diagnosis code for type 1 or type 2 diabetes
   */
  val icd9DA = List(
    "250.0", "250.00", "250.01", "250.02", "250.03",
    "250.1", "250.10", "250.11", "250.12", "250.13",
    "250.2", "250.20", "250.21", "250.22", "250.23",
    "250.3", "250.30", "250.31", "250.32", "250.33",
    "250.4", "250.40", "250.41", "250.42", "250.43",
    "250.5", "250.50", "250.51", "250.52", "250.53",
    "250.6", "250.60", "250.61", "250.62", "250.63",
    "250.7", "250.70", "250.71", "250.72", "250.73",
    "250.8", "250.80", "250.81", "250.82", "250.83",
    "250.9", "250.90", "250.91", "250.92", "250.93",
    "357.2", "366.41",
    "362.0", "362.01", "362.02", "362.03", "362.04", "362.05", "362.06", "362.07",
    "648.0", "648.01", "648.02", "648.03", "648.04")
  val icd9DAS = icd9DA.toSet

  /**
   * ICD Diagnosis for Gestational diabetes or steroid-induced diabetes
   * to identify exclusions.
   */
  val icd9DB = List(
    "249.0", "249.00", "249.01",
    "249.1", "249.10", "249.11",
    "249.2", "249.20", "249.21",
    "249.3", "249.30", "249.31",
    "249.4", "249.40", "249.41",
    "249.5", "249.50", "249.51",
    "249.6", "249.60", "249.61",
    "249.7", "249.70", "249.71",
    "249.8", "249.80", "249.81",
    "249.9", "249.90", "249.91",
    "251.8",
    "648.8", "648.80", "648.81", "648.82", "648.83", "648.84",
    "962.0")
  val icd9DBS = icd9DB.toSet

  /**
   * CPT codes for face-to-face encounters in an ambulatory or nonacute inpatient
   * setting with a diagnosis of diabetes
   */
  val cptA = List(
    "99201", "99202", "99203", "99204", "99205",
    "99211", "99212", "99213", "99214", "99215",
    "99217", "99218", "99219", "99220",
    "99241", "99242", "99243", "99244", "99245",
    "99301", "99302", "99303", "99315", "99316", "99318",
    "99321", "99322", "99323", "99324", "99325", "99326", "99327", "99328",
    "99331", "99332", "99333", "99334", "99335", "99336", "99337",
    "99341", "99342", "99343", "99344", "99345",
    "99347", "99348", "99349", "99350",
    "99384", "99385", "99386", "99387",
    "99394", "99395", "99396", "99397",
    "99401", "99402", "99403", "99404",
    "99411", "99412", "99420", "99429", "99455", "99456")

  /**
   * UB Revenue codes for face-to-face encounters in an ambulatory or nonacute inpatient
   * setting with a diagnosis of diabetes
   */
  val ubA = List(
    "0118", "0128", "0138", "0148", "0158",
    "0190", "0191", "0192", "0193", "0194", "0195", "0196", "0197", "0198", "0199", "019X",
    "0510", "0511", "0512", "0513", "0514", "0515", "0516", "0517", "0518", "0519", "051X",
    "0520", "0521", "0522", "0523", "0524", "0525", "0526", "0527", "0528", "0529", "052X",
    "0550", "0551", "0552", "0553", "0554", "0555", "0556", "0557", "0558", "0559", "055X",
    "0570", "0571", "0572", "0573", "0574", "0575", "0576", "0577", "0578", "0579", "057X",
    "0580", "0581", "0582", "0583", "0584", "0585", "0586", "0587", "0588", "0589", "058X",
    "0590", "0591", "0592", "0593", "0594", "0595", "0596", "0597", "0598", "0599",
    "0660", "0661", "0662", "0663", "0664", "0665", "0666", "0667", "0668", "0669", "066X",
    "0820", "0821", "0822", "0823", "0824", "0825", "0826", "0827", "0828", "0829", "082X",
    "0830", "0831", "0832", "0833", "0834", "0835", "0836", "0837", "0838", "0839", "083X",
    "0840", "0841", "0842", "0843", "0844", "0845", "0846", "0847", "0848", "0849", "084X",
    "0850", "0851", "0852", "0853", "0854", "0855", "0856", "0857", "0858", "0859",
    "0880", "0881", "0882", "0883", "0884", "0885", "0886", "0887", "0888", "0889", "088X",
    "0982", "0983")

  /**
   * Place-of-Service codes for face-to-face encounters in an ambulatory or nonacute inpatient
   * setting with a diagnosis of diabetes
   */
  val posA = List(
    "4", "5", "6", "7", "8", "9", "10", "11", "12", "15", "20", "22", "24",
    "26", "49", "50", "52", "53", "57", "61", "62", "65", "71", "72", "95", "99")

  /**
   * UB Revenue codes for face-to-face encounter in an acute
   * inpatient or emergency room setting with a diagnosis
   * of diabetes
   */
  val ubB = List(
    "0100", "0101", "0102", "0103", "0104", "0105", "0106", "0107", "0108", "0109", "010X",
    "0110", "0111", "0112", "0113", "0114", "0119",
    "0120", "0121", "0122", "0123", "0124", "0129",
    "0130", "0131", "0132", "0133", "0134", "0139",
    "0140", "0141", "0142", "0143", "0144", "0149",
    "0150", "0151", "0152", "0153", "0154", "0159",
    "0160", "0161", "0162", "0163", "0164", "0165", "0166", "0167", "0168", "0169", "016X",
    "0200", "0201", "0202", "0203", "0204", "0205", "0206", "0207", "0208", "0209", "020X",
    "0210", "0211", "0212", "0213", "0214", "0215", "0216", "0217", "0218", "0219",
    "0450", "0451", "0452", "0453", "0454", "0455", "0456", "0457", "0458", "0459", "045X",
    "0720", "0721", "0722", "0723", "0724", "0725", "0726", "0727", "0728", "0729", "072X",
    "0800", "0801", "0802", "0803", "0804", "0805", "0806", "0807", "0808", "0809", "080X",
    "0981", "0987")

  /**
   * CPT codes for face-to-face encounter in an acute
   * inpatient or emergency room setting with a diagnosis
   * of diabetes
   */
  val cptB = List(
    "99221", "99222", "99223", "99231", "99232", "99233",
    "99238", "99239", "99251", "99252", "99253", "99254", "99255", "99261", "99262", "99263",
    "99281", "99282", "99283", "99284", "99285", "99291")

  /**
   * Place-of-Service codes for face-to-face encounter in an acute
   * inpatient or emergency room setting with a diagnosis
   * of diabetes
   */
  val posB = List("21", "23", "25", "51", "55")
}

/**
 * Comprehensive Diabetes Control Base Rule
 *
 * The base rule implements rules that are common to all CDC rules, such the determination if the
 * patient has diabetes.
 *
 * DENOMINATOR:
 * Identifies the unique count of patients, aged 18 to 75 years, with type 1 or type 2 diabetes. It excludes patients with a previous
 * diagnosis of polycystic ovaries, gestational diabetes, or steroid-induced diabetes.
 *
 * EXCLUSION:
 * Excludes from the eligible population all patients with a history of polycystic ovaries (based on claims included in the database),
 * gestational diabetes, or steroid-induced diabetes, and who did not have a face-to-face encounter with the diagnosis of diabetes in
 * any setting during the measurement year or the year prior.
 */
abstract class CDCRuleBase(config: RuleConfig, hedisDate: DateTime) extends HEDISRuleBase(config, hedisDate) {

  import CDC._

  //
  // isPatientMeetDemographic
  //
  override def isPatientMeetDemographic(patient: Patient): Boolean = {
    val age = patient.age(hedisDate)
    age > 17 && age < 76
  }

  //
  // generateEligibleAndExclusionClaims
  //
  override def generateEligibleAndExclusionClaims(pl: PersistenceLayer, patient: Patient, provider: Provider, isExcluded: Boolean): List[Claim] = {

    val days = new Interval(hedisDate.minusYears(2), hedisDate).toDuration().getStandardDays().toInt
    val dos1 = hedisDate.minusDays(Random.nextInt(days))
    val dos2 = hedisDate.minusDays(Random.nextInt(days))
    val excludedClaim = pl.createMedClaim(patient.patientID, provider.providerID, dos1, dos1, icdDPri = pickOne(icd9DB), cpt = pickOne(cptA))
    pickOne(List(

      // One possible set of claims based on NDC code - patient taking diabetes drug
      () => {
        val ndc1 = pickOne(ndcA)
        val eligibleClaims = List(
          pl.createRxClaim(patient.patientID, provider.providerID, dos1, ndc = ndc1, daysSupply = 90, qty = 90),
          pl.createRxClaim(patient.patientID, provider.providerID, dos1.minusDays(80 + Random.nextInt(20)), ndc = ndc1, daysSupply = 90, qty = 90),
          pl.createRxClaim(patient.patientID, provider.providerID, dos1.minusDays(170 + Random.nextInt(30)), ndc = ndc1, daysSupply = 90, qty = 90))

        if (isExcluded) List.concat(eligibleClaims, generateExclusionClaims(pl, patient, provider))
        else eligibleClaims
      },

      // Another possible set of claims based on 2 face-to-face diagnosis
      () => {
        val eligibleClaims = List(
        pickOne(List(
          pl.createMedClaim(patient.patientID, provider.providerID, dos1, dos1, icdDPri = pickOne(icd9DA), cpt = pickOne(cptA)),
          pl.createMedClaim(patient.patientID, provider.providerID, dos1, dos1, icdDPri = pickOne(icd9DA), ubRevenue = pickOne(ubA), hcfaPOS = pickOne(posA)))),
        pickOne(List(
          pl.createMedClaim(patient.patientID, provider.providerID, dos2, dos2, icdDPri = pickOne(icd9DA), ubRevenue = pickOne(ubA), hcfaPOS = pickOne(posA)),
          pl.createMedClaim(patient.patientID, provider.providerID, dos2, dos2, icdDPri = pickOne(icd9DA), cpt = pickOne(cptA)))))

        if (isExcluded) excludedClaim :: eligibleClaims
        else eligibleClaims
      },

      // Another possible set of claims based on 1 face to face in ER
      () => {
        val eligibleClaim = pl.createMedClaim(patient.patientID, provider.providerID, dos1, dos1, icdDPri = pickOne(icd9DA), cpt = pickOne(cptB)) 
        if(isExcluded) List(eligibleClaim, excludedClaim)
        else List(eligibleClaim)
      },
      () => {
        val eligibleClaim = pl.createMedClaim(patient.patientID, provider.providerID, dos1, dos1, icdDPri = pickOne(icd9DA), ubRevenue = pickOne(ubB), hcfaPOS = pickOne(posB)) 
        if(isExcluded) List(eligibleClaim, excludedClaim)
        else List(eligibleClaim)
      },

      // Another possible set of claims based on 1 face to face in ER with gestional diabetes or steroid-induced diabetes (test the exclusion creteria)
      () => {
        val eligibleClaim = pl.createMedClaim(patient.patientID, provider.providerID, dos1, dos1, icdDPri = pickOne(icd9DB), icdD = Set(pickOne(icd9DA)), cpt = pickOne(cptB)) 
        if(isExcluded) List(eligibleClaim, excludedClaim)
        else List(eligibleClaim)
      },
      () => {
        val eligibleClaim = pl.createMedClaim(patient.patientID, provider.providerID, dos1, dos1, icdDPri = pickOne(icd9DB), icdD = Set(pickOne(icd9DA)), ubRevenue = pickOne(ubB), hcfaPOS = pickOne(posB)) 
        if(isExcluded) List(eligibleClaim, excludedClaim)
        else List(eligibleClaim)
      }))()
  }

  //
  // isPatientEligible
  //
  override def isPatientEligible(patient: Patient, ph: PatientHistory): Boolean = {

    val measurementInterval = new Interval(hedisDate.minusYears(2), hedisDate)

    // returns the date of a claim that match the face 2 face criteria that is on a different date than the argument
    // otherwise returns None
    def hasFace2Face(d: DateTime, cptL: List[String], ubL: List[String], posL: List[String]): Option[DateTime] = {
      var ret: Option[DateTime] = None
      firstMatch(ph.icdD, icd9DAS, { claim: MedClaim =>
        claim.dos != d && measurementInterval.contains(claim.dos) && (
          firstTrue(cptL, { cpt: String => if (claim.cpt == cpt) { ret = Some(claim.dos); true } else false }) ||
          firstTrue(ubL, { ubRevenue: String => claim.ubRevenue == ubRevenue && firstTrue(posL, { pos: String => if (claim.hcfaPOS == pos) { ret = Some(claim.dos); true } else false }) }))
      })
      ret
    }

    def rules = List[() => Boolean](

      // Check if patient is taking diabetes drugs
      () => firstMatch(ph.ndc, ndcAS, { claim: RxClaim => measurementInterval.contains(claim.fillD) }),

      // check if patient has 2 face 2 face on different dates
      () => {
        val date = hasFace2Face(hedisDate, cptA, ubA, posA)
        !(date map { hasFace2Face(_, cptA, ubA, posA) } isEmpty)
      },

      // check if patient has 1 face 2 face in ER
      () => !(hasFace2Face(hedisDate, cptB, ubB, posB) isEmpty))

    isPatientMeetDemographic(patient) && isAnyRuleMatch(rules)
  }

  //
  // generateExclusionClaims
  //
  override def generateExclusionClaims(pl: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = {

    val days = new Interval(hedisDate.minusYears(2), hedisDate).toDuration().getStandardDays().toInt
    val dos1 = hedisDate.minusDays(Random.nextInt(days))
    pickOne(List(

      // One possible set of claims based on Polycystic ovaries (anytime prior to or during the measurement year)
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos1, dos1, icdDPri = "256.4")),

      // Another possible set of claims based on Gestational diabetes or steroid-induced diabetes and no face 2 face diagnosis
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos1, dos1, icdDPri = pickOne(icd9DB), cpt = pickOne(cptA))),
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos1, dos1, icdDPri = pickOne(icd9DB), ubRevenue = pickOne(ubA), hcfaPOS = pickOne(posA))),
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos1, dos1, icdDPri = pickOne(icd9DB), cpt = pickOne(cptB))),
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos1, dos1, icdDPri = pickOne(icd9DB), ubRevenue = pickOne(ubB), hcfaPOS = pickOne(posB)))))()
  }

  //
  // isPatientExcluded
  //
  override def isPatientExcluded(patient: Patient, ph: PatientHistory): Boolean = {

    val measurementInterval = new Interval(hedisDate.minusYears(2), hedisDate)

    def rules = List[() => Boolean](
      // Check if patient had Polycystic ovaries (anytime prior to or during the measurement year)
      () => firstTrue(ph.claims4ICDD("256.4"), { claim: MedClaim => measurementInterval.contains(claim.dos) }),

      // Check if patient had a Gestational diabetes or steroid-induced diabetes and no face 2 face diagnosis
      // - Find a claim with diagnosis for Gestational diabetes or steroid-induced diabetes (icd9DBS)
      // - Check that the claim does not have a primary or secondary diagnosis for diabetes (icd9DAS)
      () => firstMatch(ph.icdD, icd9DBS, { claim: MedClaim => (claim.icdD + claim.icdDPri).find({ icd9DAS.contains(_) }) isEmpty }))

    isAnyRuleMatch(rules)
  }
}

