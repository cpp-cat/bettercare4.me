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
import com.nickelsoftware.bettercare4me.models.RxClaim
import com.nickelsoftware.bettercare4me.models.Patient
import com.nickelsoftware.bettercare4me.models.PatientHistory
import com.nickelsoftware.bettercare4me.models.PersistenceLayer
import com.nickelsoftware.bettercare4me.models.Provider
import com.nickelsoftware.bettercare4me.models.RuleConfig

object ASM {

  val name5 = "ASM-5-11-HEDIS-2014"
  val name12 = "ASM-12-18-HEDIS-2014"
  val name19 = "ASM-19-50-HEDIS-2014"
  val name50 = "ASM-50-64-HEDIS-2014"

  // Table ASM­-C: Asthma Medications (eligibility criteria list)
  // -----------
  // NDC of short acting agents only (all meds minus controller meds)
  // - eligible but does not meet the measure (for generateEligibleClaims w/o triggering meet measure)
  // - These meds are all inhaler meds
  val ndcET = List("")
  
  // NDC of all asthma medications (Table ASM­-C)
  val ndcE = List("")
  val ndcES = ndcE.toSet
  
  // Asthma Medication attributes
  // List(ndc_code,	brand_name,	generic_product_name,	route,	category,	drug_id)
  val NDCCODE = 0
  val ROUTE = 3
  val CATEGORY = 4
  val asthmaMeds = Map(("" -> List("")))
  
  // -----------
  // NDC of Oral Asthma Medications (excluding leukotriene modifiers)
  val ndcEO = List("")
  
  // NDC of Inhaler Asthma Medications
  val ndcEI = List("")
  
  // NDC of Asthma Medications with Leukotriene Modifier (therefore oral medications)
  val ndcEL = List("")
  

  // Table ASM­-D: Asthma Controller Medications (meet measure medication list)
  // -----------
  
  // Identify people with asthma 
  //@TODO expand codes
  val icdDA = List("493.0*", "493.1*", "493.8*", "493.9*")
  val icdDAS = icdDA.toSet
  
  // CPT for ED visit type
  //@TODO expand codes
  val cptA = List("99281-99285")
  val cptAS = cptA.toSet
  
  // UB for ED visit type
  //@TODO expand codes
  val ubA = List("045*, 0981")
  val ubAS = ubA.toSet
  
  // CPT for Acute Inpatient visit type
  //@TODO expand codes
  val cptB = List("99221-99223", "99231-99233", "99238", "99239", "99251-99255", "99261-99263", "99291")
  val cptBS = cptB.toSet
  
  // UB for Acute Inpatient visit type
  //@TODO expand codes
  val ubB = List("010*", "0110-0114", "0119", "0120-0124", "0129", "0130-0134", "0139", "0140-0144", "0149", "0150-0154", "0159", "016*", "020*", "021*", "072*", "0987")
  val ubBS = ubB.toSet
  
  // CPT for Outpatient visit type
  //@TODO expand codes
  val cptC = List("99201-99205", "99211-99215", "99217-99220", "99241-99245", "99341-99345", "99347-99350", "99382-99386", "99392-99396", "99401-99404", "99411", "99412", "99420", "99429")
  val cptCS = cptC.toSet
  
