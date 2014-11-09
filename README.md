# BetterCare4.me Application

> This Reactive Web Application is based on Play Framework, Akka and Apache Spark using the Scala language.

# Product backlog

## Mobile Features
- Create single page mobile app using PhoneGap, Backbone, Handlebar and jQuery Mobile
- Develop media files for backgrounds and logo
- Put in place a left menu and right pane for dialog with left/right page navigation
- Simple login using email address
- Loging using OAuth with Google and Facebook
- Search result page without recommendations
- Search result page with recommendations on top
- Search result page with recommendations and features events
- User profile page
- Add past activity to user profile page
- Provider profile page with user discussion with star rating
- Facility profile page with user discussion and star rating
- Personal calendar for personalized health roadmap (Gaps in Care Opportunities)
- Personal list of Gaps in Care (dashboard)
- A 2-questions survey for closing opportunity
- Care event submission page (gap closing)

## Font End Features (DRAFT)
- Create an index.html with a login
- Create a dashboard page to to show a payor quality rating (HEDIS measures)
- Create a dashboard page to to show a primary care organization (by provider breakdown) quality rating (HEDIS measures)
- Create a dashboard page to to show a primary care physician quality rating (HEDIS measures)
- Make a user profile page

## Data Management Features (DRAFT)
- Add Cassandra database for storage
- Investigate if need Akka Circuit Breaker pattern or supervision for claim generation agents
- Create a security model with a user login using email and OAuth 

## Recommender Engine
- Make recommendations using Collaborative Filtering
- Make recommendations based on content-based search criteria.


# Sprint 7: Simulated HEDIS Report using Batch Claim Generation

- Version: 0.8.0.00 (v0.8.0.00_11-XX-2014)
- Start Date: 11/06/2014
- Target Date: 11/16/2014
- Actual Date: 11/XX/2014

## Product Features:
- Provide an user interface to kicking off the batch jobs
- Batch generation of claims on local file system using Akka agent supervision
- Generation of HEDIS 2014 Summary Report using Akka agent supervision.

## User Stories Sprint Backlog.
- Simple user interface using jQuery Mobile generated from Play Twirl, basic index page
- User interface for kicking off the claim generator
- User interface for kicking off the HEDIS report generation
- Agent supervision for claim generation and report generation
- Simple web application to kick off the the batch jobs

