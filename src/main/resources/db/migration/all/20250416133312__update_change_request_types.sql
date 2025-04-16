UPDATE cas1_change_request_reasons SET change_request_type = 'PLACEMENT_APPEAL' WHERE  change_request_type = 'APPEAL';
UPDATE cas1_change_request_reasons SET change_request_type = 'PLACEMENT_EXTENSION' WHERE  change_request_type = 'EXTENSION';

UPDATE cas1_change_request_rejection_reasons SET change_request_type = 'PLACEMENT_APPEAL' WHERE  change_request_type = 'APPEAL';
UPDATE cas1_change_request_rejection_reasons SET change_request_type = 'PLACEMENT_EXTENSION' WHERE  change_request_type = 'EXTENSION';
