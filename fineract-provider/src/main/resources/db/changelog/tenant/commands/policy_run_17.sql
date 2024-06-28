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

-- Percentage of members of the same group who they can have parallel product --
INSERT INTO stretchy_report (report_name,report_type,report_category,report_sql,description,core_report,use_report,self_service_user_report)
VALUES ("Percentage of members of the same group who they can have parallel product Policy Check", "Table", "Prequalification",
"SELECT
 COALESCE(prequalification_details.percentage, 0) AS percentage,
 CASE
     WHEN (${loanProductId} = 7) AND ((COALESCE(prequalification_details.percentage, 0)) <= 50) THEN 'GREEN'
 END AS color
 FROM m_prequalification_group pg
 INNER JOIN (
     SELECT
     mpg.id AS prequalification_id, ((COUNT(DISTINCT details.client_id) * 100) / (${numberOfMembers})) AS percentage
     FROM m_prequalification_group mpg
     LEFT JOIN (
         SELECT mpg.id AS prequalification_id, mc.id AS client_id
         FROM m_prequalification_group mpg
         LEFT JOIN m_prequalification_group_members mpgm ON mpgm.group_id = mpg.id
         LEFT JOIN m_client mc ON mc.dpi = mpgm.dpi
         LEFT JOIN m_loan ml ON ml.client_id = mc.id
         WHERE ml.loan_status_id = 300 AND ml.product_id = ${loanProductId}
         GROUP BY ml.id, mpg.id
     ) details ON details.prequalification_id = mpg.id
     WHERE mpg.id = ${prequalificationId}
     GROUP BY mpg.id
 ) prequalification_details ON prequalification_details.prequalification_id = pg.id
 WHERE pg.id = ${prequalificationId}", "Percentage of members of the same group who they can have parallel product Policy Check", 0, 0, 0);
