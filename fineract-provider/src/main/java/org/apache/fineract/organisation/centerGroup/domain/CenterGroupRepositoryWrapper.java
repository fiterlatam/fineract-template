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
package org.apache.fineract.organisation.centerGroup.domain;

import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.apache.fineract.organisation.centerGroup.exception.CenterGroupNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * <p>
 * Wrapper for {@link CenterGroupRepository} that adds NULL checking and Error handling capabilities
 * </p>
 */
@Service
public class CenterGroupRepositoryWrapper {

    private final CenterGroupRepository repository;

    @Autowired
    public CenterGroupRepositoryWrapper(final CenterGroupRepository repository) {
        this.repository = repository;
    }

    public CenterGroup findOneWithNotFoundDetection(final Long id) {
        return this.repository.findById(id).orElseThrow(() -> new CenterGroupNotFoundException(id));
    }

    public CenterGroup findOne(final Long id) {
        Optional<CenterGroup> centerGoup = this.repository.findById(id);
        return centerGoup.isPresent()?centerGoup.get():null;
    }

    public List<CenterGroup> findOverLappingCenterGroups(Long portfolioCenter, LocalTime startTime, LocalTime endTime) {
        return this.repository.findOverlappingCenterGroups(portfolioCenter, startTime, endTime);
    }

    public Collection<CenterGroup> findCenterGroupsByCenterIdAndMeetingTimes(final Long center, final LocalTime startTime,
            final LocalTime endTime) {
        return this.repository.findCenterGroupsByCenterIdAndMeetingTimes(center, startTime, endTime);
    }

    public CenterGroup save(final CenterGroup entity) {
        return this.repository.save(entity);
    }

    public CenterGroup saveAndFlush(final CenterGroup entity) {
        return this.repository.saveAndFlush(entity);
    }

    public void delete(final CenterGroup entity) {
        this.repository.delete(entity);
    }
}
