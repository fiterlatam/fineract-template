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

--
-- Remove if exists other loan products and any associated record
--

DELETE FROM m_product_loan_variations_borrower_cycle WHERE loan_product_id > 1;
DELETE FROM m_product_loan_charge WHERE product_loan_id > 1;
DELETE FROM m_product_loan_configurable_attributes WHERE loan_product_id > 1;
DELETE FROM m_product_loan WHERE id > 1;

/*
-- Query: SELECT * FROM fineract_default.m_product_loan
LIMIT 0, 1000

-- Date: 2023-07-07 15:41
*/
INSERT INTO `m_product_loan` (`id`,`short_name`,`currency_code`,`currency_digits`,`currency_multiplesof`,`principal_amount`,`min_principal_amount`,`max_principal_amount`,`arrearstolerance_amount`,`name`,`description`,`fund_id`,`is_linked_to_floating_interest_rates`,`allow_variabe_installments`,`nominal_interest_rate_per_period`,`min_nominal_interest_rate_per_period`,`max_nominal_interest_rate_per_period`,`interest_period_frequency_enum`,`annual_nominal_interest_rate`,`interest_method_enum`,`interest_calculated_in_period_enum`,`allow_partial_period_interest_calcualtion`,`repay_every`,`repayment_period_frequency_enum`,`number_of_repayments`,`min_number_of_repayments`,`max_number_of_repayments`,`grace_on_principal_periods`,`recurring_moratorium_principal_periods`,`grace_on_interest_periods`,`grace_interest_free_periods`,`amortization_method_enum`,`accounting_type`,`loan_transaction_strategy_id`,`external_id`,`include_in_borrower_cycle`,`use_borrower_cycle`,`start_date`,`close_date`,`allow_multiple_disbursals`,`max_disbursals`,`max_outstanding_loan_balance`,`grace_on_arrears_ageing`,`overdue_days_for_npa`,`days_in_month_enum`,`days_in_year_enum`,`interest_recalculation_enabled`,`min_days_between_disbursal_and_first_repayment`,`hold_guarantee_funds`,`principal_threshold_for_last_installment`,`account_moves_out_of_npa_only_on_arrears_completion`,`can_define_fixed_emi_amount`,`instalment_amount_in_multiples_of`,`can_use_for_topup`,`sync_expected_with_disbursement_date`,`is_equal_amortization`,`fixed_principal_percentage_per_installment`,`disallow_expected_disbursements`,`allow_approved_disbursed_amounts_over_applied`,`over_applied_calculation_type`,`over_applied_number`)
VALUES (2,'BCO','GTQ',2,1,1000.000000,1000.000000,15000.000000,NULL,'Banco Comunal','Producto Banco Comunal',NULL,0,0,3.000000,3.000000,3.500000,2,NULL,0,1,0,1,2,12,1,60,NULL,NULL,NULL,NULL,1,1,1,NULL,1,1,NULL,NULL,0,NULL,NULL,NULL,NULL,1,1,0,NULL,0,0.00,0,0,1.000000,0,0,0,NULL,0,0,NULL,NULL);

