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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class LoanRepaymentImportData {

    private Long id;
    private String bankingAgency;
    private Long customerCode;
    private String agency;
    private String loanCode;
    private String productCode;
    private BigDecimal amount;
    private String receiptNumber;
    private Long status;
    private LocalDateTime uploadDate;
    private LocalTime uploadTime;
    private LocalDateTime mifosProcessingDate;
    private LocalTime mifosProcessingTime;
    private String mifosFileName;
    private String bankName;
    private Long groupNumber;
    private Long mifosProductCode;
    private LocalDate paymentDate;
    private String operationResult;
    private BigDecimal scheduledPaymentAmount;
    private Long paymentNumber;
    private String lastPayment;
    private String tolerance;
    private Long errorId;
}
