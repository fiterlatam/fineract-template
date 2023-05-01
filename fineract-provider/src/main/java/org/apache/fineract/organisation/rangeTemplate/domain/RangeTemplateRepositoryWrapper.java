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
package org.apache.fineract.organisation.rangeTemplate.domain;

import org.apache.fineract.organisation.rangeTemplate.exception.RangeTemplateNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * <p>
 * Wrapper for {@link RangeTemplateRepository} that adds NULL checking and Error handling capabilities
 * </p>
 */
@Service
public class RangeTemplateRepositoryWrapper {

    private final RangeTemplateRepository repository;

    @Autowired
    public RangeTemplateRepositoryWrapper(final RangeTemplateRepository repository) {
        this.repository = repository;
    }

    public RangeTemplate findOneWithNotFoundDetection(final Long id) {
        return this.repository.findById(id).orElseThrow(() -> new RangeTemplateNotFoundException(id));
    }

    public RangeTemplate save(final RangeTemplate entity) {
        return this.repository.save(entity);
    }

    public RangeTemplate saveAndFlush(final RangeTemplate entity) {
        return this.repository.saveAndFlush(entity);
    }
}