INSERT INTO `m_product_loan` (`id`,`short_name`,`currency_code`,`currency_digits`,`currency_multiplesof`,`principal_amount`,`min_principal_amount`,`max_principal_amount`,`arrearstolerance_amount`,`name`,`description`,`fund_id`,`is_linked_to_floating_interest_rates`,`allow_variabe_installments`,`nominal_interest_rate_per_period`,`min_nominal_interest_rate_per_period`,`max_nominal_interest_rate_per_period`,`interest_period_frequency_enum`,`annual_nominal_interest_rate`,`interest_method_enum`,`interest_calculated_in_period_enum`,`allow_partial_period_interest_calcualtion`,`repay_every`,`repayment_period_frequency_enum`,`number_of_repayments`,`min_number_of_repayments`,`max_number_of_repayments`,`grace_on_principal_periods`,`recurring_moratorium_principal_periods`,`grace_on_interest_periods`,`grace_interest_free_periods`,`amortization_method_enum`,`accounting_type`,`loan_transaction_strategy_id`,`external_id`,`include_in_borrower_cycle`,`use_borrower_cycle`,`start_date`,`close_date`,`allow_multiple_disbursals`,`max_disbursals`,`max_outstanding_loan_balance`,`grace_on_arrears_ageing`,`overdue_days_for_npa`,`days_in_month_enum`,`days_in_year_enum`,`interest_recalculation_enabled`,`min_days_between_disbursal_and_first_repayment`,`hold_guarantee_funds`,`principal_threshold_for_last_installment`,`account_moves_out_of_npa_only_on_arrears_completion`,`can_define_fixed_emi_amount`,`instalment_amount_in_multiples_of`,`can_use_for_topup`,`sync_expected_with_disbursement_date`,`is_equal_amortization`,`fixed_principal_percentage_per_installment`,`disallow_expected_disbursements`,`allow_approved_disbursed_amounts_over_applied`,`over_applied_calculation_type`,`over_applied_number`)
VALUES (3,'CHA','GTQ',2,1,1000.000000,1000.000000,3000.000000,NULL,'Chanim','Producto Chanim',NULL,0,0,3.000000,3.000000,3.500000,2,NULL,0,1,0,1,2,12,1,60,NULL,NULL,NULL,NULL,1,1,1,NULL,1,1,NULL,NULL,0,NULL,NULL,NULL,NULL,1,1,0,NULL,0,0.00,0,0,1.000000,0,0,0,NULL,0,0,NULL,NULL);

INSERT INTO `m_product_loan` (`id`,`short_name`,`currency_code`,`currency_digits`,`currency_multiplesof`,`principal_amount`,`min_principal_amount`,`max_principal_amount`,`arrearstolerance_amount`,`name`,`description`,`fund_id`,`is_linked_to_floating_interest_rates`,`allow_variabe_installments`,`nominal_interest_rate_per_period`,`min_nominal_interest_rate_per_period`,`max_nominal_interest_rate_per_period`,`interest_period_frequency_enum`,`annual_nominal_interest_rate`,`interest_method_enum`,`interest_calculated_in_period_enum`,`allow_partial_period_interest_calcualtion`,`repay_every`,`repayment_period_frequency_enum`,`number_of_repayments`,`min_number_of_repayments`,`max_number_of_repayments`,`grace_on_principal_periods`,`recurring_moratorium_principal_periods`,`grace_on_interest_periods`,`grace_interest_free_periods`,`amortization_method_enum`,`accounting_type`,`loan_transaction_strategy_id`,`external_id`,`include_in_borrower_cycle`,`use_borrower_cycle`,`start_date`,`close_date`,`allow_multiple_disbursals`,`max_disbursals`,`max_outstanding_loan_balance`,`grace_on_arrears_ageing`,`overdue_days_for_npa`,`days_in_month_enum`,`days_in_year_enum`,`interest_recalculation_enabled`,`min_days_between_disbursal_and_first_repayment`,`hold_guarantee_funds`,`principal_threshold_for_last_installment`,`account_moves_out_of_npa_only_on_arrears_completion`,`can_define_fixed_emi_amount`,`instalment_amount_in_multiples_of`,`can_use_for_topup`,`sync_expected_with_disbursement_date`,`is_equal_amortization`,`fixed_principal_percentage_per_installment`,`disallow_expected_disbursements`,`allow_approved_disbursed_amounts_over_applied`,`over_applied_calculation_type`,`over_applied_number`)
VALUES (4,'GSOL','GTQ',2,1,6000.000000,6000.000000,20000.000000,NULL,'Grupo Solidario','Producto Grupo Solidario',NULL,0,0,3.000000,3.000000,3.500000,2,NULL,0,1,0,1,2,12,1,60,NULL,NULL,NULL,NULL,1,1,1,NULL,1,1,NULL,NULL,0,NULL,NULL,NULL,NULL,1,1,0,NULL,0,0.00,0,0,1.000000,0,0,0,NULL,0,0,NULL,NULL);

