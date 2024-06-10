package org.apache.fineract.portfolio.interestrates.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.domain.AbstractAuditableWithUTCDateTimeCustom;

@Setter
@Getter
@Entity
@Table(name = "m_interest_rate", uniqueConstraints = { @UniqueConstraint(columnNames = { "name" }, name = "unq_name") })
public class InterestRate extends AbstractAuditableWithUTCDateTimeCustom {

    @Column(name = "name")
    private String name;

    @Column(name = "current_rate")
    private BigDecimal currentRate;

    @Column(name = "appliedon_date")
    private LocalDate appliedOnDate;

    @Column(name = "is_active")
    private boolean active;

    @Column(name = "interest_rate_type_id")
    private Integer interestRateTypeId;

    public static InterestRate createNew(final JsonCommand command) {
        final InterestRate interestRate = new InterestRate();
        final String name = command.stringValueOfParameterNamed("name");
        interestRate.setName(name);
        final Integer interestRateId = command.integerValueOfParameterNamed("interestRateTypeId");
        interestRate.setInterestRateTypeId(interestRateId);
        final BigDecimal currentRate = command.bigDecimalValueOfParameterNamed("currentRate");
        interestRate.setCurrentRate(currentRate);
        final LocalDate appliedOnDate = command.localDateValueOfParameterNamed("appliedOnDate");
        interestRate.setAppliedOnDate(appliedOnDate);
        final Boolean active = command.booleanObjectValueOfParameterNamed("active");
        interestRate.setActive(active);
        return interestRate;
    }

    public Map<String, Object> update(final JsonCommand command) {
        final Map<String, Object> actualChanges = new LinkedHashMap<>();
        final String name = "name";
        if (command.isChangeInStringParameterNamed(name, this.name)) {
            final String newValue = command.stringValueOfParameterNamed(name);
            actualChanges.put(name, newValue);
            this.name = newValue;
        }
        final String interestRateTypeId = "interestRateTypeId";
        if (command.isChangeInIntegerParameterNamed(interestRateTypeId, this.interestRateTypeId)) {
            final Integer newValue = command.integerValueOfParameterNamed(interestRateTypeId);
            actualChanges.put(interestRateTypeId, newValue);
            this.interestRateTypeId = newValue;
        }
        final String currentRate = "currentRate";
        if (command.isChangeInBigDecimalParameterNamed(currentRate, this.currentRate)) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamed(currentRate);
            actualChanges.put(currentRate, newValue);
            this.currentRate = newValue;
        }

        final String appliedOnDate = "appliedOnDate";
        if (command.isChangeInLocalDateParameterNamed(appliedOnDate, this.appliedOnDate)) {
            final LocalDate newValue = command.localDateValueOfParameterNamed(appliedOnDate);
            actualChanges.put(appliedOnDate, newValue);
            this.appliedOnDate = newValue;
        }

        final String active = "active";
        if (command.isChangeInBooleanParameterNamed(active, this.active)) {
            final Boolean newValue = command.booleanObjectValueOfParameterNamed(active);
            actualChanges.put(active, newValue);
            this.active = newValue;
        }
        return actualChanges;
    }
}
