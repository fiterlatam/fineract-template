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

CREATE TABLE IF NOT EXISTS m_pae_loan_additional_data (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    loan_id BIGINT NOT NULL,
    case_id VARCHAR(100) NOT NULL,
    verificacion_vivienda_id BIGINT,
    verificacion_negocio_id BIGINT,
    entrevista_cliente_id BIGINT,
    verificacion_del_fiador_id BIGINT,
    calificacion_del_supervisor_id BIGINT,
    created_on DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    lastmodified_on DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_loan_id (loan_id),
    INDEX idx_case_id (case_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- Migration: Create Verificacion Vivienda Table
-- =====================================================
-- changeset id:002-create-verificacion-vivienda author:development
CREATE TABLE IF NOT EXISTS m_pae_verificacion_vivienda (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    loan_additional_pae_id BIGINT NOT NULL,
    fecha_supervision DATE,
    vivienda_propia VARCHAR(50),
    es_guatemalteca VARCHAR(50),
    rango_edad_20_60 VARCHAR(50),
    recibo_servicios_con_direccion_exacta VARCHAR(50),
    recibo_servicios_propio VARCHAR(50),
    cuenta_con_servicios_basicos VARCHAR(50),
    direccion_coincide_con_expediente VARCHAR(50),
    ubicacion_vivienda VARCHAR(255),
    created_on DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    lastmodified_on DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_loan_pae (loan_additional_pae_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- Migration: Create Verificacion Negocio Table
-- =====================================================
-- changeset id:003-create-verificacion-negocio author:development
CREATE TABLE IF NOT EXISTS m_pae_verificacion_negocio (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    loan_additional_pae_id BIGINT NOT NULL,
    negocio_propio_y_manejado_por_cliente VARCHAR(50),
    antiguedad_mayor_a_3_anios VARCHAR(50),
    fotocopia_tarjeta_de_salud VARCHAR(50),
    boleta_o_tarjeta_derecho_de_piso VARCHAR(50),
    fotocopia_facturas_compra_venta VARCHAR(50),
    copia_rtu VARCHAR(50),
    fotografias_coinciden_con_expediente VARCHAR(50),
    valor_ventas_compras_coinciden_con_expediente VARCHAR(50),
    negocio_ordenado_y_limpio VARCHAR(50),
    negocio_concurrido VARCHAR(50),
    negocio_elegible_segun_politica VARCHAR(50),
    pago_de_prestamos_coinciden_con_expediente VARCHAR(50),
    ubicacion_negocio VARCHAR(255),
    nombre_negocio VARCHAR(255),
    descripcion_negocio VARCHAR(500),
    created_on DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    lastmodified_on DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_loan_pae (loan_additional_pae_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- Migration: Create Entrevista Cliente Table
-- =====================================================
-- changeset id:004-create-entrevista-cliente author:development
CREATE TABLE IF NOT EXISTS m_pae_entrevista_cliente (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    loan_additional_pae_id BIGINT NOT NULL,
    entienden_lo_que_dice_el_facilitador VARCHAR(50),
    tiene_claro_tasa_de_interes VARCHAR(50),
    monto_y_plazo_coinciden_con_expediente VARCHAR(50),
    realizo_algun_pago_al_facilitador VARCHAR(50),
    destino_del_prestamo_coincide_con_plan VARCHAR(50),
    tiene_claro_que_debe_de_participar_en_capacitaciones VARCHAR(50),
    facilitador_atendio_bien_y_resolvio_sus_dudas VARCHAR(50),
    cliente_apto_para_continuar_con_el_proceso VARCHAR(50),
    tipo_de_garantia VARCHAR(100),
    created_on DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    lastmodified_on DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_loan_pae (loan_additional_pae_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- Migration: Create Verificacion Del Fiador Table
-- =====================================================
-- changeset id:005-create-verificacion-del-fiador author:development
CREATE TABLE IF NOT EXISTS m_pae_verificacion_del_fiador (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    loan_additional_pae_id BIGINT NOT NULL,
    conoce_a_clienta VARCHAR(50),
    si_es_familiar_muestra_independencia_economica VARCHAR(50),
    sabe_que_es_fiador_y_conoce_el_monto VARCHAR(50),
    rango_edad_20_60 VARCHAR(50),
    direccion_coincide_con_expediente VARCHAR(50),
    esta_solvente_en_pda VARCHAR(50),
    anio_de_laborar_o_3_anios_en_negocio VARCHAR(50),
    es_fiador_de_otra_persona VARCHAR(50),
    cuenta_con_constancia_de_ingresos VARCHAR(50),
    cuenta_con_constancia_de_propiedad_del_negocio VARCHAR(50),
    negocio_elegible_segun_politica VARCHAR(50),
    created_on DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    lastmodified_on DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_loan_pae (loan_additional_pae_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- Migration: Create Calificacion Del Supervisor Table
-- =====================================================
-- changeset id:006-create-calificacion-del-supervisor author:development
CREATE TABLE IF NOT EXISTS m_pae_calificacion_del_supervisor (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    loan_additional_pae_id BIGINT NOT NULL,
    punteo DECIMAL(19, 6),
    calificacion VARCHAR(100),
    ubicacion VARCHAR(255),
    supervisor VARCHAR(255),
    comentarios LONGTEXT,
    created_on DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    lastmodified_on DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_loan_pae (loan_additional_pae_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- Migration: Add Foreign Key Constraints
-- =====================================================
-- changeset id:007-add-pae-foreign-keys author:development
ALTER TABLE m_pae_loan_additional_data
ADD CONSTRAINT fk_pae_loan FOREIGN KEY (loan_id)
REFERENCES m_loan(id) ON DELETE CASCADE;

ALTER TABLE m_pae_loan_additional_data
ADD CONSTRAINT fk_pae_vivienda FOREIGN KEY (verificacion_vivienda_id)
REFERENCES m_pae_verificacion_vivienda(id) ON DELETE CASCADE;

ALTER TABLE m_pae_loan_additional_data
ADD CONSTRAINT fk_pae_negocio FOREIGN KEY (verificacion_negocio_id)
REFERENCES m_pae_verificacion_negocio(id) ON DELETE CASCADE;

ALTER TABLE m_pae_loan_additional_data
ADD CONSTRAINT fk_pae_entrevista FOREIGN KEY (entrevista_cliente_id)
REFERENCES m_pae_entrevista_cliente(id) ON DELETE CASCADE;

ALTER TABLE m_pae_loan_additional_data
ADD CONSTRAINT fk_pae_fiador FOREIGN KEY (verificacion_del_fiador_id)
REFERENCES m_pae_verificacion_del_fiador(id) ON DELETE CASCADE;

ALTER TABLE m_pae_loan_additional_data
ADD CONSTRAINT fk_pae_supervisor FOREIGN KEY (calificacion_del_supervisor_id)
REFERENCES m_pae_calificacion_del_supervisor(id) ON DELETE CASCADE;

ALTER TABLE m_pae_verificacion_vivienda
ADD CONSTRAINT fk_vivienda_pae FOREIGN KEY (loan_additional_pae_id)
REFERENCES m_pae_loan_additional_data(id) ON DELETE CASCADE;

ALTER TABLE m_pae_verificacion_negocio
ADD CONSTRAINT fk_negocio_pae FOREIGN KEY (loan_additional_pae_id)
REFERENCES m_pae_loan_additional_data(id) ON DELETE CASCADE;

ALTER TABLE m_pae_entrevista_cliente
ADD CONSTRAINT fk_entrevista_pae FOREIGN KEY (loan_additional_pae_id)
REFERENCES m_pae_loan_additional_data(id) ON DELETE CASCADE;

ALTER TABLE m_pae_verificacion_del_fiador
ADD CONSTRAINT fk_fiador_pae FOREIGN KEY (loan_additional_pae_id)
REFERENCES m_pae_loan_additional_data(id) ON DELETE CASCADE;

ALTER TABLE m_pae_calificacion_del_supervisor
ADD CONSTRAINT fk_supervisor_pae FOREIGN KEY (loan_additional_pae_id)
REFERENCES m_pae_loan_additional_data(id) ON DELETE CASCADE;

-- =====================================================
-- Migration: Add Performance Indexes
-- =====================================================
-- changeset id:008-add-pae-performance-indexes author:development
CREATE INDEX idx_pae_case_id ON m_pae_loan_additional_data(case_id);
CREATE INDEX idx_pae_loan_id ON m_pae_loan_additional_data(loan_id);
CREATE INDEX idx_vivienda_created ON m_pae_verificacion_vivienda(created_on);
CREATE INDEX idx_negocio_created ON m_pae_verificacion_negocio(created_on);
CREATE INDEX idx_entrevista_created ON m_pae_entrevista_cliente(created_on);
CREATE INDEX idx_fiador_created ON m_pae_verificacion_del_fiador(created_on);
CREATE INDEX idx_supervisor_created ON m_pae_calificacion_del_supervisor(created_on);

-- =====================================================
-- Rollback Scripts (in case of failure)
-- =====================================================
-- To rollback, execute in reverse order:
-- DROP TABLE IF EXISTS m_pae_calificacion_del_supervisor;
-- DROP TABLE IF EXISTS m_pae_verificacion_del_fiador;
-- DROP TABLE IF EXISTS m_pae_entrevista_cliente;
-- DROP TABLE IF EXISTS m_pae_verificacion_negocio;
-- DROP TABLE IF EXISTS m_pae_verificacion_vivienda;
-- DROP TABLE IF EXISTS m_pae_loan_additional_data;

