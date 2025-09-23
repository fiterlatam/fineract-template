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
package org.apache.fineract.custom.portfolio.blockaccounts.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

/**
 * Swagger documentation for LoanAccountBlockApiResource.
 */
final class LoanAccountBlockApiResourceSwagger {

    private LoanAccountBlockApiResourceSwagger() {
        // Solo para documentación Swagger
    }

    @Schema(description = "GetLoanAccountBlockResponse")
    public static final class GetLoanAccountBlockResponse {

        private GetLoanAccountBlockResponse() {}

        @Schema(example = "1001", description = "Identificador del bloqueo de cuenta de préstamo")
        public Long id;

        @Schema(example = "2005", description = "ID del préstamo asociado")
        public Long loanId;

        @Schema(example = "Pago atrasado", description = "Razón del bloqueo")
        public String reason;

        @Schema(example = "ACTIVE", description = "Estado del bloqueo (ej. ACTIVE, UNBLOCKED)")
        public String status;

        @Schema(example = "2025-09-23", description = "Fecha de creación del bloqueo")
        public LocalDate createdOn;
    }

    @Schema(description = "GetLoanAccountBlockHistoryResponse")
    public static final class GetLoanAccountBlockHistoryResponse {

        private GetLoanAccountBlockHistoryResponse() {}

        @Schema(description = "Historial de bloqueos de la cuenta de préstamo")
        public List<GetLoanAccountBlockResponse> history;
    }

    @Schema(description = "PostLoanAccountBlockRequest")
    public static final class PostLoanAccountBlockRequest {

        private PostLoanAccountBlockRequest() {}

        @Schema(example = "Falta de pago", description = "Razón del bloqueo")
        public String reason;

        @Schema(example = "ADMIN", description = "Usuario que realiza la acción")
        public String requestedBy;
    }

    @Schema(description = "PostLoanAccountBlockResponse")
    public static final class PostLoanAccountBlockResponse {

        private PostLoanAccountBlockResponse() {}

        @Schema(example = "3001", description = "Identificador del recurso creado")
        public Long resourceId;
    }

    @Schema(description = "PostUnblockLoanAccountRequest")
    public static final class PostUnblockLoanAccountRequest {

        private PostUnblockLoanAccountRequest() {}

        @Schema(example = "Pago recibido", description = "Razón para desbloquear")
        public String reason;
    }

    @Schema(description = "PostUnblockLoanAccountResponse")
    public static final class PostUnblockLoanAccountResponse {

        private PostUnblockLoanAccountResponse() {}

        @Schema(example = "3001", description = "Identificador del recurso desbloqueado")
        public Long resourceId;
    }

    @Schema(description = "PutLoanAccountBlockRequest")
    public static final class PutLoanAccountBlockRequest {

        private PutLoanAccountBlockRequest() {}

        @Schema(example = "Pago parcial recibido", description = "Nueva razón o detalle del bloqueo")
        public String reason;

        @Schema(example = "IN_REVIEW", description = "Nuevo estado del bloqueo")
        public String status;
    }

    @Schema(description = "PutLoanAccountBlockResponse")
    public static final class PutLoanAccountBlockResponse {

        private PutLoanAccountBlockResponse() {}

        @Schema(example = "3001", description = "ID del bloqueo actualizado")
        public Long resourceId;

        @Schema(description = "Cambios aplicados al recurso")
        public PutLoanAccountBlockChanges changes;

        public static final class PutLoanAccountBlockChanges {

            private PutLoanAccountBlockChanges() {}

            @Schema(example = "Pago parcial recibido", description = "Razón actualizada")
            public String reason;

            @Schema(example = "IN_REVIEW", description = "Estado actualizado")
            public String status;
        }
    }
}