INSERT INTO `m_product_loan` (`id`,`short_name`,`currency_code`,`currency_digits`,`currency_multiplesof`,`principal_amount`,`min_principal_amount`,`max_principal_amount`,`arrearstolerance_amount`,`name`,`description`,`fund_id`,`is_linked_to_floating_interest_rates`,`allow_variabe_installments`,`nominal_interest_rate_per_period`,`min_nominal_interest_rate_per_period`,`max_nominal_interest_rate_per_period`,`interest_period_frequency_enum`,`annual_nominal_interest_rate`,`interest_method_enum`,`interest_calculated_in_period_enum`,`allow_partial_period_interest_calcualtion`,`repay_every`,`repayment_period_frequency_enum`,`number_of_repayments`,`min_number_of_repayments`,`max_number_of_repayments`,`grace_on_principal_periods`,`recurring_moratorium_principal_periods`,`grace_on_interest_periods`,`grace_interest_free_periods`,`amortization_method_enum`,`accounting_type`,`loan_transaction_strategy_id`,`external_id`,`include_in_borrower_cycle`,`use_borrower_cycle`,`start_date`,`close_date`,`allow_multiple_disbursals`,`max_disbursals`,`max_outstanding_loan_balance`,`grace_on_arrears_ageing`,`overdue_days_for_npa`,`days_in_month_enum`,`days_in_year_enum`,`interest_recalculation_enabled`,`min_days_between_disbursal_and_first_repayment`,`hold_guarantee_funds`,`principal_threshold_for_last_installment`,`account_moves_out_of_npa_only_on_arrears_completion`,`can_define_fixed_emi_amount`,`instalment_amount_in_multiples_of`,`can_use_for_topup`,`sync_expected_with_disbursement_date`,`is_equal_amortization`,`fixed_principal_percentage_per_installment`,`disallow_expected_disbursements`,`allow_approved_disbursed_amounts_over_applied`,`over_applied_calculation_type`,`over_applied_number`)
VALUES (5,'GSA','GTQ',2,1,6000.000000,6000.000000,20000.000000,NULL,'Grupo Solidario Agricola','Producto Grupo Solidario Agricola',NULL,0,0,3.000000,3.000000,3.500000,2,NULL,0,1,0,1,2,12,1,60,NULL,NULL,NULL,NULL,1,1,1,NULL,1,1,NULL,NULL,0,NULL,NULL,NULL,NULL,1,1,0,NULL,0,0.00,0,0,1.000000,0,0,0,NULL,0,0,NULL,NULL);

INSERT INTO `m_product_loan` (`id`,`short_name`,`currency_code`,`currency_digits`,`currency_multiplesof`,`principal_amount`,`min_principal_amount`,`max_principal_amount`,`arrearstolerance_amount`,`name`,`description`,`fund_id`,`is_linked_to_floating_interest_rates`,`allow_variabe_installments`,`nominal_interest_rate_per_period`,`min_nominal_interest_rate_per_period`,`max_nominal_interest_rate_per_period`,`interest_period_frequency_enum`,`annual_nominal_interest_rate`,`interest_method_enum`,`interest_calculated_in_period_enum`,`allow_partial_period_interest_calcualtion`,`repay_every`,`repayment_period_frequency_enum`,`number_of_repayments`,`min_number_of_repayments`,`max_number_of_repayments`,`grace_on_principal_periods`,`recurring_moratorium_principal_periods`,`grace_on_interest_periods`,`grace_interest_free_periods`,`amortization_method_enum`,`accounting_type`,`loan_transaction_strategy_id`,`external_id`,`include_in_borrower_cycle`,`use_borrower_cycle`,`start_date`,`close_date`,`allow_multiple_disbursals`,`max_disbursals`,`max_outstanding_loan_balance`,`grace_on_arrears_ageing`,`overdue_days_for_npa`,`days_in_month_enum`,`days_in_year_enum`,`interest_recalculation_enabled`,`min_days_between_disbursal_and_first_repayment`,`hold_guarantee_funds`,`principal_threshold_for_last_installment`,`account_moves_out_of_npa_only_on_arrears_completion`,`can_define_fixed_emi_amount`,`instalment_amount_in_multiples_of`,`can_use_for_topup`,`sync_expected_with_disbursement_date`,`is_equal_amortization`,`fixed_principal_percentage_per_installment`,`disallow_expected_disbursements`,`allow_approved_disbursed_amounts_over_applied`,`over_applied_calculation_type`,`over_applied_number`)
VALUES (6,'IND','GTQ',2,1,10000.000000,10000.000000,125000.000000,NULL,'Individual','Producto Individual',NULL,0,0,2.830000,2.830000,3.670000,2,NULL,0,1,0,1,2,12,1,60,NULL,NULL,NULL,NULL,1,1,1,NULL,1,1,NULL,NULL,0,NULL,NULL,NULL,NULL,1,1,0,NULL,0,0.00,0,0,1.000000,0,0,0,NULL,0,0,NULL,NULL);

