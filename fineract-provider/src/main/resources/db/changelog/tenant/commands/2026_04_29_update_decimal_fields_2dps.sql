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
--     id: update decimal fields
--     author: development
--     changes:

ALTER TABLE PDA_MIG_019_P_HISTORICO_PAGOS2 MODIFY Capital DECIMAL(34,2);
ALTER TABLE PDA_MIG_019_P_HISTORICO_PAGOS2 MODIFY Exceso DECIMAL(34,2);
ALTER TABLE PDA_MIG_019_P_HISTORICO_PAGOS2 MODIFY Garantia DECIMAL(34,2);
ALTER TABLE PDA_MIG_019_P_HISTORICO_PAGOS2 MODIFY Interes DECIMAL(34,2);
ALTER TABLE PDA_MIG_019_P_HISTORICO_PAGOS2 MODIFY Mora DECIMAL(34,2);
ALTER TABLE bitacora_details MODIFY amount DECIMAL(19,2);
ALTER TABLE bitacora_details MODIFY diferential DECIMAL(19,2);
ALTER TABLE bitacora_master MODIFY amount DECIMAL(19,2);
ALTER TABLE bitacora_master MODIFY exchange_rate DECIMAL(19,2);
ALTER TABLE committee_approval_limits MODIFY from_amount DECIMAL(10,2);
ALTER TABLE committee_approval_limits MODIFY to_amount DECIMAL(19,2);
ALTER TABLE glim_accounts MODIFY application_id DECIMAL(10,2);
ALTER TABLE glim_accounts MODIFY principal_amount DECIMAL(19,2);
ALTER TABLE gsim_accounts MODIFY application_id DECIMAL(10,2);
ALTER TABLE gsim_accounts MODIFY parent_deposit DECIMAL(19,2);
ALTER TABLE m_account_transfer_standing_instructions MODIFY amount DECIMAL(19,2);
ALTER TABLE m_account_transfer_standing_instructions_history MODIFY amount DECIMAL(19,2);
ALTER TABLE m_account_transfer_transaction MODIFY amount DECIMAL(19,2);
ALTER TABLE m_address MODIFY latitude DECIMAL(10,2);
ALTER TABLE m_address MODIFY longitude DECIMAL(10,2);
ALTER TABLE m_bank_check MODIFY guarantee_amount DECIMAL(19,2);
ALTER TABLE m_bank_check MODIFY required_guarantee_amount DECIMAL(19,2);
ALTER TABLE m_cashier_transactions MODIFY txn_amount DECIMAL(19,2);
ALTER TABLE m_center_group MODIFY latitude DECIMAL(19,2);
ALTER TABLE m_center_group MODIFY longitude DECIMAL(19,2);
ALTER TABLE m_charge MODIFY amount DECIMAL(19,2);
ALTER TABLE m_charge MODIFY max_cap DECIMAL(19,2);
ALTER TABLE m_charge MODIFY min_cap DECIMAL(19,2);
ALTER TABLE m_charge_range MODIFY fee_rate DECIMAL(19,2);
ALTER TABLE m_client_blacklist MODIFY balance DECIMAL(19,2);
ALTER TABLE m_client_blacklist MODIFY disbursement_amount DECIMAL(19,2);
ALTER TABLE m_client_charge MODIFY amount DECIMAL(19,2);
ALTER TABLE m_client_charge MODIFY amount_outstanding_derived DECIMAL(19,2);
ALTER TABLE m_client_charge MODIFY amount_paid_derived DECIMAL(19,2);
ALTER TABLE m_client_charge MODIFY amount_waived_derived DECIMAL(19,2);
ALTER TABLE m_client_charge MODIFY amount_writtenoff_derived DECIMAL(19,2);
ALTER TABLE m_client_charge_paid_by MODIFY amount DECIMAL(19,2);
ALTER TABLE m_client_collateral_management MODIFY quantity DECIMAL(20,2);
ALTER TABLE m_client_loan_additional_properties MODIFY activo_corriente DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY activo_no_corriente DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY alimentacion DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY alquiler_cliente DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY alquiler_gasto DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY alquiler_local DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY bienes_inmuebles DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY bienes_inmuebles_familiares DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY capacidad_pago DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY capital_de_trabajo DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY comunal_vigente DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY costo_unitario DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY costo_venta DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY cuanto_pagar DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY cuentas_por_cobrar DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY cuentas_por_pagar DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY cuota DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY cuota_otros DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY cuota_puente DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY cuotas_pendientes_bc DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY detalle_compras DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY detalle_otros_ingresos DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY detalle_recuperacion_cuentas DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY detalle_ventas DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY educacion DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY efectivo DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY efectivo_uso_familia DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY efectivo_uso_negocio DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY endeudamiento_actual DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY endeudamiento_futuro DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY flujo_disponible DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY gastos_familiares DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY gastos_negocio DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY herramientas DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY hipotecas DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY impuestos DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY ingreso_familiar DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY inventarios DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY inversion_total DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY menaje_del_hogar DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY mobiliario_y_equipo DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY monto_autorizado DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY monto_otros_ingresos DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY monto_solicitado DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY otros_activos_familia DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY otros_activos_negocio DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY pasivo_corriente DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY pasivo_no_Corriente DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY pensiones DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY prestamo_puente DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY propuesta_facilitador DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY relacion_gastos DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY Relacion_otros_ingresos DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY rentabilidad_neta DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY rotacion_inventario DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY salario_cliente DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY salarios DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY servicios_basicos DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY servicios_gasto DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY servicios_medicos DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY tarjetas DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY tasa DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY total_activo DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY total_costo_ventas DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY total_cuentas_por_cobrar DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY total_cuota_mensual DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY total_deuda DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY total_efectivo DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY total_gastos_negocio DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY total_gastos_vivienda DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY total_ingresos DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY total_ingresos_familiares DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY total_inmueble_familia DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY total_inmueble_negocio DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY total_inmuebles DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY total_inventario DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY total_maquinaria DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY total_menaje_de_hogar DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY total_mobiliario_equipo DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY total_otros_activos DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY total_pasivo DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY total_precio_ventas DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY transporte_gasto DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY transporte_negocio DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY utilidad_bruta DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY utilidad_neta DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY valor_garantia DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY vehiculos DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY ventas DECIMAL(19,2);
ALTER TABLE m_client_loan_additional_properties MODIFY vestimenta DECIMAL(19,2);
ALTER TABLE m_client_transaction MODIFY amount DECIMAL(19,2);
ALTER TABLE m_collateral_management MODIFY base_price DECIMAL(20,2);
ALTER TABLE m_collateral_management MODIFY pct_to_base DECIMAL(20,2);
ALTER TABLE m_cupo MODIFY amount DECIMAL(19,2);
ALTER TABLE m_cupo MODIFY amount_approved DECIMAL(19,2);
ALTER TABLE m_cupo MODIFY amount_available DECIMAL(19,2);
ALTER TABLE m_cupo MODIFY amount_in_hold DECIMAL(19,2);
ALTER TABLE m_cupo MODIFY amount_submitted DECIMAL(19,2);
ALTER TABLE m_cupo_transaction MODIFY amount DECIMAL(19,2);
ALTER TABLE m_deposit_account_on_hold_transaction MODIFY amount DECIMAL(19,2);
ALTER TABLE m_deposit_account_recurring_detail MODIFY mandatory_recommended_deposit_amount DECIMAL(19,2);
ALTER TABLE m_deposit_account_recurring_detail MODIFY total_overdue_amount DECIMAL(19,2);
ALTER TABLE m_deposit_account_term_and_preclosure MODIFY deposit_amount DECIMAL(19,2);
ALTER TABLE m_deposit_account_term_and_preclosure MODIFY maturity_amount DECIMAL(19,2);
ALTER TABLE m_deposit_account_term_and_preclosure MODIFY pre_closure_penal_interest DECIMAL(19,2);
ALTER TABLE m_deposit_product_term_and_preclosure MODIFY deposit_amount DECIMAL(19,2);
ALTER TABLE m_deposit_product_term_and_preclosure MODIFY max_deposit_amount DECIMAL(19,2);
ALTER TABLE m_deposit_product_term_and_preclosure MODIFY min_deposit_amount DECIMAL(19,2);
ALTER TABLE m_deposit_product_term_and_preclosure MODIFY pre_closure_penal_interest DECIMAL(19,2);
ALTER TABLE m_exchange MODIFY TipoCambio DECIMAL(19,2);
ALTER TABLE m_floating_rates_periods MODIFY interest_rate DECIMAL(19,2);
ALTER TABLE m_group MODIFY latitude DECIMAL(19,2);
ALTER TABLE m_group MODIFY longitude DECIMAL(19,2);
ALTER TABLE m_guarantor_funding_details MODIFY amount DECIMAL(19,2);
ALTER TABLE m_guarantor_funding_details MODIFY amount_released_derived DECIMAL(19,2);
ALTER TABLE m_guarantor_funding_details MODIFY amount_remaining_derived DECIMAL(19,2);
ALTER TABLE m_guarantor_funding_details MODIFY amount_transfered_derived DECIMAL(19,2);
ALTER TABLE m_interest_incentives MODIFY amount DECIMAL(19,2);
ALTER TABLE m_interest_rate_slab MODIFY amount_range_from DECIMAL(19,2);
ALTER TABLE m_interest_rate_slab MODIFY amount_range_to DECIMAL(19,2);
ALTER TABLE m_interest_rate_slab MODIFY annual_interest_rate DECIMAL(19,2);
ALTER TABLE m_loan_additionals_group MODIFY agency_authorized_amount DECIMAL(19,2);
ALTER TABLE m_loan_additionals_group MODIFY authorized_fee DECIMAL(19,2);
ALTER TABLE m_loan_additionals_group MODIFY available_monthly DECIMAL(19,2);
ALTER TABLE m_loan_additionals_group MODIFY business_profit DECIMAL(19,2);
ALTER TABLE m_loan_additionals_group MODIFY business_purchases DECIMAL(19,2);
ALTER TABLE m_loan_additionals_group MODIFY client_profit DECIMAL(19,2);
ALTER TABLE m_loan_additionals_group MODIFY current_credit_value DECIMAL(19,2);
ALTER TABLE m_loan_additionals_group MODIFY debt_level DECIMAL(19,2);
ALTER TABLE m_loan_additionals_group MODIFY f_a_c DECIMAL(19,2);
ALTER TABLE m_loan_additionals_group MODIFY facilitator_proposed_value DECIMAL(19,2);
ALTER TABLE m_loan_additionals_group MODIFY family_expenses DECIMAL(19,2);
ALTER TABLE m_loan_additionals_group MODIFY group_authorized_value DECIMAL(19,2);
ALTER TABLE m_loan_additionals_group MODIFY inventories DECIMAL(19,2);
ALTER TABLE m_loan_additionals_group MODIFY monthly_income DECIMAL(19,2);
ALTER TABLE m_loan_additionals_group MODIFY monthly_payment_capacity DECIMAL(19,2);
ALTER TABLE m_loan_additionals_group MODIFY mortgage_fee DECIMAL(19,2);
ALTER TABLE m_loan_additionals_group MODIFY other_income DECIMAL(19,2);
ALTER TABLE m_loan_additionals_group MODIFY payment_capacity DECIMAL(19,2);
ALTER TABLE m_loan_additionals_group MODIFY proposed_fee DECIMAL(19,2);
ALTER TABLE m_loan_additionals_group MODIFY rent_fee DECIMAL(19,2);
ALTER TABLE m_loan_additionals_group MODIFY requested_value DECIMAL(19,2);
ALTER TABLE m_loan_additionals_group MODIFY sales_value DECIMAL(19,2);
ALTER TABLE m_loan_additionals_group MODIFY total_expenditures DECIMAL(19,2);
ALTER TABLE m_loan_additionals_group MODIFY total_external_loan_amount DECIMAL(19,2);
ALTER TABLE m_loan_additionals_group MODIFY total_income DECIMAL(19,2);
ALTER TABLE m_loan_additionals_group MODIFY total_installments DECIMAL(19,2);
ALTER TABLE m_loan_collateral MODIFY value DECIMAL(19,2);
ALTER TABLE m_loan_collateral_management MODIFY quantity DECIMAL(20,2);
ALTER TABLE m_loan_disbursement_detail MODIFY net_disbursal_amount DECIMAL(19,2);
ALTER TABLE m_loan_disbursement_detail MODIFY principal DECIMAL(19,2);
ALTER TABLE m_loan_external_existing_loans MODIFY balance DECIMAL(19,2);
ALTER TABLE m_loan_external_existing_loans MODIFY fees DECIMAL(19,2);
ALTER TABLE m_loan_external_existing_loans MODIFY loan_amount DECIMAL(19,2);
ALTER TABLE m_loan_term_variations MODIFY decimal_value DECIMAL(19,2);
ALTER TABLE m_loan_topup MODIFY topup_amount DECIMAL(19,2);
ALTER TABLE m_loanproduct_provisioning_entry MODIFY reseve_amount DECIMAL(20,2);
ALTER TABLE m_mandatory_savings_schedule MODIFY deposit_amount DECIMAL(19,2);
ALTER TABLE m_mandatory_savings_schedule MODIFY deposit_amount_completed_derived DECIMAL(19,2);
ALTER TABLE m_mandatory_savings_schedule MODIFY total_paid_in_advance_derived DECIMAL(19,2);
ALTER TABLE m_mandatory_savings_schedule MODIFY total_paid_late_derived DECIMAL(19,2);
ALTER TABLE m_office_transaction MODIFY transaction_amount DECIMAL(19,2);
ALTER TABLE m_pae_calificacion_del_supervisor MODIFY punteo DECIMAL(19,2);
ALTER TABLE m_prequalification_group_members MODIFY approved_amount DECIMAL(19,2);
ALTER TABLE m_prequalification_group_members MODIFY original_amount DECIMAL(19,2);
ALTER TABLE m_prequalification_group_members MODIFY requested_amount DECIMAL(19,2);
ALTER TABLE m_prequalification_status_range MODIFY max_amount DECIMAL(19,2);
ALTER TABLE m_prequalification_status_range MODIFY min_amount DECIMAL(19,2);
ALTER TABLE m_product_loan MODIFY annual_nominal_interest_rate DECIMAL(19,2);
ALTER TABLE m_product_loan MODIFY arrearstolerance_amount DECIMAL(19,2);
ALTER TABLE m_product_loan MODIFY fixed_principal_percentage_per_installment DECIMAL(5,2);
ALTER TABLE m_product_loan MODIFY instalment_amount_in_multiples_of DECIMAL(19,2);
ALTER TABLE m_product_loan MODIFY max_nominal_interest_rate_per_period DECIMAL(19,2);
ALTER TABLE m_product_loan MODIFY max_outstanding_loan_balance DECIMAL(19,2);
ALTER TABLE m_product_loan MODIFY max_principal_amount DECIMAL(19,2);
ALTER TABLE m_product_loan MODIFY min_nominal_interest_rate_per_period DECIMAL(19,2);
ALTER TABLE m_product_loan MODIFY min_principal_amount DECIMAL(19,2);
ALTER TABLE m_product_loan MODIFY nominal_interest_rate_per_period DECIMAL(19,2);
ALTER TABLE m_product_loan MODIFY payment_tolerance_limit DECIMAL(19,2);
ALTER TABLE m_product_loan MODIFY principal_amount DECIMAL(19,2);
ALTER TABLE m_product_loan MODIFY principal_threshold_for_last_installment DECIMAL(5,2);
ALTER TABLE m_product_loan MODIFY required_guarantee_percent DECIMAL(19,2);
ALTER TABLE m_product_loan_floating_rates MODIFY default_differential_lending_rate DECIMAL(19,2);
ALTER TABLE m_product_loan_floating_rates MODIFY interest_rate_differential DECIMAL(19,2);
ALTER TABLE m_product_loan_floating_rates MODIFY max_differential_lending_rate DECIMAL(19,2);
ALTER TABLE m_product_loan_floating_rates MODIFY min_differential_lending_rate DECIMAL(19,2);
ALTER TABLE m_product_loan_guarantee_details MODIFY mandatory_guarantee DECIMAL(19,2);
ALTER TABLE m_product_loan_guarantee_details MODIFY minimum_guarantee_from_guarantor_funds DECIMAL(19,2);
ALTER TABLE m_product_loan_guarantee_details MODIFY minimum_guarantee_from_own_funds DECIMAL(19,2);
ALTER TABLE m_product_loan_variations_borrower_cycle MODIFY default_value DECIMAL(19,2);
ALTER TABLE m_product_loan_variations_borrower_cycle MODIFY max_value DECIMAL(19,2);
ALTER TABLE m_product_loan_variations_borrower_cycle MODIFY min_value DECIMAL(19,2);
ALTER TABLE m_provisioning_criteria_definition MODIFY provision_percentage DECIMAL(5,2);
ALTER TABLE m_rate MODIFY percentage DECIMAL(10,2);
ALTER TABLE m_repayment_with_post_dated_checks MODIFY amount DECIMAL(20,2);
ALTER TABLE m_restructure_credit_requests MODIFY extension_amount DECIMAL(10,2);
ALTER TABLE m_restructure_credit_requests MODIFY total_loan_amount DECIMAL(19,2);
ALTER TABLE m_restructure_credits_loans_mapping MODIFY outstanding_balance DECIMAL(19,2);
ALTER TABLE m_savings_account_charge MODIFY amount DECIMAL(19,2);
ALTER TABLE m_savings_account_charge MODIFY amount_outstanding_derived DECIMAL(19,2);
ALTER TABLE m_savings_account_charge MODIFY amount_paid_derived DECIMAL(19,2);
ALTER TABLE m_savings_account_charge MODIFY amount_waived_derived DECIMAL(19,2);
ALTER TABLE m_savings_account_charge MODIFY amount_writtenoff_derived DECIMAL(19,2);
ALTER TABLE m_savings_account_charge MODIFY calculation_on_amount DECIMAL(19,2);
ALTER TABLE m_savings_account_charge MODIFY calculation_percentage DECIMAL(19,2);
ALTER TABLE m_savings_account_charge_paid_by MODIFY amount DECIMAL(19,2);
ALTER TABLE m_savings_account_interest_rate_slab MODIFY amount_range_from DECIMAL(19,2);
ALTER TABLE m_savings_account_interest_rate_slab MODIFY amount_range_to DECIMAL(19,2);
ALTER TABLE m_savings_account_interest_rate_slab MODIFY annual_interest_rate DECIMAL(19,2);
ALTER TABLE m_savings_interest_incentives MODIFY amount DECIMAL(19,2);
ALTER TABLE m_savings_product MODIFY lockin_period_frequency DECIMAL(19,2);
ALTER TABLE m_savings_product MODIFY max_allowed_lien_limit DECIMAL(19,2);
ALTER TABLE m_savings_product MODIFY min_balance_for_interest_calculation DECIMAL(19,2);
ALTER TABLE m_savings_product MODIFY min_overdraft_for_interest_calculation DECIMAL(19,2);
ALTER TABLE m_savings_product MODIFY min_required_balance DECIMAL(19,2);
ALTER TABLE m_savings_product MODIFY min_required_opening_balance DECIMAL(19,2);
ALTER TABLE m_savings_product MODIFY nominal_annual_interest_rate DECIMAL(19,2);
ALTER TABLE m_savings_product MODIFY nominal_annual_interest_rate_overdraft DECIMAL(19,2);
ALTER TABLE m_savings_product MODIFY overdraft_limit DECIMAL(19,2);
ALTER TABLE m_savings_product MODIFY withdrawal_fee_amount DECIMAL(19,2);
ALTER TABLE m_share_account MODIFY lockin_period_frequency DECIMAL(19,2);
ALTER TABLE m_share_account MODIFY minimum_active_period_frequency DECIMAL(19,2);
ALTER TABLE m_share_account_charge MODIFY amount DECIMAL(19,2);
ALTER TABLE m_share_account_charge MODIFY amount_outstanding_derived DECIMAL(19,2);
ALTER TABLE m_share_account_charge MODIFY amount_paid_derived DECIMAL(19,2);
ALTER TABLE m_share_account_charge MODIFY amount_waived_derived DECIMAL(19,2);
ALTER TABLE m_share_account_charge MODIFY amount_writtenoff_derived DECIMAL(19,2);
ALTER TABLE m_share_account_charge MODIFY calculation_on_amount DECIMAL(19,2);
ALTER TABLE m_share_account_charge MODIFY calculation_percentage DECIMAL(19,2);
ALTER TABLE m_share_account_charge MODIFY charge_amount_or_percentage DECIMAL(19,2);
ALTER TABLE m_share_account_charge MODIFY max_cap DECIMAL(19,2);
ALTER TABLE m_share_account_charge MODIFY min_cap DECIMAL(19,2);
ALTER TABLE m_share_account_charge_paid_by MODIFY amount DECIMAL(20,2);
ALTER TABLE m_share_account_dividend_details MODIFY amount DECIMAL(19,2);
ALTER TABLE m_share_account_transactions MODIFY amount DECIMAL(20,2);
ALTER TABLE m_share_account_transactions MODIFY amount_paid DECIMAL(20,2);
ALTER TABLE m_share_account_transactions MODIFY charge_amount DECIMAL(20,2);
ALTER TABLE m_share_account_transactions MODIFY unit_price DECIMAL(10,2);
ALTER TABLE m_share_product MODIFY capital_amount DECIMAL(20,2);
ALTER TABLE m_share_product MODIFY lockin_period_frequency DECIMAL(19,2);
ALTER TABLE m_share_product MODIFY minimum_active_period_frequency DECIMAL(19,2);
ALTER TABLE m_share_product MODIFY unit_price DECIMAL(10,2);
ALTER TABLE m_share_product_dividend_pay_out MODIFY amount DECIMAL(19,2);
ALTER TABLE m_share_product_market_price MODIFY share_value DECIMAL(10,2);
ALTER TABLE m_survey_lookup_tables MODIFY score DECIMAL(5,2);
ALTER TABLE m_tax_component MODIFY percentage DECIMAL(19,2);
ALTER TABLE m_tax_component_history MODIFY percentage DECIMAL(19,2);
ALTER TABLE m_trial_balance MODIFY amount DECIMAL(19,2);
ALTER TABLE m_trial_balance MODIFY closing_balance DECIMAL(19,2);
ALTER TABLE p_destino MODIFY monto DECIMAL(10,2);
ALTER TABLE p_fiador MODIFY costo_ventas_totales DECIMAL(10,2);
ALTER TABLE p_fiador MODIFY couta_nuevo_credito_pae DECIMAL(10,2);
ALTER TABLE p_fiador MODIFY cuotas_prestamos DECIMAL(10,2);
ALTER TABLE p_fiador MODIFY cuotas_prestamos_externos DECIMAL(10,2);
ALTER TABLE p_fiador MODIFY diferencia_ingresos_gastos DECIMAL(10,2);
ALTER TABLE p_fiador MODIFY disponible DECIMAL(10,2);
ALTER TABLE p_fiador MODIFY disponible_pagar_cuota DECIMAL(10,2);
ALTER TABLE p_fiador MODIFY ingresos DECIMAL(10,2);
ALTER TABLE p_fiador MODIFY monto_ventas_mensuales DECIMAL(10,2);
ALTER TABLE p_fiador MODIFY total_gastos_familiares DECIMAL(10,2);
ALTER TABLE p_fiador MODIFY total_gastos_negocio DECIMAL(10,2);
ALTER TABLE p_garantia MODIFY valor_garantia DECIMAL(19,2);
ALTER TABLE p_solicitante MODIFY costo_ventas_totales DECIMAL(19,2);
ALTER TABLE p_solicitante MODIFY cuotas_prestamos_externos DECIMAL(19,2);
ALTER TABLE p_solicitante MODIFY diferencia_ingresos_gastos DECIMAL(19,2);
ALTER TABLE p_solicitante MODIFY disponible_pagar_cuota DECIMAL(19,2);
ALTER TABLE p_solicitante MODIFY Monto_de_creditos_retener DECIMAL(10,2);
ALTER TABLE p_solicitante MODIFY monto_ventas_mensuales DECIMAL(19,2);
ALTER TABLE p_solicitante MODIFY pago_credito_pdA DECIMAL(19,2);
ALTER TABLE p_solicitante MODIFY total_gastos_familiares DECIMAL(19,2);
ALTER TABLE p_solicitante MODIFY total_gastos_negocio DECIMAL(19,2);
ALTER TABLE renegotiations MODIFY proposed_amount DECIMAL(19,2);
ALTER TABLE renegotiations MODIFY proposed_interest DECIMAL(19,2);
