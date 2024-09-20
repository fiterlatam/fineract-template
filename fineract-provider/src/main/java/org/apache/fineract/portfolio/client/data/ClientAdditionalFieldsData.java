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

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.portfolio.client.domain.LegalForm;

/**
 * Immutable data object representing a Loan Additional Fields Data
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClientAdditionalFieldsData {

    private Long clientId;
    private String tipo;
    private String nit;
    private String cedula;
    private BigDecimal cupo;
    private EnumOptionData status;
    private String clientName;
    private Integer legalForm;

    public boolean isPerson() {
        LegalForm legalForm = LegalForm.fromInt(this.legalForm);
        return legalForm != null && legalForm.isPerson();
    }
}