## Completed User Stories
- Setup claim generator configuration (yaml configuration) to match the NCQA The State of Health Care Quality 2014 Report for Commercial HMO (http://www.ncqa.org/Portals/0/Newsroom/2014/SOHC-web.pdf)



# Sprint 6: Batch Claim Generation Ready and Simulated HEDIS Report

- Version: 0.7.0.00 (v0.7.0.00_10-18-2014)
- Start Date: 10/10/2014
- Target Date: 10/17/2014
- Actual Date: 10/18/2014

## Product Features:
- Batch generation of claims on local file system
- Testing the integration of all HEDIS 2014 rules.

## User Stories Sprint Backlog.

## Completed User Stories
- Testing the integration of the rules to avoid conflics.
- Define rule dependency to use same random generated numbers to avoid skewing the results.
    + Adding optional simulationParity attribute to rule configuration
    + Rules linked with simulationParity must have same meetMeasureRate
- Generating the claims for all rules based on YAML configuration --- complete test case.



# Sprint 5: Completed HEDIS rules Implementation

- Version: 0.6.0.00 (v0.6.0.00_10-10-2014)
- Start Date: 9/29/2014
- Target Date: 10/05/2014
- Actual Date: 10/10/2014

## Product Features:
- Completing the implementation of the HEDIS 2014 Rules

## User Stories Sprint Backlog

## Completed User Stories
- Added CDC - Diabetes Lipid Test < 100 mg/dL HEDIS Rule
- Added CDC - Cervical Cancer Screening HEDIS Rule
- Added CDC - BP Test (Test Performed) HEDIS Rule
- Added CDC - BP Test (<140/80 mmHg) HEDIS Rule
- Added CDC - BP Test (<140/90 mmHg) HEDIS Rule
- Added Chlamydia Screening by Age HEDIS Rule
- Added Colorectal Cancer Screening HEDIS Rule
- Added Chicken Pox Immunization HEDIS Rule
- Added DTaP Immunization HEDIS Rule
- Added Hep B Immunization HEDIS Rule
- Added Influenza Type B Immunization HEDIS Rule
- Added Measles / Mumps / Rubella Immunization HEDIS Rule
- Added Pneumococcal Conjugate Immunization HEDIS Rule
- Added Polio Immunization HEDIS Rule
- Added Well-Child Visits in the First 15 Months of Life HEDIS Rule
- Added Well-Child Visits in the 3 - 6 Years of Life HEDIS Rule
- Added Adolescent Well Care Visits HEDIS Rule
- Added Asthma Medication Ratio by Age HEDIS Rule
- Added Cholesterol Management for Patient with Cardiovascular Conditions HEDIS Rule
- Added Annual Monitoring for Patients on Persistent Medications (ACE/ARB) HEDIS rule
- Added Annual Monitoring for Patients on Persistent Medications (Digoxin) HEDIS rule
- Added Annual Monitoring for Patients on Persistent Medications (Diuretics) HEDIS rule
- Added Annual Monitoring for Patients on Persistent Medications (Carbamazepine) HEDIS rule
- Added Annual Monitoring for Patients on Persistent Medications (Phenobarbital) HEDIS rule
- Added Annual Monitoring for Patients on Persistent Medications (Phenytoin) HEDIS rule
- Added Annual Monitoring for Patients on Persistent Medications (Valproic) HEDIS rule
- Added Appropriate Testing for Children with Pharyngitis HEDIS rule
- Added Appropriate Treatment for Children with Upper Repiratory Infection HEDIS rule
- Added Avoidance of Antibiotics Treatment in Adults with Acute Bronchitis HEDIS rule
- Added Use of Imaging Studies for Low Back Pain HEDIS rule
- Fix HEDIS date range
- Fix CDC and CHL rules where the eligible criteria was also trigering the meet measure and creating false positive
- Added parameters to PatientHistory and some refactoring
- Factored out CDC_LTest and LDL_C_TestValue into separate classes used by CDC_LDL_C and CMC


# Sprint 4: Enhanced Rule Framework for HEDIS rules

- Version: 0.5.0.00 (v0.5.0.00_09-28-2014)
- Start Date: 9/24/2014
- Target Date: 9/29/2014
- Actual Date: 9/28/2014

## Product Features:
- Enhancing the rule framework to be able to report which claim and which rule made the patient
  to meet the measure criteria (denominator), be excluded, and meet the measure.

## User Stories Sprint Backlog

## Completed User Stories
- Define MedClaim, RxClaim and LabClaim using the same Claim base trait
- Create a Scorecard class to keep track of the score of each prediates.
- Refactor HEDISRuleBase class to leverage Scorecard class to scoring HEDIS measure using claims
- Added CDC - Urine Microalbumin Test HEDIS Rule


# Sprint 3: RxClaim detail class and using NDCs in HEDIS rules

- Version: 0.4.0.00 (v0.4.0.00_09-23-2014)
- Start Date: 9/16/2014
- Target Date: 9/21/2014
- Actual Date: 9/23/2014

## Product Features:
- Add RxClaim class to capture pharmacy claims.
- Add LabClaim class to capture lab claims.
- Add NDCs as component for HEDIS Rule framework

## User Stories Sprint Backlog
- Detail CDC HEDIS rules

## Completed User Stories
- Define MedClaim, RxClaim and LabClaim using the same Claim base trait
- Rename class Claim to MedClaim, representing medical claim
- Add ability in ClaimParser to create the correct claim instance class based on claim type (Pharmacy vs Medical vs Lab)
- Refactor package naming to add com.nickelsoftware.bettercare4me to all packages.
- Created package com.nickelsoftware.bettercare4me.hedis to put base HEDIS rules framework classes
- Created package com.nickelsoftware.bettercare4me.hedis.hedis2014 for all HEDIS 2014 rules
- Read NDC from CSV file for use in HEDIS rules
- Created CDCRuleBase as base class for all CDC rule classes. This base class implement CDC meet and exlusion criteria
- Added CDC - Diabetes HbA1c Test HEDIS rule
- Added CDC - Diabetes HbA1c Test with values < 7% HEDIS rule
- Added CDC - Diabetes HbA1c Test with values < 8% HEDIS rule
- Added CDC - Diabetes HbA1c Test with values > 9% HEDIS rule
- Added CDC - Diabetes Eye Exam Hedis Rule
- Added NUCC Provider Specialty to Claim class
- Refactored Claim class due to excessive number of class parameters.

## HEDIS Measures Information
- http://www.bcbstx.com/provider/pdf/diabetes_2014.pdf
- http://www.bcbstx.com/provider/pdf/bronchitis_2014.pdf
- http://www.bcbstx.com/provider/pdf/anticonvulsants_2014.pdf
- http://www.bcbstx.com/provider/pdf/med_monitoring_2014.pdf
- http://www.bcbstx.com/provider/pdf/asthma_2014.pdf
- http://www.bcbstx.com/provider/pdf/chlamydia_2014.pdf
- http://www.bcbstx.com/provider/pdf/antirheumatic_2014.pdf
- http://www.bcbstx.com/provider/pdf/uri_2014.pdf
- http://www.bcbstx.com/provider/pdf/mammography_2014.pdf
- http://www.bcbstx.com/provider/pdf/pharyngitis_2014.pdf
- http://www.bcbstx.com/provider/pdf/colorectal_2014.pdf
- http://www.bcbstx.com/provider/pdf/cervical_2014.pdf
- http://www.bcbstx.com/provider/pdf/backpain_2014.pdf
- http://www.bcbstx.com/provider/pdf/well_child_2014.pdf
- http://www.bcbstx.com/provider/pdf/cardiovascular_2014.pdf
- Also of interest: http://www.bcbstx.com/provider/pdf/ebm_guidelines_desc_14.pdf


# Sprint 2: Claim detail class and HEDIS rules

- Version: 0.3.0.00 (v0.3.0.00_09-13-2014)
- Start Date: 9/07/2014
- Target Date: 9/14/2014
- Actual Date: 9/13/2014

## Product Features:
- Augment Claim class to capture all neccessairy details for HEDIS processing.
- Complete HEDIS Rule framework

## User Stories Sprint Backlog

## Completed User Stories
- Augment Claim class to represent all details of a claim with exception to patient demographics and payment information
- Adjust domain model for proper date processing
- Refactor PatientHistory class to map ICD, CPT, HCPCS, and UB billing codes to claims
- Detail a full HEDIS rule (Breast Cancer Screening)


# Sprint 1: Claim simulator framework

- Version: 0.2.0.00
- Start Date: 8/26/2014
- Target Date: 8/30/2014
- Actual Date: 9/06/2014

## Product Features:
- Agent-based claim simulator framework.
- Generate patient profiles, physician profile (minimum viable)
- Generate claims for HEDIS reporting simulation (framework in place, actual HEDIS rule in separate sprint).

## User Stories Sprint Backlog

## Completed User Stories
- Added dependency on scala-csv (https://github.com/tototoshi/scala-csv) for creating and reading csv files.
- Added dependency on joda-time for DateTime (http://www.joda.org/joda-time/)
- Added dependency on scalatest (scalatest.org) and scalactic (scalactic.org) for testing and error handling
- Added dependency on snakeyaml (https://code.google.com/p/snakeyaml/) for parsing YAML config files
- Generate a pool of physicians with minimum profile information
- Generate claims and patient profile for each HEDIS measures, based on a in-measure target rate.
- The HEDIS measures are represented by placeholders for the purpose of this sprint.
- Generated claims are stored in CSV files, configurable number of files are generated each with a target number of patients.
- The population is generated using a realistic representation of the age distribution of the population

# Sprint 0: Seeding the project

- Version: 0.1.0.00
- Start Date: 8/26/2014
- Target Date: 8/26/2014
- Actual Date: 8/26/2014

## Product Features:
- Initial generic project seed files (scala + play + akka + spark)

## User Stories Sprint Backlog

## Completed User Stories
- Spark 1.1.0-SNAPSHOT, compiled locally with Akka 2.3.4
- Implement SimpleActor using Akka 2.3.4
- Using Play Framework version 2.3.3.

