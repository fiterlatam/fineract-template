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
package org.apache.fineract.organisation.committee.domain;

import java.util.List;
import org.apache.fineract.organisation.committee.exception.CommitteeNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * <p>
 * Wrapper for {@link CommitteeRepository} that adds NULL checking and Error handling capabilities
 * </p>
 */
@Service
public class CommitteeApprovalLimitsRepositoryWrapper {

    private final CommitteeApprovalLimitsRepository repository;

    @Autowired
    public CommitteeApprovalLimitsRepositoryWrapper(final CommitteeApprovalLimitsRepository repository) {
        this.repository = repository;
    }

    public CommitteeApprovalLimits findOneWithNotFoundDetection(final Long id) {
        return this.repository.findById(id).orElseThrow(() -> new CommitteeNotFoundException(id));
    }

    public CommitteeApprovalLimits save(final CommitteeApprovalLimits entity) {
        return this.repository.save(entity);
    }

    public CommitteeApprovalLimits saveAndFlush(final CommitteeApprovalLimits entity) {
        return this.repository.saveAndFlush(entity);
    }

    public void delete(final CommitteeApprovalLimits entity) {
        this.repository.delete(entity);
    }

    public void deleteByCommittee(final Long committeeId) {
        this.repository.deleteByCommittee(committeeId);
    }

    public List<CommitteeApprovalLimits> getApprovallimitsByCommittee(final Long committeeId) {
        return this.repository.getCommitteeApprovalLimitsByCommittee(committeeId);
    }
}
