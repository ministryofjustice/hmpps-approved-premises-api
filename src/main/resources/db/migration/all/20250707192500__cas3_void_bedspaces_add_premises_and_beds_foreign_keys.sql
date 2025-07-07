ALTER TABLE cas3_void_bedspaces ADD FOREIGN KEY (bed_id) REFERENCES beds(id);
ALTER TABLE cas3_void_bedspaces ADD FOREIGN KEY (premises_id) REFERENCES premises(id);