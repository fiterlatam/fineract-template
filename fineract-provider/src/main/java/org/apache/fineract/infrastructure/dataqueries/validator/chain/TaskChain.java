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

package org.apache.fineract.infrastructure.dataqueries.validator.chain;

import java.util.Map;
import org.apache.fineract.infrastructure.dataqueries.validator.chain.processor.InformacionAdicionalValidateProcessor;
import org.apache.fineract.infrastructure.dataqueries.validator.data.DataTableMetaData;
import org.springframework.stereotype.Component;

@Component
public class TaskChain {

    private CustomFieldValidationProcessor chain;

    public TaskChain() {
        buildChain();
    }

    private void buildChain() {
        // Here we can define the chain order
        // chain = new Post BackupProcessor(new EmailSummaryProcessor(null)))));
        chain = new InformacionAdicionalValidateProcessor(null);
    }

    public void process(Object parentObject, DataTableMetaData metData, Map<String, String> dataParams,
            Map<String, Object> auxliaryObjects) {
        chain.process(parentObject, metData, dataParams, auxliaryObjects);
    }
}
