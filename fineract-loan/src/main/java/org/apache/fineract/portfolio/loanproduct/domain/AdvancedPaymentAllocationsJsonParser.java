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
package org.apache.fineract.portfolio.loanproduct.domain;

import com.google.common.base.Enums;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.LoanScheduleProcessingType;
import org.apache.fineract.portfolio.loanproduct.LoanProductConstants;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AdvancedPaymentAllocationsJsonParser {

    public final AdvancedPaymentAllocationsValidator advancedPaymentAllocationsValidator;

    public List<LoanProductPaymentAllocationRule> assembleLoanProductPaymentAllocationRules(final JsonCommand command,
            String loanTransactionProcessingStrategyCode) {
        JsonArray paymentAllocations = null;
        JsonArray paymentAllocationsFromCommand = command.arrayOfParameterNamed("paymentAllocation");
        if (paymentAllocationsFromCommand != null && !paymentAllocationsFromCommand.isEmpty()) {
            paymentAllocations = updatePaymentAllocationTypesArray(command, command.arrayOfParameterNamed("paymentAllocation"));
        } else {
            paymentAllocations = paymentAllocationsFromCommand;
        }

        List<LoanProductPaymentAllocationRule> productPaymentAllocationRules = null;
        if (paymentAllocations != null) {
            productPaymentAllocationRules = paymentAllocations.asList().stream().map(json -> {
                Map<String, JsonElement> map = json.getAsJsonObject().asMap();
                LoanProductPaymentAllocationRule loanProductPaymentAllocationRule = new LoanProductPaymentAllocationRule();
                populatePaymentAllocationRules(map, loanProductPaymentAllocationRule);
                populateFutureInstallment(map, loanProductPaymentAllocationRule);
                populateTransactionType(map, loanProductPaymentAllocationRule);
                return loanProductPaymentAllocationRule;
            }).toList();
        }
        advancedPaymentAllocationsValidator.validate(productPaymentAllocationRules, loanTransactionProcessingStrategyCode);
        return productPaymentAllocationRules;
    }

    public JsonArray updatePaymentAllocationTypesArray(JsonCommand command, JsonArray array) {
        if (array.isEmpty()) {
            return array;
        }
        JsonArray updatedOrderList = new JsonArray();
        int index = 1;
        JsonArray orderList = array.get(0).getAsJsonObject().getAsJsonArray("paymentAllocationOrder");

        String loanScheduleProcessingType = command.stringValueOfParameterNamed(LoanProductConstants.LOAN_SCHEDULE_PROCESSING_TYPE);
        if (loanScheduleProcessingType == null) {
            loanScheduleProcessingType = "";
        }
        if (loanScheduleProcessingType.equals(LoanScheduleProcessingType.HORIZONTAL.name())) {
            for (int i = 1; i <= 3; i++) {
                for (Object obj : orderList) {
                    JsonObject object = (JsonObject) obj;
                    JsonObject newObj = new JsonObject();
                    String val = object.get("paymentAllocationRule").getAsString();
                    newObj.add("order", new JsonPrimitive(index));
                    if (i == 1) {
                        newObj.add("paymentAllocationRule", new JsonPrimitive("PAST_DUE_" + val));
                    } else if (i == 2) {
                        newObj.add("paymentAllocationRule", new JsonPrimitive("DUE_" + val));
                    } else if (i == 3) {
                        newObj.add("paymentAllocationRule", new JsonPrimitive("IN_ADVANCE_" + val));
                    }
                    if (!updatedOrderList.contains(newObj)) {
                        updatedOrderList.add(newObj);
                    }
                    index++;
                }
            }

        } else if (loanScheduleProcessingType.equals(LoanScheduleProcessingType.VERTICAL.name())) {

            /*
             * for (Object obj : orderList) { JsonObject object = (JsonObject) obj; String val =
             * object.get("paymentAllocationRule").getAsString(); for (int i = 1; i <= 3; i++) { JsonObject newObj = new
             * JsonObject(); newObj.add("order", new JsonPrimitive(index)); if (i == 1) {
             * newObj.add("paymentAllocationRule", new JsonPrimitive("PAST_DUE_" + val)); } else if (i == 2) {
             * newObj.add("paymentAllocationRule", new JsonPrimitive("DUE_" + val)); } else if (i == 3) {
             * newObj.add("paymentAllocationRule", new JsonPrimitive("IN_ADVANCE_" + val)); }
             * updatedOrderList.add(newObj); index++; } }
             *
             */
            // Vertical Loan repayments should behave like Horizontal for Past Due and Due installments
            for (int i = 1; i <= 3; i++) {
                for (Object obj : orderList) {
                    JsonObject object = (JsonObject) obj;
                    JsonObject newObj = new JsonObject();
                    String val = object.get("paymentAllocationRule").getAsString();
                    newObj.add("order", new JsonPrimitive(index));
                    if (i == 1) {
                        newObj.add("paymentAllocationRule", new JsonPrimitive("PAST_DUE_" + val));
                    } else if (i == 2) {
                        newObj.add("paymentAllocationRule", new JsonPrimitive("DUE_" + val));
                    } else if (i == 3) {
                        newObj.add("paymentAllocationRule", new JsonPrimitive("IN_ADVANCE_" + val));
                    }
                    if (!updatedOrderList.contains(newObj)) {
                        updatedOrderList.add(newObj);
                    }
                    index++;
                }
            }
        }
        array.get(0).getAsJsonObject().remove("paymentAllocationOrder");
        array.get(0).getAsJsonObject().add("paymentAllocationOrder", updatedOrderList);

        return array;

    }

    private void populatePaymentAllocationRules(Map<String, JsonElement> map,
            LoanProductPaymentAllocationRule loanProductPaymentAllocationRule) {
        JsonArray paymentAllocationOrder = asJsonArrayOrNull(map.get("paymentAllocationOrder"));
        if (paymentAllocationOrder != null) {
            loanProductPaymentAllocationRule.setAllocationTypes(getPaymentAllocationTypes(paymentAllocationOrder));
        }
    }

    private void populateFutureInstallment(Map<String, JsonElement> map,
            LoanProductPaymentAllocationRule loanProductPaymentAllocationRule) {
        String futureInstallmentAllocationRule = asStringOrNull(map.get("futureInstallmentAllocationRule"));
        if (futureInstallmentAllocationRule != null) {
            loanProductPaymentAllocationRule.setFutureInstallmentAllocationRule(
                    Enums.getIfPresent(FutureInstallmentAllocationRule.class, futureInstallmentAllocationRule).orNull());
        }
    }

    private void populateTransactionType(Map<String, JsonElement> map, LoanProductPaymentAllocationRule loanProductPaymentAllocationRule) {
        String transactionType = asStringOrNull(map.get("transactionType"));
        if (transactionType != null) {
            loanProductPaymentAllocationRule
                    .setTransactionType(Enums.getIfPresent(PaymentAllocationTransactionType.class, transactionType).orNull());
        }
    }

    @NotNull
    private List<PaymentAllocationType> getPaymentAllocationTypes(JsonArray paymentAllocationOrder) {
        if (paymentAllocationOrder != null) {
            List<Pair<Integer, PaymentAllocationType>> parsedListWithOrder = paymentAllocationOrder.asList().stream().map(json -> {
                Map<String, JsonElement> map = json.getAsJsonObject().asMap();
                PaymentAllocationType paymentAllocationType = null;
                String paymentAllocationRule = asStringOrNull(map.get("paymentAllocationRule"));
                if (paymentAllocationRule != null) {
                    paymentAllocationType = Enums.getIfPresent(PaymentAllocationType.class, paymentAllocationRule).orNull();
                }
                return Pair.of(asIntegerOrNull(map.get("order")), paymentAllocationType);
            }).sorted(Comparator.comparing(Pair::getLeft)).toList();
            advancedPaymentAllocationsValidator.validatePairOfOrderAndPaymentAllocationType(parsedListWithOrder);
            return parsedListWithOrder.stream().map(Pair::getRight).toList();
        } else {
            return List.of();
        }
    }

    private Integer asIntegerOrNull(JsonElement element) {
        if (!element.isJsonNull()) {
            return element.getAsInt();
        }
        return null;
    }

    private String asStringOrNull(JsonElement element) {
        if (!element.isJsonNull()) {
            return element.getAsString();
        }
        return null;
    }

    private JsonArray asJsonArrayOrNull(JsonElement element) {
        if (!element.isJsonNull()) {
            return element.getAsJsonArray();
        }
        return null;
    }

}
