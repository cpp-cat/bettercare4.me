/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
//
// bettercare4me.cql
//
///////////////////////////////////////////////////////////////////
//
// BetterCare4.me Schema Definition
//
// Command used:
// $ ccm start
// $ ccm status
// $ ccm node1 cqlsh
// cqlsh> source 'data/bettercare4me.cql'
///////////////////////////////////////////////////////////////////
//
DROP KEYSPACE IF EXISTS bettercare4me;

CREATE KEYSPACE bettercare4me WITH replication = {'class':'SimpleStrategy','replication_factor':1};

USE bettercare4me;
/////////////////////////////////////////////////////////////////////////////////////////////

// Patients table
//--------------
//   batch_id is the load batch id (should have batch_id >> nbr of nodes)
//   id is patient id
//   data is patient demographic data serialized as a list of strings
CREATE TABLE patients (
  batch_id INT,
  id TEXT,
  data LIST<TEXT>,
  PRIMARY KEY (batch_id, id)
);

// Providers table
//--------------
//   batch_id is the load batch id (should have batch_id >> nbr of nodes)
//   id is provider id
//   data is provider data serialized as a list of string
CREATE TABLE providers (
  batch_id INT,
  id TEXT,
  data LIST<TEXT>,
  PRIMARY KEY (batch_id, id)
);

// Claims table
//--------------
//   batch_id is the load batch id (should have batch_id >> nbr of nodes)
//   id is the claim id
//   patient_id / provider_id
//   dos is claim date of service
//   data is claim data serialized as a list of string
CREATE TABLE claims_patients (
  batch_id INT,
  id TEXT,
  patient_id TEXT,
  dos TIMESTAMP,
  data LIST<TEXT>,
  PRIMARY KEY (batch_id, patient_id, dos, id)
);

CREATE TABLE claims_providers (
  batch_id INT,
  id TEXT,
  provider_id TEXT,
  dos TIMESTAMP,
  data LIST<TEXT>,
  PRIMARY KEY (batch_id, provider_id, dos, id)
);

/////////////////////////////////////////////////////////////////////////////////////////////

// HEDISScoreSummary Table - top level dashboard at population level
//-----------------------------------------------------------------
//   name is HEDIS run name
//   hedis_date is end date of evaluation period
//   patient_count is number of patient in population
//   score_summary is a list of RuleScoreSummary.toParseString
//   rule_config is the yaml rule configuration
CREATE TABLE hedis_summary (
  name TEXT,
  hedis_date TIMESTAMP,
  patient_count BIGINT,
  score_summaries LIST<TEXT>,
  claim_generator_config TEXT,
  PRIMARY KEY (name, hedis_date)
)
WITH CLUSTERING ORDER BY (hedis_date DESC);


// Rule Scorecard Table - rule scorecard summaries
//------------------------------------------------
//   rule_name rule identifier
//   hedis_date is end date of evaluation period
//   batch_id batch partition (used to complement the patient_id to read Patient Scorecard)
//   patient_id patient identifier (only patients that are eligible are included)
//   patient_data patient demographics
//   is_excluded is patient excluded from measure
//   is_meet_measure indicates if patient meets the rule measure
CREATE TABLE rule_scorecards (
  rule_name TEXT,
  hedis_date TIMESTAMP,
  batch_id INT,
  patient_id TEXT,
  patient_data LIST<TEXT>,
  is_excluded BOOLEAN,
  is_meet_criteria BOOLEAN,
  PRIMARY KEY (rule_name, hedis_date, patient_id)
)
WITH CLUSTERING ORDER BY (hedis_date DESC);

