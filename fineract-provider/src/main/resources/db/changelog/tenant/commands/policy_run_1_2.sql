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
VALUES ("Client Categorization Policy Check", "Table", "Prequalification",
"SELECT CASE
      -- Banco Comunal normal
      WHEN (${loanProductId} = 2) AND (IFNULL(client_cycles.number_of_loan_cycles, 0) > 0) THEN 'RECURRING'
      WHEN (${loanProductId} = 2) AND (IFNULL(client_cycles.number_of_loan_cycles, 0) = 0) THEN 'NEW'
      WHEN (${loanProductId} = 2) AND (IFNULL(client_years.rejoining_years, 0) >= 1 OR IFNULL(client_closed_loans.number_of_closed_loans, 0) <= 2) THEN 'NEW'

      -- Banco Comunal temporal
      WHEN (${loanProductId} = 9) AND (IFNULL(client_cycles.number_of_loan_cycles, 0) > 0) THEN 'RECURRING'
      WHEN (${loanProductId} = 9) AND (IFNULL(client_cycles.number_of_loan_cycles, 0) = 0) THEN 'NEW'
      WHEN (${loanProductId} = 9) AND (IFNULL(client_years.rejoining_years, 0) >= 1) THEN 'NEW'
      WHEN (${loanProductId} = 9) AND (IFNULL(client_closed_loans.number_of_closed_loans, 0) > 0) THEN 'NEW'

      -- Banco Comunal Agricola
      WHEN (${loanProductId} = 8) AND (IFNULL(client_cycles.number_of_loan_cycles, 0) > 0) THEN 'RECURRING'
      WHEN (${loanProductId} = 8) AND (IFNULL(client_cycles.number_of_loan_cycles, 0) = 0) THEN 'NEW'
      WHEN (${loanProductId} = 8) AND (IFNULL(client_years.rejoining_years, 0) >= 1) THEN 'NEW'
      WHEN (${loanProductId} = 8) AND (IFNULL(client_closed_loans.number_of_closed_loans, 0) > 0) THEN 'NEW'

      -- Grupo Solidario
      WHEN (${loanProductId} = 4) AND (IFNULL(client_closed_loans.number_of_closed_loans, 0) >= 3) THEN 'RECURRING'
      WHEN (${loanProductId} = 4) AND (IFNULL(client_cycles.number_of_loan_cycles, 0) = 0) THEN 'NEW'
      WHEN (${loanProductId} = 4) AND (IFNULL(client_years.rejoining_years, 0) >= 1) THEN 'NEW'
      WHEN (${loanProductId} = 4) AND (IFNULL(client_closed_loans.number_of_closed_loans, 0) > 0) THEN 'NEW'

      -- Grupo Solidario Agricola
      WHEN (${loanProductId} = 5) AND (IFNULL(client_closed_loans.number_of_closed_loans, 0) >= 3) THEN 'RECURRING'
      WHEN (${loanProductId} = 5) AND (IFNULL(client_cycles.number_of_loan_cycles, 0) = 0) THEN 'NEW'
      WHEN (${loanProductId} = 5) AND (IFNULL(client_years.rejoining_years, 0) >= 1) THEN 'NEW'
      WHEN (${loanProductId} = 5) AND (IFNULL(client_closed_loans.number_of_closed_loans, 0) > 0) THEN 'NEW'

      -- PARALELO
      WHEN (${loanProductId} = 7) AND (IFNULL(client_closed_loans.number_of_closed_loans, 0) >= 3) THEN 'RECURRING'
      WHEN (${loanProductId} = 7) AND (IFNULL(client_closed_loans.number_of_closed_loans, 0) = 2) THEN 'RECURRING'
      WHEN (${loanProductId} = 7) AND (IFNULL(client_closed_loans.number_of_closed_loans, 0) < 2) THEN 'RECURRING'

      -- CHANIM-CHANIM
      WHEN (${loanProductId} = 3) AND (IFNULL(client_closed_loans.number_of_closed_loans, 0) >= 1) THEN 'RECURRING'
      WHEN (${loanProductId} = 3) AND (IFNULL(client_closed_loans.number_of_closed_loans, 0) = 0) THEN 'RECURRING'

      ELSE 'NEW'
 END AS clientCategorization,
 'GREEN' AS color,
 client_years.submittedon_years,
 client_years.rejoining_years,
 client_cycles.number_of_loan_cycles,
 client_closed_loans.number_of_closed_loans
 FROM m_client mc
 LEFT JOIN (
     SELECT mcl.id AS client_id,
     TIMESTAMPDIFF(YEAR, mcl.submittedon_date, CURDATE()) AS submittedon_years,
     IFNULL(TIMESTAMPDIFF(YEAR, mcl.submittedon_date, mcl.reactivated_on_date), 0) AS rejoining_years
     FROM  m_client mcl
 )client_years ON client_years.client_id = mc.id
 LEFT JOIN (
     SELECT COALESCE(MAX(mcl.loan_counter), 0) AS number_of_loan_cycles , mcl.client_id AS client_id FROM m_loan mcl GROUP BY mcl.client_id
 )client_cycles ON client_cycles.client_id = mc.id
 LEFT JOIN (
     SELECT COALESCE(COUNT(*), 0) AS number_of_closed_loans, mln.client_id AS client_id FROM m_loan mln WHERE mln.loan_status_id IN (700, 600, 602, 601) GROUP BY mln.client_id
 ) client_closed_loans ON client_closed_loans.client_id = mc.id
 WHERE mc.id = ${clientId}
 GROUP BY mc.id
", "Client Categorization Policy Check", 0, 0, 0);

INSERT INTO stretchy_report_parameter
(report_id, parameter_id, report_parameter_name)
VALUES((SELECT sr.id FROM stretchy_report sr WHERE sr.report_name = "Client Categorization Policy Check"), (SELECT p.id FROM stretchy_parameter p WHERE parameter_name = "clientIdSelectAll"), "clientId");

INSERT INTO stretchy_report_parameter
(report_id, parameter_id, report_parameter_name)
VALUES((SELECT sr.id FROM stretchy_report sr WHERE sr.report_name = "Client Categorization Policy Check"), (SELECT p.id FROM stretchy_parameter p WHERE parameter_name = "loanProductIdSelectAll"), "loanProductId");

INSERT INTO stretchy_report_parameter
(report_id, parameter_id, report_parameter_name)
VALUES((SELECT sr.id FROM stretchy_report sr WHERE sr.report_name = "Client Categorization Policy Check"), (SELECT p.id FROM stretchy_parameter p WHERE parameter_name = "OfficeIdSelectOne"), "officeId");


INSERT INTO stretchy_report_parameter
(report_id, parameter_id, report_parameter_name)
VALUES((SELECT sr.id FROM stretchy_report sr WHERE sr.report_name = "Client Categorization Policy Check"), (SELECT p.id FROM stretchy_parameter p WHERE parameter_name = "currencyIdSelectAll"), "currencyId");
