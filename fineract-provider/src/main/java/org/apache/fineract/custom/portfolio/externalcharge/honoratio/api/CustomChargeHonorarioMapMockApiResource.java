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
package org.apache.fineract.custom.portfolio.externalcharge.honoratio.api;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.custom.portfolio.externalcharge.honoratio.constants.CustomChargeHonorarioMapApiConstants;
import org.apache.fineract.custom.portfolio.externalcharge.honoratio.data.CustomChargeHonorarioMapData;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.core.api.ApiRequestParameterHelper;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.organisation.monetary.domain.MonetaryCurrency;
import org.apache.fineract.organisation.monetary.domain.MoneyHelper;
import org.apache.fineract.portfolio.delinquency.data.DelinquencyRangeData;
import org.apache.fineract.portfolio.delinquency.domain.DelinquencyRange;
import org.apache.fineract.portfolio.delinquency.service.DelinquencyReadPlatformService;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepaymentScheduleInstallment;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Path("/v1/customchargehonorariomap/mock")
@Component
@Scope("singleton")
public class CustomChargeHonorarioMapMockApiResource {

    private final DefaultToApiJsonSerializer<CustomChargeHonorarioMapData> toApiJsonSerializer;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    private final PlatformSecurityContext context;
    private final ApiRequestParameterHelper apiRequestParameterHelper;
    private final LoanRepository loanRepository;
    private final ConfigurationDomainService configurationDomainService;
    private final DelinquencyReadPlatformService delinquencyReadPlatformService;

    @Autowired
    public CustomChargeHonorarioMapMockApiResource(final DefaultToApiJsonSerializer<CustomChargeHonorarioMapData> toApiJsonSerializer,
            final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService, final PlatformSecurityContext context,
            final ApiRequestParameterHelper apiRequestParameterHelper, LoanRepository loanRepository,
            ConfigurationDomainService configurationDomainService, DelinquencyReadPlatformService delinquencyReadPlatformService) {
        this.toApiJsonSerializer = toApiJsonSerializer;
        this.commandsSourceWritePlatformService = commandsSourceWritePlatformService;
        this.context = context;
        this.apiRequestParameterHelper = apiRequestParameterHelper;
        this.loanRepository = loanRepository;
        this.configurationDomainService = configurationDomainService;
        this.delinquencyReadPlatformService = delinquencyReadPlatformService;
    }

    @GET
    @Path("{loanid}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String get(@PathParam("loanid") @Parameter(description = "loanid") final Long id, @Context final UriInfo uriInfo) {
        this.context.authenticatedUser().validateHasReadPermission(CustomChargeHonorarioMapApiConstants.RESOURCE_NAME);

        List<Map<String, Object>> generatedFata = generateRandomMockedData(id);

        return this.toApiJsonSerializer.serialize(generatedFata);
    }

    private Long loanInstallmentNr = 1L;
    private String clientDocumentId = "0501120631-9";
    private String nit = "120843958";

