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
package org.apache.fineract.portfolio.loanaccount.service;

import com.google.common.base.Splitter;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

import lombok.RequiredArgsConstructor;
import org.apache.fineract.custom.portfolio.customcharge.data.CustomChargeEntityData;
import org.apache.fineract.custom.portfolio.customcharge.data.CustomChargeTypeData;
import org.apache.fineract.custom.portfolio.customcharge.data.CustomChargeTypeMapData;
import org.apache.fineract.custom.portfolio.customcharge.exception.CustomChargeTypeMapNotFoundException;
import org.apache.fineract.custom.portfolio.customcharge.service.CustomChargeEntityReadWritePlatformService;
import org.apache.fineract.custom.portfolio.customcharge.service.CustomChargeTypeMapReadWritePlatformService;
import org.apache.fineract.custom.portfolio.customcharge.service.CustomChargeTypeReadWritePlatformService;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.domain.ExternalId;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.core.service.ExternalIdFactory;
import org.apache.fineract.organisation.monetary.domain.MoneyHelper;
import org.apache.fineract.portfolio.charge.domain.Charge;
import org.apache.fineract.portfolio.charge.domain.ChargeCalculationType;
import org.apache.fineract.portfolio.charge.domain.ChargePaymentMode;
import org.apache.fineract.portfolio.charge.domain.ChargeRepositoryWrapper;
import org.apache.fineract.portfolio.charge.domain.ChargeTimeType;
import org.apache.fineract.portfolio.charge.exception.LoanChargeCannotBeAddedException;
import org.apache.fineract.portfolio.charge.exception.LoanChargeWithoutMandatoryFieldException;
import org.apache.fineract.portfolio.loanaccount.api.LoanApiConstants;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanCharge;
import org.apache.fineract.portfolio.loanaccount.domain.LoanChargeRepository;
import org.apache.fineract.portfolio.loanaccount.domain.LoanDisbursementDetails;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTrancheDisbursementCharge;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProduct;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProductRepository;
import org.apache.fineract.portfolio.loanproduct.exception.LoanProductNotFoundException;

@RequiredArgsConstructor
public class LoanChargeAssembler {

    private final FromJsonHelper fromApiJsonHelper;
    private final ChargeRepositoryWrapper chargeRepository;
    private final LoanChargeRepository loanChargeRepository;
    private final LoanProductRepository loanProductRepository;
    private final ExternalIdFactory externalIdFactory;
    private final CustomChargeEntityReadWritePlatformService customChargeService;
    private final CustomChargeTypeReadWritePlatformService customChargeTypeService;
    private final CustomChargeTypeMapReadWritePlatformService customChargeTypeMapService;

