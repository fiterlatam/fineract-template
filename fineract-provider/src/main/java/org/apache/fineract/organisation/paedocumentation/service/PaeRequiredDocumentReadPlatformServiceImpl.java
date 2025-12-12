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
package org.apache.fineract.organisation.paedocumentation.service;

import java.util.List;
import java.util.stream.Collectors;
import org.apache.fineract.organisation.paedocumentation.data.PaeRequiredDocumentData;
import org.apache.fineract.organisation.paedocumentation.domain.PaeRequiredDocument;
import org.apache.fineract.organisation.paedocumentation.domain.PaeRequiredDocumentRepository;
import org.springframework.stereotype.Service;

@Service
public class PaeRequiredDocumentReadPlatformServiceImpl implements PaeRequiredDocumentReadPlatformService {

    private final PaeRequiredDocumentRepository repository;

    public PaeRequiredDocumentReadPlatformServiceImpl(PaeRequiredDocumentRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<PaeRequiredDocumentData> retrieveByCategory(Long categoryId) {
        List<PaeRequiredDocument> entities = repository.findByCategory_Id(categoryId);
        return entities.stream().map(e -> new PaeRequiredDocumentData(e.getId(), e.getCategory().getId(), e.getDocumentName(),
                e.getDescription(), e.getAcceptedFormat(), e.getRequired())).collect(Collectors.toList());
    }
}
