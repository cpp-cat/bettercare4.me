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

// Patients table
//--------------
CREATE TABLE patients (
  id TEXT,
  data LIST<TEXT>,
  PRIMARY KEY (id)
);

// Providers table
//--------------
CREATE TABLE providers (
  id TEXT,
  data LIST<TEXT>,
  PRIMARY KEY (id)
);

// Claims table
//--------------
// person_id can be either patient.id or provider.id, both are maintained so claims can be retreived by
// patients and also by provider.
CREATE TABLE claims (
  id TEXT,
  person_id TEXT,
  dos TIMESTAMP,
  data LIST<TEXT>,
PRIMARY KEY (person_id, dos, id)
);

// Test Data
INSERT INTO patients (id, data) VALUES ('patient-1-0', ['patient-1-0','MARCIA','COOPER','F','1964-09-30']);
INSERT INTO patients (id, data) VALUES ('patient-1-1', ['patient-1-1','COURTNEY','HUDSON','M','1959-01-31']);
INSERT INTO patients (id, data) VALUES ('patient-1-2', ['patient-1-2','CECELIA','LLOYD','F','1962-10-31']);

INSERT INTO providers (id, data) VALUES ('provider-1-0', ['provider-1-0','MIREYA','MADDOX']);
INSERT INTO providers (id, data) VALUES ('provider-1-1', ['provider-1-1','SHERIKA','MEJIA']);
INSERT INTO providers (id, data) VALUES ('provider-1-2', ['provider-1-2','HERMELINDA','KIRKLAND']);

INSERT INTO claims (id, person_id, dos, data) VALUES ('c-md-1-0', 'patient-1-0', '2013-02-25T00:00:00.00-05:00', ['c-md-1-0','patient-1-0','provider-1-2','2013-02-25','2013-02-25','','','','','','0','N','','','','','','','','','','','','','','','','','','','','','','','','','','','','G0225','']);
INSERT INTO claims (id, person_id, dos, data) VALUES ('c-md-1-0', 'provider-1-2', '2013-02-25T00:00:00.00-05:00', ['c-md-1-0','patient-1-0','provider-1-2','2013-02-25','2013-02-25','','','','','','0','N','','','','','','','','','','','','','','','','','','','','','','','','','','','','G0225','']);

INSERT INTO claims (id, person_id, dos, data) VALUES ('c-md-1-1', 'patient-1-0', '2013-03-15T00:00:00.00-05:00', ['c-md-1-1','patient-1-0','provider-1-2','2013-03-15','2013-03-15','','','','','','0','N','','','','','','','','','','','','','','','','','','','','','','','','','','','','G0315','']);
INSERT INTO claims (id, person_id, dos, data) VALUES ('c-md-1-1', 'provider-1-2', '2013-03-15T00:00:00.00-05:00', ['c-md-1-1','patient-1-0','provider-1-2','2013-03-15','2013-03-15','','','','','','0','N','','','','','','','','','','','','','','','','','','','','','','','','','','','','G0315','']);

INSERT INTO claims (id, person_id, dos, data) VALUES ('c-md-1-2', 'patient-1-0', '2013-04-15T00:00:00.00-05:00', ['c-md-1-2','patient-1-0','provider-1-1','2013-04-15','2013-03-15','','','','','','0','N','','','','','','','','','','','','','','','','','','','','','','','','','','','','G0415','']);
INSERT INTO claims (id, person_id, dos, data) VALUES ('c-md-1-2', 'provider-1-1', '2013-04-15T00:00:00.00-05:00', ['c-md-1-2','patient-1-0','provider-1-1','2013-04-15','2013-03-15','','','','','','0','N','','','','','','','','','','','','','','','','','','','','','','','','','','','','G0415','']);

