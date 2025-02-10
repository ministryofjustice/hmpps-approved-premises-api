--function to add working days onto a given date.
CREATE OR REPLACE FUNCTION add_working_days(
    start_date DATE,
    working_days_to_add INTEGER,
    bank_holidays DATE[]
) RETURNS DATE AS
$$
DECLARE
    date_to_check DATE    := start_date;
    added_days    INTEGER := 0;
BEGIN
    -- If no working days need to be added, return the given date
    IF working_days_to_add = 0 THEN
        RETURN start_date;
    END IF;

    -- loop until the required number of working days have been added.
    WHILE added_days < working_days_to_add
        LOOP
            date_to_check := date_to_check + INTERVAL '1 day';

            -- count only if it's a working day (Mon-Fri, and not a bank holiday)
            IF EXTRACT(ISODOW FROM date_to_check) NOT IN (6, 7)
                AND date_to_check NOT IN (SELECT UNNEST(bank_holidays)) THEN
                added_days := added_days + 1;
            END IF;
        END LOOP;

    RETURN date_to_check;
END;
$$ LANGUAGE plpgsql;
