ALTER TABLE arrivals ADD COLUMN arrival_date_time TIMESTAMP WITH TIME ZONE;

UPDATE arrivals SET arrival_date_time = cast(arrival_date as timestamp) at time zone 'utc';
