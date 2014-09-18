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


# Sprint 3: RxClaim detail class and using NDCs in HEDIS rules

- Version: 0.4.0.00 (v0.4.0.00_09-XX-2014)
- Start Date: 9/16/2014
- Target Date: 9/21/2014
- Actual Date: 9/XX/2014

## Product Features:
- Add RxClaim class to capture pharmacy claims.
- Add LabClaim class to capture lab claims.
- Add NDCs as component for HEDIS Rule framework

## User Stories Sprint Backlog
- Read NDC from CSV file for use in HEDIS rules
- Detail CDC HEDIS rules

## Completed User Stories
- Define MedClaim, RxClaim and LabClaim using the same Claim base trait
- Rename class Claim to MedClaim, representing medical claim
- Add ability in ClaimParser to create the correct claim instance class based on claim type (Pharmacy vs Medical vs Lab)
- Refactor package naming to add com.nickelsoftware.bettercare4me to all packages.
- Created package com.nickelsoftware.bettercare4me.hedis to put base HEDIS rules framework classes
- Created package com.nickelsoftware.bettercare4me.hedis.hedis2014 for all HEDIS 2014 rules


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

