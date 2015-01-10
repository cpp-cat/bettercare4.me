/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package com.nickelsoftware.bettercare4me.hedis

import org.joda.time.DateTime

import com.nickelsoftware.bettercare4me.hedis.hedis2014.AAB
import com.nickelsoftware.bettercare4me.hedis.hedis2014.AAB_Rule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.ASM
import com.nickelsoftware.bettercare4me.hedis.hedis2014.ASM_12_18_Rule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.ASM_19_50_Rule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.ASM_51_64_Rule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.ASM_5_11_Rule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.AWC
import com.nickelsoftware.bettercare4me.hedis.hedis2014.AWC_Rule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.BCS
import com.nickelsoftware.bettercare4me.hedis.hedis2014.BCSRule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CCS
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CCS_Rule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CDCEE
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CDCEERule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CDCHbA1cTest
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CDCHbA1cTest7Rule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CDCHbA1cTest8Rule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CDCHbA1cTest9Rule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CDCHbA1cTestRule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CDCHbA1cTestValue
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CDC_BPC
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CDC_BPC_C1_Rule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CDC_BPC_C2_Rule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CDC_BPC_T_Rule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CDC_LDL_C
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CDC_LDL_C_TestRule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CDC_LDL_C_TestValueRule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CDC_LDL_C_Value
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CDC_MAN
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CDC_MAN_Rule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CHL
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CHL_16_20_Rule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CHL_21_26_Rule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CIS_DTaP
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CIS_DTaP_Rule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CIS_HB
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CIS_HB_Rule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CIS_HiB
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CIS_HiB_Rule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CIS_IPV
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CIS_IPV_Rule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CIS_MMR
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CIS_MMR_Rule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CIS_PC
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CIS_PC_Rule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CIS_VZV
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CIS_VZV_Rule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CMC
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CMC_LDL_C_TestRule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CMC_LDL_C_TestValueRule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.COL
import com.nickelsoftware.bettercare4me.hedis.hedis2014.COL_Rule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CWP
import com.nickelsoftware.bettercare4me.hedis.hedis2014.CWP_Rule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.LBP
import com.nickelsoftware.bettercare4me.hedis.hedis2014.LBP_Rule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.MPM_AC
import com.nickelsoftware.bettercare4me.hedis.hedis2014.MPM_ACE_ARB_Rule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.MPM_AC_C_Rule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.MPM_AC_P1_Rule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.MPM_AC_P2_Rule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.MPM_AC_V_Rule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.MPM_ADD
import com.nickelsoftware.bettercare4me.hedis.hedis2014.MPM_DGX_Rule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.MPM_DUT_Rule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.URI
import com.nickelsoftware.bettercare4me.hedis.hedis2014.URI_Rule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.W15
import com.nickelsoftware.bettercare4me.hedis.hedis2014.W15_Rule
import com.nickelsoftware.bettercare4me.hedis.hedis2014.W34
import com.nickelsoftware.bettercare4me.hedis.hedis2014.W34_Rule
import com.nickelsoftware.bettercare4me.models.Claim
import com.nickelsoftware.bettercare4me.models.Patient
import com.nickelsoftware.bettercare4me.models.PatientHistory
import com.nickelsoftware.bettercare4me.models.PersistenceLayer
import com.nickelsoftware.bettercare4me.models.Provider
import com.nickelsoftware.bettercare4me.models.RuleConfig
import com.nickelsoftware.bettercare4me.utils.NickelException

object HEDISRules {

