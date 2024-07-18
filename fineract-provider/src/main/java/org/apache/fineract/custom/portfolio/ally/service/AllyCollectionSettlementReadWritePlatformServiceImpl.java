package org.apache.fineract.custom.portfolio.ally.service;

import jakarta.persistence.PersistenceException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.custom.portfolio.ally.data.ClientAllyPointOfSalesCollectionData;
import org.apache.fineract.custom.portfolio.ally.domain.AllyCollectionSettlement;
import org.apache.fineract.custom.portfolio.ally.domain.AllyCollectionSettlementRepository;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.core.service.database.DatabaseSpecificSQLGenerator;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AllyCollectionSettlementReadWritePlatformServiceImpl implements AllyCollectionSettlementReadWritePlatformService {

    private final JdbcTemplate jdbcTemplate;
    private final DatabaseSpecificSQLGenerator sqlGenerator;
    private final PlatformSecurityContext context;

    @Autowired
    public AllyCollectionSettlementReadWritePlatformServiceImpl(final JdbcTemplate jdbcTemplate,
            final DatabaseSpecificSQLGenerator sqlGenerator, final PlatformSecurityContext context) {
        this.jdbcTemplate = jdbcTemplate;
        this.sqlGenerator = sqlGenerator;
        this.context = context;
    }

    private static final class ClientAllyPointOfSalesCollectionRowMapper implements RowMapper<ClientAllyPointOfSalesCollectionData> {

        public String schema() {
            return " cca.last_job_run as lastJobsRun, ccbp.requested_date as collectionDate, cca.nit as nit , cca.company_name as allyName,"
                    + " ccapos.client_ally_id as clientAllyId, ccapos.id as pointofsalesid, ccapos.\"name\"  as pointOfSalesName,"
                    + " ccapos.code as pointOfSalesCode , ccapos.city_id, ccbp.amount, ccapos.settled_comission as settledComission,"
                    + " tax_profile_id, ccbp.channel_id , ccbp.loan_id ,ccbp.client_id, cca.liquidation_frequency_id as liquidationFrequencyId, ml.loan_status_id as loanStatusId "
                    + " FROM custom.c_client_buy_process ccbp  inner join m_loan ml on ml.id =ccbp.loan_id "
                    + " inner join custom.c_client_ally_point_of_sales ccapos on ccapos.id = ccbp.point_if_sales_id "
                    + " inner join custom.c_client_ally cca on cca.id =ccapos.client_ally_id ";
        }

        @Override
        public ClientAllyPointOfSalesCollectionData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum)
                throws SQLException {
            return ClientAllyPointOfSalesCollectionData.builder().lastJobsRun(rs.getString("lastJobsRun"))
                    .collectionDate(rs.getString("collectionDate")).nit(rs.getString("nit")).name(rs.getString("allyName"))
                    .clientAllyId(rs.getLong("clientAllyId")).pointOfSalesId(rs.getLong("pointofsalesid"))
                    .pointOfSalesName(rs.getString("pointOfSalesName")).amount(rs.getBigDecimal("amount"))
                    .settledComission(rs.getInt("settledComission")).cityId(rs.getLong("city_id")).taxId(rs.getInt("tax_profile_id"))
                    .channelId(rs.getLong("channel_id")).loanId(rs.getLong("loan_id")).clientId(rs.getLong("client_id"))
                    .liquidationFrequencyId(rs.getLong("liquidationFrequencyId")).loanStatusId(rs.getInt("loanStatusId")).build();
        }
    }

    @Override
    public List<ClientAllyPointOfSalesCollectionData> getCollectionData() {
        final AllyCollectionSettlementReadWritePlatformServiceImpl.ClientAllyPointOfSalesCollectionRowMapper rm = new AllyCollectionSettlementReadWritePlatformServiceImpl.ClientAllyPointOfSalesCollectionRowMapper();
        final String sql = "SELECT " + rm.schema() + " ";

        return this.jdbcTemplate.query(sql, rm);
    }

    @Override
    public ClientAllyPointOfSalesCollectionData getCollectionDataByLoanId(Long loanId) {
        final AllyCollectionSettlementReadWritePlatformServiceImpl.ClientAllyPointOfSalesCollectionRowMapper rm = new AllyCollectionSettlementReadWritePlatformServiceImpl.ClientAllyPointOfSalesCollectionRowMapper();
        final String sql = "SELECT " + rm.schema() + " Where loan_id = ?";
        return this.jdbcTemplate.queryForObject(sql, rm, loanId);
    }

    @Autowired
    private AllyCollectionSettlementRepository repository;

    public void create(AllyCollectionSettlement allyCollectionSettlement) {
        try {
            this.context.authenticatedUser();
            repository.saveAndFlush(allyCollectionSettlement);
        } catch (final PersistenceException e) {
            throw new PlatformDataIntegrityException("error.msg.allyCollection.unknown.data.integrity.issue",
                    "Unknown data integrity issue with resource.");
        }
    }

    public void update(AllyCollectionSettlement allyCollectionSettlement) {
        try {
            this.context.authenticatedUser();
            repository.save(allyCollectionSettlement);
        } catch (final PersistenceException e) {
            throw new PlatformDataIntegrityException("error.msg.allyCollection.unknown.data.integrity.issue",
                    "Unknown data integrity issue with resource.");
        }
    }

}
