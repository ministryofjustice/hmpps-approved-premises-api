-- Retire "Error in booking" reason
UPDATE cancellation_reasons SET is_active = false WHERE id = '3a5afbfc-3c0f-11ee-be56-0242ac120002';