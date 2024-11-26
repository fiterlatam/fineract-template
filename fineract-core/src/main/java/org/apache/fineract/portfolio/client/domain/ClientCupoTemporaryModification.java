package org.apache.fineract.portfolio.client.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.apache.fineract.infrastructure.core.domain.AbstractAuditableWithUTCDateTimeCustom;

@Setter
@Getter
@Entity
@Table(name = "m_cupo_temporary_modification")
public class ClientCupoTemporaryModification extends AbstractAuditableWithUTCDateTimeCustom {

    @Column(name = "client_id")
    private Long clientId;

    @Column(name = "document_type")
    private String documentType;

    @Column(name = "is_increment")
    private boolean increment;

    @Column(name = "cupo_max_amount")
    private BigDecimal cupoMaxAmount;

    @Column(name = "original_cupo_max_amount")
    private BigDecimal originalCupoMaxAmount;

    @Column(name = "start_date")
    private LocalDate startOnDate;

    @Column(name = "end_date")
    private LocalDate endOnDate;

    public static ClientCupoTemporaryModification createNew(final Long clientId, final String documentType, final boolean increment,
            final BigDecimal cupoMaxAmount, final BigDecimal originalCupoMaxAmount, final LocalDate startOnDate,
            final LocalDate endOnDate) {
        final ClientCupoTemporaryModification clientCupoTemporaryModification = new ClientCupoTemporaryModification();
        clientCupoTemporaryModification.setClientId(clientId);
        clientCupoTemporaryModification.setDocumentType(documentType);
        clientCupoTemporaryModification.setIncrement(increment);
        clientCupoTemporaryModification.setCupoMaxAmount(cupoMaxAmount);
        clientCupoTemporaryModification.setOriginalCupoMaxAmount(originalCupoMaxAmount);
        clientCupoTemporaryModification.setStartOnDate(startOnDate);
        clientCupoTemporaryModification.setEndOnDate(endOnDate);
        return clientCupoTemporaryModification;
    }
}
