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

INSERT INTO pae_required_documents (`category_id`, `document_name`, `description`, `accepted_format`, `created_by`, `created_on`, `required`) VALUES ( (select id from m_code_value where code_value='Vehiculo' limit 1), 'Carta de consentimiento (a nombre de terceros)', 'Carta de consentimiento (a nombre de terceros)', 'PDF/IMAGE', 5, '2026-04-23 17:10:32', b'0');
INSERT INTO pae_required_documents (`category_id`, `document_name`, `description`, `accepted_format`, `created_by`, `created_on`, `required`) VALUES ( (select id from m_code_value where code_value='Vehiculo' limit 1), 'Avalúo por empresa valuadora', 'Avalúo por empresa valuadora', 'PDF', 5, '2026-04-23 17:10:21', b'1');
INSERT INTO pae_required_documents (`category_id`, `document_name`, `description`, `accepted_format`, `created_by`, `created_on`, `required`) VALUES ( (select id from m_code_value where code_value='Vehiculo' limit 1), 'Fotografias de la garantia debe figurar la solicitante, con coordenadas)', 'Fotografias de la garantia debe figurar la solicitante, con coordenadas)', 'IMAGE', 5, '2026-04-23 17:10:07', b'1');
INSERT INTO pae_required_documents (`category_id`, `document_name`, `description`, `accepted_format`, `created_by`, `created_on`, `required`) VALUES ( (select id from m_code_value where code_value='Vehiculo' limit 1), 'Documento que compruebe pago de impuesto de circulación del año', 'Documento que compruebe pago de impuesto de circulación del año', 'PDF', 5, '2026-04-23 17:09:53', b'0');
INSERT INTO pae_required_documents (`category_id`, `document_name`, `description`, `accepted_format`, `created_by`, `created_on`, `required`) VALUES ( (select id from m_code_value where code_value='Vehiculo' limit 1), 'Tarjeta de circulación', 'Tarjeta de circulación', 'PDF/IMAGE', 5, '2026-04-23 17:09:45', b'1');
INSERT INTO pae_required_documents (`category_id`, `document_name`, `description`, `accepted_format`, `created_by`, `created_on`, `required`) VALUES ( (select id from m_code_value where code_value='Vehiculo' limit 1), 'Título de propiedad del vehículo o documento que ampare propiedad del bien mueble', 'Título de propiedad del vehículo o documento que ampare propiedad del bien mueble', 'PDF', 5, '2026-04-23 17:09:32', b'0');
INSERT INTO pae_required_documents (`category_id`, `document_name`, `description`, `accepted_format`, `created_by`, `created_on`, `required`) VALUES ( (select id from m_code_value where code_value='Vehiculo' limit 1), 'Constancia de residencia del propietario de la garantía', 'Constancia de residencia del propietario de la garantía', 'PDF/IMAGE', 5, '2026-04-23 17:09:00', b'1');
INSERT INTO pae_required_documents (`category_id`, `document_name`, `description`, `accepted_format`, `created_by`, `created_on`, `required`) VALUES ( (select id from m_code_value where code_value='Vehiculo' limit 1), 'DPI de propietario de la garantía', 'DPI de propietario de la garantía', 'PDF/IMAGE', 5, '2026-04-23 17:08:40', b'1');
