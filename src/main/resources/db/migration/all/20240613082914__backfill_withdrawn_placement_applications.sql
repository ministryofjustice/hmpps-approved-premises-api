UPDATE placement_applications
SET is_withdrawn = TRUE
WHERE
    decision = 'WITHDRAW' OR
    decision = 'WITHDRAWN_BY_PP'