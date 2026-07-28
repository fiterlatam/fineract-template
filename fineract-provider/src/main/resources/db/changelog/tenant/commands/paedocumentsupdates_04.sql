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

INSERT INTO pae_required_documents (`category_id`, `document_name`, `description`, `accepted_format`, `required`) VALUES ( (select id from m_code_value where code_id = (select id from m_code where code_name='PaeRequiredGuarantees' limit 1) and code_value='Hipoteca' limit 1), 'DPI de propietario de la garantía (a nombre de terceros)', 'DPI de propietario de la garantía (a nombre de terceros)', 'PDF/IMAGE', b'0');
INSERT INTO pae_required_documents (`category_id`, `document_name`, `description`, `accepted_format`, `required`) VALUES ( (select id from m_code_value where code_id = (select id from m_code where code_name='PaeRequiredGuarantees' limit 1) and code_value='Hipoteca' limit 1), 'Constancia de residencia del propietario de la garantía (a nombre de terceros)', 'Constancia de residencia del propietario de la garantía (a nombre de terceros)', 'PDF/IMAGE', b'0');
INSERT INTO pae_required_documents (`category_id`, `document_name`, `description`, `accepted_format`, `required`) VALUES ( (select id from m_code_value where code_id = (select id from m_code where code_name='PaeRequiredGuarantees' limit 1) and code_value='Hipoteca' limit 1), 'Visto bueno de Coordinadora Legal (si es la misma garantía agregar correo de VoBo)', 'Visto bueno de Coordinadora Legal (si es la misma garantía agregar correo de VoBo)', 'PDF', b'1');
INSERT INTO pae_required_documents (`category_id`, `document_name`, `description`, `accepted_format`, `required`) VALUES ( (select id from m_code_value where code_id = (select id from m_code where code_name='PaeRequiredGuarantees' limit 1) and code_value='Hipoteca' limit 1), 'Primer testimonio inscrito en el Registro Nacional de la propiedad ', 'Primer testimonio inscrito en el Registro Nacional de la propiedad ', 'PDF', b'1');
INSERT INTO pae_required_documents (`category_id`, `document_name`, `description`, `accepted_format`, `required`) VALUES ( (select id from m_code_value where code_id = (select id from m_code where code_name='PaeRequiredGuarantees' limit 1) and code_value='Hipoteca' limit 1), 'Certificación del Registro de la Propiedad (sin gravámenes)', 'Certificación del Registro de la Propiedad (sin gravámenes)', 'PDF', b'1');
INSERT INTO pae_required_documents (`category_id`, `document_name`, `description`, `accepted_format`, `required`) VALUES ( (select id from m_code_value where code_id = (select id from m_code where code_name='PaeRequiredGuarantees' limit 1) and code_value='Hipoteca' limit 1), 'Fotografias de la garantia y debe figurar la solicitante, con coordenadas)', 'Fotografias de la garantia y debe figurar la solicitante, con coordenadas)', 'PDF/IMAGE', b'1');
INSERT INTO pae_required_documents (`category_id`, `document_name`, `description`, `accepted_format`, `required`) VALUES ( (select id from m_code_value where code_id = (select id from m_code where code_name='PaeRequiredGuarantees' limit 1) and code_value='Hipoteca' limit 1), 'Avalúo por empresa valuadora ', 'Avalúo por empresa valuadora ', 'IMAGE', b'1');
INSERT INTO pae_required_documents (`category_id`, `document_name`, `description`, `accepted_format`, `required`) VALUES ( (select id from m_code_value where code_id = (select id from m_code where code_name='PaeRequiredGuarantees' limit 1) and code_value='Hipoteca' limit 1), 'Carta de consentimiento (a nombre de terceros)', 'Carta de consentimiento (a nombre de terceros)', 'PDF/IMAGE', b'0');
