UPDATE beds
SET end_date = CURRENT_DATE
WHERE end_date IS NULL
AND room_id IN (
	SELECT distinct(r.id)
	FROM ROOMS r
	JOIN PREMISES p ON (p.id=r.premises_id)
	WHERE p.SERVICE='temporary-accommodation' AND p.STATUS='archived'
);