package org.apache.fineract.custom.infrastructure.bulkimport.service;

import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import org.apache.fineract.accounting.glaccount.service.GLAccountReadPlatformService;
import org.apache.fineract.custom.infrastructure.bulkimport.data.CustomGlobalEntityType;
import org.apache.fineract.custom.infrastructure.bulkimport.populator.clientally.ClientAllyWorkbookPopulator;
import org.apache.fineract.custom.infrastructure.bulkimport.populator.clientallypointsofsales.ClientAllyPointsOfSalesWorkbookPopulator;
import org.apache.fineract.custom.infrastructure.codes.service.CustomCodeValueReadPlatformService;
import org.apache.fineract.custom.portfolio.ally.data.ClientAllyData;
import org.apache.fineract.custom.portfolio.ally.service.ClientAllyReadWritePlatformService;
import org.apache.fineract.infrastructure.bulkimport.constants.TemplatePopulateImportConstants;
import org.apache.fineract.infrastructure.bulkimport.populator.WorkbookPopulator;
import org.apache.fineract.infrastructure.bulkimport.service.BulkImportWorkbookPopulatorServiceImpl;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.organisation.monetary.service.CurrencyReadPlatformService;
import org.apache.fineract.organisation.office.service.OfficeReadPlatformService;
import org.apache.fineract.organisation.office.service.OfficeReadPlatformServiceImpl;
import org.apache.fineract.organisation.staff.service.StaffReadPlatformService;
import org.apache.fineract.portfolio.charge.service.ChargeReadPlatformService;
import org.apache.fineract.portfolio.client.service.ClientReadPlatformService;
import org.apache.fineract.portfolio.fund.service.FundReadPlatformService;
import org.apache.fineract.portfolio.group.service.CenterReadPlatformService;
import org.apache.fineract.portfolio.group.service.GroupReadPlatformService;
import org.apache.fineract.portfolio.loanaccount.service.LoanReadPlatformService;
import org.apache.fineract.portfolio.loanproduct.service.LoanProductReadPlatformService;
import org.apache.fineract.portfolio.paymenttype.service.PaymentTypeReadPlatformService;
import org.apache.fineract.portfolio.products.service.ShareProductReadPlatformService;
import org.apache.fineract.portfolio.savings.service.DepositProductReadPlatformService;
import org.apache.fineract.portfolio.savings.service.SavingsAccountReadPlatformService;
import org.apache.fineract.portfolio.savings.service.SavingsProductReadPlatformService;
import org.apache.fineract.useradministration.service.RoleReadPlatformService;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CustomBulkImportWorkbookPopulatorServiceImpl extends BulkImportWorkbookPopulatorServiceImpl {

    @Autowired
    public CustomBulkImportWorkbookPopulatorServiceImpl(PlatformSecurityContext context,
            OfficeReadPlatformService officeReadPlatformService, StaffReadPlatformService staffReadPlatformService,
            ClientReadPlatformService clientReadPlatformService, CenterReadPlatformService centerReadPlatformService,
            GroupReadPlatformService groupReadPlatformService, FundReadPlatformService fundReadPlatformService,
            PaymentTypeReadPlatformService paymentTypeReadPlatformService, LoanProductReadPlatformService loanProductReadPlatformService,
            CurrencyReadPlatformService currencyReadPlatformService, LoanReadPlatformService loanReadPlatformService,
            GLAccountReadPlatformService glAccountReadPlatformService, SavingsAccountReadPlatformService savingsAccountReadPlatformService,
            CustomCodeValueReadPlatformService codeValueReadPlatformService,
            SavingsProductReadPlatformService savingsProductReadPlatformService,
            ShareProductReadPlatformService shareProductReadPlatformService, ChargeReadPlatformService chargeReadPlatformService,
            DepositProductReadPlatformService depositProductReadPlatformService, RoleReadPlatformService roleReadPlatformService) {
        super(context, officeReadPlatformService, staffReadPlatformService, clientReadPlatformService, centerReadPlatformService,
                groupReadPlatformService, fundReadPlatformService, paymentTypeReadPlatformService, loanProductReadPlatformService,
                currencyReadPlatformService, loanReadPlatformService, glAccountReadPlatformService, savingsAccountReadPlatformService,
                codeValueReadPlatformService, savingsProductReadPlatformService, shareProductReadPlatformService, chargeReadPlatformService,
                depositProductReadPlatformService, roleReadPlatformService);
    }

    @Autowired
    private ClientAllyReadWritePlatformService clientAllyReadWritePlatformService;

    @Autowired
    private OfficeReadPlatformServiceImpl officeReadPlatformService;

    @Override
    public Response getTemplate(String entityType, Long officeId, Long staffId, final String dateFormat) {
        WorkbookPopulator populator = null;
        final Workbook workbook = new HSSFWorkbook();

        if (entityType != null) {
            if (entityType.trim().equalsIgnoreCase(CustomGlobalEntityType.CLIENT_ALLY.getCode())) {
                populator = populateStaffWorkbook(officeId);
            } else if (entityType.trim().equalsIgnoreCase(CustomGlobalEntityType.CLIENT_ALLY_POINTS_OF_SALES.getCode())) {
                populator = populatePointsOfSalesWorkbook(officeId);

            } else {
                throw new GeneralPlatformDomainRuleException("error.msg.unable.to.find.resource", "Unable to find requested resource");
            }
            populator.populate(workbook, dateFormat);
            return buildResponse(workbook, entityType);
        } else {
            throw new GeneralPlatformDomainRuleException("error.msg.given.entity.type.null", "Given Entity type is null");
        }
    }

    private WorkbookPopulator populateStaffWorkbook(Long officeId) {
        this.context.authenticatedUser().validateHasReadPermission(TemplatePopulateImportConstants.OFFICE_ENTITY_TYPE);

        return new ClientAllyWorkbookPopulator((CustomCodeValueReadPlatformService) this.codeValueReadPlatformService);
    }

    private WorkbookPopulator populatePointsOfSalesWorkbook(Long officeId) {
        this.context.authenticatedUser().validateHasReadPermission(TemplatePopulateImportConstants.OFFICE_ENTITY_TYPE);

        List<ClientAllyData> clientsAllies = fetchClientsAllies(officeId);

        return new ClientAllyPointsOfSalesWorkbookPopulator((CustomCodeValueReadPlatformService) this.codeValueReadPlatformService,
                CustomGlobalEntityType.CLIENT_ALLY_POINTS_OF_SALES.getCode(), clientsAllies);
    }

    private List<ClientAllyData> fetchClientsAllies(final Long officeId) {
        List<ClientAllyData> offices = null;
        if (officeId == null) {
            Boolean includeAllOffices = Boolean.TRUE;
            offices = this.clientAllyReadWritePlatformService.findAllActive();
        } else {
            offices = new ArrayList<>();
            offices.add(this.clientAllyReadWritePlatformService.findById(officeId));
        }
        return offices;
    }
}
