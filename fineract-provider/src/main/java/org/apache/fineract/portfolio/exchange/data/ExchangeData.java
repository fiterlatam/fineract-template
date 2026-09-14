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
package org.apache.fineract.portfolio.exchange.data;

import java.math.BigDecimal;
import java.util.Date;
import org.apache.fineract.useradministration.data.AppUserData;

public final class ExchangeData {

    private final Long id;
    private final Integer destinationCurrency;
    private final Integer originCurrency;
    private final BigDecimal exchangeRate;
    private final Date validFrom;
    private final Date createdOn;
    private AppUserData createdBy;
    private final Long createdById;

    private ExchangeData(Long id, Integer destinationCurrency, Integer originCurrency, BigDecimal exchangeRate, Date validFrom,
            Date createdOn, AppUserData createdBy) {
        this.id = id;
        this.destinationCurrency = destinationCurrency;
        this.originCurrency = originCurrency;
        this.exchangeRate = exchangeRate;
        this.validFrom = validFrom;
        this.createdOn = createdOn;
        this.createdBy = createdBy;
        this.createdById = (createdBy != null) ? createdBy.getId() : null;
    }

    private ExchangeData(Long id, Integer destinationCurrency, Integer originCurrency, BigDecimal exchangeRate, Date validFrom,
            Date createdOn, AppUserData createdBy, Long createdById) {
        this.id = id;
        this.destinationCurrency = destinationCurrency;
        this.originCurrency = originCurrency;
        this.exchangeRate = exchangeRate;
        this.validFrom = validFrom;
        this.createdOn = createdOn;
        this.createdBy = createdBy;
        this.createdById = createdById;
    }

    public static ExchangeData template() {
        return new ExchangeData(Long.getLong("0"), 0, 0, BigDecimal.ZERO, new Date(), new Date(), null);
    }

    public static ExchangeData instance(Long id, Integer destinationCurrency, Integer originCurrency, BigDecimal exchangeRate,
            Date validFrom, Date createdOn, AppUserData createdBy, Long createdById) {
        return new ExchangeData(id, destinationCurrency, originCurrency, exchangeRate, validFrom, createdOn, createdBy, createdById);
    }

    public Long getId() {
        return id;
    }

    public Long getCreatedById() {
        return createdById;
    }

    public void setCreatedBy(AppUserData createdBy) {
        this.createdBy = createdBy;
    }
}
