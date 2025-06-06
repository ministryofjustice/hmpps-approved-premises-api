-- get counts of cas3 room characteristics data in the existing characteristics table
select count(c.*)
from premises p
join temporary_accommodation_premises tap on tap.premises_id = p.id
join rooms r on r.premises_id = p.id
join room_characteristics rc on rc.room_id = r.id
join characteristics c on c.id = rc.characteristic_id;

-- get counts where migrated data in cas3_bedspace_characteristics matches the cas3 room characteristics data in characteristics table
select count(c3bc.*)
from cas3_bedspaces c3b
join cas3_bedspace_characteristic_mappings c3bcm on c3bcm.bedspace_id = c3b.id
join cas3_bedspace_characteristics c3bc on c3bc.id = c3bcm.bedspace_id
join beds b on b.id = c3b.id
join rooms r on r.id = b.room_id
join room_characteristics rc on rc.room_id = r.id
join characteristics c on c.id = rc.characteristic_id
where c3bc.name = c.name
and (c3bc.property_name = c.property_name OR (c3bc.property_name IS NULL AND c.property_name IS NULL))
and c3bc.is_active = c.is_active