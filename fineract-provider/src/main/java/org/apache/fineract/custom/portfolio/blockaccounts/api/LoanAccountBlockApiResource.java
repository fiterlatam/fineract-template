package org.apache.fineract.custom.portfolio.blockaccounts.api;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.custom.portfolio.blockaccounts.data.LoanAccountBlockDTO;
import org.apache.fineract.custom.portfolio.blockaccounts.service.LoanAccountBlockReadPlatformService;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.security.service.PlatformUserRightsContext;
import org.springframework.stereotype.Component;

@Path("/v1/blockaccount")
@Component
@Tag(name = "blockaccount", description = "blockaccount")
@RequiredArgsConstructor
public class LoanAccountBlockApiResource {

    private final PlatformUserRightsContext platformUserRightsContext;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    private final DefaultToApiJsonSerializer<LoanAccountBlockDTO> apiJsonSerializerService;
    private final LoanAccountBlockReadPlatformService loanAccountBlockReadPlatformService;

    @POST
    @Path("{loanId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String addBlockAccount(@PathParam("loanId") final Long loanId, final String apiRequestBodyAsJson) {
        platformUserRightsContext.isAuthenticated();
        final CommandWrapper commandWrapper = new CommandWrapperBuilder().withLoanId(loanId).withJson(apiRequestBodyAsJson)
                .createLoanBlockAccount().build();
        CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandWrapper);
        return apiJsonSerializerService.serialize(result);
    }

    @GET
    @Path("{loanId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String getBlockAccounts(@PathParam("loanId") Long loanId) {
        platformUserRightsContext.isAuthenticated();
        LoanAccountBlockDTO loanAccountBlockDTO = loanAccountBlockReadPlatformService.retrieveByLoanId(loanId);
        return apiJsonSerializerService.serialize(loanAccountBlockDTO);
    }
}
