/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.portfolio.loanaccount.invoice.data;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.portfolio.client.domain.LegalForm;
import org.apache.fineract.portfolio.loanaccount.invoice.domain.FacturaElectronicaMensual;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoanDocumentData {

    /** Client fields */
    private Long loanId;
    private Long clientId;
    private Integer clientLegalForm;
    private LocalDate overdueSinceDate;
    private Integer daysInArrears;
    private Long productTypeId;
    private String productTypeName;
    private String clientDisplayName;
    private String clientLastName;
    private String clientEmailAddress;
    private String clientIdNumber;
    private String loanProductName;
    private String companyNIT;
    private String companyDocType;
    private String companyDeptCode;
    private String companyDeptName;
    private String companyCityCode;
    private String companyCityName;
    private String companyAddress;
    private String companyTelephone;
    private String clientCedula;
    private String clientAddress;
    private String clientCityCode;
    private String clientCityName;
    private String clientTelephone;

    /** Resolution fields */
    private Long productTypeParamId;
    private String billingPrefix;
    private String billingResolutionNumber;
    private Long rangeStartNumber;
    private Long rangeEndNumber;
    private Long lastInvoiceNumber;
    private Long lastCreditNoteNumber;
    private Long lastDebitNoteNumber;
    private String technicalKey;
    private String nota;
    private LoanDocumentType documentType;

    /** Document fields */
    private LocalDate lastDayOfMonth;
    private LocalDate firstDayOfMonth;
    private LocalDate secondLastDayOfMonth;

    /** Loan Paid amount fields */
    private BigDecimal interestPaid;
    private BigDecimal mandatoryInsurancePaid;
    private BigDecimal voluntaryInsurancePaid;
    private BigDecimal honorariosPaid;
    private BigDecimal penaltyChargesPaid;
    private BigDecimal totalPaid;
    private String voluntaryInsuranceCode;
    private String mandatoryInsuranceCode;
    private String voluntaryInsuranceName;
    private String mandatoryInsuranceName;
    private Integer loansCount;
    private Integer itemsCount;
    private Long loanTransactionId;

    public FacturaElectronicaMensual toEntity() {
        final FacturaElectronicaMensual facturaElectronicaMensual = new FacturaElectronicaMensual();
        final LocalDate businessLocalDate = DateUtils.getBusinessLocalDate();
        int conceptCount = 0;
        if (this.interestPaid.compareTo(BigDecimal.ZERO) > 0) {
            conceptCount++;
        }
        if (this.mandatoryInsurancePaid.compareTo(BigDecimal.ZERO) > 0) {
            conceptCount++;
        }
        if (this.voluntaryInsurancePaid.compareTo(BigDecimal.ZERO) > 0) {
            conceptCount++;
        }
        if (this.honorariosPaid.compareTo(BigDecimal.ZERO) > 0) {
            conceptCount++;
        }
        if (this.penaltyChargesPaid.compareTo(BigDecimal.ZERO) > 0) {
            conceptCount++;
        }
        this.itemsCount = conceptCount;

        // INFORMATION AT RESOLUTION LEVEL
        facturaElectronicaMensual.setCreatedDate(DateUtils.getAuditOffsetDateTime());
        facturaElectronicaMensual.setNum_resolucion(this.billingResolutionNumber);
        facturaElectronicaMensual.setPrefijo(this.billingPrefix);
        facturaElectronicaMensual.setFec_desde(this.firstDayOfMonth);
        facturaElectronicaMensual.setFec_hasta(this.secondLastDayOfMonth);
        facturaElectronicaMensual.setConsecutivo_inicial(this.rangeStartNumber);
        facturaElectronicaMensual.setConsecutivo_final(this.rangeEndNumber);
        facturaElectronicaMensual.setClave_tecnica(this.technicalKey);
        facturaElectronicaMensual.setNota(this.nota);

        // INFORMATION AT INVOICE LEVEL
        final String logo = "3";
        facturaElectronicaMensual.setFecha_factura(businessLocalDate);
        facturaElectronicaMensual.setMoneda("COP");
        facturaElectronicaMensual.setForma_pago("1");
        facturaElectronicaMensual.setMedio_pago("31");
        facturaElectronicaMensual.setFecha_vence(businessLocalDate);
        facturaElectronicaMensual.setFecha_inicial(this.firstDayOfMonth);
        facturaElectronicaMensual.setFecha_final(this.lastDayOfMonth);
        facturaElectronicaMensual.setEst_fact("C");
        facturaElectronicaMensual.setTotal_unidades(String.valueOf(this.itemsCount));
        facturaElectronicaMensual.setLogo(logo);
        if (LoanDocumentType.INVOICE.equals(this.documentType)) {
            facturaElectronicaMensual.setTipo_factura("1");
            facturaElectronicaMensual.setTip_doc(LoanDocumentType.INVOICE.getCode());
        } else {
            facturaElectronicaMensual.setTipo_factura("9");
            facturaElectronicaMensual.setTip_doc(LoanDocumentType.CREDIT_NOTE.getCode());
        }

        // INFORMATION AT COMPANY LEVEL
        final String taxInformation = "RESPONSABLE DEL IVA.  No Somos Grandes Contribuyentes. Autorretenedores Renta según Resol. No. 04314 may 16 de 20028.  Auterretenedores especiales según Decreto No. 2201 dic 30 de 2016.  Autorretenedores de ICA según Resol. No. 202150186360 del 22 de dic de 2021 Medellín";
        facturaElectronicaMensual.setInf_tributaria(taxInformation);
        facturaElectronicaMensual.setCantidad(BigDecimal.ONE);
        if (LegalForm.fromInt(this.clientLegalForm).isEntity()) {
            final String companyCountryCode = "CO";
            final String companyCountryName = "COLOMBIA";
            facturaElectronicaMensual.setNit_emisor(this.companyNIT);
            if ("NIT".equalsIgnoreCase(this.companyDocType)) {
                facturaElectronicaMensual.setTipo_docid("31");
            } else {
                facturaElectronicaMensual.setTipo_docid("13");
            }
            facturaElectronicaMensual.setNom_emisor(this.clientDisplayName);
            facturaElectronicaMensual.setCod_pais_tienda(companyCountryCode);
            facturaElectronicaMensual.setNom_pais_tienda(companyCountryName);
            facturaElectronicaMensual.setDep_tienda(this.companyDeptCode);
            facturaElectronicaMensual.setNom_dep_tienda(this.companyDeptName);
            facturaElectronicaMensual.setCod_mun_tienda("05001");
            facturaElectronicaMensual.setCiudad_tienda(this.companyCityName);
            facturaElectronicaMensual.setDireccion_tienda(this.companyAddress);
            facturaElectronicaMensual.setNombre_tienda(this.clientDisplayName);
            facturaElectronicaMensual.setTel_tienda(this.companyTelephone);
            facturaElectronicaMensual.setEmail_tienda(this.clientEmailAddress);
            facturaElectronicaMensual.setTipo_pers(1L);
            facturaElectronicaMensual.setCodigopostal(this.companyCityCode);
            facturaElectronicaMensual.setTelefono(this.companyTelephone);
            facturaElectronicaMensual.setEmail(this.clientEmailAddress);
            facturaElectronicaMensual.setId_cliente(this.clientIdNumber);
            facturaElectronicaMensual.setNombre_cliente(this.clientDisplayName);
        }

        // INFORMATION AT INDIVIDUAL CLIENT LEVEL
        if (LegalForm.fromInt(this.clientLegalForm).isPerson()) {
            facturaElectronicaMensual.setId_cliente(this.clientIdNumber);
            facturaElectronicaMensual.setTipo_docid("13");
            facturaElectronicaMensual.setTipo_pers(2L);
            facturaElectronicaMensual.setNombre_cliente(this.clientDisplayName);
            facturaElectronicaMensual.setApellido_cliente(this.clientLastName);
            facturaElectronicaMensual.setDireccion(this.clientAddress);
            facturaElectronicaMensual.setCiudad(this.clientCityName);
            facturaElectronicaMensual.setCodigopostal(this.clientCityCode);
            facturaElectronicaMensual.setTelefono(this.clientTelephone);
            facturaElectronicaMensual.setEmail(this.clientEmailAddress);
        }

        // INFORMATION AT TAX LEVEL
        facturaElectronicaMensual.setIva_codigo("01");
        facturaElectronicaMensual.setIva_name("IVA");
        facturaElectronicaMensual.setBase(BigDecimal.ZERO);
        facturaElectronicaMensual.setPorcentaje_impuesto(BigDecimal.ZERO);
        facturaElectronicaMensual.setImpuesto(BigDecimal.ZERO);
        facturaElectronicaMensual.setNota2("Estos valores corresponden a los cobros asociados a tu crédito: " + this.productTypeName);
        facturaElectronicaMensual.setTipo_prod(this.productTypeName);

        facturaElectronicaMensual.setPor_dto(this.totalPaid);
        facturaElectronicaMensual.setVal_dto(this.totalPaid);
        facturaElectronicaMensual.setTotal(this.totalPaid);
        facturaElectronicaMensual.setLoan_transaction_id(this.loanTransactionId);
        return facturaElectronicaMensual;
    }

    @AllArgsConstructor
    @Getter
    public enum LoanDocumentType {

        INVOICE("INVOIC"), //
        CREDIT_NOTE("NC"), //
        DEBIT_NOTE("ND"); //

        private final String code;
    }
}
