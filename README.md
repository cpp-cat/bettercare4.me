# BetterCare4.me Application

> This Reactive Web Application is based on Play Framework, Akka and Apache Spark using the Scala language.

# Product backlog

## Bettercare4.me Product Backlog
- Provide search capability to find patients based on gaps
- Add Gaps Summary section on Patient Scorecard page
- Add visual dashboards
  - Patient distribution by age
  - HEDIS reports with pie charts
- Add a Claim Detail page (what information to show?)

######################################################################################################

# Sprint 10: Monitoring using JMX on AWS

- Version: 0.11.0.00 (v0.11.0.00_01-XX-2015)
- Start Date: 01/15/2015
- Target Date: 01/18/2015
- Actual Date: 01/XX/2015

## Product Features:
- Monitoring application JVM using JMX on AWS
- Setting JVM memory parameters on AWS

## User Stories Sprint Backlog.
- Testing JMX remote monitoring using `jconsole` on a sample application

## Completed User Stories
  - Set the `SPARK_PUBLIC_DNS` of the master and workers in `data/spark_prod_conf/spark-env.sh`
  - Set the workers memory setting in `data/spark_prod_conf/spark-env.sh`
  - Open ports 9999 and 9998 in `bc4me-spark-cluster-master` and `bc4me-spark-cluster-slaves` security groups



# Sprint 9: Deploying on AWS

- Version: 0.10.0.00 (v0.10.0.00_01-15-2015)
- Start Date: 01/04/2015
- Target Date: 01/09/2015
- Actual Date: 01/15/2015

## Product Features:
- Deploying on AWS
- Some UI improvements

## User Stories Sprint Backlog.