INSERT INTO `m_product_loan` (`id`,`short_name`,`currency_code`,`currency_digits`,`currency_multiplesof`,`principal_amount`,`min_principal_amount`,`max_principal_amount`,`arrearstolerance_amount`,`name`,`description`,`fund_id`,`is_linked_to_floating_interest_rates`,`allow_variabe_installments`,`nominal_interest_rate_per_period`,`min_nominal_interest_rate_per_period`,`max_nominal_interest_rate_per_period`,`interest_period_frequency_enum`,`annual_nominal_interest_rate`,`interest_method_enum`,`interest_calculated_in_period_enum`,`allow_partial_period_interest_calcualtion`,`repay_every`,`repayment_period_frequency_enum`,`number_of_repayments`,`min_number_of_repayments`,`max_number_of_repayments`,`grace_on_principal_periods`,`recurring_moratorium_principal_periods`,`grace_on_interest_periods`,`grace_interest_free_periods`,`amortization_method_enum`,`accounting_type`,`loan_transaction_strategy_id`,`external_id`,`include_in_borrower_cycle`,`use_borrower_cycle`,`start_date`,`close_date`,`allow_multiple_disbursals`,`max_disbursals`,`max_outstanding_loan_balance`,`grace_on_arrears_ageing`,`overdue_days_for_npa`,`days_in_month_enum`,`days_in_year_enum`,`interest_recalculation_enabled`,`min_days_between_disbursal_and_first_repayment`,`hold_guarantee_funds`,`principal_threshold_for_last_installment`,`account_moves_out_of_npa_only_on_arrears_completion`,`can_define_fixed_emi_amount`,`instalment_amount_in_multiples_of`,`can_use_for_topup`,`sync_expected_with_disbursement_date`,`is_equal_amortization`,`fixed_principal_percentage_per_installment`,`disallow_expected_disbursements`,`allow_approved_disbursed_amounts_over_applied`,`over_applied_calculation_type`,`over_applied_number`)
VALUES (7,'PAR','GTQ',2,1,3001.000000,3001.000000,15000.000000,NULL,'Paralelo','Producto Paralelo',NULL,0,0,3.000000,3.000000,3.000000,2,NULL,0,1,0,1,2,12,1,60,NULL,NULL,NULL,NULL,1,1,1,NULL,1,1,NULL,NULL,0,NULL,NULL,NULL,NULL,1,1,0,NULL,0,0.00,0,0,1.000000,0,0,0,NULL,0,0,NULL,NULL);

