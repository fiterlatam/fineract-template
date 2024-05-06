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

package org.apache.fineract.portfolio.loanaccount.jobs.blockinactiveclients;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.exception.AbstractPlatformDomainRuleException;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.jobs.exception.JobExecutionException;
import org.apache.fineract.portfolio.client.service.ClientReadPlatformService;
import org.apache.fineract.portfolio.client.service.ClientWritePlatformService;
import org.apache.fineract.portfolio.loanaccount.service.LoanReadPlatformService;
import org.apache.fineract.portfolio.loanproduct.service.LoanProductReadPlatformService;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

@Slf4j
public class BlockInActiveClientsTasklet implements Tasklet {

    private LoanReadPlatformService loanReadPlatformService;
    private ClientReadPlatformService clientReadPlatformService;
    private ClientWritePlatformService clientWritePlatformService;
    private LoanProductReadPlatformService loanProductReadPlatformService;

    public BlockInActiveClientsTasklet(final LoanReadPlatformService loanReadPlatformService,
            final ClientReadPlatformService clientReadPlatformService, final ClientWritePlatformService clientWritePlatformService,
            final LoanProductReadPlatformService loanProductReadPlatformService) {

        this.loanReadPlatformService = loanReadPlatformService;
        this.clientReadPlatformService = clientReadPlatformService;
        this.clientWritePlatformService = clientWritePlatformService;
        this.loanProductReadPlatformService = loanProductReadPlatformService;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        // Retrieve all loan products in the system
        final Collection<Pair<Long, Integer>> loanProductData = loanProductReadPlatformService
                .retrieveLoanProductClientInActivityConfiguration();
        if (!loanProductData.isEmpty()) {

            List<Throwable> exceptions = new ArrayList<>();

            for (Pair<Long, Integer> data : loanProductData) {

                final Long loanProductId = data.getLeft();
                final Integer inactivityPeriod = data.getRight();
                // Retrieve all clients with loans that are in-active
                final Collection<Long> clientIds = loanReadPlatformService.retrieveClientsWithLoansInActive(loanProductId,
                        inactivityPeriod);

                if (!clientIds.isEmpty()) {
                    for (Long clientId : clientIds) {
                        try {
                            // Block the client
                            clientWritePlatformService.blockClientWithInActiveLoan(clientId);

                        } catch (final PlatformApiDataValidationException e) {
                            final List<ApiParameterError> errors = e.getErrors();
                            for (final ApiParameterError error : errors) {
                                log.error("Bloquear cliente para cuenta de cliente {} falló con el mensaje: {}", clientId,
                                        error.getDeveloperMessage(), e);
                            }
                            exceptions.add(e);
                        } catch (final AbstractPlatformDomainRuleException e) {
                            log.error("Bloquear cliente para cuenta de cliente {} falló con el mensaje: {}", clientId,
                                    e.getDefaultUserMessage(), e);
                            exceptions.add(e);
                        } catch (Exception e) {
                            log.error("Error al bloquear cliente para cuenta de cliente {}", clientId, e);
                            exceptions.add(e);
                        }
                    }
                }
            }

            if (!exceptions.isEmpty()) {
                throw new JobExecutionException(exceptions);
            }
        }

        log.warn("BlockInActiveClientsTasklet execute method called");
        return RepeatStatus.FINISHED;
    }
}
