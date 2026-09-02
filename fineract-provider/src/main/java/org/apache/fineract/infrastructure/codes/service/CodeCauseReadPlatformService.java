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
package org.apache.fineract.infrastructure.codes.service;

import java.util.Collection;
import org.apache.fineract.infrastructure.codes.data.CodeCauseData;
import org.apache.fineract.infrastructure.core.service.Page;

public interface CodeCauseReadPlatformService {

    Collection<CodeCauseData> retrieveAll();

    Page<CodeCauseData> retrieveAll(Integer offset, Integer limit, String searchText);

    CodeCauseData retrieveByCodigoAcrm(Integer codigoAcrm);

    Collection<CodeCauseData> retrieveByAllowOverdraft(Boolean allowOverdraft);

    Collection<CodeCauseData> retrieveByCurrencyCode(Integer currencyCodeInt);

    CodeCauseData retrieveByCauseId(Long causeId);

    CodeCauseData retrieveTemplate();
}
