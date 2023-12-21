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

-- Pagare Libre Protesto Reports --
-- Delete previous reports
DELETE FROM stretchy_report_parameter WHERE report_id = (SELECT id FROM stretchy_report WHERE report_name = 'Pagare Libre Protesto Adicional');
DELETE FROM stretchy_report_parameter WHERE report_id = (SELECT id FROM stretchy_report WHERE report_name = 'Pagare Libre Protesto Grupal');
DELETE FROM stretchy_report_parameter WHERE report_id = (SELECT id FROM stretchy_report WHERE report_name = 'Pagare Libre Protesto Individual');

DELETE FROM stretchy_report WHERE report_name = 'Pagare Libre Protesto Adicional';
DELETE FROM stretchy_report WHERE report_name = 'Pagare Libre Protesto Grupal';
DELETE FROM stretchy_report WHERE report_name = 'Pagare Libre Protesto Individual';

DELETE FROM stretchy_parameter WHERE parameter_name = 'groupIdSelectOne';
DELETE FROM stretchy_parameter WHERE parameter_name = 'selectmember';
DELETE FROM stretchy_parameter WHERE parameter_name = 'AgencyIdSelectOne';


-- Insert pentaho reports
INSERT IGNORE INTO stretchy_report(report_name, report_type, report_subtype, report_category, report_sql, description, core_report, use_report, self_service_user_report) VALUES('Pagare Libre Protesto Adicional', 'Pentaho', NULL, 'Loan', NULL, 'Pagare Libre Protesto Adicional', 0, 1, 0);
INSERT IGNORE INTO stretchy_report(report_name, report_type, report_subtype, report_category, report_sql, description, core_report, use_report, self_service_user_report) VALUES('Pagare Libre Protesto Grupal', 'Pentaho', NULL, 'Loan', NULL, 'Pagare Libre Protesto Grupal', 0, 1, 0);
INSERT IGNORE INTO stretchy_report(report_name, report_type, report_subtype, report_category, report_sql, description, core_report, use_report, self_service_user_report) VALUES('Pagare Libre Protesto Individual', 'Pentaho', NULL, 'Loan', NULL, 'Pagare Libre Protesto Individual', 0, 1, 0);


-- Insert parameters
INSERT IGNORE INTO stretchy_parameter (parameter_name, parameter_variable, parameter_label, parameter_displayType, parameter_FormatType, parameter_default, special, selectOne, selectAll, parameter_sql, parent_id) VALUES('AgencyIdSelectOne', 'agencyId', 'Agency Name', 'select', 'number', '0', NULL, 'Y', NULL, "select  distinct id,name as Agency  from m_agency ma", NULL);