    public Set<LoanCharge> fromParsedJson(final JsonElement element, List<LoanDisbursementDetails> disbursementDetails) {
        JsonArray jsonDisbursement = this.fromApiJsonHelper.extractJsonArrayNamed("disbursementData", element);
        List<Long> disbursementChargeIds = new ArrayList<>();

        if (jsonDisbursement != null && jsonDisbursement.size() > 0) {
            for (int i = 0; i < jsonDisbursement.size(); i++) {
                final JsonObject jsonObject = jsonDisbursement.get(i).getAsJsonObject();
                if (jsonObject != null && jsonObject.getAsJsonPrimitive(LoanApiConstants.loanChargeIdParameterName) != null) {
                    String chargeIds = jsonObject.getAsJsonPrimitive(LoanApiConstants.loanChargeIdParameterName).getAsString();
                    if (chargeIds != null) {
                        if (chargeIds.indexOf(",") != -1) {
                            Iterable<String> chargeId = Splitter.on(',').split(chargeIds);
                            for (String loanChargeId : chargeId) {
                                disbursementChargeIds.add(Long.parseLong(loanChargeId));
                            }
                        } else {
                            disbursementChargeIds.add(Long.parseLong(chargeIds));
                        }
                    }

                }
            }
        }

        final Set<LoanCharge> loanCharges = new LinkedHashSet<>();
        final BigDecimal principal = this.fromApiJsonHelper.extractBigDecimalWithLocaleNamed("principal", element);
        final Integer numberOfRepayments = this.fromApiJsonHelper.extractIntegerWithLocaleNamed("numberOfRepayments", element);
        final Long productId = this.fromApiJsonHelper.extractLongNamed("productId", element);
        final LoanProduct loanProduct = this.loanProductRepository.findById(productId)
                .orElseThrow(() -> new LoanProductNotFoundException(productId));
        final boolean isMultiDisbursal = loanProduct.isMultiDisburseLoan();
        LocalDate expectedDisbursementDate = null;

        if (element.isJsonObject()) {
            final JsonObject topLevelJsonElement = element.getAsJsonObject();
            final String dateFormat = this.fromApiJsonHelper.extractDateFormatParameter(topLevelJsonElement);
            final Locale locale = this.fromApiJsonHelper.extractLocaleParameter(topLevelJsonElement);
            if (topLevelJsonElement.has("charges") && topLevelJsonElement.get("charges").isJsonArray()) {
                final JsonArray array = topLevelJsonElement.get("charges").getAsJsonArray();
                for (int i = 0; i < array.size(); i++) {

                    final JsonObject loanChargeElement = array.get(i).getAsJsonObject();

                    final Long id = this.fromApiJsonHelper.extractLongNamed("id", loanChargeElement);
                    final Long chargeId = this.fromApiJsonHelper.extractLongNamed("chargeId", loanChargeElement);
                    BigDecimal amount = this.fromApiJsonHelper.extractBigDecimalNamed("amount", loanChargeElement, locale);

                    final Integer chargeTimeType = this.fromApiJsonHelper.extractIntegerNamed("chargeTimeType", loanChargeElement, locale);
                    final Integer chargeCalculationType = this.fromApiJsonHelper.extractIntegerNamed("chargeCalculationType",
                            loanChargeElement, locale);

                    final LocalDate dueDate = this.fromApiJsonHelper.extractLocalDateNamed("dueDate", loanChargeElement, dateFormat,
                            locale);
                    final Integer chargePaymentMode = this.fromApiJsonHelper.extractIntegerNamed("chargePaymentMode", loanChargeElement,
                            locale);
                    final String externalIdStr = this.fromApiJsonHelper.extractStringNamed("externalId", loanChargeElement);
                    final ExternalId externalId = externalIdFactory.create(externalIdStr);
                    if (id == null) {
                        final Charge chargeDefinition = this.chargeRepository.findOneWithNotFoundDetection(chargeId);

                        if (chargeDefinition.isOverdueInstallment()) {

                            final String defaultUserMessage = "Installment charge cannot be added to the loan.";
                            throw new LoanChargeCannotBeAddedException("loanCharge", "overdue.charge", defaultUserMessage, null,
                                    chargeDefinition.getName());
                        }

                        ChargeTimeType chargeTime = null;
                        if (chargeTimeType != null) {
                            chargeTime = ChargeTimeType.fromInt(chargeTimeType);
                        }
                        ChargeCalculationType chargeCalculation = null;
                        if (chargeCalculationType != null) {
                            chargeCalculation = ChargeCalculationType.fromInt(chargeCalculationType);
                        }

                        boolean getPercentageAmountFromTable = chargeDefinition.isGetPercentageFromTable();
                        if (getPercentageAmountFromTable) {
                            amount = getAmountPerentageFromCustomChargeTable(chargeCalculation, numberOfRepayments);
                        }

                        ChargePaymentMode chargePaymentModeEnum = null;
                        if (chargePaymentMode != null) {
                            chargePaymentModeEnum = ChargePaymentMode.fromInt(chargePaymentMode);
                        }
                        if (!isMultiDisbursal) {
                            final LoanCharge loanCharge = createNewWithoutLoan(chargeDefinition, principal, amount, chargeTime,
                                    chargeCalculation, dueDate, chargePaymentModeEnum, numberOfRepayments, externalId, getPercentageAmountFromTable);
                            loanCharges.add(loanCharge);
                        } else {
                            if (topLevelJsonElement.has("disbursementData") && topLevelJsonElement.get("disbursementData").isJsonArray()) {
                                final JsonArray disbursementArray = topLevelJsonElement.get("disbursementData").getAsJsonArray();
                                if (disbursementArray.size() > 0) {
                                    JsonObject disbursementDataElement = disbursementArray.get(0).getAsJsonObject();
                                    expectedDisbursementDate = this.fromApiJsonHelper.extractLocalDateNamed(
                                            LoanApiConstants.expectedDisbursementDateParameterName, disbursementDataElement, dateFormat,
                                            locale);
                                }
                            }

                            if (ChargeTimeType.DISBURSEMENT.getValue().equals(chargeDefinition.getChargeTimeType())) {
                                for (LoanDisbursementDetails disbursementDetail : disbursementDetails) {
                                    LoanTrancheDisbursementCharge loanTrancheDisbursementCharge = null;
                                    if (chargeDefinition.isPercentageOfApprovedAmount()
                                            && disbursementDetail.expectedDisbursementDateAsLocalDate().equals(expectedDisbursementDate)) {
                                        final LoanCharge loanCharge = createNewWithoutLoan(chargeDefinition, principal, amount, chargeTime,
                                                chargeCalculation, dueDate, chargePaymentModeEnum, numberOfRepayments, externalId, getPercentageAmountFromTable);
                                        loanCharges.add(loanCharge);
                                        if (loanCharge.isTrancheDisbursementCharge()) {
                                            loanTrancheDisbursementCharge = new LoanTrancheDisbursementCharge(loanCharge,
                                                    disbursementDetail);
                                            loanCharge.updateLoanTrancheDisbursementCharge(loanTrancheDisbursementCharge);
                                        }
                                    } else {
                                        if (disbursementDetail.expectedDisbursementDateAsLocalDate().equals(expectedDisbursementDate)) {
                                            final LoanCharge loanCharge = createNewWithoutLoan(chargeDefinition,
                                                    disbursementDetail.principal(), amount, chargeTime, chargeCalculation,
                                                    disbursementDetail.expectedDisbursementDateAsLocalDate(), chargePaymentModeEnum,
                                                    numberOfRepayments, externalId, getPercentageAmountFromTable);
                                            loanCharges.add(loanCharge);
                                            if (loanCharge.isTrancheDisbursementCharge()) {
                                                loanTrancheDisbursementCharge = new LoanTrancheDisbursementCharge(loanCharge,
                                                        disbursementDetail);
                                                loanCharge.updateLoanTrancheDisbursementCharge(loanTrancheDisbursementCharge);
                                            }
                                        }
                                    }
                                }
                            } else if (ChargeTimeType.TRANCHE_DISBURSEMENT.getValue().equals(chargeDefinition.getChargeTimeType())) {
                                LoanTrancheDisbursementCharge loanTrancheDisbursementCharge = null;
                                for (LoanDisbursementDetails disbursementDetail : disbursementDetails) {
                                    if (ChargeTimeType.TRANCHE_DISBURSEMENT.getValue().equals(chargeDefinition.getChargeTimeType())) {
                                        final LoanCharge loanCharge = createNewWithoutLoan(chargeDefinition, disbursementDetail.principal(),
                                                amount, chargeTime, chargeCalculation,
                                                disbursementDetail.expectedDisbursementDateAsLocalDate(), chargePaymentModeEnum,
                                                numberOfRepayments, externalId, getPercentageAmountFromTable);
                                        loanCharges.add(loanCharge);
                                        loanTrancheDisbursementCharge = new LoanTrancheDisbursementCharge(loanCharge, disbursementDetail);
                                        loanCharge.updateLoanTrancheDisbursementCharge(loanTrancheDisbursementCharge);
                                    }
                                }
                            } else {
                                final LoanCharge loanCharge = createNewWithoutLoan(chargeDefinition, principal, amount, chargeTime,
                                        chargeCalculation, dueDate, chargePaymentModeEnum, numberOfRepayments, externalId, getPercentageAmountFromTable);
                                loanCharges.add(loanCharge);
                            }
                        }
                    } else {
                        final Long loanChargeId = id;
                        final LoanCharge loanCharge = this.loanChargeRepository.findById(loanChargeId).orElse(null);
                        if (loanCharge != null) {
                            if (!loanCharge.isTrancheDisbursementCharge() || disbursementChargeIds.contains(loanChargeId)) {
                                loanCharge.update(amount, dueDate, numberOfRepayments);
                                loanCharges.add(loanCharge);
                            }
                        }
                    }
                }
            }
        }
        for (LoanCharge loanCharge : loanCharges) {
            if (loanCharge.getApplicableFromInstallment() == null || loanCharge.getApplicableFromInstallment() == 1) {
                loanCharge.setApplicableFromInstallment(1);
            }
        }
        return loanCharges;
    }

