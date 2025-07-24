DELETE from booking_not_mades where id IN (
    select
        bnm.id
    from booking_not_mades bnm
    inner join placement_requests pr on pr.id = bnm.placement_request_id
    where pr.reallocated_at is not null
);

DELETE from placement_requests where reallocated_at is not null;