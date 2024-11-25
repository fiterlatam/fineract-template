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
public class LoanInvoiceData {

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
    private Long creditNoteCount;
    private LocalDate lastDayOfMonth;
    private LocalDate firstDayOfMonth;
    private LocalDate secondLastDayOfMonth;

    /** Loan outstanding amount fields */
    private BigDecimal outstandingPrincipal;
    private BigDecimal currentInterest;
    private BigDecimal overdueInterest;
    private BigDecimal outstandingPenalty;
    private BigDecimal outstandingMandatoryInsurance;
    private BigDecimal outstandingVoluntaryInsurance;
    private BigDecimal outstandingAval;
    private BigDecimal outstandingHonorarios;
    private BigDecimal totalOutstanding;
    private Integer loansCount;
    private Integer itemsCount;

    public FacturaElectronicaMensual toEntity() {
        final FacturaElectronicaMensual facturaElectronicaMensual = new FacturaElectronicaMensual();
        final LocalDate businessLocalDate = DateUtils.getBusinessLocalDate();
        int conceptCount = 0;
        if (this.outstandingPrincipal.compareTo(BigDecimal.ZERO) > 0) {
            conceptCount++;
        }
        if (this.currentInterest.compareTo(BigDecimal.ZERO) > 0) {
            conceptCount++;
        }
        if (this.overdueInterest.compareTo(BigDecimal.ZERO) > 0) {
            conceptCount++;
        }
        if (this.outstandingPenalty.compareTo(BigDecimal.ZERO) > 0) {
            conceptCount++;
        }
        if (this.outstandingMandatoryInsurance.compareTo(BigDecimal.ZERO) > 0) {
            conceptCount++;
        }
        if (this.outstandingVoluntaryInsurance.compareTo(BigDecimal.ZERO) > 0) {
            conceptCount++;
        }
        if (this.outstandingAval.compareTo(BigDecimal.ZERO) > 0) {
            conceptCount++;
        }
        if (this.outstandingHonorarios.compareTo(BigDecimal.ZERO) > 0) {
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
        LoanDocumentType tipoDoc;
        String tipoFactura;
        if ("Ajuste".equalsIgnoreCase(this.loanProductName)) {
            tipoDoc = LoanDocumentType.DEBIT_NOTE;
            tipoFactura = "9";
        } else if (this.creditNoteCount > 0) {
            tipoDoc = LoanDocumentType.CREDIT_NOTE;
            tipoFactura = "8";
        } else {
            tipoDoc = LoanDocumentType.INVOICE;
            tipoFactura = "1";
        }
        this.documentType = tipoDoc;
        facturaElectronicaMensual.setTip_doc(tipoDoc.code);
        facturaElectronicaMensual.setFecha_factura(businessLocalDate);
        facturaElectronicaMensual.setTipo_factura(tipoFactura);
        facturaElectronicaMensual.setMoneda("COP");
        facturaElectronicaMensual.setForma_pago("1");
        facturaElectronicaMensual.setMedio_pago("31");
        facturaElectronicaMensual.setFecha_vence(businessLocalDate);
        facturaElectronicaMensual.setFecha_inicial(this.firstDayOfMonth);
        facturaElectronicaMensual.setFecha_final(this.lastDayOfMonth);
        facturaElectronicaMensual.setEst_fact("C");
        facturaElectronicaMensual.setFec_facafect(businessLocalDate);
        facturaElectronicaMensual.setTotal_unidades(String.valueOf(this.itemsCount));
        facturaElectronicaMensual.setLogo(logo);

        // INFORMATION AT COMPANY LEVEL
        final String taxInformation = "RESPONSABLE DEL IVA.  No Somos Grandes Contribuyentes. Autorretenedores Renta según Resol. No. 04314 may 16 de 20028.  Auterretenedores especiales según Decreto No. 2201 dic 30 de 2016.  Autorretenedores de ICA según Resol. No. 202150186360 del 22 de dic de 2021 Medellín";
        facturaElectronicaMensual.setInf_tributaria(taxInformation);
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

        facturaElectronicaMensual.setPor_dto(this.outstandingPrincipal);
        facturaElectronicaMensual.setVal_dto(this.outstandingPrincipal);
        facturaElectronicaMensual.setTotal(this.outstandingPrincipal);
        return facturaElectronicaMensual;
    }

    private BigDecimal defaultToZeroIfNull(final BigDecimal possibleNullValue) {
        BigDecimal value = BigDecimal.ZERO;
        if (possibleNullValue != null) {
            value = possibleNullValue;
        }
        return value;
    }

    public BigDecimal getOutstandingPrincipal() {
        return defaultToZeroIfNull(this.outstandingPrincipal);
    }

    public BigDecimal getCurrentInterest() {
        return defaultToZeroIfNull(this.currentInterest);
    }

    public BigDecimal getOverdueInterest() {
        return defaultToZeroIfNull(this.overdueInterest);
    }

    public BigDecimal getOutstandingPenalty() {
        return defaultToZeroIfNull(this.outstandingPenalty);
    }

    public BigDecimal getOutstandingMandatoryInsurance() {
        return defaultToZeroIfNull(this.outstandingMandatoryInsurance);
    }

    public BigDecimal getOutstandingVoluntaryInsurance() {
        return defaultToZeroIfNull(this.outstandingVoluntaryInsurance);
    }

    public BigDecimal getOutstandingAval() {
        return defaultToZeroIfNull(this.outstandingAval);
    }

    public BigDecimal getOutstandingHonorarios() {
        return defaultToZeroIfNull(this.outstandingHonorarios);
    }

    public BigDecimal getTotalOutstanding() {
        return defaultToZeroIfNull(this.totalOutstanding);
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
