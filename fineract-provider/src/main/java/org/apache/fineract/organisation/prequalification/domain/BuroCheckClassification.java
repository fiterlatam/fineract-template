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
package org.apache.fineract.organisation.prequalification.domain;

import java.util.Arrays;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;

@AllArgsConstructor
@Getter
public enum BuroCheckClassification {

    CLASSIFICATION_X(0, "Sin Categoria", "buro.check.classification.grey"), CLASSIFICATION_A(1, "A",
            "buro.check.classification.green"), CLASSIFICATION_B(2, "B", "buro.check.classification.yellow"), CLASSIFICATION_C(3, "C",
                    "buro.check.classification.orange"), CLASSIFICATION_D(4, "D", "buro.check.classification.red");

    private final Integer id;
    private final String letter;
    private final String color;

    public static BuroCheckClassification fromInt(final Integer id) {
        return Arrays.stream(values()).filter(classification -> Objects.equals(classification.id, id)).findFirst()
                .orElse(BuroCheckClassification.CLASSIFICATION_X);
    }

    public static BuroCheckClassification fromLetter(final String letter) {
        return Arrays.stream(values()).filter(classification -> Objects.equals(classification.letter, letter)).findFirst()
                .orElse(BuroCheckClassification.CLASSIFICATION_X);
    }

    public static BuroCheckClassification fromColor(final String color) {
        return Arrays.stream(values()).filter(classification -> Objects.equals(classification.color, color)).findFirst()
                .orElse(BuroCheckClassification.CLASSIFICATION_X);
    }

    public static EnumOptionData status(final Integer statusInt) {
        BuroCheckClassification buroCheckClassification = BuroCheckClassification.fromInt(statusInt);
        return new EnumOptionData(buroCheckClassification.id.longValue(), buroCheckClassification.letter, buroCheckClassification.color);
    }

}
