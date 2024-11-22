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
    private String companyDeptCode;
    private String companyDeptName;
    private String companyMunCode;
    private String companyMunName;
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
    private String billingPrefix;
    private String billingResolutionNumber;
    private Long rangeStartNumber;
    private Long rangeEndNumber;
    private Long lastInvoiceNumber;
    private Long lastCreditNoteNumber;
    private Long lastDebitNoteNumber;
    private LocalDate lastDayOfMonth;
    private LocalDate firstDayOfMonth;
    private LocalDate secondLastDayOfMonth;

    /** Invoice fields */
    private Long documentNumber;


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
        final String documentNumberString = String.valueOf(documentNumber);
        final String loansCountString = String.valueOf(this.loansCount);

        // INFORMATION AT RESOLUTION LEVEL
        facturaElectronicaMensual.setCreatedDate(DateUtils.getAuditOffsetDateTime());
        facturaElectronicaMensual.setNum_resolucion(this.billingResolutionNumber);
        facturaElectronicaMensual.setPrefijo(this.billingPrefix);
        facturaElectronicaMensual.setFec_desde(this.firstDayOfMonth);
        facturaElectronicaMensual.setFec_hasta(this.secondLastDayOfMonth);
        facturaElectronicaMensual.setConsecutivo_inicial(this.rangeStartNumber);
        facturaElectronicaMensual.setConsecutivo_final(this.rangeEndNumber);
        facturaElectronicaMensual.setClave_tecnica("4917a53d0ca4bb8eae83977f8163a18645533d4382ae22e709e0c249d497b7b8");
        final String nota = documentNumberString + " " + facturaElectronicaMensual.getFec_desde() + " " + facturaElectronicaMensual.getFec_hasta() + " " + facturaElectronicaMensual.getConsecutivo_inicial() + " " + facturaElectronicaMensual.getConsecutivo_final() + " Habilitación Facturación Electrónica";
        facturaElectronicaMensual.setNota(nota);

        // INFORMATION AT INVOICE LEVEL
        final String logo = "3";
        facturaElectronicaMensual.setNumero_doc(documentNumberString);
        String tipoDoc = "INVOIC";
        String tipoFactura = "1";
        if("Ajuste".equalsIgnoreCase(this.loanProductName)){
            tipoDoc = "ND";
            tipoFactura = "9";
        }
        facturaElectronicaMensual.setTip_doc(tipoDoc);
        facturaElectronicaMensual.setFecha_factura(businessLocalDate);
        facturaElectronicaMensual.setTipo_factura(tipoFactura);
        facturaElectronicaMensual.setMoneda("COP");
        facturaElectronicaMensual.setForma_pago("1");
        facturaElectronicaMensual.setMedio_pago("31");
        facturaElectronicaMensual.setFecha_vence(businessLocalDate);
        facturaElectronicaMensual.setFecha_inicial(this.firstDayOfMonth);
        facturaElectronicaMensual.setFecha_final(this.lastDayOfMonth);
        facturaElectronicaMensual.setEst_fact("C");
        facturaElectronicaMensual.setNum_facafect(documentNumberString);
        facturaElectronicaMensual.setFec_facafect(businessLocalDate);
        facturaElectronicaMensual.setTotal_unidades(loansCountString);
        facturaElectronicaMensual.setLogo(logo);

        // INFORMATION AT COMPANY LEVEL
        if(LegalForm.fromInt(this.clientLegalForm).isEntity()) {
            final String taxInformation = "NIT " + this.companyNIT + " DV 0";
            final String companyCountryCode = "CO";
            final String companyCountryName = "COLOMBIA";
            facturaElectronicaMensual.setNit_emisor(this.companyNIT);
            facturaElectronicaMensual.setNom_emisor(this.clientDisplayName);
            facturaElectronicaMensual.setInf_tributaria(taxInformation);
            facturaElectronicaMensual.setCod_pais_tienda(companyCountryCode);
            facturaElectronicaMensual.setNom_pais_tienda(companyCountryName);
            facturaElectronicaMensual.setDep_tienda(this.companyDeptCode);
            facturaElectronicaMensual.setNom_dep_tienda(this.companyDeptName);
            facturaElectronicaMensual.setCod_mun_tienda(this.companyMunCode);
            facturaElectronicaMensual.setCiudad_tienda(this.clientDisplayName);
            facturaElectronicaMensual.setDireccion_tienda(this.companyAddress);
            facturaElectronicaMensual.setNombre_tienda(this.clientDisplayName);
            facturaElectronicaMensual.setTel_tienda(this.companyTelephone);
            facturaElectronicaMensual.setEmail_tienda(this.clientEmailAddress);
        }

        // INFORMATION AT INDIVIDUAL CLIENT LEVEL
        if(LegalForm.fromInt(this.clientLegalForm).isPerson()) {
            facturaElectronicaMensual.setId_cliente(this.clientIdNumber);
            facturaElectronicaMensual.setTipo_docid("13");
            facturaElectronicaMensual.setTipo_pers(1L);
            facturaElectronicaMensual.setNombre_cliente(this.clientDisplayName);
            facturaElectronicaMensual.setApellido_cliente(this.clientLastName);
            facturaElectronicaMensual.setDireccion(this.clientAddress);
            facturaElectronicaMensual.setCiudad(this.clientCityName);
            facturaElectronicaMensual.setCodigopostal("codigopostal");
            facturaElectronicaMensual.setTelefono(this.clientTelephone);
            facturaElectronicaMensual.setEmail(this.clientEmailAddress);
        }

        // INFORMATION AT ITEM LEVEL
        facturaElectronicaMensual.setPosicion(1L);
        facturaElectronicaMensual.setCantidad(BigDecimal.valueOf(1));
        facturaElectronicaMensual.setCosto_total(this.totalOutstanding);
        facturaElectronicaMensual.setPrecio_unitario(this.totalOutstanding);
        facturaElectronicaMensual.setSku("sku");
        facturaElectronicaMensual.setNom_articulo("nom_articulo");
        facturaElectronicaMensual.setReferencia("referencia");
        facturaElectronicaMensual.setId_mandante("id_mandante");
        facturaElectronicaMensual.setDescripcion_mandante("descripcion_mandante");
        facturaElectronicaMensual.setCodigo_descuento("0");
        facturaElectronicaMensual.setPorcentajedescuento(BigDecimal.ZERO);
        facturaElectronicaMensual.setDescuento(BigDecimal.ZERO);
        facturaElectronicaMensual.setPorcentaje_impuesto_item(BigDecimal.ZERO);
        facturaElectronicaMensual.setImpuesto_item(BigDecimal.ZERO);

        // INFORMATION AT TAX LEVEL
        facturaElectronicaMensual.setIva_codigo("01");
        facturaElectronicaMensual.setIva_name("IVA");
        facturaElectronicaMensual.setBase(this.totalOutstanding);
        facturaElectronicaMensual.setPorcentaje_impuesto(BigDecimal.ZERO);

        // INFORMATION AT TOTAL LEVEL
        facturaElectronicaMensual.setPor_dto(this.totalOutstanding);
        facturaElectronicaMensual.setVal_dto(this.totalOutstanding);
        facturaElectronicaMensual.setTotal(this.totalOutstanding);
        facturaElectronicaMensual.setNota2("Estos valores corresponden a los cobros asociados a tu crédito: " + this.productTypeName);

        // INFORMATION AT EXCHANGE RATE LEVEL
        facturaElectronicaMensual.setTas_cambmon(null);
        facturaElectronicaMensual.setCod_moncamb(null);
        facturaElectronicaMensual.setTot_basimpo(null);
        facturaElectronicaMensual.setTot_facmon(null);
        facturaElectronicaMensual.setTip_factexport(null);
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
}
