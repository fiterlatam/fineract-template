package org.apache.fineract.portfolio.insurancenovelty.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.apache.fineract.infrastructure.core.domain.AbstractAuditableWithUTCDateTimeCustom;
import org.apache.fineract.portfolio.charge.domain.Charge;

import java.util.Set;

@Entity
@Table(name = "m_insurance_novelty")
@Getter
@Setter
public class InsuranceEvent extends AbstractAuditableWithUTCDateTimeCustom {


    @Column(name = "name", nullable = false)
    private String name;

    @ManyToMany
    @JoinTable(name = "m_insurance_novelty_linked",
            joinColumns = @JoinColumn(name = "novelty_id"),
            inverseJoinColumns = @JoinColumn(name = "insurance_id"))
    private Set<Charge> linkedInsurances;


}
