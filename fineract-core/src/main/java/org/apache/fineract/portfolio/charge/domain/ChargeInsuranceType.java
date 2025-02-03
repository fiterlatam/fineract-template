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
package org.apache.fineract.portfolio.charge.domain;

public enum ChargeInsuranceType {

    INVALID(0, "chargeInsuranceType.invalid"), //
    COMPRA(1, "chargeInsuranceType.compra"), CARGO(2, "chargeInsuranceType.cargo");

    private final Integer value;
    private final String code;

    ChargeInsuranceType(final Integer value, final String code) {
        this.value = value;
        this.code = code;
    }

    public Integer getValue() {
        return this.value;
    }

    public String getCode() {
        return this.code;
    }

    public static ChargeInsuranceType fromInt(final Integer type) {
        ChargeInsuranceType chargeInsuranceType = ChargeInsuranceType.INVALID;
        if (type != null) {
            switch (type) {
                case 1:
                    chargeInsuranceType = ChargeInsuranceType.COMPRA;
                break;
                case 2:
                    chargeInsuranceType = ChargeInsuranceType.CARGO;
                break;
            }
        }
        return chargeInsuranceType;
    }

    public boolean isCompra() {
        return this.value.equals(ChargeInsuranceType.COMPRA.getValue());
    }

    public boolean isCargo() {
        return this.value.equals(ChargeInsuranceType.CARGO.getValue());
    }
}
