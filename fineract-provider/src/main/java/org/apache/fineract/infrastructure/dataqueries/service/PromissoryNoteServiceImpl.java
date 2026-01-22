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
package org.apache.fineract.infrastructure.dataqueries.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.dataqueries.service.promissoryNoteTemplates.PromissoryNoteTemplateFive;
import org.apache.fineract.infrastructure.dataqueries.service.promissoryNoteTemplates.PromissoryNoteTemplateFour;
import org.apache.fineract.infrastructure.dataqueries.service.promissoryNoteTemplates.PromissoryNoteTemplateOne;
import org.apache.fineract.infrastructure.dataqueries.service.promissoryNoteTemplates.PromissoryNoteTemplateSix;
import org.apache.fineract.infrastructure.dataqueries.service.promissoryNoteTemplates.PromissoryNoteTemplateThree;
import org.apache.fineract.infrastructure.dataqueries.service.promissoryNoteTemplates.PromissoryNoteTemplateTwo;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepository;
import org.apache.fineract.portfolio.loanaccount.service.LoanReadPlatformService;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromissoryNoteServiceImpl implements PromissoryNoteService {

    private final PromissoryNoteTemplateOne promissoryNoteTemplateOne;
    private final PromissoryNoteTemplateTwo promissoryNoteTemplateTwo;
    private final PromissoryNoteTemplateThree promissoryNoteTemplateThree;
    private final PromissoryNoteTemplateFour promissoryNoteTemplateFour;
    private final PromissoryNoteTemplateFive promissoryNoteTemplateFive;
    private final PromissoryNoteTemplateSix promissoryNoteTemplateSix;
    private final LoanRepository loanRepository;
    private final LoanReadPlatformService loanReadPlatformService;
    private final ConfigurationDomainService configurationDomainService;

    @Override
    public String generatePromissoryNote(String json) {
        int type = 0;
        JsonObject object = JsonParser.parseString(json).getAsJsonObject();
        final Long loanId = object.get("loanId").getAsLong();

        Loan loan = loanRepository.findById(loanId).get();
        final boolean containsGuarantee = loanRepository.containsGuaranteeLoan(loanId) > 0;
        final BigDecimal limitAmount = configurationDomainService.getMaxLimitAmount();
        final boolean isApprovedAmount = loan.getApprovedPrincipal().compareTo(limitAmount) < 0;
        final boolean nonMortgage = loanRepository.containsGuaranteeByLoanIdAndName(loanId, "Hipoteca") == 0;

        // credito sin garantía ó garantía no hipotecaria y monto aprobado menor al limite y esta desembolsado
        if ((!containsGuarantee || (nonMortgage && isApprovedAmount)) && loan.isDisbursed()) {

            boolean containsFiador = loanRepository.containsFiador(loanId) > 0;
            boolean canWriteAndReadClient = loanRepository.retrieveCanWriteAndReadClientOrGuarantor("p_solicitante", loanId);
            boolean canWriteAndReadFiador = containsFiador ? loanRepository.retrieveCanWriteAndReadClientOrGuarantor("p_fiador", loanId)
                    : false;

            if (!containsFiador && !containsGuarantee) { // sin fiador y sin garantía

                if (!canWriteAndReadClient) { // cliente no sabe leer ni escribir
                    type = 1;
                } else { // cliente sabe leer y escribir
                    type = 2;
                }

            } else {
                if (containsGuarantee) { // contiene fiador y garantía

                    if (!canWriteAndReadFiador && !canWriteAndReadClient) { // cliente y fiador no saben leer ni
                                                                            // escribir
                        type = 5;
                    } else if (canWriteAndReadClient && !canWriteAndReadFiador) { // cliente sabe leer y escribir pero
                                                                                  // el fiador no
                        type = 6;
                    } else if (!canWriteAndReadClient) { // cliente no sabe leer ni escribir
                        type = 3;
                    } else { // se supone cliente y fiador sabe leer y escribir
                        type = 4;
                    }
                }
            }

        } else {
            List<ApiParameterError> list = new ArrayList<>(1);
            ApiParameterError apiParameterError = ApiParameterError.parameterError("err.msg.does.not.comply",
                    "The loan does not comply requires of amount of promissory note", "loanId");
            list.add(apiParameterError);
            throw new PlatformApiDataValidationException("err.msg.does.not.comply",
                    "The loan does not comply requires of amount of promissory note", list);
        }

        return redirectPromissoryNote(type, json);
    }

    private String redirectPromissoryNote(Integer type, String json) {

        return switch (type) {
            case 1 -> promissoryNoteTemplateOne.generatePdf(json);
            case 2 -> promissoryNoteTemplateTwo.generatePdf(json);
            case 3 -> promissoryNoteTemplateThree.generatePdf(json);
            case 4 -> promissoryNoteTemplateFour.generatePdf(json);
            case 5 -> promissoryNoteTemplateFive.generatePdf(json);
            case 6 -> promissoryNoteTemplateSix.generatePdf(json);
            default -> throw new IllegalStateException("Unexpected value: " + type);
        };
    }
}
