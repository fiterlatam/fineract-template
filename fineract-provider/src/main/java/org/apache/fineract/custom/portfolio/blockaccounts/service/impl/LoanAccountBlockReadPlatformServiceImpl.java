package org.apache.fineract.custom.portfolio.blockaccounts.service.impl;

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
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.custom.portfolio.blockaccounts.data.LoanAccountBlockDTO;
import org.apache.fineract.custom.portfolio.blockaccounts.domain.LoanAccountBlock;
import org.apache.fineract.custom.portfolio.blockaccounts.domain.LoanAccountBlockRepository;
import org.apache.fineract.custom.portfolio.blockaccounts.mapper.LoanAccountBlockMapper;
import org.apache.fineract.custom.portfolio.blockaccounts.service.LoanAccountBlockReadPlatformService;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class LoanAccountBlockReadPlatformServiceImpl implements LoanAccountBlockReadPlatformService {

    private final LoanAccountBlockRepository loanAccountBlockRepository;
    private final LoanAccountBlockMapper loanAccountBlockMapper;

    @Override
    public LoanAccountBlockDTO retrieveByLoanId(Long loanId) {
        Optional<LoanAccountBlock> loanAccountBlock = loanAccountBlockRepository.retrieveByLoanIdAndStatusActive(loanId);
        if (loanAccountBlock.isEmpty()) {
            throw new NotFoundException(String.valueOf(loanId));
        }
        return loanAccountBlockMapper.toDto(loanAccountBlock.get());
    }
}