  // UB for Outpatient visit type
  //@TODO expand codes
  val ubC = List("051*", "0520-0523", "0526-0529", "0570-0599", "0982", "0983")
  val ubCS = ubC.toSet
}
/**
 * Asthma Medication Management Rule
 *
 * This measure is based on the HEDIS measure Use of Appropriate Medications for People with Asthma (ASM).
 *
 * Asthma Medication Management indicates whether a patient with persistent asthma, aged 5 to 64 years, was prescribed an
 * inhaled corticosteroid or an acceptable alternative of preferred asthma therapy medication. This excludes patients with a
 * diagnosis of emphysema, chronic obstructive pulmonary disease (COPD), cystic fibrosis, or acute respiratory failure.
 *
 * DENOMINATOR:
 * dentifies the unique count of patients, age 5 to 64 years at the end of the measurement year, with persistent asthma. Patients
 * are identified as having persistent asthma if they meet at least one of the 5 criteria below during both the measurement year and
 * the year prior to the measurement year (criteria need not be the same across both years).
 *
 *
 * EXCLUSIONS:
 * Excludes patients with a diagnosis of emphysema, COPD, cystic fibrosis, or acute respiratory failure, anytime prior to or during
 * the measurement period (based on claims included in the database).
 *
 * NUMERATOR:
 * Identifies patients with persistent asthma, aged 5 to 64 years, who were prescribed an inhaled corticosteroid or an acceptable
 * alternative of preferred asthma therapy medication.
 * 
 * NOTE:
 * 1. NCQA has clarified the drug dispensing event calculation rules as follows:
 * 	a. NCQA criteria state that “a dispensing event is one prescription of an amount lasting 30 days or less. To calculate dispensing events for prescriptions longer than 30 days, divide the days supply by 30 and round down to convert. For example, a 100-day prescription is equal to three dispensing events (100/30 = 3.33, rounded down to 3)”.
 *	b. NCQA criteria also state that “the organization should allocate dispensing events to the appropriate year based on the date on which the prescription is filled. For two different prescriptions dispensed on the same day, sum the days supply to determine the number of dispensing events”. Since this rule is counting dispensing events rather than the number of days that a patient is on a drug, all overlap days are counted when calculating these events. According to the HEDIS auditor, NCQA issued a further clarification in February 2009 that stated that the days supply should always be summed, regardless of whether the prescriptions are for the same drug or different drugs. The days supply for all drugs should be summed prior to dividing the days by 30 when calculating the number of events.
 *	c. For inhaler dispensing events, NCQA criteria states that “inhalers count as one dispensing event; for example, an inhaler with a 90-day supply is considered one dispensing event. In addition, multiple inhalers of the same medication (as identified by Drug ID in the NDC list) filled on the same date of service should be counted as one dispensing event; for example a patient may obtain two inhalers on the same day (one for home and one for work), but intend to use both during the same 30- day period. Inhaler prescriptions are counted as one event, regardless of the number of days supply. Inhaler prescriptions for the same drug dispensed on the same date are also counted as one event. The Drug ID field is used to determine if inhaler prescriptions are for the same drug or different drugs. If the Drug ID is different, count it as a separate event.
 * 2. In 2012, the upper age limit was increased to 64.
 * 3. In 2012, ICD-9 Diagnosis code 493.2 was deleted from Table ASM-A.
 * 4. In 2012, mometasone-formoterol was added to "Inhaled steroid combinations" description in Tables ASM-C and ASM-D.
 * 5. In 2013, the definition of inhaler dispensing event was revised to indicate that multiple dispensing events of the same Drug ID on the same date of service are counted as separate dispensing events (USE THIS LATEST DEFINITION).
 *
 */
class ASM_Rule(override val name: String, tag: String, ageLo: Int, ageHi: Int, config: RuleConfig, hedisDate: DateTime) extends HEDISRuleBase(config, hedisDate) {

  val fullName = "Asthma Medication Management (" + tag + ")"
  val description = "Identifies the unique count of patients, age " + tag + " years at the end of the measurement year, with persistent asthma. Patients " +
    "are identified as having persistent asthma if they meet at least one of the 5 criteria below during both the measurement year and " +
    "the year prior to the measurement year (criteria need not be the same across both years)."

  import ASM._

  override def isPatientMeetDemographic(patient: Patient): Boolean = {
    val age = patient.age(hedisDate)
    age >= ageLo && age <= ageHi
  }