    public Set<Charge> getNewLoanTrancheCharges(final JsonElement element) {
        final Set<Charge> associatedChargesForLoan = new HashSet<>();
        if (element.isJsonObject()) {
            final JsonObject topLevelJsonElement = element.getAsJsonObject();
            if (topLevelJsonElement.has("charges") && topLevelJsonElement.get("charges").isJsonArray()) {
                final JsonArray array = topLevelJsonElement.get("charges").getAsJsonArray();
                for (int i = 0; i < array.size(); i++) {
                    final JsonObject loanChargeElement = array.get(i).getAsJsonObject();
                    final Long id = this.fromApiJsonHelper.extractLongNamed("id", loanChargeElement);
                    final Long chargeId = this.fromApiJsonHelper.extractLongNamed("chargeId", loanChargeElement);
                    if (id == null) {
                        final Charge chargeDefinition = this.chargeRepository.findOneWithNotFoundDetection(chargeId);
                        if (chargeDefinition.getChargeTimeType().equals(ChargeTimeType.TRANCHE_DISBURSEMENT.getValue())) {
                            associatedChargesForLoan.add(chargeDefinition);
                        }
                    }
                }
            }
        }
        return associatedChargesForLoan;
    }

    public LoanCharge createNewFromJson(final Loan loan, final Charge chargeDefinition, final JsonCommand command) {
        final LocalDate dueDate = command.localDateValueOfParameterNamed("dueDate");
        if (chargeDefinition.getChargeTimeType().equals(ChargeTimeType.SPECIFIED_DUE_DATE.getValue()) && dueDate == null) {
            final String defaultUserMessage = "Loan charge is missing due date.";
            throw new LoanChargeWithoutMandatoryFieldException("loanCharge", "dueDate", defaultUserMessage, chargeDefinition.getId(),
                    chargeDefinition.getName());
        }
        return createNewFromJson(loan, chargeDefinition, command, dueDate);
    }

