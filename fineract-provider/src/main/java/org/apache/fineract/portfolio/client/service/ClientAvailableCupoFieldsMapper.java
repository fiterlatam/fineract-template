package org.apache.fineract.portfolio.client.service;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.fineract.infrastructure.core.domain.JdbcSupport;
import org.apache.fineract.portfolio.client.data.ClientAvailableCupoFieldsData;
import org.jetbrains.annotations.NotNull;
import org.springframework.jdbc.core.RowMapper;

public class ClienAvailableCupoFieldsMapper implements RowMapper<ClientAvailableCupoFieldsData> {

    public String schema() {
        return """
                mc.id AS clientId,
                cce."NIT" AS nit,
                tipo.code_value AS tipo,
                ccp."Cedula" AS cedula,
                COALESCE(ccp."Cupo aprobado", cce."Cupo") AS cupo,
                (select COALESCE(SUM(ml.principal_outstanding_derived), 0)  FROM m_loan ml WHERE ml.loan_status_id = 300 and client_id =mc.id) AS totalOutstandingPrincipalAmount
                FROM m_client mc
                LEFT JOIN campos_cliente_empresas cce ON cce.client_id = mc.id
                LEFT JOIN m_code_value tipo ON tipo.id = cce."Tipo ID_cd_Tipo ID"
                LEFT JOIN campos_cliente_persona ccp ON ccp.client_id = mc.id
                """;
    }

    @Override
    public ClientAvailableCupoFieldsData mapRow(@NotNull ResultSet rs, int rowNum) throws SQLException {
        final Long clientId = JdbcSupport.getLong(rs, "clientId");
        final String tipo = rs.getString("tipo");
        final String nit = rs.getString("nit");
        final String cedula = rs.getString("cedula");
        final BigDecimal cupo = rs.getBigDecimal("cupo");
        final BigDecimal totalOutstandingPrincipalAmount = rs.getBigDecimal("totalOutstandingPrincipalAmount");
        return new ClientAvailableCupoFieldsData(clientId, tipo, nit, cedula, cupo, totalOutstandingPrincipalAmount);
    }
}
