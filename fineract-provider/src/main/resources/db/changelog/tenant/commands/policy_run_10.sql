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

-- Percentage of members with their own home --
INSERT INTO stretchy_report (report_name,report_type,report_category,report_sql,description,core_report,use_report,self_service_user_report)
VALUES ("Percentage of members with their own home Policy Check", "Table", "Prequalification",
"SELECT
   housing_type.owner_percent,
   CASE
       WHEN (${loanProductId} = 2) AND (housing_type.owner_percent <=50) THEN 'GREEN'
       WHEN (${loanProductId} = 2) AND (housing_type.owner_percent >50) THEN 'YELLOW'

       WHEN (${loanProductId} = 8) AND (housing_type.owner_percent <=50) THEN 'GREEN'
       WHEN (${loanProductId} = 8) AND (housing_type.owner_percent >50) THEN 'YELLOW'

   END AS color
   FROM m_prequalification_group mpg
   INNER JOIN ( select (((select count(*) from m_prequalification_group_members mpm INNER JOIN m_client mc on mc.dpi = mpm.dpi
    INNER JOIN m_client_contact_info mcinf on mcinf.client_id = mc.id INNER JOIN m_code_value mcv on mcv.id = mcinf.housing_type where mcv.code_value = 'Propia' and mpm.group_id = ${groupId})/
    (select count(*) from m_prequalification_group_members mpg where mpg.group_id = ${prequalificationId}))*100) as owner_percent, ${prequalificationId} as grp_id )
    housing_type ON housing_type.grp_id = mpg.id
   WHERE mpg.id = ${prequalificationId}", "Percentage of members with their own home Policy Check", 0, 0, 0);