## Completed User Stories
- Code changed
-
  - Better error handling when failures occurs during spark jobs. See `Application.reportGeneratorSubmit` handling error sent by
    `ClaimGeneratorActor.ProcessGenereatedClaims`
  - Open only one database connection to Cassandra, check perfomed in the `connect` method.
  - Reading cassandra and spark configuration file name from `com.nickelsoftware.bettercare4me.utils.Properties` (for Play Application only, not spark cluster)
    - Spark config file name from env variable `BC4ME_SPARK_CONFIG` with default of `spark.yaml` (relative to `BC4ME_DATA_DIR`)
    - Cassandra config file name from env variable `BC4ME_CASSANDRA_CONFIG` with default of `cassandra.yaml` (relative to `BC4ME_DATA_DIR`)
  - Need to use fully qualified dates, e.g., `2013-12-31T00:00:00.00-05:00` throughout the code (no LocalDate)
    - Needed to `override def equals(that: Any): Boolean = that match { ...` to use `DateTime.isEqual`
      - Overrided for Claim (MedClaim, RxClaim, and LabClaim)
  - Added error handling when connection to Cassandra fails (case where there is no database)
  - Configuring `./data` directory using environment variable `BC4ME_DATA_DIR` with `./data` as the default
  - Adding `scalax.io` incubator library for file path manipulation (http://jesseeichar.github.io/scala-io-doc/0.4.3/index.html#!/overview)
  - Removed the first-names.csv and last-names.csv from configuration.
  - Remove all hardcoded reference to `./data` and replace with `Properties.dataDir`
  - Ensure connection to Cassandra is initialized in spark slave.

- Create a Play AMI
-
  - Create a keypair for play instance: `play1-kp.pem`
  - Create  ubuntu linux ec2 instance having java 7: 
    - Instance type: m3.large
    - Image: ami-24af204c (Medidata Ubuntu 14.04 + Java 7)
    - Availability zone: us-east-1d
    - Root storage using EBS 10GB, mark to be persistent
    - Security group: bc4me-spark-cluster-master (need to open port 80 & 443 on this security group)
    - SSH key pair: play1-kp
    - Strarted with public DNS: ec2-54-205-171-3.compute-1.amazonaws.com
  - The Play instance is started
  - Copy the `spark1-kp.pem` and `cassandra1-kp.pem` onto the play instance to be able to ssh into the spark cluster:
    - `$ scp -i ~/play1-kp.pem ~/spark1-kp.pem ubuntu@ec2-54-205-171-3.compute-1.amazonaws.com:~/` (using the public DNS of the play instance)
    - `$ scp -i ~/play1-kp.pem ~/cassandra1-kp.pem ubuntu@ec2-54-205-171-3.compute-1.amazonaws.com:~/` (using the public DNS of the play instance)
  - Copy the `data/ec2_deploy` on the home directory of Play instance
    - `$ scp -i ~/play1-kp.pem data/ec2_deploy ubuntu@ec2-54-205-171-3.compute-1.amazonaws.com:~/` (using the public DNS of the play instance)

- Starting the Play Application on Play instance
-
  - Start the spark cluster and create a cassandra cluster
    - Put the spark master private IP onto `data/spark_prod_conf/masters`
    - Put the spark slaves private IP onto `data/spark_prod_conf/slaves`
    - Put the spark master url onto `data/spark-prod.yaml`
    - Put the spark public IP onto `data/spark-prod.yaml`
    - Put the private IP of the cassandra master onto `data/cassandra-prod.yaml`
    - Put spark master and cassandra master private IP in `data/ec2-deploy`
    - Commit those changes and push it to github master

  - Start the system monitoring on play instance:
    - `$ java -jar remote-linux-monitor-v1.05.jar -i ~/play1-kp.pem -H <public IP> -u ubuntu &`
  - Copy the `ec2_deploy` script to play instance
    - `$ scp -i ~\play1-kp.pem ./data/ec2_deploy ubuntu@<public IP>:~/`
  - SSH to the play ec2 instance: 
    - `$ ssh -i ~/play1-kp.pem ubuntu@<public IP>` (using the correct public DNS)
  - Deploy the Play Application on Play instance using `data/ec2_deploy` script:
    - `~$ cd bettercare4.me/play/`
    - `$ ~/ec2_deploy -v`
  - The script contains the following:
    - Clone the Bettercare4.me git repository onto the play instance, or update the repo
      - `$ sudo apt-get install git`
      - `$ git clone https://github.com/regency901/bettercare4.me.git`
      - `$ git checkout -- .` to discard modified files not on the git index
      - `$ git pull origin master`
    - Generated a new application secret key and package the application using:
      - `$ ./activator clean play-update-secret stage`
        - Packaged the application is in `target/universal/stage/`
        - Class path for the application (specified in `spark-env.sh`): `app_classpath="/root/stage/lib/*`
    - Copy the packaged application to the spark master node (we're still ssh'ed onto the play instance):
      - `$ scp -i ~/spark1-kp.pem -r target/universal/stage root@<spark master private IP>:/root/` 
      - `$ scp -i ~/spark1-kp.pem -r data root@<spark master private IP>:/root/stage/`
      - `$ scp -i ~/spark1-kp.pem data/spark_prod_conf/* root@<spark master private IP>:/root/spark/conf/`
      - `$ scp -i ~/spark1-kp.pem data/spark_prod_conf/masters root@<spark master private IP>:/root/spark-ec2/`
      - `$ scp -i ~/spark1-kp.pem data/spark_prod_conf/slaves root@<spark master private IP>:/root/spark-ec2/`
    - Copy the database schema to the cassandra master node
      - `$ scp -i ~/cassandra1-kp.pem data/bettercare4me.cql ubuntu@<cassandra master private dns>:~/`
    - Exit from play instance to return to local shell (firefly)
 
    - Logon onto the spark cluster (on master node) from local shell (firefly): 
      - `$ ssh -i ~/spark1-kp.pem root@<public IP>`
    - RSYNC the copied files to all the slaves of the cluster:
      - `$ ./spark-ec2/copy-dir /root/stage`
      - `$ ./spark-ec2/copy-dir /root/spark/conf`
    - Start the spark cluster (from the master node)
      - `$ ./spark/sbin/start-all.sh `
      - Check the cluster status at: http://<spark master public IP>:8080/ (spark master public DNS)
      - Get the spark master URL from cluster status page
      - Log files are in worker nodes in `/root/spark/work` dir based on bach job ID
    - Stop the spark cluster (from the master node)
      - `$ ./spark/sbin/stop-all.sh` 

    - SSH to the cassandra master node: 
      - `$ ssh -i cassandra1-kp.pem ubuntu@<cassandra private dns>`
    - Execute nodetool and cqlsh shell command to load the database schema
      - `ubuntu@ip-10-169-190-121:~$ cqlsh`
        - `cqlsh> source 'data/bettercare4me.cql';`
      - `ubuntu@ip-10-169-190-121:~$ nodetool status`
      - are both available directly from the prompt of the ssh shell on the EC2 instance!
      - Exit from the cassandra master

    - Running the application (from the play directory on the play instance):
      - `$ export BC4ME_DATA_DIR="/home/ubuntu/bettercare4.me/play/data"`
      - `$ export BC4ME_SPARK_CONFIG="spark-prod.yaml"`
      - `$ export BC4ME_CASSANDRA_CONFIG="cassandra-prod.yaml"`
      - `play$ sudo -E ./target/universal/stage/bin/bettercare4-me -Dhttp.port=80`
      - see available option: `target/universal/stage/bin/bettercare4-me -h`

- Create a spark cluster on AWS
-
  - Created a keypair for spark cluster: `spark1-kp.pem` 
  - Created a IAM user michel1 in group bc4me as power user (added user credential in `.bashrc`)
  - Downloaded Spark 1.2.0 in projects/spark to have ec2 deployment scripts (deployment script in bettercare4.me/ec2)
  - in downloaded spark ec2 scripts directory /home/michel/projects/spark/spark-1.2.0/ec2: 
    - `$ ./spark-ec2 --help`
    - `$ ./spark-ec2 -k spark1-kp -i ~/spark1-kp.pem -r us-east-1 -z us-east-1d -t m1.large -v 1.2.0 -s 2 --worker-instances=2 launch bc4me-spark-cluster`
      - Spark AMI: ami-5bb18832
    - `$ ./spark-ec2 -k spark1-kp -i ~/spark1-kp.pem login bc4me-spark-cluster`
    - Master status page: `http://<public IP>:8080/` 
    - Master at `spark://ip-54-146-63-114:7077` (to use in SparkContext.Master() via the `spark-prod.yaml`) 

- Created Cassandra instance on EC2 using Datastax Community  Edition (3 instance of type m3.large)
-
  - Create a keypair for Cassandra cluster: `cassandra1-kp.pem`
  - Datastax AMI: ami-ada2b6c4 - look for community AMI and search for Datastax. Select the HVM AMI.
    - Instance type m3.large
    - 3 instances
    - Availability zone: us-east-1d
    - Instance Advance Details: `--clustername bettercare4meCluster --totalnodes 3 --version community`
    - Security group: cassandra1
    - SSH key pair: cassandra1-kp
  - Connect to Opscenter: `http://<public IP>:8888/` using the AMI Launch Index 0 instance Public DNS
  - Connect to the instance using: 
    - `$ ssh -i cassandra1-kp.pem ubuntu@<public IP>`

- Made presentation improvements and fix bug
-
  - Sorted the criteria result reasons (claim summary) on Patient Scorecard page according to claim date-of-service
  - Added cheveron on the right of measure link on Patient Scorecard page
  - Fix the provider name in claim list on Patient Scorecard page - first and last were inverted


# Sprint 8: Parallelizing Batch Claim Generation and Simulated HEDIS Report using Spark and Cassandra

- Version: 0.9.0.00 (v0.9.0.00_01-04-2014)
- Start Date: 11/29/2014
- Target Date: 12/27/2014
- Actual Date: 01/04/2015

## Product Features:
- Paralleling claim generation and HEDIS report using Spark and Cassandra
- Pesisting patients, providers, claims and patient HEDIS scorecard using Cassandra

## User Stories Sprint Backlog.

## Completed User Stories
- Build tag is bettercare4.me-v0.9.0.00_01-04-2015
- Add link from patient scorecard page back to rule scorecard page
- Load claim generator configuration to sort the hedis measures on patient scorecard page
  - Removed name (run name) as primary key for hedis_summary table. Has no consquence since all
    descendant tables key only with hedis_date and not the rune name.
- Carry over the provider name to patient scorecard page
  - Added patient name and provider name to Claim model class
- Loading patient gaps-in-care / patient profile (hedis measure summary) 
  from a HEDIS measure summary page / patient list (using Play only, no Spark or Akka)
- Change all insert methods of Bettercare4me Cassandra access object to return Future[Unit]
  rather than ResultSetFuture and make sure all futures completes before spark is closed.
- Added pagination to ruleScorecard.scala.html view and associated rule_scorecard table. 
  Pagination using pre-defined page range. 
  - Solution: create a rule_scorecard_paginated table:
    - Add patient_name collumn for patient full name as a clustering collumn to rule_scorecard. 
      The primary key is now: PRIMARY KEY (rule_name, hedis_date, patient_name, patient_id)
    - Once the rule_scorecard populated, load the rule_scorecard_paginated table with primary key:
      PRIMARY KEY (rule_name, hedis_date, page, patient_name, patient_id) with only 20 records for each page.
    - Client-side code (views) would setup pages of multiple of 20 records.
  - Potential outstanding issue:
    - Updating rule_scorecard table may require to rebuid rule_scorecard_paginated, since rule_scorecard_paginated
      is essentially a view of rule_scorecard table (not an issue here since patient list in rule_scorecard is fixed for
      a reporting period)
- Added HEDIS measure information to Rule Information page, added rules_infromation table
- Loading list of patients for a HEDIS measure from the dashboard page (using Play only, no Spark or Akka)
- Loading HEDIS dashboard from Cassandra hedis_summary table (using Play ony, no Spark or Akka)
- Persisting Patient HEDIS scorecard in Cassandra
- Persisting Rule and Patient HEDIS scorecard summary in Cassandra
- Persisting HEDIS summary in Cassandra
- Upgraded to Spark 1.2.0, no need for custom fork anymore since Spark 1.2 uses Akka 2.3.4!! Yay!
- Persisting patients, providers, claims in Cassandra
- Parallelize HEDIS report generation using Spark and Cassandra
- Parallelize claim, patient, and provider generation using Spark and Cassandra
- Added simple /cassanda route to test reading the database
- Added Cassandra data access layer to access database for bettercare4me keyspace
- added ./data/cassandra.yaml configuration file for database configuration parameters
- Adding Cassandra to project
- Creating Cassandra schema and local database
- Parallelizing patients, providers and claims generation using Spark


# Sprint 7: Simulated HEDIS Report using Batch Claim Generation

- Version: 0.8.0.00 (v0.8.0.00_11-27-2014)
- Start Date: 11/06/2014
- Target Date: 11/16/2014
- Actual Date: 11/27/2014

## Product Features:
- Provide an user interface to kicking off the batch jobs
- Batch generation of claims on local file system using Akka agent
- Generation of HEDIS 2014 Summary Report using Akka agent

## User Stories Sprint Backlog.

## Completed User Stories
- Setup claim generator configuration (yaml configuration) to match the NCQA The State of Health Care Quality 2014 Report for Commercial HMO (http://www.ncqa.org/Portals/0/Newsroom/2014/SOHC-web.pdf)
- Simple web application to kick off the the batch jobs
- Refactor YAML ClaimGeneratorConfig object to read simple java collections and basic data type to prevent security vulnerability
- Refactor all test classes to be organized using a TestSuite class in each package with a MainTestSuite, use this command in activator:
	[bettercare4.me] $ test-only com.nickelsoftware.bettercare4me.MainTestSuite
- Simple user interface using jQuery Mobile generated from Play Twirl, basic index page
- User interface for kicking off the claim generator
- User interface for kicking off the HEDIS report generation
- User interface using jQuery Mobile to present the report


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

