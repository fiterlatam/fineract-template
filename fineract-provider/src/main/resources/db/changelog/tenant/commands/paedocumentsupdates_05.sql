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

INSERT INTO pae_required_documents (`category_id`, `document_name`, `description`, `accepted_format`, `required`) VALUES ( (select id from m_code_value where code_id = (select id from m_code where code_name='PaeRequiredGuarantees' limit 1) and code_value='Fiador empresario' limit 1), 'Fotocopia completa de DPI ', 'Fotocopia completa de DPI ', 'PDF/IMAGE', b'0') ON DUPLICATE KEY UPDATE category_id = category_id, document_name=document_name;
INSERT INTO pae_required_documents (`category_id`, `document_name`, `description`, `accepted_format`, `required`) VALUES ( (select id from m_code_value where code_id = (select id from m_code where code_name='PaeRequiredGuarantees' limit 1) and code_value='Fiador empresario' limit 1), 'Recibo de servicios públicos máximo dos meses atrasados donde esté la dirección exacta', 'Recibo de servicios públicos máximo dos meses atrasados donde esté la dirección exacta', 'PDF/IMAGE', b'0') ON DUPLICATE KEY UPDATE category_id = category_id, document_name=document_name;
INSERT INTO pae_required_documents (`category_id`, `document_name`, `description`, `accepted_format`, `required`) VALUES ( (select id from m_code_value where code_id = (select id from m_code where code_name='PaeRequiredGuarantees' limit 1) and code_value='Fiador empresario' limit 1), 'En caso no tenga servicios, constancia de risidencia emitida por el COCODE', 'En caso no tenga servicios, constancia de risidencia emitida por el COCODE', 'PDF/IMAGE', b'0') ON DUPLICATE KEY UPDATE category_id = category_id, document_name=document_name;
INSERT INTO pae_required_documents (`category_id`, `document_name`, `description`, `accepted_format`, `required`) VALUES ( (select id from m_code_value where code_id = (select id from m_code where code_name='PaeRequiredGuarantees' limit 1) and code_value='Fiador empresario' limit 1), 'Fotografias externas de la vivienda (con coordenadas)', 'Fotografias externas de la vivienda (con coordenadas)', 'PDF/IMAGE', b'0') ON DUPLICATE KEY UPDATE category_id = category_id, document_name=document_name;
INSERT INTO pae_required_documents (`category_id`, `document_name`, `description`, `accepted_format`, `required`) VALUES ( (select id from m_code_value where code_id = (select id from m_code where code_name='PaeRequiredGuarantees' limit 1) and code_value='Fiador empresario' limit 1), 'Constancia laboral  firmada y sellada - 1 año de antigüedad laboral', 'Constancia laboral  firmada y sellada - 1 año de antigüedad laboral', 'PDF/IMAGE', b'0') ON DUPLICATE KEY UPDATE category_id = category_id, document_name=document_name;
INSERT INTO pae_required_documents (`category_id`, `document_name`, `description`, `accepted_format`, `required`) VALUES ( (select id from m_code_value where code_id = (select id from m_code where code_name='PaeRequiredGuarantees' limit 1) and code_value='Fiador empresario' limit 1), 'Estado de cuenta de los últimos tres meses', 'Estado de cuenta de los últimos tres meses', 'PDF/IMAGE', b'0') ON DUPLICATE KEY UPDATE category_id = category_id, document_name=document_name;
INSERT INTO pae_required_documents (`category_id`, `document_name`, `description`, `accepted_format`, `required`) VALUES ( (select id from m_code_value where code_id = (select id from m_code where code_name='PaeRequiredGuarantees' limit 1) and code_value='Fiador empresario' limit 1), 'Estado patrimonial', 'Estado patrimonial', 'PDF/IMAGE', b'0') ON DUPLICATE KEY UPDATE category_id = category_id, document_name=document_name;
