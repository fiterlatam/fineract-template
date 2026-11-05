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
package org.apache.fineract.portfolio.savings.negativeholds.data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

/** Par retención (20) + liberación (21) candidato a reversarse. */
@Getter
public final class NegativeHoldPairData {

    private final Long holdId;
    private final Long releaseId;
    private final BigDecimal monto;
    private final LocalDate fechaHold;
    private final LocalDate fechaRelease;
    private final BigDecimal saldoHold;
    private final List<String> problemas = new ArrayList<>();

    public NegativeHoldPairData(final Long holdId, final Long releaseId, final BigDecimal monto, final LocalDate fechaHold,
            final LocalDate fechaRelease, final BigDecimal saldoHold) {
        this.holdId = holdId;
        this.releaseId = releaseId;
        this.monto = monto;
        this.fechaHold = fechaHold;
        this.fechaRelease = fechaRelease;
        this.saldoHold = saldoHold;
    }

    private boolean ok = true;

    public void addProblema(final String problema) {
        this.problemas.add(problema);
        this.ok = false;
    }
}
