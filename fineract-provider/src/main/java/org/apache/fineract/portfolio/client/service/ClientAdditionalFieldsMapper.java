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

package org.apache.fineract.portfolio.client.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.fineract.infrastructure.core.domain.JdbcSupport;
import org.apache.fineract.portfolio.client.data.ClientAdditionalFieldsData;
import org.jetbrains.annotations.NotNull;
import org.springframework.jdbc.core.RowMapper;

public class ClientAdditionalFieldsMapper implements RowMapper<ClientAdditionalFieldsData> {

    public String schema() {
        return """
                mc.id AS clientId,
                cce."NIT" AS nit,
                tipo.code_value AS tipo,
                ccp."Cedula" AS cedula
                FROM m_client mc
                LEFT JOIN campos_cliente_empresas cce ON cce.client_id = mc.id
                LEFT JOIN m_code_value tipo ON tipo.id = cce."Tipo ID_cd_Tipo ID"
                LEFT JOIN campos_cliente_persona ccp ON ccp.client_id = mc.id
                """;
    }

    @Override
    public ClientAdditionalFieldsData mapRow(@NotNull ResultSet rs, int rowNum) throws SQLException {
        final Long clientId = JdbcSupport.getLong(rs, "clientId");
        final String tipo = rs.getString("tipo");
        final String nit = rs.getString("nit");
        final String cedula = rs.getString("cedula");
        return new ClientAdditionalFieldsData(clientId, tipo, nit, cedula);
    }
}
