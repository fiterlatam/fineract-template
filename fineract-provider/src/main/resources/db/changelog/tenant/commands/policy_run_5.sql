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
VALUES ("Client age Policy Check", "Table", "Prequalification",
"SELECT mc.id,
  client_age.client_age AS client_age,
  mc.date_of_birth AS date_of_birth,
  CASE
     -- Banco Comunal normal
      WHEN (${loanProductId} = 2) AND ('${clientCategorization}' = 'NEW') AND (IFNULL(client_age.client_age, 0) BETWEEN 18 AND 70) THEN 'GREEN'
      WHEN (${loanProductId} = 2) AND ('${clientCategorization}' = 'NEW') AND (IFNULL(client_age.client_age, 0) BETWEEN 71 AND 75) THEN 'YELLOW'
      WHEN (${loanProductId} = 2) AND ('${clientCategorization}' = 'NEW') AND (IFNULL(client_age.client_age, 0) NOT BETWEEN 18 AND 75) THEN 'RED'
      WHEN (${loanProductId} = 2) AND ('${clientCategorization}' = 'RECURRING') AND (IFNULL(client_age.client_age, 0) BETWEEN 18 AND 75) THEN 'GREEN'
      WHEN (${loanProductId} = 2) AND ('${clientCategorization}' = 'RECURRING') AND (IFNULL(client_age.client_age, 0) NOT BETWEEN 18 AND 75) THEN 'RED'

      -- Banco Comunal temporal
      WHEN (${loanProductId} = 9) AND ('${clientCategorization}' = 'NEW') AND (IFNULL(client_age.client_age, 0) BETWEEN 18 AND 70) THEN 'GREEN'
      WHEN (${loanProductId} = 9) AND ('${clientCategorization}' = 'NEW') AND (IFNULL(client_age.client_age, 0) BETWEEN 71 AND 75) THEN 'YELLOW'
      WHEN (${loanProductId} = 9) AND ('${clientCategorization}' = 'NEW') AND (IFNULL(client_age.client_age, 0) NOT BETWEEN 18 AND 75) THEN 'RED'
      WHEN (${loanProductId} = 9) AND ('${clientCategorization}' = 'RECURRING') AND (IFNULL(client_age.client_age, 0) BETWEEN 18 AND 75) THEN 'GREEN'
      WHEN (${loanProductId} = 9) AND ('${clientCategorization}' = 'RECURRING') AND (IFNULL(client_age.client_age, 0) NOT BETWEEN 18 AND 75) THEN 'RED'


      -- Banco Comunal Agricola
      WHEN (${loanProductId} = 8) AND ('${clientCategorization}' = 'NEW') AND (IFNULL(client_age.client_age, 0) BETWEEN 18 AND 70) THEN 'GREEN'
      WHEN (${loanProductId} = 8) AND ('${clientCategorization}' = 'NEW') AND (IFNULL(client_age.client_age, 0) BETWEEN 71 AND 75) THEN 'YELLOW'
      WHEN (${loanProductId} = 8) AND ('${clientCategorization}' = 'NEW') AND (IFNULL(client_age.client_age, 0) NOT BETWEEN 18 AND 75) THEN 'RED'
      WHEN (${loanProductId} = 8) AND ('${clientCategorization}' = 'RECURRING') AND (IFNULL(client_age.client_age, 0) BETWEEN 18 AND 75) THEN 'GREEN'
      WHEN (${loanProductId} = 9) AND ('${clientCategorization}' = 'RECURRING') AND (IFNULL(client_age.client_age, 0) NOT BETWEEN 18 AND 75) THEN 'RED'


      -- Grupo Solidario
      WHEN (${loanProductId} = 4) AND ('${clientCategorization}' = 'NEW') AND (IFNULL(client_age.client_age, 0) BETWEEN 18 AND 70) THEN 'GREEN'
      WHEN (${loanProductId} = 4) AND ('${clientCategorization}' = 'NEW') AND (IFNULL(client_age.client_age, 0) BETWEEN 71 AND 75) THEN 'YELLOW'
      WHEN (${loanProductId} = 4) AND ('${clientCategorization}' = 'NEW') AND (IFNULL(client_age.client_age, 0) NOT BETWEEN 18 AND 75) THEN 'RED'
      WHEN (${loanProductId} = 4) AND ('${clientCategorization}' = 'RECURRING') AND (IFNULL(client_age.client_age, 0) BETWEEN 18 AND 75) THEN 'GREEN'
      WHEN (${loanProductId} = 4) AND ('${clientCategorization}' = 'RECURRING') AND (IFNULL(client_age.client_age, 0) NOT BETWEEN 18 AND 75) THEN 'RED'


      -- Grupo Solidario Agricola
      WHEN (${loanProductId} = 5) AND ('${clientCategorization}' = 'NEW') AND (IFNULL(client_age.client_age, 0) BETWEEN 18 AND 70) THEN 'GREEN'
      WHEN (${loanProductId} = 5) AND ('${clientCategorization}' = 'NEW') AND (IFNULL(client_age.client_age, 0) BETWEEN 71 AND 75) THEN 'YELLOW'
      WHEN (${loanProductId} = 5) AND ('${clientCategorization}' = 'NEW') AND (IFNULL(client_age.client_age, 0) NOT BETWEEN 18 AND 75) THEN 'RED'
      WHEN (${loanProductId} = 5) AND ('${clientCategorization}' = 'RECURRING') AND (IFNULL(client_age.client_age, 0) BETWEEN 18 AND 75) THEN 'GREEN'
      WHEN (${loanProductId} = 5) AND ('${clientCategorization}' = 'RECURRING') AND (IFNULL(client_age.client_age, 0) NOT BETWEEN 18 AND 75) THEN 'RED'

      ELSE 'GREEN'
  END AS color
  FROM m_client mc
  INNER JOIN (
      SELECT mct.id AS client_id, mct.date_of_birth AS date_of_birth, IFNULL(TIMESTAMPDIFF(YEAR, mct.date_of_birth, CURDATE()), 0) AS client_age
      FROM m_client mct
  ) client_age ON client_age.client_id = mc.id
  WHERE mc.id = ${clientId}", "Client Age Hard Policy Check", 0, 0, 0);

INSERT INTO stretchy_parameter
(parameter_name, parameter_variable, parameter_label, parameter_displayType, parameter_FormatType, parameter_default, special, selectOne, selectAll, parameter_sql, parent_id)
VALUES('clientCategorization', 'clientCategorization', 'Client Categorization', 'select', 'string', 'NEW', NULL, 'Y', NULL, 'SELECT ''NEW'' AS id, ''New'' AS name
UNION ALL
SELECT ''RECURRING'' AS id, ''Recurring'' AS name', NULL);

INSERT INTO stretchy_report_parameter
(report_id, parameter_id, report_parameter_name)
VALUES((SELECT sr.id FROM stretchy_report sr WHERE sr.report_name = "Client age Policy Check"), (SELECT p.id FROM stretchy_parameter p WHERE parameter_name = "clientCategorization"), "clientCategorization");

INSERT INTO stretchy_report_parameter
(report_id, parameter_id, report_parameter_name)
VALUES((SELECT sr.id FROM stretchy_report sr WHERE sr.report_name = "Client age Policy Check"), (SELECT p.id FROM stretchy_parameter p WHERE parameter_name = "clientIdSelectAll"), "clientId");

INSERT INTO stretchy_report_parameter
(report_id, parameter_id, report_parameter_name)
VALUES((SELECT sr.id FROM stretchy_report sr WHERE sr.report_name = "Client age Policy Check"), (SELECT p.id FROM stretchy_parameter p WHERE parameter_name = "loanProductIdSelectAll"), "loanProductId");

INSERT INTO stretchy_report_parameter
(report_id, parameter_id, report_parameter_name)
VALUES((SELECT sr.id FROM stretchy_report sr WHERE sr.report_name = "Client age Policy Check"), (SELECT p.id FROM stretchy_parameter p WHERE parameter_name = "OfficeIdSelectOne"), "officeId");


INSERT INTO stretchy_report_parameter
(report_id, parameter_id, report_parameter_name)
VALUES((SELECT sr.id FROM stretchy_report sr WHERE sr.report_name = "Client age Policy Check"), (SELECT p.id FROM stretchy_parameter p WHERE parameter_name = "currencyIdSelectAll"), "currencyId");
