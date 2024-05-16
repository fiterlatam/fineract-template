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
package org.apache.fineract.portfolio.client.data;

import java.time.LocalDate;

@SuppressWarnings("unused")
public final class ClientBlockingReasonData {

    private final Long id;
    private final String name;
    private final String description;
    private final LocalDate blockDate;
    private final Integer priority;
    private final Long clientId;
    private final String displayName;

    public static ClientBlockingReasonData create(final Long id, final String name, final String description, final LocalDate blockDate,
            final Integer priority, final long clientId, final String displayName) {
        return new ClientBlockingReasonData(id, name, description, blockDate, priority, clientId, displayName);
    }

    private ClientBlockingReasonData(final Long id, final String name, final String description, final LocalDate blockDate,
            final Integer priority, final long clientId, final String displayName) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.blockDate = blockDate;
        this.priority = priority;
        this.clientId = clientId;
        this.displayName = displayName;

    }

}
