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
package org.apache.fineract.custom.ally.service;

import org.apache.fineract.custom.ally.data.ClientAllyPoibfOfSaleCodeValueData;
import org.apache.fineract.custom.ally.data.ClientAllyPointOfSalesData;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;

import java.util.List;

public interface ClientAllyPointOfSalesReadWritePlatformService {

    ClientAllyPoibfOfSaleCodeValueData getTemplateForInsertAndUpdate();

    List<ClientAllyPointOfSalesData> findAllActive();

    List<ClientAllyPointOfSalesData> findByName(Long parentId, String name);

    ClientAllyPointOfSalesData findById(Long id);

    CommandProcessingResult create(JsonCommand command, Long clientAllyId);

    CommandProcessingResult delete(Long id);

    CommandProcessingResult update(JsonCommand command, Long clientAllyId, Long id);
}
