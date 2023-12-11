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
package org.apache.fineract.organisation.bankcheque.service;

import java.util.List;
import org.apache.fineract.infrastructure.core.data.PaginationParameters;
import org.apache.fineract.infrastructure.core.service.Page;
import org.apache.fineract.organisation.bankcheque.data.BatchData;
import org.apache.fineract.organisation.bankcheque.data.ChequeData;
import org.apache.fineract.organisation.bankcheque.data.ChequeSearchParams;
import org.apache.fineract.organisation.bankcheque.data.GuaranteeData;

public interface ChequeReadPlatformService {

    BatchData retrieveBatch(final Long batchId);

    BatchData retrieveTemplate(final Long bankAccId);

    List<GuaranteeData> retrieveGuarantees(final String caseId, final String locale);

    Page<ChequeData> retrieveAll(final ChequeSearchParams chequeSearchParams, final PaginationParameters parameters);

}
