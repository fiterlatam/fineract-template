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
package org.apache.fineract.infrastructure.clientblockingreasons.data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import lombok.Data;
import org.apache.fineract.infrastructure.codes.data.CodeValueData;

@Data
public class BlockingReasonsData implements Serializable {

    private Long id;
    private Integer priority;
    private String description;
    private String nameOfReason;
    private String level;
    private LocalDate createdDate;
    private CodeValueData customerLevel;
    private CodeValueData creditLevel;

    private List<CodeValueData> customerLevelOptions;
    private List<CodeValueData> creditLevelOptions;

    public BlockingReasonsData() {}

    public BlockingReasonsData(Long id, Integer priority, String description, String nameOfReason, String level, LocalDate createdDate,
            CodeValueData customerLevel, CodeValueData creditLevel) {
        this.id = id;
        this.priority = priority;
        this.description = description;
        this.nameOfReason = nameOfReason;
        this.level = level;
        this.createdDate = createdDate;
        this.customerLevel = customerLevel;
        this.creditLevel = creditLevel;
    }
}
