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
package org.apache.fineract.infrastructure.codes.data;

import java.util.Collection;
import lombok.Getter;
import lombok.Setter;
import org.apache.fineract.organisation.paedocumentation.data.PaeRequiredDocumentData;

/**
 * Immutable data object represent code-value data in system.
 */
public class CodeValueDataExtended extends CodeValueData {

    @Getter
    @Setter
    private Collection<PaeRequiredDocumentData> extraData;

    public CodeValueDataExtended(Long id) {
        super(id);
    }

    public static CodeValueDataExtended instance(final Long id, final String name, final Integer position, final String description,
            final boolean isActive, final boolean mandatory) {
        return new CodeValueDataExtended(id, name, position, description, isActive, mandatory);
    }

    private CodeValueDataExtended(final Long id, final String name, final Integer position, final String description, final boolean active,
            final boolean mandatory) {
        super(id, name, position, description, active, mandatory);
    }

}
