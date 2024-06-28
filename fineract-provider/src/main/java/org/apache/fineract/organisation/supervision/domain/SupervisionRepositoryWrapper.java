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
package org.apache.fineract.organisation.supervision.domain;

import org.apache.fineract.organisation.supervision.exception.SupervisionNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * <p>
 * Wrapper for {@link SupervisionRepository} that adds NULL checking and Error handling capabilities
 * </p>
 */
@Service
public class SupervisionRepositoryWrapper {

    private final SupervisionRepository repository;

    @Autowired
    public SupervisionRepositoryWrapper(final SupervisionRepository repository) {
        this.repository = repository;
    }

    public Supervision findOneWithNotFoundDetection(final Long id) {
        return this.repository.findById(id).orElseThrow(() -> new SupervisionNotFoundException(id));
    }

    public Supervision save(final Supervision entity) {
        return this.repository.save(entity);
    }

    public Supervision saveAndFlush(final Supervision entity) {
        return this.repository.saveAndFlush(entity);
    }

    public void delete(final Supervision entity) {
        this.repository.delete(entity);
    }
}