INSERT INTO `m_product_loan` (`id`,`short_name`,`currency_code`,`currency_digits`,`currency_multiplesof`,`principal_amount`,`min_principal_amount`,`max_principal_amount`,`arrearstolerance_amount`,`name`,`description`,`fund_id`,`is_linked_to_floating_interest_rates`,`allow_variabe_installments`,`nominal_interest_rate_per_period`,`min_nominal_interest_rate_per_period`,`max_nominal_interest_rate_per_period`,`interest_period_frequency_enum`,`annual_nominal_interest_rate`,`interest_method_enum`,`interest_calculated_in_period_enum`,`allow_partial_period_interest_calcualtion`,`repay_every`,`repayment_period_frequency_enum`,`number_of_repayments`,`min_number_of_repayments`,`max_number_of_repayments`,`grace_on_principal_periods`,`recurring_moratorium_principal_periods`,`grace_on_interest_periods`,`grace_interest_free_periods`,`amortization_method_enum`,`accounting_type`,`loan_transaction_strategy_id`,`external_id`,`include_in_borrower_cycle`,`use_borrower_cycle`,`start_date`,`close_date`,`allow_multiple_disbursals`,`max_disbursals`,`max_outstanding_loan_balance`,`grace_on_arrears_ageing`,`overdue_days_for_npa`,`days_in_month_enum`,`days_in_year_enum`,`interest_recalculation_enabled`,`min_days_between_disbursal_and_first_repayment`,`hold_guarantee_funds`,`principal_threshold_for_last_installment`,`account_moves_out_of_npa_only_on_arrears_completion`,`can_define_fixed_emi_amount`,`instalment_amount_in_multiples_of`,`can_use_for_topup`,`sync_expected_with_disbursement_date`,`is_equal_amortization`,`fixed_principal_percentage_per_installment`,`disallow_expected_disbursements`,`allow_approved_disbursed_amounts_over_applied`,`over_applied_calculation_type`,`over_applied_number`)
VALUES (8,'BCOA','GTQ',2,1,1000.000000,1000.000000,15000.000000,NULL,'Banco Comunal Agrícola','Producto Banco Comunal Agrícola',NULL,0,0,3.000000,3.000000,3.500000,2,NULL,0,1,0,1,2,12,1,60,NULL,NULL,NULL,NULL,1,1,1,NULL,1,1,NULL,NULL,0,NULL,NULL,NULL,NULL,1,1,0,NULL,0,0.00,0,0,1.000000,0,0,0,NULL,0,0,NULL,NULL);

INSERT INTO `m_product_loan` (`id`,`short_name`,`currency_code`,`currency_digits`,`currency_multiplesof`,`principal_amount`,`min_principal_amount`,`max_principal_amount`,`arrearstolerance_amount`,`name`,`description`,`fund_id`,`is_linked_to_floating_interest_rates`,`allow_variabe_installments`,`nominal_interest_rate_per_period`,`min_nominal_interest_rate_per_period`,`max_nominal_interest_rate_per_period`,`interest_period_frequency_enum`,`annual_nominal_interest_rate`,`interest_method_enum`,`interest_calculated_in_period_enum`,`allow_partial_period_interest_calcualtion`,`repay_every`,`repayment_period_frequency_enum`,`number_of_repayments`,`min_number_of_repayments`,`max_number_of_repayments`,`grace_on_principal_periods`,`recurring_moratorium_principal_periods`,`grace_on_interest_periods`,`grace_interest_free_periods`,`amortization_method_enum`,`accounting_type`,`loan_transaction_strategy_id`,`external_id`,`include_in_borrower_cycle`,`use_borrower_cycle`,`start_date`,`close_date`,`allow_multiple_disbursals`,`max_disbursals`,`max_outstanding_loan_balance`,`grace_on_arrears_ageing`,`overdue_days_for_npa`,`days_in_month_enum`,`days_in_year_enum`,`interest_recalculation_enabled`,`min_days_between_disbursal_and_first_repayment`,`hold_guarantee_funds`,`principal_threshold_for_last_installment`,`account_moves_out_of_npa_only_on_arrears_completion`,`can_define_fixed_emi_amount`,`instalment_amount_in_multiples_of`,`can_use_for_topup`,`sync_expected_with_disbursement_date`,`is_equal_amortization`,`fixed_principal_percentage_per_installment`,`disallow_expected_disbursements`,`allow_approved_disbursed_amounts_over_applied`,`over_applied_calculation_type`,`over_applied_number`)
VALUES (9,'BCOT','GTQ',2,1,1000.000000,1000.000000,5000.000000,NULL,'Banco Comunal Temporal','Producto Banco Comunal Temporal',NULL,0,0,3.000000,3.000000,3.500000,2,NULL,0,1,0,1,2,12,1,60,NULL,NULL,NULL,NULL,1,1,1,NULL,1,1,NULL,NULL,0,NULL,NULL,NULL,NULL,1,1,0,NULL,0,0.00,0,0,1.000000,0,0,0,NULL,0,0,NULL,NULL);

