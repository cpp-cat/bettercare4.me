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

object CHL {

  val name16 = "CHL-16-20-HEDIS-2014"
  val name21 = "CHL-21-24-HEDIS-2014"

  val sexuallyActiveWomen = "Sexually Active Women"
  val chlExclusion = "X-ray or prescription for isotretinoin"
  val chlTest = "Chlamydia Test"

  //@TODO inclusion - Sexually-active women (CPT)
  val cptA = List("11975-11977", "57022", "57170", "58300", "58301", "58600-58615", "58970-58976", "59000-59899", "76801", "76805", "76811", "76813-76821", "76825-76828", "76941", "76945", "76946", "80055", "81025", "82105", "82106", "82143", "82731", "83632", "83661-83664", "84163", "84702", "84703", "84704", "86592", "86593", "86631", "86632", "87110", "87164", "87166", "87270", "87320", "87490-87492", "87590-87592", "87620-87622", "87660", "87808", "87810", "87850", "88141-88143", "88147-88155", "88164-88167", "88174", "88175", "88235", "88267", "88269")
  val cptAS = cptA.toSet

  //@TODO inclusion - Sexually-active women (ICD P)
  val icdPA = List("69.01", "69.02", "69.51", "69.52", "69.7", "72.0-75.99", "88.78", "97.24", "97.71", "97.73")
  val icdPAS = icdPA.toSet

  //@TODO inclusion - Sexually-active women (ICD D)
  val icdDA = List("042", "054.10-054.12", "054.19", "078.11", "078.88", "079.4", "079.51-079.53", "079.88", "79.98", "091.0-098.11", "098.15-098.31", "098.35-099.9", "131.00-131.9", "302.76", "339.82", "614.0-615.9", "622.3", "623.4", "625.0", "626.7", "628*", "630-679.14", "795.0*", "795.1*", "796.7*", "996.32", "V01.6", "V02.7", "V02.8", "V08", "V15.7", "V22.0-V26.51", "V26.8*", "V26.9-V28.9", "V45.5*", "V61.5-V61.7", "V69.2", "V72.3*", "V72.4*", "V73.81", "V73.88", "V73.98", "V74.5", "V76.2")
  val icdDAS = icdDA.toSet

  // inclusion - Sexually-active women (HCPCS)
  val hcpcsA = List("G0101", "G0123", "G0124, G0141", "G0143", "G0144", "G0145", "G0147", "G0148", "G0450, H1000", "H1001", "H1003", "H1004", "H1005", "P3000", "P3001", "Q0091, S0180", "S0199", "S4981", "S8055")
  val hcpcsAS = hcpcsA.toSet

  // inclusion - Sexually-active women (UB)
  val ubA = List("0112", "0122", "0132", "0142", "0152", "0720", "0721", "0722", "0724", "0729", "0923", "0925")
  val ubAS = ubA.toSet

