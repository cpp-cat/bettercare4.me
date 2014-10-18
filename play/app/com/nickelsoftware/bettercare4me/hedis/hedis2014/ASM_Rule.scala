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
import com.nickelsoftware.bettercare4me.utils.Utils
import com.github.tototoshi.csv.CSVReader
import java.io.File

object ASM {

  val name5 = "ASM-5-11-HEDIS-2014"
  val name12 = "ASM-12-18-HEDIS-2014"
  val name19 = "ASM-19-50-HEDIS-2014"
  val name51 = "ASM-51-64-HEDIS-2014"

  // Table ASM­-C: Asthma Medications (eligibility criteria list)
  // NDC of All asthma medications (Table ASM­-C) ndcA
  val ndcA = List("00006011701", "00006011728", "00006011731", "00006011754", "00006011780", "00006027501", "00006027528", "00006027531", "00006027554", "00006027582", "00006071101", "00006071128", "00006071131", "00006071154", "00006171131", "00006171154", "00006384101", "00006384130", "00006911731", "00006911754", "00006911780", "00006927531", "00006927554", "00006927582", "00037052192", "00037054192", "00037054568", "00037073192", "00054025913", "00054025922", "00054028813", "00054028822", "00054028913", "00054028922", "00074301460", "00085113201", "00085134101", "00085134102", "00085134103", "00085134104", "00085134107", "00085140101", "00085140201", "00085140202", "00085146102", "00085146107", "00085461001", "00085461005", "00085720601", "00085720607", "00093742456", "00093742498", "00093742556", "00093742598", "00093742610", "00093742656", "00093742698", "00093748756", "00121479415", "00143102001", "00143102010", "00143102501", "00143102510", "00173052000", "00173052100", "00173060002", "00173060100", "00173060102", "00173060200", "00173060202", "00173068220", "00173068221", "00173068224", "00173068254", "00173068281", "00173069500", "00173069504", "00173069600", "00173069604", "00173069700", "00173069704", "00173071520", "00173071522", "00173071620", "00173071622", "00173071700", "00173071720", "00173071722", "00173071820", "00173071920", "00173072020", "00186037020", "00186037028", "00186037220", "00186037228", "00186091612", "00186091706", "00247019020", "00247065602", "00247065604", "00247065610", "00247065614", "00247065620", "00247065628", "00247065630", "00247065660", "00247065907", "00247066708", "00247067441", "00247070307", "00247082406", "00247082410", "00247082430", "00247082460", "00247082490", "00247189700", "00247189730", "00247189760", "00247189777", "00247189790", "00247189800", "00247189814", "00247189830", "00247189860", "00247189877", "00247189890", "00247197360", "00247198360", "00247198830", "00247207260", "00247221560", "00247222701", "00258358101", "00258358105", "00258358110", "00258358301", "00258358305", "00258358310", "00258358401", "00258358405", "00258362501", "00258363401", "00258363801", "00310040160", "00310040260", "00310041160", "00310041260", "00378520193", "00378520493", "00378520593", "00456064416", "00456064816", "00456067099", "00456067299", "00456358101", "00456358105", "00456358110", "00456430101", "00456430201", "00456430301", "00456431001", "00456432000", "00456432001", "00456432002", "00456433000", "00456433001", "00456433002", "00456434501", "00485005916", "00490008000", "00490008030", "00490008060", "00490008090", "00525037616", "00603119058", "00603465302", "00603465316", "00603465328", "00603465332", "00603465402", "00603465416", "00603465428", "00603465432", "00603465502", "00603465516", "00603465528", "00603465532", "00603465534", "00603594421", "00603594428", "00603594521", "00603594528", "00603594532", "00603594621", "00603594628", "00603594632", "00603595021", "00603595121", "00603595221", "00677000301", "00677000701", "00677084601", "00781555431", "00781555492", "00781555531", "00781555592", "00781556031", "00781556092", "00904161061", "00904161160", "00904161161", "00904161261", "00904588761", "00904588861", "00904588961", "10122090112", "10122090212", "10892015065", "12280004290", "12280017360", "12280040915", "13411015101", "13411015103", "13411015106", "13411015109", "13411015115", "13411016001", "13411016003", "13411016006", "13411016009", "13411016015", "13668007905", "13668007930", "13668007990", "13668008005", "13668008030", "13668008090", "13668008130", "13668008190", "15370002110", "16590002520", "17856064430", "17856064431", "21695019601", "21695019701", "21695085185", "23155006201", "23155006301", "23490735501", "23490754201", "23490797201", "24839022601", "24839022716", "29033000101", "29033000201", "29336081521", "35356009914", "35356012660", "35356015701", "35356016601", "38130001201", "42291062110", "42291062130", "42291062190", "42291062230", "42291062290", "42291062330", "42291062390", "42858070101", "42858070201", "43063038015", "43063038030", "43063038121", "43063038130", "45985064701", "49708064490", "49884030302", "49884030402", "49999030028", "49999053330", "49999053390", "49999055000", "49999061401", "49999081960", "49999088430", "49999088490", "49999090767", "49999090885", "49999092130", "49999092215", "49999095230", "49999098460", "49999098560", "50111045901", "50111045902", "50111045903", "50111048201", "50111048202", "50111048203", "50111048301", "50111048302", "50111051801", "50242004062", "50474010001", "50474020001", "50474020050", "50474030001", "50474030050", "50474040001", "50991020016", "50991021416", "50991040001", "50991041301", "51079022301", "51079022320", "51862013116", "51991053601", "52959013100", "52959027930", "52959028603", "52959056901", "52959097801", "54569004900", "54569005300", "54569006501", "54569006502", "54569006505", "54569101200", "54569101300", "54569166600", "54569397600", "54569460401", "54569460500", "54569462100", "54569473600", "54569486700", "54569516700", "54569524100", "54569524200", "54569524300", "54569566300", "54569567100", "54569577700", "54569585300", "54569592800", "54569616600", "54569616700", "54569626500", "54569626600", "54569632100", "54569634800", "54838055680", "54839051380", "54868002800", "54868002801", "54868002802", "54868002803", "54868002805", "54868002806", "54868002900", "54868002902", "54868002903", "54868002905", "54868002906", "54868002907", "54868126801", "54868143800", "54868143801", "54868146101", "54868146102", "54868188301", "54868189402", "54868271000", "54868271001", "54868282101", "54868282200", "54868328300", "54868328301", "54868328302", "54868417200", "54868417201", "54868417202", "54868448100", "54868451600", "54868451700", "54868451800", "54868463000", "54868484700", "54868497200", "54868497201", "54868529400", "54868536200", "54868554700", "54868554701", "54868554702", "54868563700", "54868584400", "54868585700", "54868585800", "54868593600", "54868593700", "54868598900", "54868599000", "54868599500", "54868605000", "54868605001", "54868605100", "55045186803", "55045252007", "55045306300", "55045335100", "55045335400", "55045338801", "55045349401", "55045368601", "55045369508", "55045376808", "55111059330", "55111059390", "55111059430", "55111059490", "55111062560", "55111062660", "55111072510", "55111072530", "55111072590", "55111072594", "55111076303", "55289078930", "55289096115", "55289096130", "55289098921", "55289098930", "55289099021", "55289099030", "55887004318", "55887012090", "55887027730", "55887067860", "55887084760", "55887084790", "58016460401", "58016481301", "58864065830", "58864069430", "59243002110", "59310017540", "59310017780", "59310020240", "59310020480", "59310057920", "59310057922", "59762003001", "59762003002", "59762004501", "59762004502", "59762004601", "59762004602", "60258033516", "60258033601", "60258037116", "60346028274", "60505356203", "60505356208", "60505356209", "60505357303", "60505357308", "60505357309", "60505357403", "60505357408", "60505357409", "60793001108", "60793001114", "61392001645", "61392001654", "61392001656", "61392001691", "61392001754", "61392001756", "61392001791", "62175020432", "62175020443", "62175020446", "62175020532", "62175020543", "62175020546", "62175021032", "62175021046", "63402051001", "63402071101", "63402071201", "63402091130", "63402091164", "63629163901", "63629279201", "63629279202", "63629355101", "63874044301", "63874044315", "63874044320", "63874044330", "63874044701", "63874044715", "63874044720", "63874044730", "63874044760", "63874067501", "63874067515", "63874067520", "63874067530", "64661081416", "65162032411", "65162033510", "65862056705", "65862056730", "65862056790", "65862056805", "65862056830", "65862056890", "65862057419", "65862057430", "65862057490", "66105016402", "66105016403", "66105016406", "66105016409", "66105016410", "66105050106", "66105050206", "66336059630", "67781025101", "67781025105", "67781025201", "67801030503", "68084061901", "68084061911", "68084062001", "68084062011", "68115032860", "68115054720", "68115063860", "68115065160", "68115065201", "68115065301", "68115065701", "68115076001", "68115076917", "68115077507", "68115092330", "68115092390", "68115092460", "68258303101", "68258303203", "68258303303", "68258303701", "68462035601", "68462038001", "68462039205", "68462039230", "68462039290", "68734070010", "68734071010", "99207028040")
  val ndcAS = ndcA.toSet
  
