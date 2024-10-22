package org.apache.fineract.portfolio.loanaccount.rescheduleloan.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.apache.fineract.infrastructure.core.service.database.DatabaseSpecificSQLGenerator;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.loanaccount.data.LoanArchiveHistoryData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
public class LoanArchiveHistoryServiceReadWritePlatformImpl implements LoanArchiveHistoryReadWritePlatformService {

    private final JdbcTemplate jdbcTemplate;
    private final DatabaseSpecificSQLGenerator sqlGenerator;
    private final PlatformSecurityContext context;

    @Autowired
    public LoanArchiveHistoryServiceReadWritePlatformImpl(JdbcTemplate jdbcTemplate, DatabaseSpecificSQLGenerator sqlGenerator,
            PlatformSecurityContext context) {
        this.jdbcTemplate = jdbcTemplate;
        this.sqlGenerator = sqlGenerator;
        this.context = context;
    }

    private static final class ClientLoanArchiveHistoryMaper implements RowMapper<LoanArchiveHistoryData> {

        public String schema() {
            return "WITH RankedReasons AS (\n" + "    SELECT mcbr.client_id, \n" + "           mbrs.name_of_reason, \n"
                    + "           mbrs.priority,\n"
                    + "           ROW_NUMBER() OVER (PARTITION BY mcbr.client_id ORDER BY mbrs.priority ASC) AS row_num\n"
                    + "    FROM m_client_blocking_reason mcbr\n"
                    + "    INNER JOIN m_blocking_reason_setting mbrs ON mbrs.id = mcbr.blocking_reason_id\n"
                    + "    WHERE mcbr.unblock_date IS NULL\n" + "),\n" + "RankedCreaditReasons AS (\n" + "    SELECT mcbr.loan_id, \n"
                    + "           mbrs.name_of_reason, \n" + "           mbrs.priority,\n"
                    + "           ROW_NUMBER() OVER (PARTITION BY mcbr.loan_id ORDER BY mbrs.priority ASC) AS row_num\n"
                    + "    FROM m_credit_blocking_reason mcbr\n"
                    + "    INNER JOIN m_blocking_reason_setting mbrs ON mbrs.id = mcbr.blocking_reason_id\n"
                    + "    WHERE mcbr.is_active = false\n" + ")\n" + "SELECT mc.id AS client_id, \n" + "       ml.id AS loan_id,\n"
                    + "       COALESCE(mc.firstname,'') AS firstname, \n" + "       COALESCE(mc.middlename,'') AS middlename, \n"
                    + " COALESCE(mc.second_lastname,'') AS second_lastname, \n"
                    + " mc.display_name, COALESCE(mc.lastname,'') AS lastname, \n" + "       cbr.name_of_reason,\n"
                    + "       COALESCE(cce.\"NIT\", ccp.\"Cedula\") AS nit_empresa, COALESCE(cce.\"Cupo\", ccp.\"Cupo solicitado\") AS cupo, \n"
                    + "       COALESCE(ccp.\"Telefono\" , cce.\"Telefono\") AS telefono, \n"
                    + "       ccp.\"Celular Referencia\" AS celuar,\n" + "       mc.email_address,\n"
                    + "       COALESCE(ccp.\"Direccion\",cce.\"Direccion\") AS direction, \n"
                    + "       COALESCE(ccp.\"Ciudad_cd_Ciudad\",cce.\"Ciudad_cd_Ciudad\") as ciudad, \n"
                    + "       cce.\"Departamento_cd_Departamento\" as departamento,\n" + "       mcbr.name_of_reason AS creaditBlock, \n"
                    + "       ccp.\"Referencia\"AS referencia,\n" + "       ccp.\"Media de ingresos\" AS media_de_ingreso,  \n"
                    + "       ccp.\"Nombre empresa\" AS nombre_empresa, \n"
                    + "       mc.created_on_utc, ccp.\"Estado Civil_cd_Estado civil\" as estado_civil, \n"
                    + "       ml.disbursedon_date, \n" + "       mc.date_of_birth,\n" + "       (SELECT code_value \n"
                    + "        FROM m_code_value \n"
                    + "        WHERE id = mc.gender_cv_id) AS gender, ccp.\"Actividad Laboral_cd_Actividad laboral\" as activeLab\n"
                    + "FROM m_client mc\n" + "INNER JOIN m_loan ml ON ml.client_id = mc.id \n" + "LEFT JOIN (\n"
                    + "    SELECT client_id, name_of_reason, priority\n" + "    FROM RankedReasons\n" + "    WHERE row_num = 1\n"
                    + ") AS cbr ON cbr.client_id = mc.id\n" + "LEFT JOIN campos_cliente_persona ccp ON ccp.client_id = mc.id\n"
                    + "LEFT JOIN campos_cliente_empresas cce ON cce.client_id = mc.id \n" + "LEFT JOIN (\n"
                    + "    SELECT loan_id, name_of_reason, priority\n" + "    FROM RankedCreaditReasons\n" + "    WHERE row_num = 1\n"
                    + ") AS mcbr ON mcbr.loan_id = ml.id\n" + "WHERE total_outstanding_derived > 0  \n"
                    + "  and loan_status_id NOT IN (500, 601, 602, 600)\n" + "ORDER BY mc.id, ml.id DESC;\n";
        }

        @Override
        public LoanArchiveHistoryData mapRow(ResultSet rs, int rowNum) throws SQLException {
            return LoanArchiveHistoryData.builder().identificacion(rs.getInt("client_id")).primerNombre(rs.getString("firstname"))
                    .segundoNombre(rs.getString("middlename")).segundoApellido(rs.getString("second_lastname"))
                    .primerApellido(rs.getString("lastname")).estadoCliente(rs.getString("name_of_reason"))
                    .numeroObligacion(rs.getString("loan_id")).nitEmpresa(rs.getString("nit_empresa")).telefonoSac(rs.getString("telefono"))
                    .celularSac(rs.getString("celuar")).emailSac(rs.getString("email_address")).direccionSac(rs.getString("direction"))
                    .barrioSac(rs.getString("direction")).ciudadSac(rs.getInt("ciudad")).departamento(rs.getString("departamento"))
                    .razonSocial(rs.getString("firstname") + " " + rs.getString("middlename") + " " + rs.getString("second_lastname"))
                    .nombreFamiliar(rs.getString("nombre_empresa")).parentescoFamiliar(rs.getString("referencia"))
                    .fechaFinanciacion(rs.getString("disbursedon_date")).genero(rs.getString("gender"))
                    .ingresos(rs.getBigDecimal("media_de_ingreso")).antiguedadCliente(rs.getString("disbursedon_date"))
                    .actividadLaboral(rs.getString("activeLab")).creSaldo(rs.getBigDecimal("cupo")).cuoSaldo(rs.getBigDecimal("cupo"))
                    .cuoEstado(rs.getString("name_of_reason")).estadoCivil(rs.getString("estado_civil"))
                    .fechaNacimiento(rs.getString("date_of_birth")).build();
        }
    }

    @Override
    public List<LoanArchiveHistoryData> getLoanArchiveCollectionData() {
        final LoanArchiveHistoryServiceReadWritePlatformImpl.ClientLoanArchiveHistoryMaper rm = new LoanArchiveHistoryServiceReadWritePlatformImpl.ClientLoanArchiveHistoryMaper();
        final String sql = rm.schema() + " ";
        return this.jdbcTemplate.query(sql, rm);
    }
}
