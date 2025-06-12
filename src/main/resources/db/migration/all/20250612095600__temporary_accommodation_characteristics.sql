delete from premises_characteristics
where characteristic_id in (
    select id
    from characteristics
    where service_scope = 'temporary-accommodation'
    and model_scope = 'room'
    and name = 'Wheelchair accessible'
)
