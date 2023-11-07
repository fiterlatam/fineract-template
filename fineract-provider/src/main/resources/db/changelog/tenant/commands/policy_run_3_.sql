--
-- Licensed to the Apache Software Foundation (ASF) under one
-- or more contributor license agreements. See the NOTICE file
-- distributed with this work for additional information
-- regarding copyright ownership. The ASF licenses this file
-- to you under the Apache License, Version 2.0 (the
-- "License"); you may not use this file except in compliance
-- with the License. You may obtain a copy of the License at
--
-- http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing,
-- software distributed under the License is distributed on an
-- "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
-- KIND, either express or implied. See the License for the
-- specific language governing permissions and limitations
-- under the License.
--

-- liquibase formatted sql
-- changeset fineract:1
-- MySQL dump 10.13  Distrib 5.1.60, for Win32 (ia32)
--
-- Host: localhost    Database: fineract_default
-- ------------------------------------------------------
-- Server version	5.1.60-community

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES UTF8MB4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

-- Client Age Hard Policy Check --
INSERT INTO stretchy_report (report_name,report_type,report_category,report_sql,description,core_report,use_report,self_service_user_report)
VALUES ("Increase percentage Policy Check", "Table", "Prequalification",
"SELECT
 CASE
     WHEN (${loanProductId} = 2) AND (IFNULL(previous_loans.percentage_increase, 0) = 200)  THEN 'GREEN'
     WHEN (${loanProductId} = 2) AND (IFNULL(previous_loans.percentage_increase, 0) BETWEEN 201 AND 500) THEN 'YELLOW'
     WHEN (${loanProductId} = 2) AND (IFNULL(previous_loans.percentage_increase, 0) > 500) THEN 'RED'
     WHEN (${loanProductId} = 9) AND (IFNULL(previous_loans.percentage_increase, 0) = 200) THEN 'GREEN'
     WHEN (${loanProductId} = 9) AND (IFNULL(previous_loans.percentage_increase, 0) BETWEEN 201 AND 500) THEN 'YELLOW'
     WHEN (${loanProductId} = 9) AND (IFNULL(previous_loans.percentage_increase, 0) > 500) THEN 'RED'
     WHEN (${loanProductId} = 8) AND (IFNULL(previous_loans.percentage_increase, 0) = 200) THEN 'GREEN'
     WHEN (${loanProductId} = 8) AND (IFNULL(previous_loans.percentage_increase, 0) BETWEEN 201 AND 500) THEN 'YELLOW'
     WHEN (${loanProductId} = 8) AND (IFNULL(previous_loans.percentage_increase, 0) > 500) THEN 'RED'
     WHEN (${loanProductId} = 4) AND (IFNULL(previous_loans.percentage_increase, 0) <= 60) THEN 'GREEN'
     WHEN (${loanProductId} = 4) AND (IFNULL(previous_loans.percentage_increase, 0) > 60) THEN 'ORANGE'
     WHEN (${loanProductId} = 5) AND (IFNULL(previous_loans.percentage_increase, 0) <= 60) THEN 'GREEN'
     WHEN (${loanProductId} = 5) AND (IFNULL(previous_loans.percentage_increase, 0) > 60) THEN 'ORANGE'
 END AS color,
 IFNULL(previous_loans.percentage_increase, 0) AS percentage_increase,
 IFNULL(previous_loans.principal_disbursed_derived, 0) AS principal_disbursed_derived
 FROM m_client mc
 LEFT JOIN (
     SELECT (100 *(${requestedAmount} - mln.principal_disbursed_derived) / mln.principal_disbursed_derived) AS percentage_increase, COALESCE(mln.principal_disbursed_derived, 0) AS principal_disbursed_derived, mln.client_id AS client_id, MAX(mln.disbursedon_date) AS disbursedon_date FROM m_loan mln WHERE mln.loan_status_id IN (700, 600, 601, 602) GROUP BY mln.id ORDER BY mln.disbursedon_date DESC
 ) previous_loans ON previous_loans.client_id = mc.id
 WHERE mc.id = ${clientId}
 GROUP BY mc.id", "Increase percentage Policy Check", 0, 0, 0);

INSERT INTO stretchy_report_parameter
(report_id, parameter_id, report_parameter_name)
VALUES((SELECT sr.id FROM stretchy_report sr WHERE sr.report_name = "Increase percentage Policy Check"), (SELECT p.id FROM stretchy_parameter p WHERE parameter_name = "clientIdSelectAll"), "clientId");

INSERT INTO stretchy_report_parameter
(report_id, parameter_id, report_parameter_name)
VALUES((SELECT sr.id FROM stretchy_report sr WHERE sr.report_name = "Increase percentage Policy Check"), (SELECT p.id FROM stretchy_parameter p WHERE parameter_name = "loanProductIdSelectAll"), "loanProductId");

INSERT INTO stretchy_report_parameter
(report_id, parameter_id, report_parameter_name)
VALUES((SELECT sr.id FROM stretchy_report sr WHERE sr.report_name = "Increase percentage Policy Check"), (SELECT p.id FROM stretchy_parameter p WHERE parameter_name = "OfficeIdSelectOne"), "officeId");


INSERT INTO stretchy_report_parameter
(report_id, parameter_id, report_parameter_name)
VALUES((SELECT sr.id FROM stretchy_report sr WHERE sr.report_name = "Increase percentage Policy Check"), (SELECT p.id FROM stretchy_parameter p WHERE parameter_name = "currencyIdSelectAll"), "currencyId");
