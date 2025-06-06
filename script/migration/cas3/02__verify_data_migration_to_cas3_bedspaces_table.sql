-- get counts of cas3 data in the existing beds table
select count(b.*)
from premises p
join temporary_accommodation_premises tap on tap.premises_id = p.id
join rooms r on r.premises_id = p.id
join beds b on b.room_id = r.id;

-- get counts where migrated data in cas3_bedspaces matches the cas3 data in the rooms and beds tables
select count(c3b.*)
from cas3_bedspaces c3b
join beds b on b.id = c3b.id
join rooms r on r.id = b.room_id
join premises p on p.id = r.premises_id
join temporary_accommodation_premises tap on tap.premises_id = p.id
where c3b.reference = r.name
and (c3b.notes = r.notes OR (c3b.notes IS NULL AND r.notes IS NULL))
and (c3b.start_date = b.start_date OR (c3b.start_date IS NULL AND b.start_date IS NULL))
and (c3b.end_date = b.end_date  OR (c3b.end_date IS NULL AND b.end_date IS NULL))
and (c3b.created_at = b.created_at  OR (c3b.created_at IS NULL AND b.created_at IS NULL));