/*
-- Query: SELECT * FROM fineract_default.m_product_loan_configurable_attributes
LIMIT 0, 1000

-- Date: 2023-07-07 15:42
*/
INSERT INTO `m_product_loan_configurable_attributes` (`loan_product_id`,`amortization_method_enum`,`interest_method_enum`,`loan_transaction_strategy_id`,`interest_calculated_in_period_enum`,`arrearstolerance_amount`,`repay_every`,`moratorium`,`grace_on_arrears_ageing`)
VALUES (2,1,1,1,1,1,1,1,1);

INSERT INTO `m_product_loan_configurable_attributes` (`loan_product_id`,`amortization_method_enum`,`interest_method_enum`,`loan_transaction_strategy_id`,`interest_calculated_in_period_enum`,`arrearstolerance_amount`,`repay_every`,`moratorium`,`grace_on_arrears_ageing`)
VALUES (3,1,1,1,1,1,1,1,1);

INSERT INTO `m_product_loan_configurable_attributes` (`loan_product_id`,`amortization_method_enum`,`interest_method_enum`,`loan_transaction_strategy_id`,`interest_calculated_in_period_enum`,`arrearstolerance_amount`,`repay_every`,`moratorium`,`grace_on_arrears_ageing`)
VALUES (4,1,1,1,1,1,1,1,1);

INSERT INTO `m_product_loan_configurable_attributes` (`loan_product_id`,`amortization_method_enum`,`interest_method_enum`,`loan_transaction_strategy_id`,`interest_calculated_in_period_enum`,`arrearstolerance_amount`,`repay_every`,`moratorium`,`grace_on_arrears_ageing`)
VALUES (5,1,1,1,1,1,1,1,1);

INSERT INTO `m_product_loan_configurable_attributes` (`loan_product_id`,`amortization_method_enum`,`interest_method_enum`,`loan_transaction_strategy_id`,`interest_calculated_in_period_enum`,`arrearstolerance_amount`,`repay_every`,`moratorium`,`grace_on_arrears_ageing`)
VALUES (6,1,1,1,1,1,1,1,1);

INSERT INTO `m_product_loan_configurable_attributes` (`loan_product_id`,`amortization_method_enum`,`interest_method_enum`,`loan_transaction_strategy_id`,`interest_calculated_in_period_enum`,`arrearstolerance_amount`,`repay_every`,`moratorium`,`grace_on_arrears_ageing`)
VALUES (7,1,1,1,1,1,1,1,1);

INSERT INTO `m_product_loan_configurable_attributes` (`loan_product_id`,`amortization_method_enum`,`interest_method_enum`,`loan_transaction_strategy_id`,`interest_calculated_in_period_enum`,`arrearstolerance_amount`,`repay_every`,`moratorium`,`grace_on_arrears_ageing`)
VALUES (8,1,1,1,1,1,1,1,1);

INSERT INTO `m_product_loan_configurable_attributes` (`loan_product_id`,`amortization_method_enum`,`interest_method_enum`,`loan_transaction_strategy_id`,`interest_calculated_in_period_enum`,`arrearstolerance_amount`,`repay_every`,`moratorium`,`grace_on_arrears_ageing`)
VALUES (9,1,1,1,1,1,1,1,1);

