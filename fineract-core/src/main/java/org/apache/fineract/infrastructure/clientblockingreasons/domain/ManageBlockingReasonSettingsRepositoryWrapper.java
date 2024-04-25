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
package org.apache.fineract.infrastructure.clientblockingreasons.domain;

import java.util.List;
import org.apache.fineract.infrastructure.clientblockingreasons.exception.BlockingReasonExceptionNotFoundException;
import org.apache.fineract.infrastructure.codes.domain.CodeValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class ManageBlockingReasonSettingsRepositoryWrapper {

    private final ManageBlockingReasonSettingsRepository repository;

    @Autowired
    public ManageBlockingReasonSettingsRepositoryWrapper(final ManageBlockingReasonSettingsRepository repository) {
        this.repository = repository;
    }

    public BlockingReasonSetting findOneWithNotFoundDetection(final Long id) {
        return this.repository.findById(id).orElseThrow(() -> new BlockingReasonExceptionNotFoundException(id));
    }

    public void save(final BlockingReasonSetting blockingReasonSetting) {
        this.repository.save(blockingReasonSetting);
    }

    public void saveAndFlush(final BlockingReasonSetting blockingReasonSetting) {
        this.repository.saveAndFlush(blockingReasonSetting);
    }

    public void delete(final BlockingReasonSetting blockingReasonSetting) {
        this.repository.delete(blockingReasonSetting);
    }

    public List<BlockingReasonSetting> getBlockingReasonSettingByCustomerLevel(CodeValue customerLevel) {
        return this.repository.getBlockingReasonSettingByCustomerLevel(customerLevel);
    }

    public List<BlockingReasonSetting> getBlockingReasonSettingByCreditLevel(CodeValue creditLevel) {
        return this.repository.getBlockingReasonSettingByCreditLevel(creditLevel);
    }

    public List<BlockingReasonSetting> getBlockingReasonSettingByPriority(Integer priority, String level) {
        return this.repository.getBlockingReasonSettingByPriority(priority, level);
    }
}
