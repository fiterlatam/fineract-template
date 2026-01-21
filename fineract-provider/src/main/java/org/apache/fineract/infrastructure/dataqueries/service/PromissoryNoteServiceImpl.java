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
        String type = "";
        JsonObject object = JsonParser.parseString(json).getAsJsonObject();
        final Long loanId = object.get("loanId").getAsLong();

        Loan loan = loanRepository.findById(loanId).get();
        final boolean containsGuarantee = loanRepository.containsGuaranteeLoan(loanId) > 0;
        final BigDecimal limitAmount = configurationDomainService.getMaxLimitAmount();
        final boolean isApprovedAmount = loan.getApprovedPrincipal().compareTo(limitAmount) < 0;
        final boolean nonMortgage = loanRepository.containsGuaranteeByLoanIdAndName(loanId, "Hipoteca") == 0;

        // credito sin garantía ó garantía no hipotecaria y monto aprobado menor al limite
        if (!containsGuarantee || (nonMortgage && isApprovedAmount)) {

            boolean containsFiador = loanRepository.containsFiador(loanId) > 0;

            // FIXME -> it´s temporally
            // canoo be implemented becasue there are some parameters that are needed
            // testigo para deudor siempre cumple por que si no hay toma los del usuario, esta bien ?
            // testigo para fiador se captura en front pero no existe en bd
            // casos mal contemplados

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

    private String redirectPromissoryNote(String type, String json) {

        return switch (type) {
            case "1" -> promissoryNoteTemplateOne.generatePdf(json);
            case "2" -> promissoryNoteTemplateTwo.generatePdf(json);
            case "3" -> promissoryNoteTemplateThree.generatePdf(json);
            case "4" -> promissoryNoteTemplateFour.generatePdf(json);
            case "5" -> promissoryNoteTemplateFive.generatePdf(json);
            case "6" -> promissoryNoteTemplateSix.generatePdf(json);
            default -> throw new IllegalStateException("Unexpected value: " + type);
        };
    }
}