// Patient Scorecard Table - patient scorecard
//--------------------------------------------
//   batch_id batch partition
//   patient_id id of patient
//   patient_data patient demographics
//   rule_name rule identifier
//   XXX: one of (eligible, excluded, meetMeasure)
//   is_XXX indicates whether the XXX criteria is met or not (RuleCriteriaScore.isCriteriaMet)
//   XXX_score represent RuleCriteriaScore.criteriaScore; list of [(claim_id, provider_last_name, provider_first_name, dos, reason).mkString]
CREATE TABLE patient_scorecards (
  batch_id INT,
  hedis_date TIMESTAMP,
  patient_id TEXT,
  patient_data LIST<TEXT>,
  rule_name TEXT,
  is_eligible BOOLEAN,
  eligible_score LIST<TEXT>,
  is_excluded BOOLEAN,
  excluded_score LIST<TEXT>,
  is_meet_criteria BOOLEAN,
  meet_criteria_score LIST<TEXT>,
  PRIMARY KEY (batch_id, patient_id, hedis_date, rule_name)
)
WITH CLUSTERING ORDER BY (patient_id ASC, hedis_date DESC);


// Test Data
INSERT INTO patients (batch_id, id, data) VALUES (991, 'patient-1-0', ['patient-1-0','MARCIA','COOPER','F','1964-09-30']);
INSERT INTO patients (batch_id, id, data) VALUES (992, 'patient-1-1', ['patient-1-1','COURTNEY','HUDSON','M','1959-01-31']);
INSERT INTO patients (batch_id, id, data) VALUES (993, 'patient-1-2', ['patient-1-2','CECELIA','LLOYD','F','1962-10-31']);

INSERT INTO providers (batch_id, id, data) VALUES (991, 'provider-1-0', ['provider-1-0','MIREYA','MADDOX']);
INSERT INTO providers (batch_id, id, data) VALUES (992, 'provider-1-1', ['provider-1-1','SHERIKA','MEJIA']);
INSERT INTO providers (batch_id, id, data) VALUES (991, 'provider-1-2', ['provider-1-2','HERMELINDA','KIRKLAND']);

INSERT INTO claims_patients  (batch_id, id, patient_id, dos, data) VALUES (991, 'c-md-1-0', 'patient-1-0', '2013-02-25T00:00:00.00-05:00', ['c-md-1-0','patient-1-0','provider-1-2','2013-02-25','2013-02-25','','','','','','0','N','','','','','','','','','','','','','','','','','','','','','','','','','','','','G0225','']);
INSERT INTO claims_providers (batch_id, id, provider_id, dos, data) VALUES (991, 'c-md-1-0', 'provider-1-2', '2013-02-25T00:00:00.00-05:00', ['c-md-1-0','patient-1-0','provider-1-2','2013-02-25','2013-02-25','','','','','','0','N','','','','','','','','','','','','','','','','','','','','','','','','','','','','G0225','']);

INSERT INTO claims_patients  (batch_id, id, patient_id, dos, data) VALUES (991, 'c-md-1-1', 'patient-1-0', '2013-03-15T00:00:00.00-05:00', ['c-md-1-1','patient-1-0','provider-1-2','2013-03-15','2013-03-15','','','','','','0','N','','','','','','','','','','','','','','','','','','','','','','','','','','','','G0315','']);
INSERT INTO claims_providers (batch_id, id, provider_id, dos, data) VALUES (991, 'c-md-1-1', 'provider-1-2', '2013-03-15T00:00:00.00-05:00', ['c-md-1-1','patient-1-0','provider-1-2','2013-03-15','2013-03-15','','','','','','0','N','','','','','','','','','','','','','','','','','','','','','','','','','','','','G0315','']);

INSERT INTO claims_patients  (batch_id, id, patient_id, dos, data) VALUES (991, 'c-md-1-2', 'patient-1-0', '2013-04-15T00:00:00.00-05:00', ['c-md-1-2','patient-1-0','provider-1-1','2013-04-15','2013-03-15','','','','','','0','N','','','','','','','','','','','','','','','','','','','','','','','','','','','','G0415','']);
INSERT INTO claims_providers (batch_id, id, provider_id, dos, data) VALUES (991, 'c-md-1-2', 'provider-1-1', '2013-04-15T00:00:00.00-05:00', ['c-md-1-2','patient-1-0','provider-1-1','2013-04-15','2013-03-15','','','','','','0','N','','','','','','','','','','','','','','','','','','','','','','','','','','','','G0415','']);