    private List<Map<String, Object>> generateRandomMockedData(Long loanId) {

        List<Map<String, Object>> jsonList = new ArrayList<>();

        // Generate Random Data for each installment
        Optional<Loan> loans = loanRepository.findById(loanId);

        if (loans.isPresent()) {
            Loan loan = loans.get();
            Integer ageOverdue = loan.getAgeOfOverdueDays(DateUtils.getBusinessLocalDate()).intValue();
            BigDecimal delinquencyValue = BigDecimal.ZERO;
            Integer vatConfig = configurationDomainService.retriveIvaConfiguration();
            BigDecimal vatPercentage = BigDecimal.valueOf(vatConfig).divide(new BigDecimal(100), 2, MoneyHelper.getRoundingMode());
            MonetaryCurrency currency = loan.getCurrency();
            DelinquencyRangeData delinquencyRangeData = delinquencyReadPlatformService.retrieveCurrentDelinquencyTag(loan.getId());
            if (delinquencyRangeData != null) {
                delinquencyValue = BigDecimal.valueOf(delinquencyRangeData.getPercentageValue());
            } else {
                DelinquencyRange delinquencyRange = delinquencyReadPlatformService.retrieveDelinquencyRangeCategeory(ageOverdue);
                if (delinquencyRange != null) {
                    delinquencyValue = BigDecimal.valueOf(delinquencyRange.getPercentageValue());
                }
            }

            BigDecimal deliquncyrange = delinquencyValue.divide(new BigDecimal(100), 2, MoneyHelper.getRoundingMode());

            if (ageOverdue > 0) {
                List<LoanRepaymentScheduleInstallment> loanRepaymentScheduleInstallments = loan.getRepaymentScheduleInstallments();
                for (LoanRepaymentScheduleInstallment loanRepaymentScheduleInstallment : loanRepaymentScheduleInstallments) {
                    BigDecimal feeBaseAmount = BigDecimal.ZERO;
                    BigDecimal feeVatAmount = BigDecimal.ZERO;
                    BigDecimal feeTotalAmount = BigDecimal.ZERO;
                    Map<String, Object> jsonMap = new HashMap<>();

                    if (LocalDate.now().isAfter(loanRepaymentScheduleInstallment.getDueDate())
                            && !loanRepaymentScheduleInstallment.isObligationsMet()) {
                        BigDecimal paidAmount = loanRepaymentScheduleInstallment.getTotalOutstanding(currency).getAmount();

                        BigDecimal delinquentPortion = paidAmount.divide(
                                BigDecimal.ONE.add(deliquncyrange.multiply(BigDecimal.ONE.add(vatPercentage))), 2,
                                MoneyHelper.getRoundingMode());

                        BigDecimal feewithTax = delinquentPortion.multiply(deliquncyrange.multiply(BigDecimal.ONE.add(vatPercentage)))
                                .setScale(2, MoneyHelper.getRoundingMode());
                        BigDecimal feeBasis = feewithTax.divide(BigDecimal.ONE.add(vatPercentage), 2, MoneyHelper.getRoundingMode());

                        BigDecimal feeVat = feewithTax.subtract(feeBasis).setScale(2, MoneyHelper.getRoundingMode());
                        BigDecimal feeHono = feeVat.add(feeBasis).setScale(0, MoneyHelper.getRoundingMode());

                        feeBaseAmount = feeBasis;
                        feeVatAmount = feeVat;
                        feeTotalAmount = feeHono;

                    }

                    jsonMap.put("loanId", loanId);
                    jsonMap.put("nit", nit);
                    jsonMap.put("loanInstallmentNr", loanRepaymentScheduleInstallment.getInstallmentNumber());
                    if (feeTotalAmount.compareTo(BigDecimal.ZERO) == 0) {
                        jsonMap.put("feeTotalAmount", "0.00");
                    } else {
                        jsonMap.put("feeTotalAmount", String.format("%.2f", feeTotalAmount));
                    }
                    if (feeBaseAmount.compareTo(BigDecimal.ZERO) == 0) {
                        jsonMap.put("feeBaseAmount", "0.00");
                    } else {
                        jsonMap.put("feeBaseAmount", String.format("%.2f", feeBaseAmount));
                    }
                    if (feeVatAmount.compareTo(BigDecimal.ZERO) == 0) {
                        jsonMap.put("feeVatAmount", "0.00");
                    } else {
                        jsonMap.put("feeVatAmount", String.format("%.2f", feeVatAmount));
                    }
                    jsonMap.put("dateFormat", "dd/MM/yyyy");
                    jsonMap.put("locale", "en");
                    jsonList.add(jsonMap);
                }
            }
        }

        return jsonList;
    }

    // Generate a random number
    private double randomFeeTotalAmount() {
        Random rand = new Random();
        return rand.nextDouble() * (200 - 100) + 100;
    }
}
