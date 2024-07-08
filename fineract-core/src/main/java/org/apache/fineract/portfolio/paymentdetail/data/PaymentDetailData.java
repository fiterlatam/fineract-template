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
package org.apache.fineract.portfolio.paymentdetail.data;

import java.io.Serializable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.fineract.portfolio.client.data.PointOfSalesData;
import org.apache.fineract.portfolio.paymenttype.data.PaymentTypeData;

/**
 * Immutable data object representing a payment.
 */
@Getter
@EqualsAndHashCode
@RequiredArgsConstructor
@Setter
public class PaymentDetailData implements Serializable {

    private final Long id;
    private final PaymentTypeData paymentType;
    private final String accountNumber;
    private final String checkNumber;
    private final String routingCode;
    private final String receiptNumber;
    private final String bankNumber;
    private final String channelName;
    private final String channelHash;
    private final PointOfSalesData pointOfSales;
    private Long bankId;
    private String bankName;

    public PaymentDetailData(final Long id, PaymentTypeData paymentType, final String accountNumber, final String checkNumber,
            final String routingCode, final String receiptNumber, final String bankNumber) {
        this.id = id;
        this.paymentType = paymentType;
        this.accountNumber = accountNumber;
        this.checkNumber = checkNumber;
        this.routingCode = routingCode;
        this.receiptNumber = receiptNumber;
        this.bankNumber = bankNumber;
        this.channelName = null;
        this.channelHash = null;
        this.pointOfSales = null;
    }

    public PaymentDetailData(final Long id, PaymentTypeData paymentType, final String accountNumber, final String checkNumber,
            final String routingCode, final String receiptNumber, final String bankNumber, final String channelName) {
        this.id = id;
        this.paymentType = paymentType;
        this.accountNumber = accountNumber;
        this.checkNumber = checkNumber;
        this.routingCode = routingCode;
        this.receiptNumber = receiptNumber;
        this.bankNumber = bankNumber;
        this.channelName = channelName;
        this.channelHash = null;
        this.pointOfSales = null;
    }

    public PaymentDetailData(final Long id, PaymentTypeData paymentType, final String accountNumber, final String checkNumber,
            final String routingCode, final String receiptNumber, final String bankNumber, final String channelName,
            final String channelHash) {
        this.id = id;
        this.paymentType = paymentType;
        this.accountNumber = accountNumber;
        this.checkNumber = checkNumber;
        this.routingCode = routingCode;
        this.receiptNumber = receiptNumber;
        this.bankNumber = bankNumber;
        this.channelName = channelName;
        this.channelHash = channelHash;
        this.pointOfSales = null;
    }

}
