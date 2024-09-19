ALTER TABLE ap_areas ADD default_cru1_management_area_id uuid NULL;
ALTER TABLE ap_areas ADD CONSTRAINT ap_areas_cas1_cru_management_areas_fk FOREIGN KEY (default_cru1_management_area_id) REFERENCES cas1_cru_management_areas(id);

UPDATE ap_areas SET default_cru1_management_area_id = 'de20dec8-cdff-403c-89b1-5930724eaa49' WHERE identifier = 'LON';
UPDATE ap_areas SET default_cru1_management_area_id = '169a3e01-15c2-4f26-90c4-f48549960fb1' WHERE identifier = 'Mids';
UPDATE ap_areas SET default_cru1_management_area_id = '64ad8602-5130-41da-bb2b-1c287b88fd90' WHERE identifier = 'NE';
UPDATE ap_areas SET default_cru1_management_area_id = 'eccc3d1a-5c68-4a58-a333-eda33af54a4f' WHERE identifier = 'NW';
UPDATE ap_areas SET default_cru1_management_area_id = 'ddf23e67-053a-4384-b81b-48d576d3ceef' WHERE identifier = 'SEE';
UPDATE ap_areas SET default_cru1_management_area_id = '667cc74b-60f9-4848-822b-2e8f7712cdf1' WHERE identifier = 'SWSC';
UPDATE ap_areas SET default_cru1_management_area_id = '25ef55a4-27e0-45a6-b154-f92f226344d5' WHERE identifier = 'Wales';

ALTER TABLE ap_areas ALTER COLUMN default_cru1_management_area_id DROP NOT NULL;