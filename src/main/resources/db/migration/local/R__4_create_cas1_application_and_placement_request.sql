-- ${flyway:timestamp}
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
    '51c50b75-e142-4fdf-a02e-57957541770a',
    'aa30f20a-84e3-4baa-bef0-3c9bd51879ad',
    'X371199',
    '{}',
    'approved-premises',
    'A7779DY',
    '49df96e4-f1b6-4622-9355-729f5adaf042',
    '2023-11-07 12:35:38.648488',
    '2023-11-08 12:35:38.648488',
    null
  )
on conflict(id) do nothing;

insert into
  approved_premises_applications (
    "id",
    "is_womens_application",
    "conviction_id",
    "event_number",
    "offence_id",
    "risk_ratings",
    "release_type",
    "arrival_date",
    "is_inapplicable",
    "is_emergency_application",
    "is_withdrawn",
    "withdrawal_reason",
    "other_withdrawal_reason",
    "name",
    "target_location",
    "status",
    "ap_type"
  )
values (
  '51c50b75-e142-4fdf-a02e-57957541770a',
  NULL,
  '2500403796',
  '2',
  'M2500403796',
  '{"roshRisks":{"status":"Retrieved","value":{"overallRisk":"High","riskToChildren":"Medium","riskToPublic":"High","riskToKnownAdult":"High","riskToStaff":"Low","lastUpdated":[2023,1,31]}},"mappa":{"status":"Error","value":null},"tier":{"status":"Retrieved","value":{"level":"D2","lastUpdated":[2022,4,19]}},"flags":{"status":"Error","value":null}}',
  'licence',
  NULL,
  NULL,
  NULL,
  'false',
  NULL,
  NULL,
  'BEN DAVIES',
  NULL,
  'AWAITING_PLACEMENT',
  'NORMAL'
)
on conflict(id) do nothing;

insert into
  placement_applications (
    "allocated_at",
    "allocated_to_user_id",
    "application_id",
    "created_at",
    "created_by_user_id",
    "data",
    "decision",
    "document",
    "id",
    "placement_type",
    "reallocated_at",
    "schema_version",
    "submitted_at"
  )
values
  (
    '2023-11-09 12:36:16.618613',
    'aa30f20a-84e3-4baa-bef0-3c9bd51879ad',
    '51c50b75-e142-4fdf-a02e-57957541770a',
    '2023-11-09 12:35:38.648488',
    'aa30f20a-84e3-4baa-bef0-3c9bd51879ad',
    '{"request-a-placement":{"reason-for-placement":{"reason":"release_following_decision"},"decision-to-release":{"decisionToReleaseDate":"2023-11-11","decisionToReleaseDate-day":"11","decisionToReleaseDate-month":"11","decisionToReleaseDate-year":"2023","informationFromDirectionToRelease":"ddsdas"},"additional-documents":{"selectedDocuments":[{"id":"b2d92238-215c-4d6d-b91c-208ea747087e","level":"Offender","fileName":"lindas CR template_13092019_142956_Omic_A_X320741.DOC","createdAt":"2019-09-13T00:00:00Z","typeCode":"ADDRESS_ASSESSMENT_DOCUMENT","typeDescription":"Address assessment related document","description":"Address assessment on 02/09/2019"},{"id":"5152b060-9650-4f22-9974-038a38590d9f","level":"Offender","fileName":"TemplateWord972003.dot_13092019_162428_Omic_A_X320741","createdAt":"2019-09-13T00:00:00Z","typeCode":"PERSONAL_CONTACT_DOCUMENT","typeDescription":"Personal contact related document","description":"Personal contact of type GP with Good friend"}]},"updates-to-application":{"significantEvents":"no","significantEventsDetail":"","changedCirumstances":"no","changedCirumstancesDetail":"","riskFactors":"no","riskFactorsDetail":"","accessOrHealthcareNeeds":"no","accessOrHealthcareNeedsDetail":"","locationFactors":"no","locationFactorsDetail":""}}}',
    NULL,
    '{"request-a-placement":[{"Why are you requesting a placement?":"Release directed following parole board or other hearing/decision"},{"Enter the date of decision":"Saturday 11 November 2023","Provide relevant information from the direction to release that will impact the placement":"ddsdas"},{"lindas CR template_13092019_142956_Omic_A_X320741.DOC":"Address assessment on 02/09/2019","TemplateWord972003.dot_13092019_162428_Omic_A_X320741":"Personal contact of type GP with Good friend"},{"Have there been any significant events since the application was assessed?":"No","Has the person''s circumstances changed which affect the planned AP placement?":"No","Has the person''s risk factors changed since the application was assessed?":"No","Has the person''s access or healthcare needs changed since the application was assessed?":"No","Has the person''s location factors changed since the application was assessed?":"No"}]}',
    '219b0edd-d5cd-4eb0-b29a-6e83331b55fc',
    '1',
    NULL,
    'c6ffcbb0-3b1b-4336-ae57-ff80dd41f7e0',
    '2023-11-09 12:36:16.618508'
  )
