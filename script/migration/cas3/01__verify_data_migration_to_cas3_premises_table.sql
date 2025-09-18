-- get counts of cas3 data in the existing premises tables
select count(*)
from premises p
join temporary_accommodation_premises tap on tap.premises_id = p.id;

-- get counts where migrated data in cas3_premises matches the cas3 data in the premises and temporary_accommodation_premises tables
select count(c3p.*)
from cas3_premises c3p
join premises p on c3p.id = p.id
join temporary_accommodation_premises tap on tap.premises_id = p.id
where c3p.name = p.name
and c3p.postcode = p.postcode
and (c3p.probation_delivery_unit_id = tap.probation_delivery_unit_id OR (c3p.probation_delivery_unit_id IS NULL AND tap.probation_delivery_unit_id IS NULL))
and (c3p.local_authority_area_id = p.local_authority_area_id OR (c3p.local_authority_area_id IS NULL AND p.local_authority_area_id IS NULL))
and c3p.address_line1 = p.address_line1
and (c3p.address_line2 = p.address_line2 OR (c3p.address_line2 IS NULL AND p.address_line2 IS NULL))
and (c3p.town = p.town OR (c3p.town IS NULL AND p.town IS NULL))
and c3p.status = p.status OR (c3p.status = 'online' and p.status = 'active')
and c3p.start_date = tap.start_date
and (c3p.end_date = tap.end_date OR (c3p.end_date IS NULL AND tap.end_date IS NULL))
and (c3p.notes = p.notes OR (c3p.notes IS NULL AND p.notes IS NULL));