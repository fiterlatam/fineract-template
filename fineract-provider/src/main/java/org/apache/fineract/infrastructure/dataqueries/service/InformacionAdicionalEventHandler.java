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
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.apache.fineract.infrastructure.dataqueries.events.DatatableEntryEvent;
import org.apache.fineract.infrastructure.dataqueries.events.DatatableOperationType;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepositoryWrapper;
import org.apache.fineract.portfolio.loanaccount.domain.LoanStatus;
import org.apache.fineract.portfolio.loanaccount.event.LoanApprovalContactabilityEventProcessor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InformacionAdicionalEventHandler implements DatatableEventHandler {

    private static final String DATATABLE_NAME = "Informacion Adicional";
    private static final String FIELD_VALIDACION_MANUAL = "validacion_manual";
    private static final String FIELD_NOTIFICACION_BIENVENIDA = "notificacion_bienvenida";
    private static final String ENTITY_TYPE = "LOAN";
    private static final String ACTION = "DISBURSE";

    private final LoanApprovalContactabilityEventProcessor loanApprovalContactabilityEventProcessor;
    private final PlatformSecurityContext context;
    private final LoanRepositoryWrapper loanRepository;

    @Override
    public String getDatatableName() {
        return DATATABLE_NAME;
    }

    @Override
    public void handle(DatatableEntryEvent event) {
        String operation = event.getOperation() == DatatableOperationType.CREATE ? "created" : "updated";
        log.info("Entry {} in Informacion Adicional datatable. AppTableId: {}, DatatableId: {}, Data: {}", operation, event.getAppTableId(),
                event.getDatatableId(), event.getJsonData());

        JsonObject jsonData = event.getJsonData();

        // Extract and log the raw field values
        log.info("Raw validacion_manual value: {}",
                jsonData.has(FIELD_VALIDACION_MANUAL) ? jsonData.get(FIELD_VALIDACION_MANUAL) : "not present");
        log.info("Raw notificacion_bienvenida value: {}",
                jsonData.has(FIELD_NOTIFICACION_BIENVENIDA) ? jsonData.get(FIELD_NOTIFICACION_BIENVENIDA) : "not present");

        // Extract the boolean values from the JSON data
        boolean validacionManual = jsonData.has(FIELD_VALIDACION_MANUAL) && jsonData.get(FIELD_VALIDACION_MANUAL).getAsBoolean();
        boolean notificacionBienvenidaIsNull = !jsonData.has(FIELD_NOTIFICACION_BIENVENIDA)
                || jsonData.get(FIELD_NOTIFICACION_BIENVENIDA).isJsonNull();

        log.info("Extracted values - validacionManual: {}, notificacionBienvenidaIsNull: {}", validacionManual,
                notificacionBienvenidaIsNull);

        // Get loan details to check if it's revolving and its status
        Loan loan = loanRepository.findOneWithNotFoundDetection(event.getAppTableId());
        boolean isRevolvingLoan = loan.isRevolvingLoan();
        LoanStatus loanStatus = loan.getStatus();

        log.info("Loan details - Is Revolving Loan: {}, Status: {}", isRevolvingLoan, loanStatus);

        if (shouldTriggerWebhook(loan, validacionManual, notificacionBienvenidaIsNull)) {
            publishWebhook(event.getAppTableId());

            log.info(
                    "Webhook published for loan ID: {} - All conditions met: IsRevolvingLoan={}, Status={}, ValidacionManual={}, NotificacionBienvenidaIsNull={}",
                    event.getAppTableId(), isRevolvingLoan, loanStatus, validacionManual, notificacionBienvenidaIsNull);
        } else {
            log.info(
                    "No webhook needed for loan ID: {} - Conditions not met: IsRevolvingLoan={}, Status={}, ValidacionManual={}, NotificacionBienvenidaIsNull={}",
                    event.getAppTableId(), isRevolvingLoan, loanStatus, validacionManual, notificacionBienvenidaIsNull);
        }
    }

    /**
     * Checks if the webhook should be triggered based on the loan and field conditions
     *
     * @param loan
     *            the loan to check
     * @param validacionManual
     *            whether validacion_manual is true
     * @param notificacionBienvenidaIsNull
     *            whether notificacion_bienvenida is null
     * @return true if all conditions are met to trigger the webhook
     */
    private boolean shouldTriggerWebhook(Loan loan, boolean validacionManual, boolean notificacionBienvenidaIsNull) {
        return loan.isRevolvingLoan() && loan.getStatus().isApproved() && validacionManual && notificacionBienvenidaIsNull;
    }

    /**
     * Publishes the webhook with the loan approval contactability event
     *
     * @param loanId
     *            the ID of the loan
     */
    private void publishWebhook(Long loanId) {
        CommandProcessingResult result = new CommandProcessingResultBuilder().withLoanId(loanId).build();

        Map<String, Object> payload = loanApprovalContactabilityEventProcessor.generateSuccessResponse(result);

        loanApprovalContactabilityEventProcessor.publish(payload, ENTITY_TYPE, ACTION, context.authenticatedUser(),
                ThreadLocalContextUtil.getContext());
    }
}