  val rules: Map[String, (RuleConfig, DateTime) => HEDISRule] = Map(
    "TEST" -> { (c, d) => new TestRule(c, d) },
    BCS.name -> { (c, d) => new BCSRule(c, d) },
    CDCHbA1cTest.name -> { (c, d) => new CDCHbA1cTestRule(c, d) },
    CDCHbA1cTestValue.name7 -> { (c, d) => new CDCHbA1cTest7Rule(c, d) },
    CDCHbA1cTestValue.name8 -> { (c, d) => new CDCHbA1cTest8Rule(c, d) },
    CDCHbA1cTestValue.name9 -> { (c, d) => new CDCHbA1cTest9Rule(c, d) },
    CDCEE.name -> { (c, d) => new CDCEERule(c, d) },
    CDC_LDL_C.name -> { (c, d) => new CDC_LDL_C_TestRule(c, d) },
    CDC_LDL_C_Value.name -> { (c, d) => new CDC_LDL_C_TestValueRule(c, d) },
    CDC_MAN.name -> { (c, d) => new CDC_MAN_Rule(c, d) },
    CDC_BPC.nameTest -> { (c, d) => new CDC_BPC_T_Rule(c, d) },
    CDC_BPC.nameC1 -> { (c, d) => new CDC_BPC_C1_Rule(c, d) },
    CDC_BPC.nameC2 -> { (c, d) => new CDC_BPC_C2_Rule(c, d) },
    CCS.name -> { (c, d) => new CCS_Rule(c, d) },
    CHL.name16 -> { (c, d) => new CHL_16_20_Rule(c, d) },
    CHL.name21 -> { (c, d) => new CHL_21_26_Rule(c, d) },
    COL.name -> { (c, d) => new COL_Rule(c, d) },
    CIS_VZV.name -> { (c, d) => new CIS_VZV_Rule(c, d) },
    CIS_DTaP.name -> { (c, d) => new CIS_DTaP_Rule(c, d) },
    CIS_HB.name -> { (c, d) => new CIS_HB_Rule(c, d) },
    CIS_HiB.name -> { (c, d) => new CIS_HiB_Rule(c, d) },
    CIS_MMR.name -> { (c, d) => new CIS_MMR_Rule(c, d) },
    CIS_PC.name -> { (c, d) => new CIS_PC_Rule(c, d) },
    CIS_IPV.name -> { (c, d) => new CIS_IPV_Rule(c, d) },
    W15.name -> { (c, d) => new W15_Rule(c, d) },
    W34.name -> { (c, d) => new W34_Rule(c, d) },
    AWC.name -> { (c, d) => new AWC_Rule(c, d) },
    ASM.name5 -> { (c, d) => new ASM_5_11_Rule(c, d) },
    ASM.name12 -> { (c, d) => new ASM_12_18_Rule(c, d) },
    ASM.name19 -> { (c, d) => new ASM_19_50_Rule(c, d) },
    ASM.name51 -> { (c, d) => new ASM_51_64_Rule(c, d) },
    CMC.nameTest -> { (c, d) => new CMC_LDL_C_TestRule(c, d) },
    CMC.nameTestValue -> { (c, d) => new CMC_LDL_C_TestValueRule(c, d) },
    MPM_ADD.nameACE -> { (c, d) => new MPM_ACE_ARB_Rule(c, d) },
    MPM_ADD.nameDGX -> { (c, d) => new MPM_DGX_Rule(c, d) },
    MPM_ADD.nameDUT -> { (c, d) => new MPM_DUT_Rule(c, d) },
    MPM_AC.nameC -> { (c, d) => new MPM_AC_C_Rule(c, d) },
    MPM_AC.nameP1 -> { (c, d) => new MPM_AC_P1_Rule(c, d) },
    MPM_AC.nameP2 -> { (c, d) => new MPM_AC_P2_Rule(c, d) },
    MPM_AC.nameV -> { (c, d) => new MPM_AC_V_Rule(c, d) },
    CWP.name -> { (c, d) => new CWP_Rule(c, d) },
    URI.name -> { (c, d) => new URI_Rule(c, d) },
    AAB.name -> { (c, d) => new AAB_Rule(c, d) },
    LBP.name -> { (c, d) => new LBP_Rule(c, d) })

  def createRuleByName(name: String, config: RuleConfig, hedisDate: DateTime): HEDISRule = {
    if (!rules.contains(name)) throw NickelException("HEDISRules: Cannot create HEDISRule; No such rule with name: " + name)
    else rules(name)(config, hedisDate)
  }
}

class TestRule(config: RuleConfig, hedisDate: DateTime) extends HEDISRuleBase(config, hedisDate) {

  val name = "TEST"
  val fullName = "Test Rule"
  val description = "This rule is for testing."

  def isPatientMeetDemographic(patient: Patient): Boolean = true
  def isPatientExcluded(patient: Patient, patientHistory: PatientHistory): Boolean = false
  def isPatientMeetMeasure(patient: Patient, patientHistory: PatientHistory): Boolean = true

  override def generateClaims(persistenceLayer: PersistenceLayer, patient: Patient, provider: Provider, eligibleSimScore: Int, excludedSimScore: Int, meetCriteriaSimScore: Int): List[Claim] = {
    val dos = new DateTime(2014, 9, 5, 0, 0)
    List(
      persistenceLayer.createMedClaim(
        patient.patientID, patient.firstName, patient.lastName, provider.providerID, provider.firstName, provider.lastName, dos, dos,
        icdDPri = "icd 1", icdD = Set("icd 1", "icd 2"), icdP = Set("icd p1"),
        hcfaPOS = "hcfaPOS", ubRevenue = "ubRevenue"))
  }

  def scorePatientMeetDemographic(scorecard: Scorecard, patient: Patient, patientHistory: PatientHistory): Scorecard =
    scorecard.addScore("TEST", "TEST", HEDISRule.meetDemographic, true)

  def scorePatientExcluded(scorecard: Scorecard, patient: Patient, patientHistory: PatientHistory): Scorecard =
    scorecard.addScore("TEST", "TEST", HEDISRule.excluded, true)

  def scorePatientMeetMeasure(scorecard: Scorecard, patient: Patient, patientHistory: PatientHistory): Scorecard =
    scorecard.addScore("TEST", "TEST", HEDISRule.meetMeasure, true)
}