  // inclusion - Sexually-active women (NDC)
  val ndcA = List("00008111720", "00008111730", "00008251402", "00008253505", "00008253601", "00008253605", "00009074630", "00009074635", "00009470901", "00009470913", "00009737604", "00009737607", "00009737611", "00027013160", "00027013180", "00052026106", "00052027201", "00052027301", "00052027303", "00052027401", "00052028306", "00062125100", "00062125115", "00062125120", "00062133220", "00062141116", "00062141123", "00062171400", "00062171415", "00062176100", "00062176115", "00062178100", "00062178115", "00062179600", "00062179615", "00062190120", "00062190320", "00062190700", "00062190715", "00062191000", "00062191015", "00062192001", "00062192015", "00062192024", "00062313301", "00062318501", "00062325001", "00062330100", "00062330200", "00062330300", "00062330400", "00062330500", "00062330600", "00062330700", "00062330800", "00062330900", "00062331000", "00062331100", "00062331200", "00062331300", "00062334100", "00062334200", "00062334300", "00062334400", "00062334500", "00062334600", "00062334700", "00062334800", "00062334900", "00062335000", "00062335100", "00062335200", "00062338100", "00062338200", "00062338300", "00062338400", "00062338500", "00062338600", "00062338700", "00062338800", "00062338900", "00062364103", "00062364300", "00085005001", "00093209028", "00093209058", "00093313482", "00093542328", "00093542358", "00093566128", "00093566158", "00093614882", "00234005100", "00234013100", "00234013150", "00234013155", "00234013160", "00234013165", "00234013170", "00234013175", "00234013180", "00234013185", "00234013190", "00234013195", "00234013600", "00234013660", "00234013665", "00234013670", "00234013675", "00234013680", "00234013685", "00234013690", "00234013695", "00247052028", "00247069028", "00247069128", "00247069228", "00247139828", "00247151328", "00247151628", "00247151728", "00247176404", "00247176421", "00247176521", "00247198621", "00247198628", "00247200828", "00247201004", "00247201008", "00247201028", "00247201228", "00247201328", "00247202602", "00247210801", "00247214728", "00247216928", "00247217028", "00247223028", "00247223528", "00247226028", "00247226828", "00378655053", "00378727253", "00378729253", "00396401065", "00396401070", "00396401075", "00396401080", "00430042014", "00430048214", "00430053014", "00430053550", "00430057014", "00430057045", "00430058014", "00430058045", "00430058114", "00430058514", "00430058545", "00555034458", "00555071558", "00555900867", "00555900942", "00555901058", "00555901258", "00555901467", "00555901658", "00555901858", "00555902058", "00555902542", "00555902557", "00555902658", "00555902742", "00555902757", "00555902858", "00555903270", "00555903458", "00555904358", "00555904558", "00555904758", "00555904958", "00555905058", "00555905158", "00555905167", "00555906458", "00555906467", "00555906558", "00555906658", "00555906667", "00555912366", "00555913167", "00555913179", "00603359017", "00603359049", "00603752117", "00603752149", "00603752517", "00603752549", "00603754017", "00603754049", "00603760615", "00603760648", "00603760715", "00603760748", "00603760817", "00603760917", "00603762517", "00603762549", "00603763417", "00603763449", "00603764017", "00603764217", "00603766317", "00603766517", "00703680101", "00703680104", "00703681121", "00781558307", "00781558315", "00781558336", "00781558436", "00781558491", "00781565615", "00781565815", "16714034004", "16714034604", "16714034704", "16714034804", "16714035904", "16714036004", "16714036304", "16714036504", "16714037003", "21695076928", "21695077028", "23490585401", "23490765301", "23490767001", "23490769901", "24090080184", "24090096184", "30014030312", "34362030010", "34362030213", "35356001468", "35356001568", "35356002168", "35356025528", "35356037028", "43386062030", "45802084054", "50419040201", "50419040203", "50419040303", "50419040503", "50419040701", "50419040703", "50419041112", "50419041128", "50419042101", "50419042201", "50419043306", "50419043312", "50452025115", "50458017115", "50458017615", "50458017815", "50458019115", "50458019201", "50458019215", "50458019411", "50458019416", "50458019615", "50458019715", "50458025115", "50486022112", "51285005866", "51285007997", "51285008070", "51285008198", "51285008297", "51285008370", "51285008498", "51285008787", "51285009158", "51285009287", "51285011458", "51285043165", "51285054628", "51285076993", "51285094288", "51285094388", "52544014331", "52544017572", "52544020431", "52544021028", "52544021928", "52544022829", "52544023528", "52544023531", "52544024531", "52544024728", "52544024828", "52544025428", "52544025928", "52544025988", "52544026528", "52544026531", "52544026829", "52544026884", "52544027428", "52544027431", "52544027536", "52544027928", "52544028754", "52544029128", "52544029231", "52544029241", "52544029528", "52544038328", "52544038428", "52544047536", "52544055028", "52544055228", "52544055428", "52544062928", "52544063028", "52544063128", "52544084728", "52544084828", "52544089228", "52544093628", "52544094028", "52544094928", "52544095021", "52544095121", "52544095328", "52544095428", "52544095931", "52544096691", "52544096728", "52544098131", "52544098231", "52925011201", "52925011202", "52925011203", "52925031214", "52959045002", "54569067900", "54569068500", "54569068501", "54569068900", "54569068901", "54569143900", "54569370100", "54569384400", "54569422200", "54569422201", "54569426900", "54569427301", "54569481700", "54569487800", "54569487801", "54569489000", "54569490400", "54569498400", "54569499700", "54569499800", "54569516100", "54569534300", "54569534900", "54569541300", "54569549300", "54569549302", "54569552700", "54569561600", "54569579600", "54569579700", "54569579800", "54569581600", "54569582600", "54569586500", "54569603200", "54569612800", "54569614400", "54569614500", "54569621900", "54569627200", "54569628000", "54569628100", "54868042800", "54868044300", "54868050200", "54868050700", "54868050801", "54868050901", "54868051600", "54868151200", "54868156400", "54868231600", "54868260600", "54868270100", "54868361300", "54868377200", "54868386300", "54868394800", "54868409300", "54868410000", "54868410001", "54868419600", "54868423900", "54868436900", "54868453800", "54868459000", "54868460700", "54868467000", "54868473000", "54868473100", "54868474200", "54868474500", "54868475400", "54868477600", "54868481400", "54868482800", "54868483201", "54868485100", "54868486000", "54868491100", "54868502800", "54868525700", "54868528600", "54868532600", "54868535600", "54868582600", "54868582800", "54868594200", "55045283902", "55045348506", "55045349701", "55045349801", "55045350501", "55045378106", "55045378206", "55045378302", "55289024708", "55289088704", "55887005228", "55887028628", "55887075401", "58016474701", "58016482701", "59762453701", "59762453702", "59762453801", "59762453802", "59762453809", "66993061128", "66993061528", "68180084313", "68180084413", "68180084613", "68180084813", "68180085413", "68180087611", "68180087613", "68180089713", "68180089813", "68180090213", "68462030329", "68462030529", "68462030929", "68462031629", "68462031829", "68462038829", "68462039429", "68462055629", "68462056529")
  val ndcAS = ndcA.toSet