    public LoanCharge createNewFromJson(final Loan loan, final Charge chargeDefinition, final JsonCommand command,
            final LocalDate dueDate) {
        final Locale locale = command.extractLocale();
        BigDecimal amount = command.bigDecimalValueOfParameterNamed("amount", locale);

        final ChargeTimeType chargeTime = null;
        final ChargeCalculationType chargeCalculation = null;
        final ChargePaymentMode chargePaymentMode = null;
        BigDecimal amountPercentageAppliedTo = BigDecimal.ZERO;

        ChargeCalculationType chargeCalculationType = ChargeCalculationType.fromInt(chargeDefinition.getChargeCalculation());
        boolean getPercentageAmountFromTable = chargeDefinition.isGetPercentageFromTable();
        if (getPercentageAmountFromTable) {
            amount = getAmountPerentageFromCustomChargeTable(chargeCalculationType, loan.getNumberOfRepayments());
        }

        // Ammend amount components as configred
        if (chargeCalculationType.isFlat()) {
            amountPercentageAppliedTo = amountPercentageAppliedTo.add(chargeDefinition.getAmount());
        }

        if (chargeCalculationType.isPercentageOfDisbursement()) {
            if (chargeCalculationType.isCustomPercentageBasedDistributedCharge()) {
                amountPercentageAppliedTo = amountPercentageAppliedTo.add(loan.getPrincipal().getAmount());
            } else {
                amountPercentageAppliedTo = amountPercentageAppliedTo.add(loan.getDisbursedAmount());
            }
        }

        if (chargeCalculationType.isPercentageOfInstallmentPrincipal()) {
            if (command.hasParameter("principal")) {
                amountPercentageAppliedTo = amountPercentageAppliedTo.add(command.bigDecimalValueOfParameterNamed("principal"));
            } else {
                amountPercentageAppliedTo = amountPercentageAppliedTo.add(loan.getPrincipal().getAmount());
            }
        }

        if (chargeCalculationType.isPercentageOfInstallmentInterest()) {
            if (command.hasParameter("interest")) {
                amountPercentageAppliedTo = amountPercentageAppliedTo.add(command.bigDecimalValueOfParameterNamed("interest"));
            } else {
                amountPercentageAppliedTo = amountPercentageAppliedTo.add(loan.getTotalInterest());
            }
        }

        if (chargeCalculationType.isCustomPercentageOfOutstandingPrincipalCharge()) {
            if (command.hasParameter("principal")) {
                amountPercentageAppliedTo = amountPercentageAppliedTo.add(command.bigDecimalValueOfParameterNamed("principal"));
            } else {
                amountPercentageAppliedTo = amountPercentageAppliedTo.add(loan.getPrincipal().getAmount());
            }
        }


        BigDecimal loanCharge = BigDecimal.ZERO;
        if (ChargeTimeType.fromInt(chargeDefinition.getChargeTimeType()).equals(ChargeTimeType.INSTALMENT_FEE)) {
            BigDecimal percentage = amount;
            if (percentage == null) {
                percentage = chargeDefinition.getAmount();
            }
            if (chargeCalculationType.isCustomPercentageBasedDistributedCharge()) {
                loanCharge = percentageOf(loan.getPrincipal().getAmount(), amount);
            } else {
                loanCharge = loan.calculatePerInstallmentChargeAmount(ChargeCalculationType.fromInt(chargeDefinition.getChargeCalculation()),
                        percentage, null, chargeDefinition.getParentChargeId());
            }
        }

        // If charge type is specified due date and loan is multi disburment
        // loan.
        // Then we need to get as of this loan charge due date how much amount
        // disbursed.
        if (chargeDefinition.getChargeTimeType().equals(ChargeTimeType.SPECIFIED_DUE_DATE.getValue()) && loan.isMultiDisburmentLoan()) {
            amountPercentageAppliedTo = BigDecimal.ZERO;
            for (final LoanDisbursementDetails loanDisbursementDetails : loan.getDisbursementDetails()) {
                if (!DateUtils.isAfter(loanDisbursementDetails.expectedDisbursementDate(), dueDate)) {
                    amountPercentageAppliedTo = amountPercentageAppliedTo.add(loanDisbursementDetails.principal());
                }
            }
        }

        ExternalId externalId = externalIdFactory.createFromCommand(command, "externalId");
        return new LoanCharge(loan, chargeDefinition, amountPercentageAppliedTo, amount, chargeTime, chargeCalculation, dueDate,
                chargePaymentMode, null, loanCharge, externalId, getPercentageAmountFromTable);
    }

