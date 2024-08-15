package org.apache.fineract.portfolio.insurancenovelty.service;

import org.apache.fineract.portfolio.insurancenovelty.data.InsuranceNoveltyData;

import java.util.Collection;


public interface InsuranceNoveltyService
{
    Collection<InsuranceNoveltyData> retrieveAllInsuranceNovelties();
}
