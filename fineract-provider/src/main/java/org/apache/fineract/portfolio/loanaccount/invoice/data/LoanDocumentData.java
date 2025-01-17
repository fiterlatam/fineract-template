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
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.portfolio.client.domain.LegalForm;
import org.apache.fineract.portfolio.loanaccount.invoice.domain.FacturaElectronicaMensual;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProductType;

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
    private String clientFirstName;
    private String clientMiddleName;
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

        BigDecimal totalPaidAmount = this.interestPaid.add(this.mandatoryInsurancePaid).add(this.voluntaryInsurancePaid)
                .add(this.honorariosPaid).add(this.penaltyChargesPaid);
        // INFORMATION AT RESOLUTION LEVEL
        facturaElectronicaMensual.setCreatedDate(DateUtils.getAuditOffsetDateTime());
        facturaElectronicaMensual.setNumResolucion(this.billingResolutionNumber);
        facturaElectronicaMensual.setPrefijo(this.billingPrefix);
        facturaElectronicaMensual.setFecDesde(this.firstDayOfMonth);
        facturaElectronicaMensual.setFecHasta(this.secondLastDayOfMonth);
        facturaElectronicaMensual.setConsecutivoInicial(this.rangeStartNumber);
        facturaElectronicaMensual.setConsecutivoFinal(this.rangeEndNumber);
        facturaElectronicaMensual.setClaveTecnica(this.technicalKey);
        facturaElectronicaMensual.setNota(this.nota);

        // INFORMATION AT INVOICE LEVEL
        final String logo = "3";
        facturaElectronicaMensual.setFechaFactura(businessLocalDate);
        facturaElectronicaMensual.setMoneda("COP");
        facturaElectronicaMensual.setFormaPago("1");
        facturaElectronicaMensual.setMedioPago("31");
        facturaElectronicaMensual.setFechaVence(businessLocalDate);
        facturaElectronicaMensual.setFechaInicial(this.firstDayOfMonth);
        facturaElectronicaMensual.setFechaFinal(this.lastDayOfMonth);
        facturaElectronicaMensual.setEstFact("C");
        facturaElectronicaMensual.setTotalUnidades(String.valueOf(this.itemsCount));
        facturaElectronicaMensual.setLogo(logo);
        if (LoanDocumentType.INVOICE.equals(this.documentType)) {
            facturaElectronicaMensual.setTipoFactura("1");
            facturaElectronicaMensual.setTipDoc(LoanDocumentType.INVOICE.getCode());
        } else {
            facturaElectronicaMensual.setTipoFactura("9");
            facturaElectronicaMensual.setTipDoc(LoanDocumentType.CREDIT_NOTE.getCode());
        }

        // INFORMATION AT COMPANY LEVEL
        final String taxInformation = StringUtils.stripAccents(
                "RESPONSABLE DEL IVA.  No Somos Grandes Contribuyentes. Autorretenedores Renta según Resol. No. 04314 may 16 de 20028.  Auterretenedores especiales según Decreto No. 2201 dic 30 de 2016.  Autorretenedores de ICA según Resol. No. 202150186360 del 22 de dic de 2021 Medellín.");
        facturaElectronicaMensual.setInfTributaria(taxInformation);
        facturaElectronicaMensual.setCantidad(BigDecimal.ONE);
        if (LegalForm.fromInt(this.clientLegalForm).isEntity()) {
            if ("NIT".equalsIgnoreCase(this.companyDocType)) {
                facturaElectronicaMensual.setTipoDocid("31");
            } else {
                facturaElectronicaMensual.setTipoDocid("13");
            }
            facturaElectronicaMensual.setTipoPers(1L);
            facturaElectronicaMensual.setIdCliente(this.clientIdNumber);
            facturaElectronicaMensual.setNombreCliente(this.getFirstNameAndMiddleName());
        }

        // INFORMATION AT INDIVIDUAL CLIENT LEVEL
        if (LegalForm.fromInt(this.clientLegalForm).isPerson()) {
            facturaElectronicaMensual.setIdCliente(this.clientIdNumber);
            facturaElectronicaMensual.setTipoDocid("13");
            facturaElectronicaMensual.setTipoPers(2L);
            facturaElectronicaMensual.setNombreCliente(this.getFirstNameAndMiddleName());
            facturaElectronicaMensual.setApellidoCliente(this.clientLastName);
            facturaElectronicaMensual.setDireccion(this.clientAddress);
            facturaElectronicaMensual.setCiudad(this.clientCityName);
            facturaElectronicaMensual.setEmail(this.clientEmailAddress);
        }

        // INFORMATION AT TAX LEVEL
        facturaElectronicaMensual.setIvaCodigo("01");
        facturaElectronicaMensual.setIvaName("IVA");
        facturaElectronicaMensual.setBase(totalPaidAmount);

        if (LoanProductType.SUMAS_PAY.getCode().equals(this.productTypeName)) {
            facturaElectronicaMensual.setNota2("Estos valores corresponden a los cobros asociados a tu Credito Sumas Pay.");
        }
        facturaElectronicaMensual.setTipoProd(this.productTypeName);

        facturaElectronicaMensual.setPorDto(totalPaidAmount);
        facturaElectronicaMensual.setValDto(totalPaidAmount);
        facturaElectronicaMensual.setTotal(totalPaidAmount);
        facturaElectronicaMensual.setLoanTransactionId(this.loanTransactionId);
        facturaElectronicaMensual.setTelefono(this.clientTelephone);

        // SU+ Constant Fields
        facturaElectronicaMensual.setNitEmisor("800139398-6");
        String companyName = StringUtils.stripAccents("Intercrédito de Colombia S.A.S");
        facturaElectronicaMensual.setNomEmisor(companyName);

        // Static values for specified fields
        facturaElectronicaMensual.setCodPaisTienda("CO");
        facturaElectronicaMensual.setNomPaisTienda("COLOMBIA");
        facturaElectronicaMensual.setDepTienda("5");
        facturaElectronicaMensual.setNomDepTienda("ANTIOQUIA");
        facturaElectronicaMensual.setCodMunTienda("5001");
        facturaElectronicaMensual.setCiudadTienda("MEDELLIN");
        facturaElectronicaMensual.setDireccionTienda("Calle 4 SUR 43AA 30 OFICINA 901");
        facturaElectronicaMensual.setTelTienda("18000187373");
        facturaElectronicaMensual.setNombreTienda(null);
        facturaElectronicaMensual.setEmailTienda(null);

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

    private String getFirstNameAndMiddleName() {
        if (StringUtils.isAllBlank(this.clientFirstName, this.clientMiddleName)) {
            return StringUtils.stripAccents(this.clientDisplayName);
        }
        return StringUtils.stripAccents(
                String.format("%s %s", Objects.toString(this.clientFirstName, ""), Objects.toString(this.clientMiddleName, "")));
    }
}