    /*
     * loanPrincipal is required for charges that are percentage based
     */
    public LoanCharge createNewWithoutLoan(final Charge chargeDefinition, final BigDecimal loanPrincipal, final BigDecimal amount,
            final ChargeTimeType chargeTime, final ChargeCalculationType chargeCalculation, final LocalDate dueDate,
            final ChargePaymentMode chargePaymentMode, final Integer numberOfRepayments, final ExternalId externalId, boolean getPercentageAmountFromTable) {
        return new LoanCharge(null, chargeDefinition, loanPrincipal, amount, chargeTime, chargeCalculation, dueDate, chargePaymentMode,
                numberOfRepayments, BigDecimal.ZERO, externalId, getPercentageAmountFromTable);
    }

    private BigDecimal percentageOf(final BigDecimal value, final BigDecimal percentage) {

        BigDecimal percentageOf = BigDecimal.ZERO;

        if (value.compareTo(BigDecimal.ZERO) > 0) {
            final MathContext mc = MoneyHelper.getMathContext();
            final BigDecimal multiplicand = percentage.divide(BigDecimal.valueOf(100L), mc);
            percentageOf = value.multiply(multiplicand, mc);
        }
        return percentageOf;
    }

    private BigDecimal getAmountPerentageFromCustomChargeTable(ChargeCalculationType type, Integer numberOfInstallments) {
        BigDecimal percentage = BigDecimal.ZERO;
        boolean found = false;
        List<CustomChargeEntityData> customChargeEntityDataList = this.customChargeService.findByIsExternalService(false);
        for (CustomChargeEntityData entity : customChargeEntityDataList) {
            if ((entity.getName().equalsIgnoreCase("Insurance") && (type.isPercentageBasedMandatoryInsurance() || type.isCustomPercentageOfOutstandingPrincipalCharge()))
            || (entity.getName().equalsIgnoreCase("Aval") && (type.isPercentageBasedMandatoryInsurance() || type.isCustomPercentageOfOutstandingPrincipalCharge()))) {
                List<CustomChargeTypeData> customChargeTypeDataList = customChargeTypeService.findAllByEntityId(entity.getId());
                for (CustomChargeTypeData customChargeTypeData : customChargeTypeDataList) {
                    List<CustomChargeTypeMapData> customChargeTypeMapDataList = this.customChargeTypeMapService.findAllActive(customChargeTypeData.getId());
                    if (customChargeTypeMapDataList != null && !customChargeTypeMapDataList.isEmpty()) {
                        Optional<CustomChargeTypeMapData> data = customChargeTypeMapDataList.stream().filter(map -> map.getTerm().intValue() == numberOfInstallments)
                                .reduce((a, b) -> {
                                    throw new CustomChargeTypeMapNotFoundException("error.msg.customchargetypemap.id.multiple.values");
                                });

                        if (data.isPresent()) {
                            CustomChargeTypeMapData map = data.get();
                            percentage = map.getPercentage();
                            found = true;
                            break;
                        }
                    }
                }
            }
            if (found) {
                break;
            }
        }
        if (percentage.equals(BigDecimal.ZERO)) {
            throw new CustomChargeTypeMapNotFoundException();
        }
        return percentage;
    }
}
