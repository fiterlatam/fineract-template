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

package org.apache.fineract.portfolio.loanaccount.data;

import java.time.LocalDateTime;
import org.apache.fineract.infrastructure.clientblockingreasons.data.BlockingReasonsData;

@lombok.Builder
public class LoanBlockingReasonData {

    private final Long id;
    private final Long loanId;
    private final BlockingReasonsData blockReasonSetting;
    private final boolean isActive;
    private final String comment;
    private LocalDateTime createdDate;
}
