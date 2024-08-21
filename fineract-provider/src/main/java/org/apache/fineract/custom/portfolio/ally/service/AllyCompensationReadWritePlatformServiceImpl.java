package org.apache.fineract.custom.portfolio.ally.service;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.custom.portfolio.ally.data.AllySettlementCompansationCollectionData;
import org.apache.fineract.infrastructure.core.service.database.DatabaseSpecificSQLGenerator;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
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

    // private static final class AllyCompensationSettlementRowMaper implements
    // RowMapper<AllySettlementCompansationCollectionData> {
    //
    // public String schema() {
    // return " distinct cca.nit,cca.company_name,mpd.payment_bank_cv_id,maps.purchase_amount, commision_amount,
    // amount_va_commision,purchase_settled_comission, \n"
    // + "collection_amount, purchase_amount-collection_amount as pagar \n" + "from custom.c_client_ally cca \n"
    // + "inner join custom.c_client_ally_point_of_sales ccapos on ccapos.client_ally_id = cca.id \n"
    // + "inner join (select SUM(maps.amount_to_pay) purchase_amount, SUM(maps.amount_comission) as commision_amount,\n"
    // + "client_ally_id,SUM(maps.amount_va_commision) as amount_va_commision, SUM(maps.settled_comission) as
    // purchase_settled_comission\n"
    // + "from m_ally_purchase_settlement maps\n" + "inner join custom.c_client_ally cca on cca.id = maps.client_ally_id
    // \n"
    // + "group by client_ally_id\n" + ") maps on maps.client_ally_id = cca.id\n"
    // + "inner join (select client_ally_id, SUM(macs.collection_amount)as collection_amount \n"
    // + "from m_ally_collection_settlement macs \n" + "inner join custom.c_client_ally cca on cca.id =
    // macs.client_ally_id \n"
    // + "group by client_ally_id ) macs on macs.client_ally_id = cca.id\n"
    // + "left join m_payment_detail mpd on mpd.point_of_sales_code = ccapos.code";
    // }
    //
    // @Override
    // public AllySettlementCompansationCollectionData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int
    // rowNum)
    // throws SQLException {
    // return AllySettlementCompansationCollectionData.builder().lastJobsRun(rs.getString("lastJobsRun"))
    // .nit(rs.getString("nit"))
    // .companyName(rs.getString("company_name"))
    // .bankId(rs.getString("nit"))
    // .nit(rs.getString("nit"))
    // .nit(rs.getString("nit"))
    // .nit(rs.getString("nit"))
    // .build();
    // }
    // }

    @Override
    public List<AllySettlementCompansationCollectionData> getListCompensationSettlement() {
        // final AllyCompensationReadWritePlatformServiceImpl.AllyCompensationSettlementRowMaper rm = new
        // AllyCompensationReadWritePlatformServiceImpl.AllyCompensationSettlementRowMaper();
        // final String sql = "SELECT " + rm.schema() + " ";
        // return this.jdbcTemplate.query(sql, rm);
        return null;
    }
}
