ALTER TABLE lost_beds DROP CONSTRAINT lost_bed_reason_id_fk;
ALTER TABLE lost_bed_cancellations DROP CONSTRAINT lost_bed_cancellations_lost_bed_id_fkey;

DELETE FROM lost_bed_cancellations
WHERE lost_bed_id in(
    SELECT  id
    FROM  lost_beds
    where premises_id in (select id from premises where service='approved-premises'));

DELETE FROM lost_beds
where premises_id in (select id from premises where service='approved-premises');

ALTER TABLE lost_beds RENAME TO cas3_void_bedspaces;

ALTER TABLE lost_bed_cancellations RENAME TO cas3_void_bedspace_cancellations;

DELETE FROM lost_bed_reasons WHERE service_scope='approved-premises';
ALTER TABLE lost_bed_reasons DROP COLUMN service_scope;
ALTER TABLE lost_bed_reasons RENAME TO cas3_void_bedspace_reasons;

ALTER TABLE cas3_void_bedspaces RENAME COLUMN lost_bed_reason_id TO cas3_void_bedspace_reason_id;
ALTER TABLE cas3_void_bedspaces ADD CONSTRAINT cas3_void_bedspaces_cas3_void_bedspace_reason_id_fk FOREIGN KEY (cas3_void_bedspace_reason_id) REFERENCES cas3_void_bedspace_reasons(id);

ALTER TABLE cas3_void_bedspace_cancellations RENAME COLUMN lost_bed_id TO cas3_void_bedspace_id;
ALTER TABLE cas3_void_bedspace_cancellations ADD CONSTRAINT cas3_void_bedspace_cancellations_cas3_void_bedspace_id_fk FOREIGN KEY (cas3_void_bedspace_id) REFERENCES cas3_void_bedspaces(id);