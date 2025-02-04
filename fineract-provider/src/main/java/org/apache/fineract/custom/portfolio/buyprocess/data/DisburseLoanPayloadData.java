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
package org.apache.fineract.custom.portfolio.buyprocess.data;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.apache.tika.utils.StringUtils;

@Builder
@Getter
@Setter
public class DisburseLoanPayloadData {

    /*
     * { "actualDisbursementDate": "04 abril 2024", "transactionAmount": 100, "externalId": "", "paymentTypeId": "",
     * "note": "", "dateFormat": "dd MMMM yyyy", "locale": "es" }
     */

    private String actualDisbursementDate;
    private BigDecimal transactionAmount;
    private String externalId = StringUtils.EMPTY;
    private String paymentTypeId = StringUtils.EMPTY;
    private String note = StringUtils.EMPTY;
    private String dateFormat;
    private String locale;
    private String channelName;
    private boolean isWriteoffPunish;

}
