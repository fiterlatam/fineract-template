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
package org.apache.fineract.portfolio.blacklist.service;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.codes.data.CodeValueData;
import org.apache.fineract.infrastructure.codes.service.CodeValueReadPlatformService;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.blacklist.command.BlacklistApiConstants;
import org.apache.fineract.portfolio.blacklist.command.BlacklistDataValidator;
import org.apache.fineract.portfolio.blacklist.domain.BlacklistClients;
import org.apache.fineract.portfolio.blacklist.domain.BlacklistClientsRepository;
import org.apache.fineract.portfolio.client.service.ClientChargeWritePlatformServiceJpaRepositoryImpl;
import org.apache.fineract.portfolio.client.service.ClientReadPlatformService;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProduct;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProductRepository;
import org.apache.fineract.portfolio.loanproduct.exception.LoanProductNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class BlacklistClientWritePlatformServiceImpl implements BlacklistClientWritePlatformService {

    private static final Logger LOG = LoggerFactory.getLogger(ClientChargeWritePlatformServiceJpaRepositoryImpl.class);

    private final PlatformSecurityContext context;
    private final BlacklistDataValidator dataValidator;
    private final LoanProductRepository loanProductRepository;
    private final ClientReadPlatformService clientReadPlatformService;
    private final CodeValueReadPlatformService codeValueReadPlatformService;
    private final BlacklistClientsRepository blacklistClientsRepository;

    @Autowired
    public BlacklistClientWritePlatformServiceImpl(final PlatformSecurityContext context, final BlacklistDataValidator dataValidator,
            final LoanProductRepository loanProductRepository, final ClientReadPlatformService clientReadPlatformService,
            final CodeValueReadPlatformService codeValueReadPlatformService, final BlacklistClientsRepository blacklistClientsRepository) {
        this.context = context;
        this.dataValidator = dataValidator;
        this.loanProductRepository = loanProductRepository;
        this.clientReadPlatformService = clientReadPlatformService;
        this.codeValueReadPlatformService = codeValueReadPlatformService;
        this.blacklistClientsRepository = blacklistClientsRepository;
    }

    @Override
    public CommandProcessingResult addClientToBlacklist(JsonCommand command) {
        Long clientId = command.getClientId();
        this.dataValidator.validateForCreate(command.json());
        final Long productId = command.longValueOfParameterNamed(BlacklistApiConstants.productIdParamName);

        Optional<LoanProduct> productOption = this.loanProductRepository.findById(productId);
        if (productOption.isEmpty()) throw new LoanProductNotFoundException(productId);
        LoanProduct loanProduct = productOption.get();

        CodeValueData typification = codeValueReadPlatformService
                .retrieveCodeValue(command.longValueOfParameterNamed(BlacklistApiConstants.typificationParamName));
        BlacklistClients blacklistClient = BlacklistClients.fromJson(this.context.authenticatedUser(), loanProduct, typification, command);

        this.blacklistClientsRepository.saveAndFlush(blacklistClient);

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withClientId(clientId) //
                .withEntityId(blacklistClient.getId()) //
                .build();
    }
}