  // exclusion - Pregnancy test (CPT)
  val cptB = List("81025", "84702", "84703")
  val cptBS = cptB.toSet

  // exclusion - Pregnancy test (UB)
  val ubB = List("0925")
  val ubBS = ubB.toSet

  // exclusion - X-ray or prescription for isotretinoin (CPT)
  val cptC = List("70010", "70015", "70030", "70100", "70110", "70120", "70130", "70134", "70140", "70150", "70160", "70170", "70190", "70200", "70210", "70220", "70240", "70250", "70260", "70300", "70310", "70320", "70328", "70330", "70332", "70336", "70350", "70355", "70360", "70370", "70371", "70373", "70380", "70390", "70450", "70460", "70470", "70480", "70481", "70482", "70486", "70487", "70488", "70490", "70491", "70492", "70496", "70498", "70540", "70542", "70543", "70544", "70545", "70546", "70547", "70548", "70549", "70551", "70552", "70553", "70554", "70555", "70557", "70558", "70559", "71010", "71015", "71020", "71021", "71022", "71023", "71030", "71034", "71035", "71040", "71060", "71090", "71100", "71101", "71110", "71111", "71120", "71130", "71250", "71260", "71270", "71275", "71550", "71551", "71552", "71555", "72010", "72020", "72040", "72050", "72052", "72069", "72070", "72072", "72074", "72080", "72090", "72100", "72110", "72114", "72120", "72125", "72126", "72127", "72128", "72129", "72130", "72131", "72132", "72133", "72141", "72142", "72146", "72147", "72148", "72149", "72156", "72157", "72158", "72159", "72170", "72190", "72191", "72192", "72193", "72194", "72195", "72196", "72197", "72198", "72200", "72202", "72220", "72240", "72255", "72265", "72270", "72275", "72285", "72291", "72292", "72295", "73000", "73010", "73020", "73030", "73040", "73050", "73060", "73070", "73080", "73085", "73090", "73092", "73100", "73110", "73115", "73120", "73130", "73140", "73200", "73201", "73202", "73206", "73218", "73219", "73220", "73221", "73222", "73223", "73225", "73500", "73510", "73520", "73525", "73530", "73540", "73542", "73550", "73560", "73562", "73564", "73565", "73580", "73590", "73592", "73600", "73610", "73615", "73620", "73630", "73650", "73660", "73700", "73701", "73702", "73706", "73718", "73719", "73720", "73721", "73722", "73723", "73725", "74000", "74010", "74020", "74022", "74150", "74160", "74170", "74174", "74175", "74181", "74182", "74183", "74185", "74190", "74210", "74220", "74230", "74235", "74240", "74241", "74245", "74246", "74247", "74249", "74250", "74251", "74260", "74270", "74280", "74283", "74290", "74291", "74300", "74301", "74305", "74320", "74327", "74328", "74329", "74330", "74340", "74350", "74355", "74360", "74363", "74400", "74410", "74415", "74420", "74425", "74430", "74440", "74445", "74450", "74455", "74470", "74475", "74480", "74485", "74710", "74740", "74742", "74775", "75557", "75559", "75561", "75563", "75600", "75605", "75625", "75630", "75635", "75650", "75658", "75660", "75662", "75665", "75671", "75676", "75680", "75685", "75705", "75710", "75716", "75722", "75724", "75726", "75731", "75733", "75736", "75741", "75743", "75746", "75756", "75774", "75801", "75803", "75805", "75807", "75809", "75810", "75820", "75822", "75825", "75827", "75831", "75833", "75840", "75842", "75860", "75870", "75872", "75880", "75885", "75887", "75889", "75891", "75893", "75894", "75896", "75898", "75900", "75901", "75902", "75940", "75945", "75946", "75952", "75953", "75954", "75956", "75957", "75958", "75959", "75960", "75961", "75962", "75964", "75966", "75968", "75970", "75978", "75980", "75982", "75984", "75989", "76000", "76001", "76010", "76080", "76098", "76100", "76101", "76102", "76120", "76125", "76140", "76376", "76377", "76380", "76390", "76496", "76497", "76498", "76499")
  val cptCS = cptC.toSet

