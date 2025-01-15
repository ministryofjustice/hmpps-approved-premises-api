-- ${flyway:timestamp}

DELETE FROM domain_events WHERE domain_events.type in (
    'CAS2_APPLICATION_STATUS_UPDATED', 'CAS2_APPLICATION_SUBMITTED'
);