/*
-- Query: SELECT * FROM fineract_default.m_product_loan_charge
LIMIT 0, 1000

-- Date: 2023-07-07 15:42
*/
-- INSERT INTO `m_product_loan_charge` (`product_loan_id`,`charge_id`) VALUES (2,9);
INSERT INTO `m_product_loan_charge` (`product_loan_id`,`charge_id`) SELECT 2, id FROM m_charge WHERE name = 'Interes por Mora';
INSERT INTO `m_product_loan_charge` (`product_loan_id`,`charge_id`) SELECT 3, id FROM m_charge WHERE name = 'Interes por Mora';
INSERT INTO `m_product_loan_charge` (`product_loan_id`,`charge_id`) SELECT 4, id FROM m_charge WHERE name = 'Interes por Mora';
INSERT INTO `m_product_loan_charge` (`product_loan_id`,`charge_id`) SELECT 5, id FROM m_charge WHERE name = 'Interes por Mora';
INSERT INTO `m_product_loan_charge` (`product_loan_id`,`charge_id`) SELECT 6, id FROM m_charge WHERE name = 'Interes por Mora';
INSERT INTO `m_product_loan_charge` (`product_loan_id`,`charge_id`) SELECT 7, id FROM m_charge WHERE name = 'Interes por Mora';
INSERT INTO `m_product_loan_charge` (`product_loan_id`,`charge_id`) SELECT 8, id FROM m_charge WHERE name = 'Interes por Mora';
INSERT INTO `m_product_loan_charge` (`product_loan_id`,`charge_id`) SELECT 9, id FROM m_charge WHERE name = 'Interes por Mora';

/*
-- Query: SELECT * FROM fineract_default.m_product_loan_variations_borrower_cycle
LIMIT 0, 1000

-- Date: 2023-07-07 15:43
*/
INSERT INTO `m_product_loan_variations_borrower_cycle` (`loan_product_id`,`borrower_cycle_number`,`value_condition`,`param_type`,`default_value`,`max_value`,`min_value`)
VALUES (2,1,2,2,3.000000,3.500000,2.600000);

INSERT INTO `m_product_loan_variations_borrower_cycle` (`loan_product_id`,`borrower_cycle_number`,`value_condition`,`param_type`,`default_value`,`max_value`,`min_value`)
VALUES (2,2,2,2,3.000000,3.500000,2.600000);

INSERT INTO `m_product_loan_variations_borrower_cycle` (`loan_product_id`,`borrower_cycle_number`,`value_condition`,`param_type`,`default_value`,`max_value`,`min_value`)
VALUES (2,3,2,2,3.000000,3.500000,2.600000);

INSERT INTO `m_product_loan_variations_borrower_cycle` (`loan_product_id`,`borrower_cycle_number`,`value_condition`,`param_type`,`default_value`,`max_value`,`min_value`)
VALUES (2,3,3,2,2.600000,3.500000,2.600000);

INSERT INTO `m_product_loan_variations_borrower_cycle` (`loan_product_id`,`borrower_cycle_number`,`value_condition`,`param_type`,`default_value`,`max_value`,`min_value`)
VALUES (3,1,2,2,3.000000,3.500000,2.600000);

INSERT INTO `m_product_loan_variations_borrower_cycle` (`loan_product_id`,`borrower_cycle_number`,`value_condition`,`param_type`,`default_value`,`max_value`,`min_value`)
VALUES (3,2,2,2,3.000000,3.500000,2.600000);

INSERT INTO `m_product_loan_variations_borrower_cycle` (`loan_product_id`,`borrower_cycle_number`,`value_condition`,`param_type`,`default_value`,`max_value`,`min_value`)
VALUES (3,3,2,2,3.000000,3.500000,2.600000);

INSERT INTO `m_product_loan_variations_borrower_cycle` (`loan_product_id`,`borrower_cycle_number`,`value_condition`,`param_type`,`default_value`,`max_value`,`min_value`)
VALUES (3,3,3,2,2.600000,3.500000,2.600000);

INSERT INTO `m_product_loan_variations_borrower_cycle` (`loan_product_id`,`borrower_cycle_number`,`value_condition`,`param_type`,`default_value`,`max_value`,`min_value`)
VALUES (4,1,2,2,3.500000,3.500000,2.300000);