on conflict(id) do nothing;


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
    'f2e0de20-cb6b-43d3-82f1-defe5190ba51',
    'aa30f20a-84e3-4baa-bef0-3c9bd51879ad',
    'X371199',
    '{}',
    'approved-premises',
    'A7779DY',
    '49df96e4-f1b6-4622-9355-729f5adaf042',
    '2023-11-07 12:35:38.648488',
    '2023-11-08 12:35:38.648488',
    null
  )
on conflict(id) do nothing;

insert into
  approved_premises_applications (
    "id",
    "is_womens_application",
    "conviction_id",
    "event_number",
    "offence_id",
    "risk_ratings",
    "release_type",
    "arrival_date",
    "is_inapplicable",
    "is_emergency_application",
    "is_withdrawn",
    "withdrawal_reason",
    "other_withdrawal_reason",
    "name",
    "target_location",
    "status",
    "ap_type"
  )
values (
  'f2e0de20-cb6b-43d3-82f1-defe5190ba51',
  NULL,
  '2500403796',
  '2',
  'M2500403796',
  '{"roshRisks":{"status":"Retrieved","value":{"overallRisk":"High","riskToChildren":"Medium","riskToPublic":"High","riskToKnownAdult":"High","riskToStaff":"Low","lastUpdated":[2023,1,31]}},"mappa":{"status":"Error","value":null},"tier":{"status":"Retrieved","value":{"level":"D2","lastUpdated":[2022,4,19]}},"flags":{"status":"Error","value":null}}',
  'licence',
  NULL,
  NULL,
  NULL,
  'false',
  NULL,
  NULL,
  'BEN DAVIES',
  NULL,
  'STARTED',
  'NORMAL'
)
on conflict(id) do nothing;

insert into
  placement_applications (
    "allocated_at",
    "allocated_to_user_id",
    "application_id",
    "created_at",
    "created_by_user_id",
    "data",
    "decision",
    "document",
    "id",
    "placement_type",
    "reallocated_at",
    "schema_version",
    "submitted_at"
  )
values
  (
    '2023-11-09 12:36:16.618613',
    'aa30f20a-84e3-4baa-bef0-3c9bd51879ad',
    '51c50b75-e142-4fdf-a02e-57957541770a',
    '2023-11-09 12:35:38.648488',
    'aa30f20a-84e3-4baa-bef0-3c9bd51879ad',
    '{"request-a-placement":{"reason-for-placement":{"reason":"release_following_decision"},"decision-to-release":{"decisionToReleaseDate":"2023-11-11","decisionToReleaseDate-day":"11","decisionToReleaseDate-month":"11","decisionToReleaseDate-year":"2023","informationFromDirectionToRelease":"ddsdas"},"additional-documents":{"selectedDocuments":[{"id":"b2d92238-215c-4d6d-b91c-208ea747087e","level":"Offender","fileName":"lindas CR template_13092019_142956_Omic_A_X320741.DOC","createdAt":"2019-09-13T00:00:00Z","typeCode":"ADDRESS_ASSESSMENT_DOCUMENT","typeDescription":"Address assessment related document","description":"Address assessment on 02/09/2019"},{"id":"5152b060-9650-4f22-9974-038a38590d9f","level":"Offender","fileName":"TemplateWord972003.dot_13092019_162428_Omic_A_X320741","createdAt":"2019-09-13T00:00:00Z","typeCode":"PERSONAL_CONTACT_DOCUMENT","typeDescription":"Personal contact related document","description":"Personal contact of type GP with Good friend"}]},"updates-to-application":{"significantEvents":"no","significantEventsDetail":"","changedCirumstances":"no","changedCirumstancesDetail":"","riskFactors":"no","riskFactorsDetail":"","accessOrHealthcareNeeds":"no","accessOrHealthcareNeedsDetail":"","locationFactors":"no","locationFactorsDetail":""}}}',
    NULL,
    '{"request-a-placement":[{"Why are you requesting a placement?":"Release directed following parole board or other hearing/decision"},{"Enter the date of decision":"Saturday 11 November 2023","Provide relevant information from the direction to release that will impact the placement":"ddsdas"},{"lindas CR template_13092019_142956_Omic_A_X320741.DOC":"Address assessment on 02/09/2019","TemplateWord972003.dot_13092019_162428_Omic_A_X320741":"Personal contact of type GP with Good friend"},{"Have there been any significant events since the application was assessed?":"No","Has the person''s circumstances changed which affect the planned AP placement?":"No","Has the person''s risk factors changed since the application was assessed?":"No","Has the person''s access or healthcare needs changed since the application was assessed?":"No","Has the person''s location factors changed since the application was assessed?":"No"}]}',
    'c77df477-21f5-4b05-9c83-29638ae94b95',
    '1',
    NULL,
    'c6ffcbb0-3b1b-4336-ae57-ff80dd41f7e0',
    '2023-11-09 12:36:16.618508'
  )
on conflict(id) do nothing;

update "approved_premises_applications" set "status" = 'AWAITING_PLACEMENT' where "id" = 'f2e0de20-cb6b-43d3-82f1-defe5190ba51';

update "approved_premises_applications" set "status" = 'AWAITING_PLACEMENT' where "id" = '51c50b75-e142-4fdf-a02e-57957541770a';