  override def generateEligibleClaims(pl: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = {

    val offset = getIntervalFromYears(1).toDuration().getStandardDays().toInt
	val days = offset/4
    
    // compute dos from a base nbr of days and a variable nbr of days (called ext)
    def dos(base: Int, ext: Int) = hedisDate.minusDays(base + Random.nextInt(ext))
    
  /**
   * For the purpose of generating claims that trigger criteria eligibility (denominator) 
   * w/o meeting the measure criteria (numerator), the following cases are used to determine eligible patients:
   * - One emergency department (ED) visit with a principal diagnosis of asthma
   * - One acute inpatient discharge with a principal diagnosis of asthma
   * - Four outpatient asthma visits and at 2 asthma medication dispensing events (using short acting agents only)
   * - Four asthma medication dispensing events (using short acting agents only)
   */
    def eligibleClaims(offset: Int): List[Claim] = {
      val d=dos(offset, days)
	    pickOne(List(
	
	      // At least one emergency department (ED) visit with a principal diagnosis of asthma
	      () => List(pl.createMedClaim(patient.patientID, provider.providerID, d, d, icdDPri=pickOne(icdDA), cpt = pickOne(cptA))),
	      () => List(pl.createMedClaim(patient.patientID, provider.providerID, d, d, icdDPri=pickOne(icdDA), ubRevenue = pickOne(ubA))),
	
	      // At least one acute inpatient discharge with a principal diagnosis of asthma
	      () => List(pl.createMedClaim(patient.patientID, provider.providerID, d, d, icdDPri=pickOne(icdDA), cpt = pickOne(cptB))),
	      () => List(pl.createMedClaim(patient.patientID, provider.providerID, d, d, icdDPri=pickOne(icdDA), ubRevenue = pickOne(ubB))),
	
	      // At least 4 outpatient asthma visits and at least 2 asthma medication dispensing events
	      () => List(
	          pl.createMedClaim(patient.patientID, provider.providerID, d, d, icdDPri=pickOne(icdDA), cpt = pickOne(cptC)),
	          pl.createMedClaim(patient.patientID, provider.providerID, d.minusDays(20), d.minusDays(20), icdDPri=pickOne(icdDA), cpt = pickOne(ubC)),
	          pl.createMedClaim(patient.patientID, provider.providerID, d.minusDays(40), d.minusDays(40), icdDPri=pickOne(icdDA), cpt = pickOne(cptC)),
	          pl.createMedClaim(patient.patientID, provider.providerID, d.minusDays(60), d.minusDays(60), icdDPri=pickOne(icdDA), cpt = pickOne(ubC)),
	          pl.createRxClaim(patient.patientID, provider.providerID, d, ndc=pickOne(ndcET), daysSupply=30, qty=1),
	          pl.createRxClaim(patient.patientID, provider.providerID, d.minusDays(44), ndc=pickOne(ndcET), daysSupply=30, qty=1)
	          ),
	      
	      // At least 4 asthma medication dispensing events
	      () => List(
	          pl.createRxClaim(patient.patientID, provider.providerID, d, ndc=pickOne(ndcEO), daysSupply=30, qty=1),
	          pl.createRxClaim(patient.patientID, provider.providerID, d.minusDays(30), ndc=pickOne(ndcET), daysSupply=30, qty=1),
	          pl.createRxClaim(patient.patientID, provider.providerID, d.minusDays(60), ndc=pickOne(ndcET), daysSupply=30, qty=1),
	          pl.createRxClaim(patient.patientID, provider.providerID, d.minusDays(90), ndc=pickOne(ndcET), daysSupply=30, qty=1)
	          )	      
	      ))()
	    }
    List.concat(eligibleClaims(0), eligibleClaims(offset))
  }

  /**
   * Formal test for eligible patient is:
   * - At least one emergency department (ED) visit with a principal diagnosis of asthma
   * - At least one acute inpatient discharge with a principal diagnosis of asthma
   * - At least 4 outpatient asthma visits and at least 2 asthma medication dispensing events
   * - At least 4 asthma medication dispensing events where a leukotriene modifier was not the sole medication dispensed
   * - At least 4 asthma medication dispensing events where a leukotriene modifier was the sole medication dispensed, and there was a diagnosis of asthma in any setting in the same year
   */
  override def scorePatientEligible(scorecard: Scorecard, patient: Patient, ph: PatientHistory): Scorecard = {

    // year during the measurement year
    val measurementInterval1 = getIntervalFromYears(1)
    
    // year prior the measurement year
    val temp = hedisDate.minusYears(1).plusDays(1)
    val measurementInterval2 = new Interval(temp.minusYears(1), temp)

    def nbrDispensingEvents(claims: List[RxClaim]): Int = {
      //@TODO Not checking if (fillD + daysSupplies) is after hedisDate and remove the days past hedisDate before dividing by 30
      //@TODO Using dispensed quantity of inhalers (qty) to count the nbr of dispensing events (spec is silent on that)
      
      val orals = claims filter {c => asthmaMeds.get(c.ndc) map {_.apply(ROUTE) == "oral"} getOrElse false }
      val de1 = orals.foldLeft(0)({(n, c) => n+c.daysSupply}) / 30
      
      val inhalers = claims filter {c => asthmaMeds.get(c.ndc) map {_.apply(ROUTE) == "inhalation"} getOrElse false }
      val de2 = inhalers.foldLeft(0)({(n, c) => n+c.qty})
      de1 + de2
    }
    
    def eligiblePatients(measurementInterval: Interval): List[Claim] = {

      val rules = List[() => List[Claim]](
            
	  // At least one emergency department (ED) visit with a principal diagnosis of asthma
      () => filterClaims(ph.icdD, icdDAS, { claim: MedClaim => measurementInterval.contains(claim.dos) && (cptAS.contains(claim.cpt) || ubAS.contains(claim.ubRevenue)) }),
	
	  // At least one acute inpatient discharge with a principal diagnosis of asthma
      () => filterClaims(ph.icdD, icdDAS, { claim: MedClaim => measurementInterval.contains(claim.dos) && (cptBS.contains(claim.cpt) || ubBS.contains(claim.ubRevenue)) }),

	  // At least 4 outpatient asthma visits and at least 2 asthma medication dispensing events
      () => {
        val medClaims = filterClaims(ph.icdD, icdDAS, { claim: MedClaim => measurementInterval.contains(claim.dos) && (cptCS.contains(claim.cpt) || ubCS.contains(claim.ubRevenue)) })
        val rxClaims = filterClaims(ph.ndc, ndcES, { claim: RxClaim => measurementInterval.contains(claim.fillD) })
        if(hasDifferentDates(4, medClaims) && nbrDispensingEvents(rxClaims) >= 2) List.concat(medClaims, rxClaims)
        else List.empty
      },
      
      // At least 4 asthma medication dispensing events where a leukotriene modifier was not the sole medication dispensed
      // At least 4 asthma medication dispensing events where a leukotriene modifier was the sole medication dispensed, and there was a diagnosis of asthma in any setting in the same year
      () => {
        val medClaims = filterClaims(ph.icdD, icdDAS, { claim: MedClaim => measurementInterval.contains(claim.dos) })
        val rxClaims = filterClaims(ph.ndc, ndcES, { claim: RxClaim => measurementInterval.contains(claim.fillD) })
        
        // compute the nbr of Dispensing Events
        if(nbrDispensingEvents(rxClaims) == 4) {
        
        // get the claims with Non Leukotriene Modifier meds
        val nlm = rxClaims filter {c => asthmaMeds.get(c.ndc) map {_.apply(CATEGORY) != "leukotriene modifiers"} getOrElse false }
        
        // if the dispensing events where a leukotriene modifier was not the sole medication dispensed
        if(!nlm.isEmpty) List.concat(medClaims, rxClaims)
        
        // if dispensing events where a leukotriene modifier was the sole medication dispensed, and there was a diagnosis of asthma in any setting in the same year
        else if(!medClaims.isEmpty) List.concat(medClaims, rxClaims)
        
        // missing a diagnosis of asthma
        else List.empty
        } else List.empty
      })
      
      rules.foldLeft[List[Claim]](List.empty)({(claims, f) => List.concat(claims, f())})
    }
    
    // check the eligibility in both years (measurement year and prior year), patient meet eligibility if claims are returned
    val y1 = eligiblePatients(measurementInterval1)
    val y2 = eligiblePatients(measurementInterval2)
    
    if (!isPatientMeetDemographic(patient)) scorecard.addScore(name, HEDISRule.eligible, false)
    else if(y1.isEmpty || y2.isEmpty) scorecard
    else scorecard.addScore(name, HEDISRule.eligible, "Patient Eligible", List.concat(y1, y2))
  }

  override def generateExclusionClaims(pl: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = {

    val days = getIntervalFromDays(350).toDuration().getStandardDays().toInt
    val dos = hedisDate.minusDays(Random.nextInt(days)).minusDays(10)
    val dos2 = dos.plusDays(Random.nextInt(7))

    pickOne(List(

      // exclusion - Pregnancy test (UB)
      // exclusion - X-ray or prescription for isotretinoin (CPT)
      () => List(
        pickOne(List(
          pl.createMedClaim(patient.patientID, provider.providerID, dos, dos, cpt = pickOne(cptB)),
          pl.createMedClaim(patient.patientID, provider.providerID, dos, dos, ubRevenue = pickOne(ubB)))),
        pickOne(List(
          pl.createMedClaim(patient.patientID, provider.providerID, dos2, dos2, cpt = pickOne(cptC)),
          pl.createMedClaim(patient.patientID, provider.providerID, dos2, dos2, ubRevenue = pickOne(ubC)),
          pl.createRxClaim(patient.patientID, provider.providerID, dos2, ndc = pickOne(ndcB)))))))()
  }

  override def scorePatientExcluded(scorecard: Scorecard, patient: Patient, ph: PatientHistory): Scorecard = {

    val measurementInterval = getIntervalFromYears(1)

    // exclusion - Pregnancy test (UB)
    // exclusion - X-ray or prescription for isotretinoin (CPT)

    val claims = List.concat(
      filterClaims(ph.cpt, cptBS, { claim: MedClaim => measurementInterval.contains(claim.dos) }),
      filterClaims(ph.ubRevenue, ubBS, { claim: MedClaim => measurementInterval.contains(claim.dos) }))

    val claims2 = List.concat(
      filterClaims(ph.cpt, cptCS, { claim: MedClaim => measurementInterval.contains(claim.dos) }),
      filterClaims(ph.ubRevenue, ubCS, { claim: MedClaim => measurementInterval.contains(claim.dos) }),
      filterClaims(ph.ndc, ndcBS, { claim: RxClaim => measurementInterval.contains(claim.fillD) }))

    val claimsEx: List[List[Claim]] = for {
      c1: Claim <- claims
      c2: Claim <- claims2
      if ((new Interval(c1.date, c1.date.plusDays(8))).contains(c2.date))
    } yield List(c1, c2)

    scorecard.addScore(name, HEDISRule.excluded, chlExclusion, claimsEx.flatten)
  }

  override def generateMeetMeasureClaims(pl: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = {

    val days = getIntervalFromYears(1).toDuration().getStandardDays().toInt
    val dos = hedisDate.minusDays(Random.nextInt(days))

    List(pl.createMedClaim(patient.patientID, provider.providerID, dos, dos, cpt = pickOne(cptD)))
  }

  override def scorePatientMeetMeasure(scorecard: Scorecard, patient: Patient, ph: PatientHistory): Scorecard = {

    val measurementInterval = getIntervalFromYears(1)

    // Check if patient had tested
    val claims = filterClaims(ph.cpt, cptDS, { claim: MedClaim => measurementInterval.contains(claim.dos) })
    scorecard.addScore(name, HEDISRule.meetMeasure, chlTest, claims)

  }
}

class ASM_16_20_Rule(config: RuleConfig, hedisDate: DateTime) extends ASM_Rule(ASM.name16, "16 - 20", 16, 20, config, hedisDate)
class ASM_21_26_Rule(config: RuleConfig, hedisDate: DateTime) extends ASM_Rule(ASM.name21, "21 - 26", 21, 26, config, hedisDate)