INSERT INTO `m_product_loan_variations_borrower_cycle` (`loan_product_id`,`borrower_cycle_number`,`value_condition`,`param_type`,`default_value`,`max_value`,`min_value`)
VALUES (4,2,2,2,3.500000,3.500000,2.300000);

INSERT INTO `m_product_loan_variations_borrower_cycle` (`loan_product_id`,`borrower_cycle_number`,`value_condition`,`param_type`,`default_value`,`max_value`,`min_value`)
VALUES (4,3,2,2,3.500000,3.500000,2.300000);

INSERT INTO `m_product_loan_variations_borrower_cycle` (`loan_product_id`,`borrower_cycle_number`,`value_condition`,`param_type`,`default_value`,`max_value`,`min_value`)
VALUES (4,3,3,2,2.300000,3.500000,2.300000);

INSERT INTO `m_product_loan_variations_borrower_cycle` (`loan_product_id`,`borrower_cycle_number`,`value_condition`,`param_type`,`default_value`,`max_value`,`min_value`)
VALUES (5,1,2,2,3.500000,3.500000,2.300000);

INSERT INTO `m_product_loan_variations_borrower_cycle` (`loan_product_id`,`borrower_cycle_number`,`value_condition`,`param_type`,`default_value`,`max_value`,`min_value`)
VALUES (5,2,2,2,3.500000,3.500000,2.300000);

INSERT INTO `m_product_loan_variations_borrower_cycle` (`loan_product_id`,`borrower_cycle_number`,`value_condition`,`param_type`,`default_value`,`max_value`,`min_value`)
VALUES (5,3,2,2,3.500000,3.500000,2.300000);

INSERT INTO `m_product_loan_variations_borrower_cycle` (`loan_product_id`,`borrower_cycle_number`,`value_condition`,`param_type`,`default_value`,`max_value`,`min_value`)
VALUES (5,3,3,2,3.000000,3.500000,2.300000);

INSERT INTO `m_product_loan_variations_borrower_cycle` (`loan_product_id`,`borrower_cycle_number`,`value_condition`,`param_type`,`default_value`,`max_value`,`min_value`)
VALUES (8,1,2,2,3.000000,3.500000,2.600000);

INSERT INTO `m_product_loan_variations_borrower_cycle` (`loan_product_id`,`borrower_cycle_number`,`value_condition`,`param_type`,`default_value`,`max_value`,`min_value`)
VALUES (8,2,2,2,3.000000,3.500000,2.600000);

INSERT INTO `m_product_loan_variations_borrower_cycle` (`loan_product_id`,`borrower_cycle_number`,`value_condition`,`param_type`,`default_value`,`max_value`,`min_value`)
VALUES (8,3,2,2,3.000000,3.500000,2.600000);

INSERT INTO `m_product_loan_variations_borrower_cycle` (`loan_product_id`,`borrower_cycle_number`,`value_condition`,`param_type`,`default_value`,`max_value`,`min_value`)
VALUES (8,3,3,2,2.600000,3.500000,2.600000);

INSERT INTO `m_product_loan_variations_borrower_cycle` (`loan_product_id`,`borrower_cycle_number`,`value_condition`,`param_type`,`default_value`,`max_value`,`min_value`)
VALUES (9,1,2,2,3.000000,3.500000,2.600000);

INSERT INTO `m_product_loan_variations_borrower_cycle` (`loan_product_id`,`borrower_cycle_number`,`value_condition`,`param_type`,`default_value`,`max_value`,`min_value`)
VALUES (9,2,2,2,3.000000,3.500000,2.600000);

INSERT INTO `m_product_loan_variations_borrower_cycle` (`loan_product_id`,`borrower_cycle_number`,`value_condition`,`param_type`,`default_value`,`max_value`,`min_value`)
VALUES (9,3,2,2,3.000000,3.500000,2.600000);

INSERT INTO `m_product_loan_variations_borrower_cycle` (`loan_product_id`,`borrower_cycle_number`,`value_condition`,`param_type`,`default_value`,`max_value`,`min_value`)
VALUES (9,3,3,2,2.600000,3.500000,2.600000);
