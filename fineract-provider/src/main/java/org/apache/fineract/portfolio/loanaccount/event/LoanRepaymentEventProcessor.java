package org.apache.fineract.portfolio.loanaccount.event;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.commands.event.BaseCustomWebhookEventProcessorImpl;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.springframework.stereotype.Component;

@Component()
@Slf4j
public class LoanRepaymentEventProcessor extends BaseCustomWebhookEventProcessorImpl {

    @Override
    protected List<Map<String, String>> getSupportedEvents() {
        Map<String, String> loanEvent = Map.of("entityName", "LOAN", "actionName", "REPAYMENT");
        return Collections.singletonList(loanEvent);
    }

    @Override
    public Map<String, Object> transform(String entityName, String actionName, JsonCommand command, Object result) {
        Map<String, Object> response = new HashMap<>();
        response.put("loanId", "000123"); // {ACCOUNT_ID}
        response.put("amount", "1.234,56"); // FORMAT{TRANSACTION_AMOUNT,numericPattern=#.###,##}
        response.put("arrearDue", "234,50"); // FORMAT{ARREARS_POSITION,numericPattern=#.###,##}
        response.put("paymentDate", "2024-01-15"); // FORMAT_DATE{TRANSACTION_DATE,datePattern=yyyy-MM-dd}
        response.put("document", "123456789"); // {CF:LOAN:NÚMERO DE IDENTIFICACIÓN}
        response.put("documentType", "CC"); // {CF:LOAN:TIPO IDENTIFICACIÓN}
        response.put("fullname", "John Doe"); // {RECIPIENT_NAME}
        response.put("loanAmount", "5.000,00"); // FORMAT{LOAN_AMOUNT,numericPattern=#.###,##}
        response.put("mobilePhone", "+1234567890"); // {RECIPIENT_MOBILE_PHONE}
        response.put("principalBalance", "3.765,44"); // FORMAT{PRINCIPAL_BALANCE,numericPattern=#.###,##}
        response.put("totalDue", "4.000,00"); // FORMAT{TOTAL_DUE,numericPattern=#.###,##}
        response.put("externalId", "EXT123"); // {CF:CLIENT:EXTERNAL ID}

        return response;
    }
}
