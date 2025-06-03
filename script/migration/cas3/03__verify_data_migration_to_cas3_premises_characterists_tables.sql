-- get counts of cas3 premises characteristics data in the existing characteristics table
select count(c.*)
from premises p
join temporary_accommodation_premises tap on tap.premises_id = p.id
join premises_characteristics pc on pc.premises_id = p.id
join characteristics c on c.id = pc.characteristic_id;

-- get counts where migrated data in cas3_premises_characteristics matches the cas3 premises characteristics data in characteristics table
select count(c3pc.*)
from cas3_premises c3p
join cas3_premises_characteristic_mappings c3pcm on c3pcm.premises_id = c3p.id
join cas3_premises_characteristics c3pc on c3pc.id = c3pcm.premises_id
join premises_characteristics pc on pc.premises_id = c3p.id
join characteristics c on c.id = pc.characteristic_id
where c3pc.name = c.name
and (c3pc.property_name = c.property_name OR (c3pc.property_name IS NULL AND c.property_name IS NULL))
and c3pc.is_active = c.is_active