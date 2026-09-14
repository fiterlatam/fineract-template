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
package org.apache.fineract.organisation.monetary.domain;

import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.RuleBasedNumberFormat;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Locale;
import javax.annotation.PostConstruct;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MoneyHelper {

    private static RoundingMode roundingMode = null;
    private static MathContext mathContext;
    private static final int PRECISION = 12;

    private static ConfigurationDomainService staticConfigurationDomainService;

    @Autowired
    private ConfigurationDomainService configurationDomainService;

    @PostConstruct
    // This is a hack, but fixing this is not trivial, because some @Entity
    // domain classes use this helper
    @SuppressFBWarnings("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
    public void someFunction() {
        staticConfigurationDomainService = configurationDomainService;
    }

    public static RoundingMode getRoundingMode() {
        if (roundingMode == null) {
            roundingMode = RoundingMode.valueOf(staticConfigurationDomainService.getRoundingMode());
        }
        return roundingMode;
    }

    public static MathContext getMathContext() {
        if (mathContext == null) {
            mathContext = new MathContext(PRECISION, getRoundingMode());
        }
        return mathContext;
    }

    public static String getMoneyString(BigDecimal value) {

        if (value == null) {
            return "";
        }

        value = value.setScale(2, RoundingMode.HALF_UP);
        String moneyFormatted = "(" + getNumberMoneyFormat(value) + ")";
        String result;
        if (value.stripTrailingZeros().scale() <= 0) {
            result = getNumberInLetters(value, true).concat(" QUETZALES EXACTOS ");
        } else {
            int centavos = value.remainder(BigDecimal.ONE).movePointRight(2).intValue();
            String centavosStr = getNumberInLetters(new BigDecimal(centavos), true);
            result = getNumberInLetters(value.setScale(0, RoundingMode.DOWN), false).concat(" QUETZALES CON " + centavosStr + " CENTAVOS ");
        }

        return result + " " + moneyFormatted;
    }

    private static String getNumberInLetters(BigDecimal value, Boolean uppercase) {
        RuleBasedNumberFormat rbnf = new RuleBasedNumberFormat(new Locale("es"), RuleBasedNumberFormat.SPELLOUT);
        BigDecimal bd = value.setScale(2, RoundingMode.HALF_UP);
        return uppercase ? rbnf.format(bd).toUpperCase() : rbnf.format(bd);
    }

    private static String getNumberMoneyFormat(BigDecimal bd) {
        if (bd == null) {
            return "";
        }

        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("es", "GT"));
        String formatted = nf.format(bd);
        formatted = formatted.replace("GTQ", "Q.");

        return formatted;
    }
}
