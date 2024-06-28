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
VALUES ("Rental Age Policy Check", "Table", "Prequalification",
"SELECT
 client_details.years_of_residence,
  CASE
      WHEN (${loanProductId} = 6) AND ('${recreditCategorization}' = 'NUEVO') AND (client_details.years_of_residence >= 3)  THEN 'GREEN'
      WHEN (${loanProductId} = 6) AND ('${recreditCategorization}' = 'NUEVO') AND (client_details.years_of_residence  < 3)  THEN 'YELLOW'
      WHEN (${loanProductId} = 6) AND ('${recreditCategorization}' = 'RECREDITO') AND (client_details.years_of_residence >= 3)  THEN 'GREEN'
      WHEN (${loanProductId} = 6) AND ('${recreditCategorization}' = 'RECREDITO') AND (client_details.years_of_residence  < 3)  THEN 'YELLOW'
      WHEN (${loanProductId} = 6) AND (mc.status_enum = 300)  THEN 'GREEN'
  END AS color
  FROM m_client mc
  INNER JOIN (
      SELECT
      mct.id AS client_id,
      COALESCE(mcci.years_of_residence, 0) AS years_of_residence
      FROM m_client mct
      LEFT JOIN m_client_contact_info mcci ON mcci.client_id = mct.id
      LEFT JOIN m_code_value housing_type ON housing_type.id = mcci.housing_type
  ) client_details ON client_details.client_id = mc.id
  WHERE mc.id = ${clientId}", "Rental Age Policy Check", 0, 0, 0);