INSERT IGNORE INTO stretchy_parameter(parameter_name, parameter_variable, parameter_label, parameter_displayType, parameter_FormatType, parameter_default, special, selectOne, selectAll, parameter_sql, parent_id)
VALUES('selectmember', 'memberId', 'Member', 'select', 'number', '0', NULL, 'Y', NULL, "SELECT distinct mc.id as client_id, mc.display_name  FROM m_loan ml
INNER JOIN m_client mc ON mc.id = ml.client_id
LEFT JOIN m_group_client mgc ON mgc.client_id = mc.id
LEFT JOIN m_group mg ON mg.id = mgc.group_id
LEFT JOIN m_group center ON center.id = mg.parent_id
LEFT JOIN m_portfolio mp ON mp.id = center.portfolio_id
LEFT JOIN m_supervision ms ON ms.id = mp.supervision_id
LEFT JOIN m_agency ma ON ma.id = ms.agency_id
WHERE ma.id = ${agencyId}", (SELECT params.id FROM (SELECT id FROM stretchy_parameter WHERE parameter_name = 'AgencyIdSelectOne') AS params));

INSERT IGNORE INTO stretchy_parameter
(id, parameter_name, parameter_variable, parameter_label, parameter_displayType, parameter_FormatType, parameter_default, special, selectOne, selectAll, parameter_sql, parent_id)
VALUES(1036, 'groupIdSelectOne', 'grpId', 'Group Name', 'select', 'number', '0', NULL, 'Y', NULL, "SELECT distinct mg.id as grp_id, mg.display_name  FROM m_loan ml
INNER JOIN m_client mc ON mc.id = ml.client_id
LEFT JOIN m_group_client mgc ON mgc.client_id = mc.id
LEFT JOIN m_group mg ON mg.id = mgc.group_id
LEFT JOIN m_group center ON center.id = mg.parent_id
LEFT JOIN m_portfolio mp ON mp.id = center.portfolio_id
LEFT JOIN m_supervision ms ON ms.id = mp.supervision_id
LEFT JOIN m_agency ma ON ma.id = ms.agency_id
WHERE ma.id = ${agencyId}", (SELECT params.id FROM (SELECT id FROM stretchy_parameter WHERE parameter_name = 'AgencyIdSelectOne') AS params));

-- Insert parameter links
INSERT IGNORE INTO stretchy_report_parameter(report_id, parameter_id, report_parameter_name) VALUES((SELECT sr.id FROM stretchy_report sr WHERE sr.report_name = "Pagare Libre Protesto Adicional"), (SELECT p.id FROM stretchy_parameter p WHERE parameter_name = "asOnDate"), "disbursmentDate");
INSERT IGNORE INTO stretchy_report_parameter(report_id, parameter_id, report_parameter_name) VALUES((SELECT sr.id FROM stretchy_report sr WHERE sr.report_name = "Pagare Libre Protesto Adicional"), (SELECT p.id FROM stretchy_parameter p WHERE parameter_name = "AgencyIdSelectOne"), "selected_agency");
INSERT IGNORE INTO stretchy_report_parameter(report_id, parameter_id, report_parameter_name) VALUES((SELECT sr.id FROM stretchy_report sr WHERE sr.report_name = "Pagare Libre Protesto Adicional"), (SELECT p.id FROM stretchy_parameter p WHERE parameter_name = "selectmember"), "selected_member");

INSERT IGNORE INTO stretchy_report_parameter(report_id, parameter_id, report_parameter_name) VALUES((SELECT sr.id FROM stretchy_report sr WHERE sr.report_name = "Pagare Libre Protesto Grupal"), (SELECT p.id FROM stretchy_parameter p WHERE parameter_name = "asOnDate"), "disbursmentDate");
INSERT IGNORE INTO stretchy_report_parameter(report_id, parameter_id, report_parameter_name) VALUES((SELECT sr.id FROM stretchy_report sr WHERE sr.report_name = "Pagare Libre Protesto Grupal"), (SELECT p.id FROM stretchy_parameter p WHERE parameter_name = "AgencyIdSelectOne"), "selected_agency");
INSERT IGNORE INTO stretchy_report_parameter(report_id, parameter_id, report_parameter_name) VALUES((SELECT sr.id FROM stretchy_report sr WHERE sr.report_name = "Pagare Libre Protesto Grupal"), (SELECT p.id FROM stretchy_parameter p WHERE parameter_name = "groupIdSelectOne"), "selected_group");

INSERT IGNORE INTO stretchy_report_parameter(report_id, parameter_id, report_parameter_name) VALUES((SELECT sr.id FROM stretchy_report sr WHERE sr.report_name = "Pagare Libre Protesto Individual"), (SELECT p.id FROM stretchy_parameter p WHERE parameter_name = "asOnDate"), "disbursmentDate");
INSERT IGNORE INTO stretchy_report_parameter(report_id, parameter_id, report_parameter_name) VALUES((SELECT sr.id FROM stretchy_report sr WHERE sr.report_name = "Pagare Libre Protesto Individual"), (SELECT p.id FROM stretchy_parameter p WHERE parameter_name = "AgencyIdSelectOne"), "selected_agency");
INSERT IGNORE INTO stretchy_report_parameter(report_id, parameter_id, report_parameter_name) VALUES((SELECT sr.id FROM stretchy_report sr WHERE sr.report_name = "Pagare Libre Protesto Individual"), (SELECT p.id FROM stretchy_parameter p WHERE parameter_name = "selectmember"), "selected_member");