  // Asthma Medication attributes
  // file: asm.ndc.attributes.csv
  // List(ndc_code,	brand_name,	generic_product_name,	route,	category,	drug_id)
  val NDCCODE = 0
  val ROUTE = 3
  val CATEGORY = 4
  val ndcAttributes: List[List[String]] = CSVReader.open(new File("./data/asm.ndc.attributes.csv")).all()
  val asthmaMeds: Map[String, List[String]] = ndcAttributes map { l => (l.apply(0), l)} toMap

  // Table ASM­-D: Asthma Controller Medications (meet measure medication list)
  // -----------
  val ndcC = List("00006011701", "00006011728", "00006011731", "00006011754", "00006011780", "00006027501", "00006027528", "00006027531", "00006027554", "00006027582", "00006071101", "00006071128", "00006071131", "00006071154", "00006171131", "00006171154", "00006384101", "00006384130", "00006911731", "00006911754", "00006911780", "00006927531", "00006927554", "00006927582", "00037052192", "00037054192", "00037054568", "00037073192", "00054025913", "00054025922", "00054028813", "00054028822", "00054028913", "00054028922", "00074301460", "00085134101", "00085134102", "00085134103", "00085134104", "00085134107", "00085146102", "00085146107", "00085461001", "00085461005", "00085720601", "00085720607", "00093742456", "00093742498", "00093742556", "00093742598", "00093742610", "00093742656", "00093742698", "00093748756", "00121479415", "00143102001", "00143102010", "00143102501", "00143102510", "00173060002", "00173060100", "00173060102", "00173060200", "00173060202", "00173069500", "00173069504", "00173069600", "00173069604", "00173069700", "00173069704", "00173071520", "00173071522", "00173071620", "00173071622", "00173071700", "00173071720", "00173071722", "00173071820", "00173071920", "00173072020", "00186037020", "00186037028", "00186037220", "00186037228", "00186091612", "00186091706", "00247019020", "00247065602", "00247065604", "00247065610", "00247065614", "00247065620", "00247065628", "00247065630", "00247065660", "00247065907", "00247066708", "00247067441", "00247070307", "00247082406", "00247082410", "00247082430", "00247082460", "00247082490", "00247189700", "00247189730", "00247189760", "00247189777", "00247189790", "00247189800", "00247189814", "00247189830", "00247189860", "00247189877", "00247189890", "00247197360", "00247198360", "00247198830", "00247221560", "00258358101", "00258358105", "00258358110", "00258358301", "00258358305", "00258358310", "00258358401", "00258358405", "00258362501", "00258363401", "00258363801", "00310040160", "00310040260", "00310041160", "00310041260", "00378520193", "00378520493", "00378520593", "00456064416", "00456064816", "00456067099", "00456067299", "00456358101", "00456358105", "00456358110", "00456430101", "00456430201", "00456430301", "00456431001", "00456432000", "00456432001", "00456432002", "00456433000", "00456433001", "00456433002", "00456434501", "00485005916", "00490008000", "00490008030", "00490008060", "00490008090", "00525037616", "00603119058", "00603465302", "00603465316", "00603465328", "00603465332", "00603465402", "00603465416", "00603465428", "00603465432", "00603465502", "00603465516", "00603465528", "00603465532", "00603465534", "00603594421", "00603594428", "00603594521", "00603594528", "00603594532", "00603594621", "00603594628", "00603594632", "00603595021", "00603595121", "00603595221", "00677000301", "00677000701", "00677084601", "00781555431", "00781555492", "00781555531", "00781555592", "00781556031", "00781556092", "00904161061", "00904161160", "00904161161", "00904161261", "00904588761", "00904588861", "00904588961", "10122090112", "10122090212", "10892015065", "12280004290", "13411015101", "13411015103", "13411015106", "13411015109", "13411015115", "13411016001", "13411016003", "13411016006", "13411016009", "13411016015", "13668007905", "13668007930", "13668007990", "13668008005", "13668008030", "13668008090", "13668008130", "13668008190", "15370002110", "16590002520", "17856064430", "17856064431", "21695019601", "21695019701", "23155006201", "23155006301", "23490735501", "23490754201", "24839022601", "24839022716", "29033000101", "29033000201", "35356009914", "35356012660", "35356015701", "38130001201", "42291062110", "42291062130", "42291062190", "42291062230", "42291062290", "42291062330", "42291062390", "42858070101", "42858070201", "43063038015", "43063038030", "43063038121", "43063038130", "45985064701", "49708064490", "49884030302", "49884030402", "49999053330", "49999053390", "49999055000", "49999061401", "49999081960", "49999088430", "49999088490", "49999092130", "49999095230", "49999098460", "49999098560", "50111045901", "50111045902", "50111045903", "50111048201", "50111048202", "50111048203", "50111048301", "50111048302", "50111051801", "50242004062", "50474010001", "50474020001", "50474020050", "50474030001", "50474030050", "50474040001", "50991020016", "50991021416", "50991040001", "50991041301", "51079022301", "51079022320", "51862013116", "51991053601", "52959013100", "52959027930", "52959028603", "54569004900", "54569005300", "54569006501", "54569006502", "54569006505", "54569101200", "54569101300", "54569166600", "54569397600", "54569460401", "54569460500", "54569473600", "54569516700", "54569524100", "54569524200", "54569524300", "54569566300", "54569567100", "54569592800", "54569626500", "54569626600", "54569632100", "54569634800", "54838055680", "54839051380", "54868002800", "54868002801", "54868002802", "54868002803", "54868002805", "54868002806", "54868002900", "54868002902", "54868002903", "54868002905", "54868002906", "54868002907", "54868126801", "54868143800", "54868143801", "54868146101", "54868146102", "54868188301", "54868189402", "54868271000", "54868271001", "54868282200", "54868328300", "54868328301", "54868328302", "54868417200", "54868417201", "54868417202", "54868451600", "54868451700", "54868451800", "54868463000", "54868484700", "54868529400", "54868536200", "54868554700", "54868554701", "54868554702", "54868563700", "54868584400", "54868585700", "54868585800", "54868593600", "54868593700", "54868598900", "54868599000", "54868599500", "55045186803", "55045252007", "55045306300", "55045335100", "55045335400", "55045338801", "55045368601", "55045369508", "55045376808", "55111059330", "55111059390", "55111059430", "55111059490", "55111062560", "55111062660", "55111072510", "55111072530", "55111072590", "55111072594", "55111076303", "55289078930", "55289096115", "55289096130", "55289098921", "55289098930", "55289099021", "55289099030", "55887012090", "55887027730", "55887067860", "55887084760", "55887084790", "58016460401", "58016481301", "58864065830", "58864069430", "59243002110", "59310017540", "59310017780", "59310020240", "59310020480", "59762003001", "59762003002", "59762004501", "59762004502", "59762004601", "59762004602", "60258033516", "60258033601", "60258037116", "60346028274", "60505356203", "60505356208", "60505356209", "60505357303", "60505357308", "60505357309", "60505357403", "60505357408", "60505357409", "60793001108", "60793001114", "61392001645", "61392001654", "61392001656", "61392001691", "61392001754", "61392001756", "61392001791", "62175020432", "62175020443", "62175020446", "62175020532", "62175020543", "62175020546", "62175021032", "62175021046", "63402071101", "63402071201", "63629163901", "63629279201", "63629279202", "63629355101", "63874044301", "63874044315", "63874044320", "63874044330", "63874044701", "63874044715", "63874044720", "63874044730", "63874044760", "63874067501", "63874067515", "63874067520", "63874067530", "64661081416", "65162032411", "65162033510", "65862056705", "65862056730", "65862056790", "65862056805", "65862056830", "65862056890", "65862057419", "65862057430", "65862057490", "66105016402", "66105016403", "66105016406", "66105016409", "66105016410", "66105050106", "66105050206", "66336059630", "67781025101", "67781025105", "67781025201", "67801030503", "68084061901", "68084061911", "68084062001", "68084062011", "68115032860", "68115054720", "68115063860", "68115065201", "68115065301", "68115065701", "68115076001", "68115077507", "68115092330", "68115092390", "68115092460", "68258303101", "68258303203", "68258303303", "68462035601", "68462038001", "68462039205", "68462039230", "68462039290", "68734070010", "68734071010")
  val ndcCS = ndcC.toSet
  
