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

import java.util.List;
import org.apache.fineract.custom.ally.data.CityCodeValueData;
import org.apache.fineract.custom.ally.data.ClientAllyCodeValueData;
import org.apache.fineract.custom.ally.data.ClientAllyData;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;

public interface ClientAllyReadWritePlatformService {

    List<ClientAllyData> findAllActive();

    List<ClientAllyData> findByName(String name);

    ClientAllyCodeValueData getTemplateForInsertAndUpdate();

    CityCodeValueData getCitiesByDepartment(Long departmentId);

    ClientAllyData findById(Long id);

    CommandProcessingResult create(JsonCommand command);

    CommandProcessingResult update(JsonCommand command, Long id);

    CommandProcessingResult delete(Long id);
}
