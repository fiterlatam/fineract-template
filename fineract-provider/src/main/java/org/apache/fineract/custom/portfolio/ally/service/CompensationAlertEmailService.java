package org.apache.fineract.custom.portfolio.ally.service;

public interface CompensationAlertEmailService {

    void sendCompensationAlertEmail(String to, String subject, String body);
}
