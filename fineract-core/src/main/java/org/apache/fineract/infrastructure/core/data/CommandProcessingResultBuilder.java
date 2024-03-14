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
package org.apache.fineract.infrastructure.core.data;

import java.util.Map;
import org.apache.fineract.infrastructure.core.domain.ExternalId;

/**
 * Represents the successful result of an REST API call that results in processing a command.
 */
public class CommandProcessingResultBuilder {

    private Long commandId;
    private Long officeId;
    private Long groupId;
    private Long clientId;
    private Long loanId;
    private Long savingsId;
    private String resourceIdentifier;
    private Long entityId;
    private Long subEntityId;
    private Long gsimId;
    private Long glimId;
    private String transactionId;
    private Map<String, Object> changes;
    private Map<String, Object> creditBureauReportData;
    private Long productId;
    private boolean rollbackTransaction = false;
    private ExternalId entityExternalId = ExternalId.empty();

    private ExternalId subEntityExternalId = ExternalId.empty();

    private String registroPosterior;
    private String registroAnterior;
    private String usuarioNombre;
    private String rolNombre;

    public CommandProcessingResult build() {
        CommandProcessingResult commandProcessingResult = CommandProcessingResult.fromDetails(this.commandId, this.officeId, this.groupId,
                this.clientId, this.loanId, this.savingsId, this.resourceIdentifier, this.entityId, this.gsimId, this.glimId,
                this.creditBureauReportData, this.transactionId, this.changes, this.productId, this.rollbackTransaction, this.subEntityId,
                this.entityExternalId, this.subEntityExternalId);
        commandProcessingResult.setRegistroAnterior(this.registroAnterior);
        commandProcessingResult.setRegistroPosterior(this.registroPosterior);
        commandProcessingResult.setUsuarioNombre(this.usuarioNombre);
        commandProcessingResult.setRolNombre(this.rolNombre);
        return commandProcessingResult;
    }

    public CommandProcessingResultBuilder withCommandId(final Long withCommandId) {
        this.commandId = withCommandId;
        return this;
    }

    public CommandProcessingResultBuilder with(final Map<String, Object> withChanges) {
        this.changes = withChanges;
        return this;
    }

    public CommandProcessingResultBuilder withResourceIdAsString(final String withResourceIdentifier) {
        this.resourceIdentifier = withResourceIdentifier;
        return this;
    }

    public CommandProcessingResultBuilder withEntityId(final Long withEntityId) {
        this.entityId = withEntityId;
        return this;
    }

    public CommandProcessingResultBuilder withSubEntityId(final Long withSubEntityId) {
        this.subEntityId = withSubEntityId;
        return this;
    }

    public CommandProcessingResultBuilder withOfficeId(final Long withOfficeId) {
        this.officeId = withOfficeId;
        return this;
    }

    public CommandProcessingResultBuilder withClientId(final Long withClientId) {
        this.clientId = withClientId;
        return this;
    }

    public CommandProcessingResultBuilder withGroupId(final Long withGroupId) {
        this.groupId = withGroupId;
        return this;
    }

    public CommandProcessingResultBuilder withLoanId(final Long withLoanId) {
        this.loanId = withLoanId;
        return this;
    }

    public CommandProcessingResultBuilder withSavingsId(final Long withSavingsId) {
        this.savingsId = withSavingsId;
        return this;
    }

    public CommandProcessingResultBuilder withTransactionId(final String withTransactionId) {
        this.transactionId = withTransactionId;
        return this;
    }

    public CommandProcessingResultBuilder withProductId(final Long productId) {
        this.productId = productId;
        return this;
    }

    public CommandProcessingResultBuilder withGsimId(final Long gsimId) {
        this.gsimId = gsimId;
        return this;
    }

    public CommandProcessingResultBuilder withGlimId(final Long glimId) {
        this.glimId = glimId;
        return this;
    }

    public CommandProcessingResultBuilder withCreditReport(final Map<String, Object> withCreditReport) {
        this.creditBureauReportData = withCreditReport;
        return this;
    }

    public CommandProcessingResultBuilder setRollbackTransaction(final boolean rollbackTransaction) {
        this.rollbackTransaction |= rollbackTransaction;
        return this;
    }

    public CommandProcessingResultBuilder withEntityExternalId(final ExternalId entityExternalId) {
        this.entityExternalId = entityExternalId;
        return this;
    }

    public CommandProcessingResultBuilder withSubEntityExternalId(final ExternalId subEntityExternalId) {
        this.subEntityExternalId = subEntityExternalId;
        return this;
    }

    public CommandProcessingResultBuilder setCommandId(Long commandId) {
        this.commandId = commandId;
        return this;
    }

    public CommandProcessingResultBuilder withRegistroAnterior(String registroAnterior) {
        this.registroAnterior = registroAnterior;
        return this;
    }

    public CommandProcessingResultBuilder withRegistroPosterior(String registroPosterior) {
        this.registroPosterior = registroPosterior;
        return this;
    }

    public CommandProcessingResultBuilder withUsuarioNombre(String usuarioNombre) {
        this.usuarioNombre = usuarioNombre;
        return this;
    }

    public CommandProcessingResultBuilder withRolNombre(String rolNombre) {
        this.rolNombre = rolNombre;
        return this;
    }
}
