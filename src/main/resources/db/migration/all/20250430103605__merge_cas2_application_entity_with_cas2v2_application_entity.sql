ALTER TABLE cas_2_applications
ADD application_origin TEXT DEFAULT 'homeDetentionCurfew' NOT NULL,
ADD bail_hearing_date TIMESTAMP WITH TIME ZONE;