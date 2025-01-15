-- ${flyway:timestamp}

-- Create application(referral)

insert into
  applications (
    "id",
    "created_by_user_id",
    "crn",
    "data",
    "service",
    "noms_number",
    "schema_version",
    "created_at",
    "submitted_at",
    "document"
  )
values
  (
    '959c59d1-fb85-4795-9703-4d7d23412c6b',
    'b5825da0-1553-4398-90ac-6a8e0c8a4cae',
    'X371199',
    '{}',
    'temporary-accommodation',
    'A7779DY',
    '18499823-422e-4813-be61-ef24d2534a18',
    CURRENT_DATE,
    CURRENT_DATE,
    null
  )
on conflict(id) do nothing;

--- Create CAS3 application(referral) ---

insert into
  temporary_accommodation_applications (
    "id",
    "conviction_id",
    "event_number",
    "offence_id",
    "risk_ratings",
    "probation_region_id",
    "arrival_date"
  )
values (
  '959c59d1-fb85-4795-9703-4d7d23412c6b',
  '2500403796',
  '2',
  'M2500403796',
  '{"roshRisks":{"status":"Retrieved","value":{"overallRisk":"High","riskToChildren":"Medium","riskToPublic":"High","riskToKnownAdult":"High","riskToStaff":"Low","lastUpdated":[2023,1,31]}},"mappa":{"status":"Error","value":null},"tier":{"status":"Retrieved","value":{"level":"D2","lastUpdated":[2022,4,19]}},"flags":{"status":"Error","value":null}}',
  'ca979718-b15d-4318-9944-69aaff281cad',
  CURRENT_DATE
)
on conflict(id) do nothing;

--- Create assessment ---

insert into assessments (
"id",
"application_id",
"schema_version",
"created_at",
"is_withdrawn",
"service"
)
values
(
'889ff31a-8afc-4325-9d1c-8e738fea6c18',
'959c59d1-fb85-4795-9703-4d7d23412c6b',
'325c171e-5012-43fe-840a-17fd4d7e95e6',
CURRENT_DATE,
false,
'temporary-accommodation'
)
on conflict(id) do nothing;

--- Create CAS3 assessment ---

insert into temporary_accommodation_assessments (
"assessment_id",
"completed_at",
"summary_data"
)
values
(
'889ff31a-8afc-4325-9d1c-8e738fea6c18',
CURRENT_DATE,
'{"isAbleToShare":true,"releaseType":"CAS2 (formerly Bail Accommodation Support Services)"}'
)
on conflict(assessment_id) do nothing;

--- Create another application ---

insert into
  applications (
    "id",
    "created_by_user_id",
    "crn",
    "data",
    "service",
    "noms_number",
    "schema_version",
    "created_at",
    "submitted_at",
    "document"
  )
values
  (
    'd045bd6a-0829-4cbb-9360-12c8cb522069',
    'b5825da0-1553-4398-90ac-6a8e0c8a4cae',
    'X371199',
    '{}',
    'temporary-accommodation',
    'A7779DY',
    '18499823-422e-4813-be61-ef24d2534a18',
    CURRENT_DATE + 1,
    CURRENT_DATE + 1,
    null
  )
on conflict(id) do nothing;

--- CAS3 application ---

insert into
  temporary_accommodation_applications (
    "id",
    "conviction_id",
    "event_number",
    "offence_id",
    "risk_ratings",
    "probation_region_id",
    "arrival_date"
  )
values (
  'd045bd6a-0829-4cbb-9360-12c8cb522069',
  '2500403796',
  '2',
  'M2500403796',
  '{"roshRisks":{"status":"Retrieved","value":{"overallRisk":"High","riskToChildren":"Medium","riskToPublic":"High","riskToKnownAdult":"High","riskToStaff":"Low","lastUpdated":[2023,1,31]}},"mappa":{"status":"Error","value":null},"tier":{"status":"Retrieved","value":{"level":"D2","lastUpdated":[2022,4,19]}},"flags":{"status":"Error","value":null}}',
  'ca979718-b15d-4318-9944-69aaff281cad',
  CURRENT_DATE + 1
)
on conflict(id) do nothing;

--- Create assessment ---

insert into assessments (
"id",
"application_id",
"schema_version",
"created_at",
"is_withdrawn",
"service"
)
values
(
'6439df3c-6c61-420e-9297-8947dbe39526',
'd045bd6a-0829-4cbb-9360-12c8cb522069',
'325c171e-5012-43fe-840a-17fd4d7e95e6',
CURRENT_DATE,
false,
'temporary-accommodation'
)
on conflict(id) do nothing;

--- Create CAS3 assessment ---

insert into temporary_accommodation_assessments (
"assessment_id",
"completed_at",
"summary_data"
)
values
(
'6439df3c-6c61-420e-9297-8947dbe39526',
CURRENT_DATE,
'{"isAbleToShare":true,"releaseType":"CAS2 (formerly Bail Accommodation Support Services)"}'
)
on conflict(assessment_id) do nothing;

--- Create another application linked to existing booking---

insert into
  applications (
    "id",
    "created_by_user_id",
    "crn",
    "data",
    "service",
    "noms_number",
    "schema_version",
    "created_at",
    "submitted_at",
    "document"
  )
values
  (
    'dbfd396f-1334-4dd3-b228-eb17710b40f5',
    'b5825da0-1553-4398-90ac-6a8e0c8a4cae',
    'X371199',
    '{}',
    'temporary-accommodation',
    'A7779DY',
    '18499823-422e-4813-be61-ef24d2534a18',
    CURRENT_DATE + 2,
    CURRENT_DATE + 2,
    null
  )
on conflict(id) do nothing;

--- CAS3 application ---

insert into
  temporary_accommodation_applications (
    "id",
    "conviction_id",
    "event_number",
    "offence_id",
    "risk_ratings",
    "probation_region_id",
    "arrival_date"
  )
values (
  'dbfd396f-1334-4dd3-b228-eb17710b40f5',
  '2500403796',
  '2',
  'M2500403796',
  '{"roshRisks":{"status":"Retrieved","value":{"overallRisk":"High","riskToChildren":"Medium","riskToPublic":"High","riskToKnownAdult":"High","riskToStaff":"Low","lastUpdated":[2023,1,31]}},"mappa":{"status":"Error","value":null},"tier":{"status":"Retrieved","value":{"level":"D2","lastUpdated":[2022,4,19]}},"flags":{"status":"Error","value":null}}',
  'ca979718-b15d-4318-9944-69aaff281cad',
  CURRENT_DATE + 2
)
on conflict(id) do nothing;

--- Create assessment ---

insert into assessments (
"id",
"application_id",
"schema_version",
"created_at",
"is_withdrawn",
"service"
)
values
(
'c4a42513-c53a-4507-b052-5ad1c2b54113',
'dbfd396f-1334-4dd3-b228-eb17710b40f5',
'325c171e-5012-43fe-840a-17fd4d7e95e6',
CURRENT_DATE,
false,
'temporary-accommodation'
)
on conflict(id) do nothing;

--- Create CAS3 assessment ---

insert into temporary_accommodation_assessments (
"assessment_id",
"completed_at",
"summary_data"
)
values
(
'c4a42513-c53a-4507-b052-5ad1c2b54113',
CURRENT_DATE,
'{"isAbleToShare":true,"releaseType":"CAS2 (formerly Bail Accommodation Support Services)"}'
)
on conflict(assessment_id) do nothing;

update bookings set application_id='dbfd396f-1334-4dd3-b228-eb17710b40f5' where id='1d0f6bcb-b742-4a53-8988-aeb192350824';