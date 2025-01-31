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
package org.apache.fineract.portfolio.collectionhousemanagement.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.portfolio.accountdetails.data.AccountSummaryCollectionData;
import org.apache.fineract.portfolio.accountdetails.data.LoanAccountSummaryData;
import org.apache.fineract.portfolio.accountdetails.service.AccountDetailsReadPlatformService;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.client.domain.ClientRepositoryWrapper;
import org.apache.fineract.portfolio.collectionhousemanagement.data.CollectionHouseHistoryValidator;
import org.apache.fineract.portfolio.collectionhousemanagement.domain.CollectionHouseHistoryRepository;
import org.apache.fineract.portfolio.collectionhousemanagement.domain.ColletionHouseHistory;
import org.apache.fineract.portfolio.loanaccount.data.LoanStatusEnumData;
import org.apache.fineract.portfolio.loanaccount.domain.LoanStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class CollectionHouseHistoryReadWriteServiceImpl implements CollectionHouseHistoryReadWriteService {

    private final CollectionHouseHistoryValidator collectionHouseHistoryValidator;
    private final CollectionHouseHistoryRepository collectionHouseHistoryRepository;
    private final AccountDetailsReadPlatformService accountDetailsReadPlatformService;
    private final ClientRepositoryWrapper clientRepositoryWrapper;

    @SuppressWarnings({ "squid:S3776", "squid:S1141" })
    @Override
    public CommandProcessingResult createCollectionHouseHistory(JsonCommand command) {
        collectionHouseHistoryValidator.validateForCreateCollectionHouse(command.json());
        JsonArray updatesArray = command.arrayOfParameterNamed("collectionHouseUpdates");
        List<Map<String, Object>> savedHistories = new ArrayList<>();

        try {
            collectionHouseHistoryRepository.deleteAll();
            collectionHouseHistoryRepository.flush();

            for (int i = 0; i < updatesArray.size(); i++) {
                JsonObject updateObject = updatesArray.get(i).getAsJsonObject();
                String clientAccountNo = updateObject.get("clientAccountNo").getAsString();
                String nit = updateObject.get("nit").getAsString();
                String collectionHouseCode = updateObject.get("collectionHouseCode").getAsString();

                try {
                    Client client = clientRepositoryWrapper.getClientByAccountNumber(clientAccountNo);
                    boolean hasLoanAccountsInArrears = false;
                    if (client != null) {
                        AccountSummaryCollectionData clientAccount = accountDetailsReadPlatformService
                                .retrieveClientAccountDetails(client.getId());
                        Collection<LoanAccountSummaryData> loanAccounts = clientAccount.getLoanAccounts();
                        if (!loanAccounts.isEmpty()) {
                            for (LoanAccountSummaryData loanAccountSummaryData : loanAccounts) {
                                LoanStatusEnumData statusEnumData = loanAccountSummaryData.getStatus();
                                Long status = statusEnumData.getId();
                                if (Boolean.TRUE.equals(loanAccountSummaryData.getInArrears())
                                        && LoanStatus.fromInt(status.intValue()).isActive()) {
                                    hasLoanAccountsInArrears = true;
                                }
                            }
                        }
                    }

                    if (hasLoanAccountsInArrears) {
                        ColletionHouseHistory colletionHouseHistory = new ColletionHouseHistory();
                        colletionHouseHistory.setClientAccountNumber(clientAccountNo);
                        colletionHouseHistory.setCollectionNit(nit);
                        colletionHouseHistory.setCollectionCode(collectionHouseCode);
                        collectionHouseHistoryRepository.saveAndFlush(colletionHouseHistory);
                    }

                } catch (final JpaSystemException | DataIntegrityViolationException ex) {
                    throw new PlatformDataIntegrityException("error.msg.collectionHouseHistory.save.failed",
                            "Failed to save Collection House History for clientAccountNo: " + clientAccountNo, ex);
                }
            }
            List<ColletionHouseHistory> colletionHouseHistories = this.findAllCollectionHouseHistory();
            savedHistories = colletionHouseHistories.stream().map(colletionHouseHistory -> {
                Map<String, Object> map = new HashMap<>();
                map.put("clientAccountNumber", colletionHouseHistory.getClientAccountNumber());
                map.put("collectionHouseCode", colletionHouseHistory.getCollectionCode());
                map.put("collectionNit", colletionHouseHistory.getCollectionNit());
                return map;
            }).toList();

        } catch (Exception e) {
            log.error("Error during collection house history refresh", e);
            throw e;
        }

        return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withEntityId(command.entityId())
                .withCollectionHouse(savedHistories).build();
    }

    @Override
    public ColletionHouseHistory findCollectionHouseHistoryByAcctountNo(String accountNo) {
        Optional<ColletionHouseHistory> getColletionHouseHistory = collectionHouseHistoryRepository
                .getCollectionHouseHistoryByAccountNo((accountNo));
        return getColletionHouseHistory.orElse(null);
    }

    @Override
    public List<ColletionHouseHistory> findAllCollectionHouseHistory() {
        return collectionHouseHistoryRepository.findAll();
    }
}
