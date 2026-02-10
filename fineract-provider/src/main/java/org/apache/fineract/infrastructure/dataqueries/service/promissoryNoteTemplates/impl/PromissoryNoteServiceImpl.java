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
package org.apache.fineract.infrastructure.dataqueries.service.promissoryNoteTemplates.impl;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.dataqueries.data.PromissoryNoteTemplateData;
import org.apache.fineract.infrastructure.dataqueries.domain.PromissoryNoteTemplate;
import org.apache.fineract.infrastructure.dataqueries.domain.PromissoryNoteTemplateRepository;
import org.apache.fineract.infrastructure.dataqueries.service.promissoryNoteTemplates.PromissoryNoteService;
import org.apache.fineract.infrastructure.dataqueries.service.promissoryNoteTemplates.PromissoryNoteTemplateMapper;
import org.apache.fineract.infrastructure.dataqueries.service.promissoryNoteTemplates.templates.PromissoryNoteTemplateFive;
import org.apache.fineract.infrastructure.dataqueries.service.promissoryNoteTemplates.templates.PromissoryNoteTemplateFour;
import org.apache.fineract.infrastructure.dataqueries.service.promissoryNoteTemplates.templates.PromissoryNoteTemplateOne;
import org.apache.fineract.infrastructure.dataqueries.service.promissoryNoteTemplates.templates.PromissoryNoteTemplateSix;
import org.apache.fineract.infrastructure.dataqueries.service.promissoryNoteTemplates.templates.PromissoryNoteTemplateThree;
import org.apache.fineract.infrastructure.dataqueries.service.promissoryNoteTemplates.templates.PromissoryNoteTemplateTwo;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepository;
import org.apache.fineract.portfolio.loanaccount.service.LoanReadPlatformService;
import org.springframework.stereotype.Service;

import javax.ws.rs.NotFoundException;

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
    private final PromissoryNoteTemplateRepository promissoryNoteTemplateRepository;
    private final PromissoryNoteTemplateMapper promissoryNoteTemplateMapper;

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

            final boolean containsFiador = loanRepository.containsFiador(loanId) > 0;
            final Long canWriteAndReadClientl = loanRepository.retrieveCanWriteAndReadClient(loanId);
            final boolean canWriteAndReadClient = canWriteAndReadClientl != null && canWriteAndReadClientl == 1;
            final Long canWriteAndReadFiadorL = loanRepository.retrieveCanWriteAndReadGuarantor(loanId);
            final boolean canWriteAndReadFiador = containsFiador ? canWriteAndReadFiadorL != null && canWriteAndReadFiadorL == 1 : false;

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

    @Override
    public CommandProcessingResult updatePromissoryNote(JsonCommand command) {

        final Long templateId = command.longValueOfParameterNamed("templateId");
        PromissoryNoteTemplate promissoryNoteTemplate = promissoryNoteTemplateRepository.findById(templateId).orElseThrow(() -> new NotFoundException("template not exists with id " + templateId));
        validateUpdate(command);
        Map<String, Object> changes = promissoryNoteTemplate.update(command);

        return new CommandProcessingResultBuilder()
                .withEntityId(templateId)
                .with(changes)
                .build();
    }

    private void validateUpdate(JsonCommand command) {

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors);
        final Long templateId = command.longValueOfParameterNamed("templateId");

        if (command.parameterExists("blockOne")) {
            final String blockOne = command.stringValueOfParameterNamed("blockOne");
            baseDataValidator.reset().parameter("Block One").value(blockOne).notNull().notBlank();
        }
        if (command.parameterExists("blockTwo") && templateId != null && templateId > 2) {
            final String blockTwo = command.stringValueOfParameterNamed("blockTwo");
            baseDataValidator.reset().parameter("Block Two").value(blockTwo).notNull().notBlank();
        }

        if (command.parameterExists("name")) {
            final String name = command.stringValueOfParameterNamed("name");
            baseDataValidator.reset().parameter("Name").value(name).notNull().notBlank();
        }

        if (command.parameterExists("title")) {
            final String title = command.stringValueOfParameterNamed("title");
            baseDataValidator.reset().parameter("Title").value(title).notNull().notBlank();
        }

        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }
    }

    @Override
    public List<PromissoryNoteTemplateData> getPromissoryNoteTemplates() {
        return this.promissoryNoteTemplateMapper.map(this.promissoryNoteTemplateRepository.findAll());
    }

    @Override
    public PromissoryNoteTemplateData retrievePromissoryNoteTemplate(Long id) {
        return this.promissoryNoteTemplateMapper.map(this.promissoryNoteTemplateRepository.findById(id).get());
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