  // -----------
  // NDC of short acting agents only (all meds minus controller meds)
  // - eligible but does not meet the measure (for generateEligibleClaims w/o triggering meet measure)
  // - These meds are all inhaler meds
  val ndcAT = ndcA diff ndcC
  
  
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
  
  // exclusion criteria - ICD D
  //@TODO expand codes
  val icdDB = List("277.0*", "491.2*", "492*", "493.2*", "496", "506.4", "518.1", "518.2", "518.81")
  val icdDBS = icdDB.toSet
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

//    val offset = getIntervalFromYears(1).toDuration().getStandardDays().toInt
    val offset = Utils.daysBetween(hedisDate.minusYears(1), hedisDate)
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
	          pl.createMedClaim(patient.patientID, provider.providerID, d.minusDays(20), d.minusDays(20), icdDPri=pickOne(icdDA), ubRevenue = pickOne(ubC)),
	          pl.createMedClaim(patient.patientID, provider.providerID, d.minusDays(40), d.minusDays(40), icdDPri=pickOne(icdDA), cpt = pickOne(cptC)),
	          pl.createMedClaim(patient.patientID, provider.providerID, d.minusDays(60), d.minusDays(60), icdDPri=pickOne(icdDA), ubRevenue = pickOne(ubC)),
	          pl.createRxClaim(patient.patientID, provider.providerID, d, ndc=pickOne(ndcAT), daysSupply=30, qty=1),
	          pl.createRxClaim(patient.patientID, provider.providerID, d.minusDays(44), ndc=pickOne(ndcAT), daysSupply=30, qty=1)
	          ),
	      
	      // At least 4 asthma medication dispensing events
	      () => List(
	          pl.createRxClaim(patient.patientID, provider.providerID, d, ndc=pickOne(ndcAT), daysSupply=30, qty=1),
	          pl.createRxClaim(patient.patientID, provider.providerID, d.minusDays(30), ndc=pickOne(ndcAT), daysSupply=30, qty=1),
	          pl.createRxClaim(patient.patientID, provider.providerID, d.minusDays(60), ndc=pickOne(ndcAT), daysSupply=30, qty=1),
	          pl.createRxClaim(patient.patientID, provider.providerID, d.minusDays(90), ndc=pickOne(ndcAT), daysSupply=30, qty=1)
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
        val rxClaims = filterClaims(ph.ndc, ndcAS, { claim: RxClaim => measurementInterval.contains(claim.fillD) })
        if(hasDifferentDates(4, medClaims) && nbrDispensingEvents(rxClaims) >= 2) List.concat(medClaims, rxClaims)
        else List.empty
      },
      
      // At least 4 asthma medication dispensing events where a leukotriene modifier was not the sole medication dispensed
      // At least 4 asthma medication dispensing events where a leukotriene modifier was the sole medication dispensed, and there was a diagnosis of asthma in any setting in the same year
      () => {
        val medClaims = filterClaims(ph.icdD, icdDAS, { claim: MedClaim => measurementInterval.contains(claim.dos) })
        val rxClaims = filterClaims(ph.ndc, ndcAS, { claim: RxClaim => measurementInterval.contains(claim.fillD) })
        
        // compute the nbr of Dispensing Events
        if(nbrDispensingEvents(rxClaims) >= 4) {
        
        // get the claims with Non Leukotriene Modifier meds (nlm)
        val nlm = rxClaims filter {c => asthmaMeds.get(c.ndc) map {_.apply(CATEGORY) != "leukotriene modifiers"} getOrElse false }
        
        // if the dispensing events where a leukotriene modifier was not the sole medication dispensed
        if(!nlm.isEmpty) List.concat(medClaims, rxClaims)
        
        // if dispensing events where a leukotriene modifier was the sole medication dispensed, and there was a diagnosis of asthma in any setting in the same year
        else if(!medClaims.isEmpty) List.concat(medClaims, rxClaims)
        
        // missing a diagnosis of asthma
        else List.empty
        } else List.empty
      })
      
      rules.foldLeft[List[Claim]](List.empty) {(claims, f) => List.concat(claims, f())} distinct  
    }
    
    if (!isPatientMeetDemographic(patient)) scorecard.addScore(name, HEDISRule.eligible, false)
    else {
    
    // check the eligibility in both years (measurement year and prior year), patient meet eligibility if claims are returned
    val y1 = eligiblePatients(measurementInterval1)
    val y2 = eligiblePatients(measurementInterval2)
      if(y1.isEmpty || y2.isEmpty) scorecard
    else scorecard.addScore(name, HEDISRule.eligible, "Patient Eligible", List.concat(y1, y2))
    }
  }

  override def generateExclusionClaims(pl: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = {

    val days = Utils.daysBetween(patient.dob.plusYears(3), hedisDate)
    val dos = hedisDate.minusDays(Random.nextInt(days))

    // exclusion - patients with a diagnosis of emphysema, COPD, cystic fibrosis, or acute respiratory failure
    List( pl.createMedClaim(patient.patientID, provider.providerID, dos, dos, icdDPri = pickOne(icdDB)))
  }

  override def scorePatientExcluded(scorecard: Scorecard, patient: Patient, ph: PatientHistory): Scorecard = {

    // exclusion - patients with a diagnosis of emphysema, COPD, cystic fibrosis, or acute respiratory failure
    val claims = filterClaims(ph.icdD, icdDBS, { claim: MedClaim => !claim.dos.isAfter(hedisDate) })
    scorecard.addScore(name, HEDISRule.excluded, "Excluded Patient", claims)
  }

  override def generateMeetMeasureClaims(pl: PersistenceLayer, patient: Patient, provider: Provider): List[Claim] = {

    val days = Utils.daysBetween(hedisDate.minusYears(1), hedisDate)
    val fillD = hedisDate.minusDays(Random.nextInt(days))

    List(pl.createRxClaim(patient.patientID, provider.providerID, fillD, ndc = pickOne(ndcC)))
  }

  override def scorePatientMeetMeasure(scorecard: Scorecard, patient: Patient, ph: PatientHistory): Scorecard = {

    val measurementInterval = getIntervalFromYears(1)

    // Check if patient has a controller drug
    val claims = filterClaims(ph.ndc, ndcCS, { claim: RxClaim => measurementInterval.contains(claim.fillD) })
    scorecard.addScore(name, HEDISRule.meetMeasure, "Patient Meet Measure", claims)
  }
}

class ASM_5_11_Rule(config: RuleConfig, hedisDate: DateTime) extends ASM_Rule(ASM.name5, "5 - 11", 5, 11, config, hedisDate)
class ASM_12_18_Rule(config: RuleConfig, hedisDate: DateTime) extends ASM_Rule(ASM.name12, "12 - 18", 12, 18, config, hedisDate)
class ASM_19_50_Rule(config: RuleConfig, hedisDate: DateTime) extends ASM_Rule(ASM.name19, "19 - 50", 19, 50, config, hedisDate)
class ASM_51_64_Rule(config: RuleConfig, hedisDate: DateTime) extends ASM_Rule(ASM.name51, "51 - 64", 51, 64, config, hedisDate)