  // exclusion - X-ray or prescription for isotretinoin (CPT)
  val ubC = List("0320", "0321", "0322", "0323", "0324", "0329")
  val ubCS = ubC.toSet

  // exclusion - X-ray or prescription for isotretinoin (NDC)
  val ndcB = List("00378661193", "00378661293", "00378661488", "00378661493", "00555093456", "00555093486", "00555093556", "00555093586", "00555093656", "00555093686", "00555105456", "00555105486", "00555105556", "00555105586", "00555105686", "00555105756", "00555105786", "10631011531", "10631011569", "10631011631", "10631011669", "10631011731", "10631011769", "10631011831", "10631011869", "10631044731", "10631058431", "10631058477", "10631058531", "10631058577", "10631058631", "10631058677", "54868504300", "54868512801", "54868516300", "54868530500", "55111013581", "55111013681", "55111013781", "61748030111", "61748030113", "61748030211", "61748030213", "61748030411", "61748030413", "62794061188", "62794061288", "63304044731", "63304044777", "63304058431", "63304058477", "63304058531", "63304058577", "63304058631", "63304058677")
  val ndcBS = ndcB.toSet

  // meet measure - At least one chlamydia test (CPT)
  val cptD = List("87110", "87270", "87320", "87490", "87491", "87492", "87810")
  val cptDS = cptD.toSet
}
/**
 * Chlamydia Screen Rule
 *
 * Chlamydia Screen indicates whether a woman, aged 16 to 24 years, had a chlamydia screening test done during the
 * measurement year. This excludes women who had a pregnancy test during the measurement period, followed within 7 days by
 * either a prescription for isotretinoin or an x-ray.
 *
 * DENOMINATOR:
 * Identifies the unique count of sexually-active women, aged 16 to 24 years at the end of the measurement year. It excludes
 * women who had a pregnancy test during the measurement period, followed within 7 days by either a prescription for isotretinoin
 * or an x-ray.
 *
 * EXCLUSIONS:
 * Excludes from the eligible population those women who had a pregnancy test during the measurement year, followed within 7 days by either a prescription for isotretinoin or an x-ray.
 *
 * NUMERATOR:
 * Identifies sexually-active women, aged 16 to 24 years, who had a chlamydia screening test done during the measurement year.
 *
 */
class CHL_Rule(override val name: String, tag: String, ageLo: Int, ageHi: Int, config: RuleConfig, hedisDate: DateTime) extends HEDISRuleBase(config, hedisDate) {

  val fullName = "Chlamydia Screen ("+tag+")"
  val description = "Chlamydia Screen indicates whether a woman, aged "+tag+" years, had a chlamydia screening test done during the measurement year. This excludes women who had a pregnancy test during the measurement period, followed within 7 days by either a prescription for isotretinoin or an x-ray."

  import CHL._

  override def isPatientMeetDemographic(patient: Patient): Boolean = {
    val age = patient.age(hedisDate)
    patient.gender == "F" && age >= ageLo && age <= ageHi
  }

  override def generateEligibleClaims(pl: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = {

    val days = new Interval(hedisDate.minusYears(1), hedisDate).toDuration().getStandardDays().toInt
    val dos = hedisDate.minusDays(Random.nextInt(days))

    pickOne(List(

      // Sexually-active women (CPT)
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos, dos, cpt = pickOne(cptA))),

      // Sexually-active women (ICD D)
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos, dos, icdDPri = pickOne(icdDA))),

