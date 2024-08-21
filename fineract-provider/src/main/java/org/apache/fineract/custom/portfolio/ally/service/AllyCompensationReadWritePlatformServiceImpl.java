package org.apache.fineract.custom.portfolio.ally.service;

import jakarta.persistence.PersistenceException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.custom.portfolio.ally.data.AllySettlementCompansationCollectionData;
import org.apache.fineract.custom.portfolio.ally.domain.AllyCompensation;
import org.apache.fineract.custom.portfolio.ally.domain.AllyCompensationRepository;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.core.service.database.DatabaseSpecificSQLGenerator;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AllyCompensationReadWritePlatformServiceImpl implements AllyCompensationReadWritePlatformService {

    private final JdbcTemplate jdbcTemplate;
    private final DatabaseSpecificSQLGenerator sqlGenerator;
    private final PlatformSecurityContext context;

    @Autowired
    public AllyCompensationReadWritePlatformServiceImpl(final JdbcTemplate jdbcTemplate, final DatabaseSpecificSQLGenerator sqlGenerator,
            final PlatformSecurityContext context) {
        this.jdbcTemplate = jdbcTemplate;
        this.sqlGenerator = sqlGenerator;
        this.context = context;
    }

    private static final class AllyCompensationSettlementRowMaper implements RowMapper<AllySettlementCompansationCollectionData> {

        public String schema() {
            return " distinct start_date, end_date, cca.nit, cca.company_name,mpd.payment_bank_cv_id as bank_id,maps.purchase_amount, commision_amount, amount_va_commision,purchase_settled_comission, \n"
                    + "collection_amount, purchase_amount-collection_amount as pagar, cca.last_job_run, cca.last_job_run_purchase    \n"
                    + "from custom.c_client_ally cca \n"
                    + "inner join custom.c_client_ally_point_of_sales ccapos  on ccapos.client_ally_id = cca.id \n"
                    + "inner join (select min(purchase_date) as start_date,SUM(maps.amount_to_pay) purchase_amount, SUM(maps.amount_comission) as commision_amount,\n"
                    + "client_ally_id,SUM(maps.amount_va_commision) as amount_va_commision, SUM(maps.settled_comission) as purchase_settled_comission\n"
                    + "from m_ally_purchase_settlement maps\n" + "inner join custom.c_client_ally cca on cca.id = maps.client_ally_id \n"
                    + "group by client_ally_id\n" + ") maps on maps.client_ally_id = cca.id\n"
                    + "inner join (select max(collection_date) as end_date, client_ally_id, SUM(macs.collection_amount)as collection_amount \n"
                    + "from m_ally_collection_settlement macs \n" + "inner join custom.c_client_ally cca on cca.id = macs.client_ally_id \n"
                    + "group by client_ally_id ) macs on macs.client_ally_id = cca.id\n"
                    + "left join m_payment_detail mpd on mpd.point_of_sales_code = ccapos.code";
        }

        @Override
        public AllySettlementCompansationCollectionData mapRow(ResultSet rs, int rowNum) throws SQLException {
            return AllySettlementCompansationCollectionData.builder().nit(rs.getString("nit")).companyName(rs.getString("company_name"))
                    .bankId(rs.getLong("bank_id")).purchaseAmount(rs.getBigDecimal("purchase_amount"))
                    .collectionAmount(rs.getBigDecimal("collection_amount")).vaComissionAmount(rs.getBigDecimal("amount_va_commision"))
                    .purchaceSettlementAmount(rs.getBigDecimal("purchase_settled_comission"))
                    .netOutstandingAmount(rs.getBigDecimal("pagar")).startDate(rs.getString("start_date")).endDate(rs.getString("end_date"))
                    .lastCollectionDate(rs.getString("last_job_run")).lastPurchaseDate(rs.getString("last_job_run_purchase")).build();
        }
    }

    @Override
    public List<AllySettlementCompansationCollectionData> getListCompensationSettlement() {
        final AllyCompensationReadWritePlatformServiceImpl.AllyCompensationSettlementRowMaper rm = new AllyCompensationReadWritePlatformServiceImpl.AllyCompensationSettlementRowMaper();
        final String sql = "select" + rm.schema() + " ";
        return this.jdbcTemplate.query(sql, rm);
    }

    @Autowired
    private AllyCompensationRepository repository;

    @Override
    public void create(AllyCompensation allyCompensation) {
        try {
            this.context.authenticatedUser();
            repository.saveAndFlush(allyCompensation);
        } catch (final PersistenceException e) {
            throw new PlatformDataIntegrityException("error.msg.allyCollection.unknown.data.integrity.issue",
                    "Unknown data integrity issue with resource.");
        }
    }

    @Override
    public void update(AllyCompensation allyCompensation) {
        try {
            this.context.authenticatedUser();
            repository.save(allyCompensation);
        } catch (final PersistenceException e) {
            throw new PlatformDataIntegrityException("error.msg.allyCollection.unknown.data.integrity.issue",
                    "Unknown data integrity issue with resource.");
        }
    }
}
