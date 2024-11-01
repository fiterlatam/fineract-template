package org.apache.fineract.portfolio.loanaccount.api;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.infrastructure.core.api.ApiRequestParameterHelper;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.serialization.ApiRequestJsonSerializationSettings;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.loanaccount.data.LoanCreditNoteData;
import org.apache.fineract.portfolio.loanaccount.service.LoanCreditNoteReadService;
import org.springframework.stereotype.Component;

@Path("/v1/loans/{loanId}/credit-notes")
@Component
@RequiredArgsConstructor
public class LoanCreditNoteApiResource {

    private static final String RESOURCE_NAME_FOR_PERMISSIONS = "CREDIT_NOTE";
    private final PlatformSecurityContext context;
    private final ApiRequestParameterHelper apiRequestParameterHelper;
    private final DefaultToApiJsonSerializer<LoanCreditNoteData> toApiJsonSerializer;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    private final LoanCreditNoteReadService loanCreditNoteReadService;

    @POST
    @Path("")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String addCreditNote(@PathParam("loanId") final Long loanId, final String apiRequestBodyAsJson) {
        final CommandWrapper commandRequest = new CommandWrapperBuilder().addLoanCreditNote(loanId).withJson(apiRequestBodyAsJson).build();

        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);

        return this.toApiJsonSerializer.serialize(result);

    }

    // API to get credit notes for a loan
    @GET
    @Path("")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveLoanCreditNotes(@PathParam("loanId") final Long loanId, @Context final UriInfo uriInfo) {

        this.context.authenticatedUser().validateHasReadPermission(RESOURCE_NAME_FOR_PERMISSIONS);

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        Collection<LoanCreditNoteData> loanCreditNoteCollection = this.loanCreditNoteReadService.retrieveAllCreditNotesForLoan(loanId);

        return toApiJsonSerializer.serialize(settings, loanCreditNoteCollection);
    }

    @GET
    @Path("{creditNoteId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveLoanCreditNotes(@PathParam("loanId") final Long loanId, @PathParam("creditNoteId") final Long creditNoteId,
            @Context final UriInfo uriInfo) {

        this.context.authenticatedUser().validateHasReadPermission(RESOURCE_NAME_FOR_PERMISSIONS);

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        LoanCreditNoteData loanCreditNoteData = this.loanCreditNoteReadService.retrieveCreditNoteForLoan(loanId, creditNoteId);

        return this.toApiJsonSerializer.serialize(settings, loanCreditNoteData);
    }
}