      // Sexually-active women (ICD P)
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos, dos, icdP = Set(pickOne(icdPA)))),

      // Sexually-active women (HCPCS)
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos, dos, hcpcs = pickOne(hcpcsA))),

      // Sexually-active women (UB)
      () => List(pl.createMedClaim(patient.patientID, provider.providerID, dos, dos, ubRevenue = pickOne(ubA))),

      // Sexually-active women (NDC)
      () => List(pl.createRxClaim(patient.patientID, provider.providerID, dos, ndc = pickOne(ndcA)))))()
  }

  override def scorePatientEligible(scorecard: Scorecard, patient: Patient, ph: PatientHistory): Scorecard = {

    val measurementInterval = new Interval(hedisDate.minusYears(1), hedisDate)

    def rules = List[(Scorecard) => Scorecard](

      // Sexually-active women (CPT)
      (s: Scorecard) => {
        val claims = filterClaims(ph.cpt, cptAS, { claim: MedClaim => measurementInterval.contains(claim.dos) })
        s.addScore(name, HEDISRule.eligible, sexuallyActiveWomen, claims)
      },

      // Sexually-active women (ICD D)
      (s: Scorecard) => {
        val claims = filterClaims(ph.icdD, icdDAS, { claim: MedClaim => measurementInterval.contains(claim.dos) })
        s.addScore(name, HEDISRule.eligible, sexuallyActiveWomen, claims)
      },

      // Sexually-active women (ICD P)
      (s: Scorecard) => {
        val claims = filterClaims(ph.icdP, icdPAS, { claim: MedClaim => measurementInterval.contains(claim.dos) })
        s.addScore(name, HEDISRule.eligible, sexuallyActiveWomen, claims)
      },

      // Sexually-active women (HCPCS)
      (s: Scorecard) => {
        val claims = filterClaims(ph.hcpcs, hcpcsAS, { claim: MedClaim => measurementInterval.contains(claim.dos) })
        s.addScore(name, HEDISRule.eligible, sexuallyActiveWomen, claims)
      },

      // Sexually-active women (UB)
      (s: Scorecard) => {
        val claims = filterClaims(ph.ubRevenue, ubAS, { claim: MedClaim => measurementInterval.contains(claim.dos) })
        s.addScore(name, HEDISRule.eligible, sexuallyActiveWomen, claims)
      },

      // Sexually-active women (NDC)
      (s: Scorecard) => {
        val claims = filterClaims(ph.ndc, ndcAS, { claim: RxClaim => measurementInterval.contains(claim.fillD) })
        s.addScore(name, HEDISRule.eligible, sexuallyActiveWomen, claims)
      })

    if (!isPatientMeetDemographic(patient)) scorecard.addScore(name, HEDISRule.eligible, false)
    else applyRules(scorecard, rules)
  }

  override def generateExclusionClaims(pl: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = {

    val days = new Interval(hedisDate.minusYears(350), hedisDate).toDuration().getStandardDays().toInt
    val dos = hedisDate.minusDays(Random.nextInt(days)).plusDays(10)
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

    val measurementInterval = new Interval(hedisDate.minusYears(1), hedisDate)

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

    val days = new Interval(hedisDate.minusYears(1), hedisDate).toDuration().getStandardDays().toInt
    val dos = hedisDate.minusDays(Random.nextInt(days))

    List(pl.createMedClaim(patient.patientID, provider.providerID, dos, dos, cpt = pickOne(cptD)))
  }

  override def scorePatientMeetMeasure(scorecard: Scorecard, patient: Patient, ph: PatientHistory): Scorecard = {

    val measurementInterval = new Interval(hedisDate.minusYears(1), hedisDate)

    // Check if patient had tested
    val claims = filterClaims(ph.cpt, cptDS, { claim: MedClaim => measurementInterval.contains(claim.dos) })
    scorecard.addScore(name, HEDISRule.meetMeasure, chlTest, claims)

  }
}

class CHL_16_20_Rule(config: RuleConfig, hedisDate: DateTime) extends CHL_Rule(CHL.name16, "16 - 20", 16, 20, config, hedisDate)
class CHL_21_26_Rule(config: RuleConfig, hedisDate: DateTime) extends CHL_Rule(CHL.name21, "21 - 26", 21, 26, config, hedisDate)
