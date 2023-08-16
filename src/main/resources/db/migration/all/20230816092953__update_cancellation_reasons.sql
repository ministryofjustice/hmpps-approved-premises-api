-- Retire "Booking successfully appealed" reason
UPDATE cancellation_reasons SET is_active = false WHERE id = 'acba3547-ab22-442d-acec-2652e49895f2';

-- Add "Error in booking" reason
INSERT INTO cancellation_reasons (id, name, is_active, service_scope)
VALUES ('3a5afbfc-3c0f-11ee-be56-0242ac120002', 'Error in booking', true, 'approved-premises');
