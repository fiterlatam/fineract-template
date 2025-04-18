package org.apache.fineract.infrastructure.dataqueries.service;

import org.apache.fineract.infrastructure.dataqueries.events.DatatableEntryEvent;

public interface DatatableEventHandler {

    /**
     * @return the name of the datatable this handler is responsible for
     */
    String getDatatableName();

    /**
     * Handle the datatable event
     *
     * @param event
     *            the event to handle
     */
    void handle(DatatableEntryEvent event);
}
