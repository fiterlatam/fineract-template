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
package org.apache.fineract.infrastructure.clientBlockingSettings.service;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.infrastructure.clientBlockingSettings.data.BlockingReasonsData;
import org.apache.fineract.infrastructure.codes.data.CodeValueData;
import org.apache.fineract.infrastructure.codes.service.CodeValueReadPlatformService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ManageBlockingReasonsReadPlatformServiceImpl implements ManageBlockingReasonsReadPlatformService {

    private static final Logger LOG = LoggerFactory.getLogger(ManageBlockingReasonsReadPlatformServiceImpl.class);

    private final CodeValueReadPlatformService codeValueReadPlatformService;

    @Override
    public BlockingReasonsData retrieveTemplate() {
        BlockingReasonsData blockingReasonsData = new BlockingReasonsData();
        final List<CodeValueData> customerLevelOptions = new ArrayList<>(
                this.codeValueReadPlatformService.retrieveCodeValuesByCode("Nivel Cliente"));

        final List<CodeValueData> creditLevelOptions = new ArrayList<>(
                this.codeValueReadPlatformService.retrieveCodeValuesByCode("Nivel Crédito"));
        blockingReasonsData.setCreditLevelOptions(creditLevelOptions);
        blockingReasonsData.setCustomerLevelOptions(customerLevelOptions);
        return blockingReasonsData;
    }

}
