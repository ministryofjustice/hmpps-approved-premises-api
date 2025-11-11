update departure_reasons
set is_active = false  where id = (select dr.id from departure_reasons dr
                                   where dr.service_scope ='approved-premises' and
                                       dr."name" ='Absconded' and dr.parent_reason_id is null);