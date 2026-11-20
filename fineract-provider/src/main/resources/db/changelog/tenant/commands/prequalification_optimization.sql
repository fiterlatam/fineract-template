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

-- Liquibase format for Fineract
-- databaseChangeLog:
--   changeSet:
--     id: create-pae-tables
--     author: development
--     changes:

-- Create all required tables
-- =====================================================
-- Migration: Create PAE Loan Additional Data Table
-- =====================================================

-- CRITICAL INDEXES FOR m_prequalification_group
ALTER TABLE m_prequalification_group ADD INDEX idx_product_id (product_id);
ALTER TABLE m_prequalification_group ADD INDEX idx_created_at (created_at);
ALTER TABLE m_prequalification_group ADD INDEX idx_center_id (center_id);
ALTER TABLE m_prequalification_group ADD INDEX idx_group_id (group_id);
ALTER TABLE m_prequalification_group ADD INDEX idx_prequalification_type (prequalification_type_enum);
ALTER TABLE m_prequalification_group ADD INDEX idx_added_by (added_by);
ALTER TABLE m_prequalification_group ADD INDEX idx_previous_prequalification (previous_prequalification);
ALTER TABLE m_prequalification_group ADD INDEX idx_status_created (status, created_at);

-- CRITICAL INDEXES FOR m_prequalification_group_members (NO INDEXES CURRENTLY)
ALTER TABLE m_prequalification_group_members ADD INDEX idx_group_id (group_id);
ALTER TABLE m_prequalification_group_members ADD INDEX idx_dpi (dpi);
ALTER TABLE m_prequalification_group_members ADD INDEX idx_client_id (client_id);
ALTER TABLE m_prequalification_group_members ADD INDEX idx_status (status);
ALTER TABLE m_prequalification_group_members ADD INDEX idx_created_at (created_at);
ALTER TABLE m_prequalification_group_members ADD INDEX idx_buro_check_status (buro_check_status);
ALTER TABLE m_prequalification_group_members ADD INDEX idx_group_id_status (group_id, status);
ALTER TABLE m_prequalification_group_members ADD INDEX idx_dpi_status (dpi, status);
ALTER TABLE m_prequalification_group_members ADD INDEX idx_added_by (added_by);
