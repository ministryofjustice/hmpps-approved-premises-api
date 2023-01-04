
-- ${flyway:timestamp}
TRUNCATE TABLE applications CASCADE;
TRUNCATE TABLE assessments CASCADE;


DO $$
DECLARE
   applicationData json := '{"basic-information":{"sentence-type":{"sentenceType":"bailPlacement"},"situation":{"situation":"bailSentence"},"release-date":{"releaseDate-year":"2022","releaseDate-month":"11","releaseDate-day":"14","releaseDate":"2022-11-14","knowReleaseDate":"yes"},"placement-date":{"startDateSameAsReleaseDate":"yes"},"placement-purpose":{"placementPurposes":["drugAlcoholMonitoring","otherReason"],"otherReason":"Reason"}},"type-of-ap":{"ap-type":{"type":"standard"}},"oasys-import":{"optional-oasys-sections":{"needsLinkedToReoffending":[{"section":1,"name":"accommodation","linkedToHarm":false,"linkedToReOffending":true},{"section":2,"name":"relationships","linkedToHarm":false,"linkedToReOffending":true}],"otherNeeds":[{"section":3,"name":"emotional","linkedToHarm":false,"linkedToReOffending":false},{"section":4,"name":"thinking","linkedToHarm":false,"linkedToReOffending":false}]},"rosh-summary":{"roshAnswers":["Some answer for the first RoSH question. With an extra comment 1","Some answer for the second RoSH question. With an extra comment 2","Some answer for the third RoSH question. With an extra comment 3"],"roshSummaries":[{"questionNumber":"1","label":"The first RoSH question","answer":"Some answer for the first RoSH question"},{"questionNumber":"2","label":"The second RoSH question","answer":"Some answer for the second RoSH question"},{"questionNumber":"3","label":"The third RoSH question","answer":"Some answer for the third RoSH question"}]},"offence-details":{"offenceDetailsAnswers":["Some answer for the first offence details question. With an extra comment 1","Some answer for the second offence details question. With an extra comment 2","Some answer for the third offence details question. With an extra comment 3"],"offenceDetailsSummaries":[{"questionNumber":"1","label":"The first offence details question","answer":"Some answer for the first offence details question"},{"questionNumber":"2","label":"The second offence details question","answer":"Some answer for the second offence details question"},{"questionNumber":"3","label":"The third offence details question","answer":"Some answer for the third offence details question"}]},"supporting-information":{"supportingInformationAnswers":["Some answer for the first supporting information question. With an extra comment 1","Some answer for the second supporting information question. With an extra comment 2","Some answer for the third supporting information question. With an extra comment 3"],"supportingInformationSummaries":[{"questionNumber":"1","label":"The first supporting information question","answer":"Some answer for the first supporting information question"},{"questionNumber":"2","label":"The second supporting information question","answer":"Some answer for the second supporting information question"},{"questionNumber":"3","label":"The third supporting information question","answer":"Some answer for the third supporting information question"}]},"risk-management-plan":{"riskManagementAnswers":["Some answer for the first risk management question. With an extra comment 1","Some answer for the second risk management question. With an extra comment 2","Some answer for the third risk management question. With an extra comment 3"],"riskManagementSummaries":[{"questionNumber":"1","label":"The first risk management question","answer":"Some answer for the first risk management question"},{"questionNumber":"2","label":"The second risk management question","answer":"Some answer for the second risk management question"},{"questionNumber":"3","label":"The third risk management question","answer":"Some answer for the third risk management question"}]},"risk-to-self":{"riskToSelfAnswers":["Some answer for the first risk to self question. With an extra comment 1","Some answer for the second risk to self question. With an extra comment 2","Some answer for the third risk to self question. With an extra comment 3"],"riskToSelfSummaries":[{"questionNumber":"1","label":"The first risk to self question","answer":"Some answer for the first risk to self question"},{"questionNumber":"2","label":"The second risk to self question","answer":"Some answer for the second risk to self question"},{"questionNumber":"3","label":"The third risk to self question","answer":"Some answer for the third risk to self question"}]}},"risk-management-features":{"risk-management-features":{"manageRiskDetails":"est vero nesciunt","additionalFeaturesDetails":"deleniti enim necessitatibus"},"convicted-offences":{"response":"yes"},"type-of-convicted-offence":{"offenceConvictions":["arson","sexualOffence","hateCrimes","childNonSexualOffence"]},"date-of-offence":{"arsonOffence":["current"],"hateCrime":["previous"],"inPersonSexualOffence":["current","previous"]},"rehabilitative-interventions":{"rehabilitativeInterventions":["accommodation","drugsAndAlcohol","childrenAndFamilies","health","educationTrainingAndEmployment","financeBenefitsAndDebt","attitudesAndBehaviour","abuse","other"],"otherIntervention":"Another"}},"prison-information":{"case-notes":{"caseNoteIds":["a30173ca-061f-42c9-a1a2-28c70b282d3f","4a477187-b77f-4fcc-a919-43a6633ee868"],"selectedCaseNotes":[{"authorName":"Denise Collins","id":"a30173ca-061f-42c9-a1a2-28c70b282d3f","createdAt":"2022-11-10","occurredAt":"2022-10-19","sensitive":false,"subType":"Ressettlement","type":"Social Care","note":"Note 1"},{"authorName":"Leticia Mann","id":"4a477187-b77f-4fcc-a919-43a6633ee868","createdAt":"2022-07-24","occurredAt":"2022-09-22","sensitive":true,"subType":"Quality Work","type":"General","note":"Note 2"}],"moreDetail":"some details","adjudications":[{"id":69927,"reportedAt":"2022-10-09","establishment":"Hawthorne","offenceDescription":"Nam vel nisi fugiat veniam possimus omnis.","hearingHeld":false,"finding":"NOT_PROVED"},{"id":39963,"reportedAt":"2022-07-10","establishment":"Oklahoma City","offenceDescription":"Illum maxime enim explicabo soluta sequi voluptas.","hearingHeld":true,"finding":"PROVED"},{"id":77431,"reportedAt":"2022-05-30","establishment":"Jurupa Valley","offenceDescription":"Quis porro nemo voluptates doloribus atque quis provident iure.","hearingHeld":false,"finding":"PROVED"}]}},"location-factors":{"describe-location-factors":{"postcodeArea":"SW1","positiveFactors":"Some positive factors","restrictions":"yes","restrictionDetail":"Restrictions go here","alternativeRadiusAccepted":"yes","alternativeRadius":"70","differentPDU":"no"},"pdu-transfer":{"transferStatus":"yes","probationPractitioner":"Probation Practicioner"}},"access-and-healthcare":{"access-needs":{"additionalNeeds":["mobility","learningDisability","neurodivergentConditions"],"religiousOrCulturalNeeds":"yes","religiousOrCulturalNeedsDetails":"sunt at minus","needsInterpreter":"yes","interpreterLanguage":"beatae eligendi explicabo","careActAssessmentCompleted":"yes"},"access-needs-mobility":{"needsWheelchair":"yes","mobilityNeeds":"laboriosam","visualImpairment":"exercitationem"},"covid":{"fullyVaccinated":"yes","highRisk":"yes","additionalCovidInfo":"additional info"}},"further-considerations":{"room-sharing":{"riskToStaff":"no","riskToStaffDetail":"","riskToOthers":"no","riskToOthersDetail":"","sharingConcerns":"yes","sharingConcernsDetail":"Some details here","traumaConcerns":"no","traumaConcernsDetail":"","sharingBenefits":"no","sharingBenefitsDetail":""},"vulnerability":{"exploitable":"no","exploitableDetail":"","exploitOthers":"yes","exploitOthersDetail":"Some details here"},"previous-placements":{"previousPlacement":"yes","previousPlacementDetail":"Some details here"},"complex-case-board":{"complexCaseBoard":"yes","complexCaseBoardDetail":"Some details here"},"catering":{"catering":"yes","cateringDetail":"Some details here"},"arson":{"arson":"yes","arsonDetail":"Some details here"}},"move-on":{"placement-duration":{"duration":"10","durationDetail":"Some more information"},"relocation-region":{"postcodeArea":"XX1"},"plans-in-place":{"arePlansInPlace":"yes"},"type-of-accommodation":{"accommodationType":"foreignNational","otherAccommodationType":""},"foreign-national":{"response":"yes","date-year":"2015","date-month":"1","date-day":"1","date":"2015-01-01"}},"attach-required-documents":{"attach-documents":{"selectedDocuments":[{"id":"aeb43e06-a4a1-460f-9acf-e2495de84604","level":"Offender","fileName":"Random offender document1.pdf","createdAt":"2019-09-10T00:00:00Z","typeCode":"OFFENDER_DOCUMENT","typeDescription":"Offender related","description":"Some Description 1"},{"id":"5f8a2288-0a17-4e45-afd5-5e57794a7b53","level":"Conviction","fileName":"national.asm","createdAt":"2022-09-13","typeCode":"serenade","typeDescription":"porter","description":"Some Description 2"}]}},"check-your-answers":{"review":{"reviewed":"1"}}}';
   applicationDocument json := '{"basic-information":[{"Which of the following best describes the sentence type?":"Standard determinate custody"},{"What type of release will the application support?":"Release on Temporary License (ROTL)"},{"Do you know Justyn Flatley’s release date?":"Yes","Release Date":"Saturday 1 April 2023"},{"Is Saturday 1 April 2023 the date you want the placement to start?":"Yes"},{"What is the purpose of the AP placement?":"Prevent contact, Help individual readjust to life outside custody"}],"type-of-ap":[{"Which type of AP does Justyn Flatley require?":"Standard"}],"oasys-import":[{"Needs linked to reoffending":"9. Alcohol","Needs not linked to risk of serious harm or reoffending":"5. Finance, 11. Thinking and behavioural"},{"R10.1":"[R10.1] IN CUSTODY\r\n \r\n KNOWN ADULTS:\r\n Such as Ms Elaine Underhill and any of the victims of the index offence if they are placed close to Mr Smith cell.\r\n \r\n CHILDREN:\r\n Simon Smith, Roger Smith, Lindy Smith","R10.2":"[R10.2] IN CUSTODY:\r\n \r\n KNOWN ADULTS:\r\n Intimidation, threats of violence, use of weapons or boiling water, physical education and violent assault, long term psychological impact as a result of Mr Smith violent behaviour. This harm may be cause in the course of physical altercation due to seeking revenge or holding grudges...","R10.3":"[R10.3] STATIC RISK FACTORS:\r\n \r\n Mr Smith gender - At the time of the offence, Mr Smith was 35 years old and considered to be in the higher risk group.\r\n \r\n STABLE DYNAMIC RISK FACTORS:\r\n Thinking and behaviour - limited ability to manage mood or emotions poor impulse control. Mr Smith demonstrated cognitive deficit in committing the index offences and this still remains a concern...","R10.4":"[R10.4] CIRCUMSTANCES:\r\n A lack of stable accommodation\r\n unemployment\r\n non constructive use of time\r\n breakdown of family support\r\n being unable to access benefits\r\n \r\n UNDERLYING FACTORS:\r\n pro criminal attitudes supporting commission of crime...","R10.5":"[R10.5] Engage with Mental Health Services in prison and in the community.\r\n Maintain emotional stability.\r\n Abstain from alcohol.\r\n Abstain from illegal drugs.\r\n Regular testing for alcohol and drug use, in prison and on Licence.\r\n Maintain stable accommodation..."},{"2.1":"","2.4.1":"","2.4.2":"","2.12":"","2.5":"","2.8.3":"","2.98":""},{"3.9":"[3.9] Mr Smith told me that he was renting a room in a shared house, prior to his current remand...","4.9":"[4.9] He said that he has since learnt to read and write during periods of custody and an initial assessment of his...","5.9":"[5.9] Mr Smith was in receipt of Job Seekers Allowance prior to his current remand in custody...","9.9":"[9.9] Mr Smith told me that he rarely consumes alcohol and has not had any problems associated with excessive drinking in years...","11.9":"[11.9] The circumstances of the current offence suggest that Mr Smith acted ..."},{"RM28":"[RM28] Mr Smith is currently on remand awaiting sentence","RM35":"[RM35] Placeholder content for additional comments","RM34":"[RM34] Discuss any concerns with line manager/SPO Increase frequency of reporting Bring forward MAPPA/Consider Level 2 MAPPA professionals meetings  considered if MAPPA not applicable. Consider referral to Approved Premises Consider motivational interviewing/engagement officer.  Joint interview with police OM or other relevant agencies? To raise concerns Referral to substance misues worker for additional support. Warning letters to be employed Recall/Breach if risk not manageable.","RM33":"[RM33] Undertake sentence planning with OS within 8 or 16 weeks of sentence carats - continue to work with Mr Smith and refer for relevant programmes during any prison sentence imposed. DIP supervise DRR is imposed or provide support post release if conditions/requirements to manage the specific risks.","RM32":"[RM32] Requires referral to community drug DIP upon  release. To link in with employment/training resources such as College and ETEO. Completion of victim empathy module via 1-1 supervision to build upon awareness of impact of offending.   Level setting to be undertaken by Senior Probation Officer and OMU. Multi Agency Public Protection Panel to be convened if level 2 either at start of supervision or 6 months prior to release","RM31":"[RM31] State they will have secure accommodation for Mr Smith  and partner on release, although she too is subject to a DRR at present. Supportive wider family.  Has completed previously Short Duration programme and attended one of the support sessions .  Engaged with CARATS in custody.  Complied with voluntary drug tests in custody. Some motivation to desist from alcohol/drug use in the future, this needs to be encouraged with use of community resources. Completed drug and heroin awareness programmes in custody.","RM30":"[RM30] Probation Officer, Education training and employment Officer, Prison Offender Supervisor","RM28.1":"[RM28.1] Currently on remand at HMP Wandsworth - Management of case under MAPPA - level not yet set."},{"R8.1.1":"Review 06.10.21:\r\n \r\n         There have been numerous ACCTs opened since 2013 and every subsequent year he has been in custody.  In 2021...","R8.2.1":"Has told prison staff that he swallowed batteries on one occasion due to wanting..","R8.3.1":"Review 06.10.21:\r\n \r\n         A previous assessor opined that Mr Smith was displaying all the characteristics of someone..."}],"risk-management-features":[{"Describe why an AP placement is needed to manage the risk of Justyn Flatley":"Some words","Provide details of any additional measures that will be necessary for the management of risk":"Some words"},{"Has Justyn Flatley ever been convicted of any arson offences, sexual offences, hate crimes or non-sexual offences against children?":"No"},{"Which rehabilitative interventions will support the person''s Approved Premises (AP) placement?":"Drugs and alcohol, Children and families"}],"prison-information":[{"Selected prison case notes that support this application":["Array"],"Adjudications":["Array"]}],"location-factors":[{"What is the preferred location for the AP placement?":"WS1","Give details of any positive factors for the person in this location.":"Some words","Are there any restrictions linked to placement location?":"No","If an AP Placement is not available in the persons preferred area, would a placement further away be considered?":"No","Is the person moving to a different area where they''ll be managed by a different probation delivery unit (PDU)?":"No"},{"Have you agreed Justyn Flatley''s transfer/supervision with the receiving PDU?":"No, I still need to make arrangements"}],"access-and-healthcare":[{"Does Justyn Flatley have any of the following needs?":"Learning disability, hearing impairment","Does Justyn Flatley have any religious or cultural needs?":"No","Details of religious or cultural needs":"","Does Justyn Flatley need an interpreter?":"No","Has a care act assessment been completed?":"No"},{"Has Justyn Flatley been fully vaccinated for COVID-19?":"No","Is Justyn Flatley at a higher risk from COVID-19?":"No"}],"further-considerations":[{"Is there any evidence that the person may pose a risk to AP staff?":"No","Is there any evidence that the person may pose a risk to other AP residents?":"No","Do you have any concerns about the person sharing a bedroom?":"No","Is there any evidence of previous trauma or significant event in the persons history which would indicate that room share may not be suitable?":"No","Is there potential for the person to benefit from a room share?":"No"},{"Are you aware that Justyn Flatley is vulnerable to exploitation from others?":"No","Is there any evidence or expectation that Justyn Flatley may groom, radicalise or exploit others?":"No"},{"Has Justyn Flatley stayed or been offered a placement in an AP before?":"No"},{"Does Justyn Flatley''s gender identity require a complex case board to review their application?":"No"},{"Do you have any concerns about Justyn Flatley catering for themselves?":"No"},{"Does Justyn Flatley need a specialist arson room?":"No"}],"move-on":[{"What duration of placement do you recommend?":"11 weeks","Provide any additional information":"Some words"},{"Where is Justyn Flatley most likely to live when they move on from the AP?":"WS13"},{"Are move on arrangements already in place for when the person leaves the AP?":"No"},{"What type of accommodation will Justyn Flatley have when they leave the AP?":"Supported housing"}],"attach-required-documents":[{"PRE-CONS.pdf":"Previous convictions as of 01/09/2019","Personal circumstances.pdf.pdf":"Personal circumstance of AP - Medication in Posession  - Assessment started on 11/09/2019","NSIOffender.pdf":"Non Statutory Intervention for OPD Community Pathway on 16/10/2019","paroleParom1Report_04092019_122116_OMIC_A_X320741.pdf":"Parole Assessment Report at Berwyn (HMP) requested on 05/09/2019"}],"check-your-answers":[{}]}';
BEGIN


  insert into applications (
    "id",
    "created_at",
    "created_by_user_id",
    "crn",
    "data",
    "document",
    "schema_version",
    "service",
    "submitted_at"
  )
  values
    (
      '35e63614-e68c-4d7d-8996-8dd72c7b8ea5',
      CURRENT_DATE + 25,
      '8a39870c-3a1f-4e05-ad45-a450e15b242d',
      '315VWWC',
      applicationData,
      applicationDocument,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_APPLICATION'
        LIMIT 1
      ),
      'approved-premises',
      CURRENT_DATE + 3
    );
  

  insert into approved_premises_applications (
      "conviction_id",
      "event_number",
      "id",
      "is_pipe_application",
      "is_womens_application",
      "offence_id",
      "risk_ratings"
    )
  values
    (
      '2500295345',
      '2',
      '35e63614-e68c-4d7d-8996-8dd72c7b8ea5',
      false,
      false,
      'M2500295343',
      '{"roshRisks":{"status":"Error","value":null},"mappa":{"status":"Retrieved","value":{"level":"CAT M2/LEVEL M2","lastUpdated":[2021,2,1]}},"tier":{"status":"Retrieved","value":{"level":"D2","lastUpdated":[2022,9,5]}},"flags":{"status":"Retrieved","value":["Risk to Known Adult"]}}'
    );
  

  INSERT into
  assessments (
    "id",
    "application_id",
    "allocated_to_user_id",
    "created_at",
    "allocated_at",
    "schema_version"
  )
  VALUES
    (
      '9afe9b99-71be-42d3-bd8d-bdef38065d71',
      '35e63614-e68c-4d7d-8996-8dd72c7b8ea5',
      '531455f4-c76f-4943-b4eb-3c02d8fefa69',
      CURRENT_DATE,
      CURRENT_DATE,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_ASSESSMENT'
        LIMIT 1
      )
    );
  

  insert into applications (
    "id",
    "created_at",
    "created_by_user_id",
    "crn",
    "data",
    "document",
    "schema_version",
    "service",
    "submitted_at"
  )
  values
    (
      '0ffdf858-a3e4-4fda-a1d8-1a87b4d6f71e',
      CURRENT_DATE + 10,
      '8a39870c-3a1f-4e05-ad45-a450e15b242d',
      '4Y29R9P',
      applicationData,
      applicationDocument,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_APPLICATION'
        LIMIT 1
      ),
      'approved-premises',
      CURRENT_DATE + 0
    );
  

  insert into approved_premises_applications (
      "conviction_id",
      "event_number",
      "id",
      "is_pipe_application",
      "is_womens_application",
      "offence_id",
      "risk_ratings"
    )
  values
    (
      '2500295345',
      '2',
      '0ffdf858-a3e4-4fda-a1d8-1a87b4d6f71e',
      false,
      false,
      'M2500295343',
      '{"roshRisks":{"status":"Error","value":null},"mappa":{"status":"Retrieved","value":{"level":"CAT M2/LEVEL M2","lastUpdated":[2021,2,1]}},"tier":{"status":"Retrieved","value":{"level":"D2","lastUpdated":[2022,9,5]}},"flags":{"status":"Retrieved","value":["Risk to Known Adult"]}}'
    );
  

  INSERT into
  assessments (
    "id",
    "application_id",
    "allocated_to_user_id",
    "created_at",
    "allocated_at",
    "schema_version"
  )
  VALUES
    (
      'de01f811-e10e-4845-ae33-d05521491436',
      '0ffdf858-a3e4-4fda-a1d8-1a87b4d6f71e',
      '68715a03-06af-49ee-bae5-039c824ab9af',
      CURRENT_DATE,
      CURRENT_DATE,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_ASSESSMENT'
        LIMIT 1
      )
    );
  

  insert into applications (
    "id",
    "created_at",
    "created_by_user_id",
    "crn",
    "data",
    "document",
    "schema_version",
    "service",
    "submitted_at"
  )
  values
    (
      'e422232a-9459-45d4-b48d-f8443f1774f0',
      CURRENT_DATE + 9,
      '8a39870c-3a1f-4e05-ad45-a450e15b242d',
      '4ZUIHFX',
      applicationData,
      applicationDocument,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_APPLICATION'
        LIMIT 1
      ),
      'approved-premises',
      CURRENT_DATE + 0
    );
  

  insert into approved_premises_applications (
      "conviction_id",
      "event_number",
      "id",
      "is_pipe_application",
      "is_womens_application",
      "offence_id",
      "risk_ratings"
    )
  values
    (
      '2500295345',
      '2',
      'e422232a-9459-45d4-b48d-f8443f1774f0',
      false,
      false,
      'M2500295343',
      '{"roshRisks":{"status":"Error","value":null},"mappa":{"status":"Retrieved","value":{"level":"CAT M2/LEVEL M2","lastUpdated":[2021,2,1]}},"tier":{"status":"Retrieved","value":{"level":"D2","lastUpdated":[2022,9,5]}},"flags":{"status":"Retrieved","value":["Risk to Known Adult"]}}'
    );
  

  INSERT into
  assessments (
    "id",
    "application_id",
    "allocated_to_user_id",
    "created_at",
    "allocated_at",
    "schema_version"
  )
  VALUES
    (
      '9a63ecfc-7155-4e37-a51e-04964457b4e8',
      'e422232a-9459-45d4-b48d-f8443f1774f0',
      '531455f4-c76f-4943-b4eb-3c02d8fefa69',
      CURRENT_DATE,
      CURRENT_DATE,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_ASSESSMENT'
        LIMIT 1
      )
    );
  

  insert into applications (
    "id",
    "created_at",
    "created_by_user_id",
    "crn",
    "data",
    "document",
    "schema_version",
    "service",
    "submitted_at"
  )
  values
    (
      '1f7fd593-1b4e-4843-8575-20e1e63555da',
      CURRENT_DATE + 27,
      '68715a03-06af-49ee-bae5-039c824ab9af',
      '52W7TQG',
      applicationData,
      applicationDocument,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_APPLICATION'
        LIMIT 1
      ),
      'approved-premises',
      CURRENT_DATE + 15
    );
  

  insert into approved_premises_applications (
      "conviction_id",
      "event_number",
      "id",
      "is_pipe_application",
      "is_womens_application",
      "offence_id",
      "risk_ratings"
    )
  values
    (
      '2500295345',
      '2',
      '1f7fd593-1b4e-4843-8575-20e1e63555da',
      false,
      false,
      'M2500295343',
      '{"roshRisks":{"status":"Error","value":null},"mappa":{"status":"Retrieved","value":{"level":"CAT M2/LEVEL M2","lastUpdated":[2021,2,1]}},"tier":{"status":"Retrieved","value":{"level":"D2","lastUpdated":[2022,9,5]}},"flags":{"status":"Retrieved","value":["Risk to Known Adult"]}}'
    );
  

  INSERT into
  assessments (
    "id",
    "application_id",
    "allocated_to_user_id",
    "created_at",
    "allocated_at",
    "schema_version"
  )
  VALUES
    (
      '49da0ae4-3ba7-4778-a817-7e5b5cc8c2fd',
      '1f7fd593-1b4e-4843-8575-20e1e63555da',
      '8a39870c-3a1f-4e05-ad45-a450e15b242d',
      CURRENT_DATE,
      CURRENT_DATE,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_ASSESSMENT'
        LIMIT 1
      )
    );
  

  insert into applications (
    "id",
    "created_at",
    "created_by_user_id",
    "crn",
    "data",
    "document",
    "schema_version",
    "service",
    "submitted_at"
  )
  values
    (
      'c9187c64-f8a7-4ec7-9351-7f6e414b2681',
      CURRENT_DATE + 2,
      '6dcd2559-2d14-4feb-8faf-89ad30dfa765',
      '5EC66UT',
      applicationData,
      applicationDocument,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_APPLICATION'
        LIMIT 1
      ),
      'approved-premises',
      CURRENT_DATE + 10
    );
  

  insert into approved_premises_applications (
      "conviction_id",
      "event_number",
      "id",
      "is_pipe_application",
      "is_womens_application",
      "offence_id",
      "risk_ratings"
    )
  values
    (
      '2500295345',
      '2',
      'c9187c64-f8a7-4ec7-9351-7f6e414b2681',
      false,
      false,
      'M2500295343',
      '{"roshRisks":{"status":"Error","value":null},"mappa":{"status":"Retrieved","value":{"level":"CAT M2/LEVEL M2","lastUpdated":[2021,2,1]}},"tier":{"status":"Retrieved","value":{"level":"D2","lastUpdated":[2022,9,5]}},"flags":{"status":"Retrieved","value":["Risk to Known Adult"]}}'
    );
  

  INSERT into
  assessments (
    "id",
    "application_id",
    "allocated_to_user_id",
    "created_at",
    "allocated_at",
    "schema_version"
  )
  VALUES
    (
      'e7393185-6c71-4e14-bd38-bc67c7270fb3',
      'c9187c64-f8a7-4ec7-9351-7f6e414b2681',
      '531455f4-c76f-4943-b4eb-3c02d8fefa69',
      CURRENT_DATE,
      CURRENT_DATE,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_ASSESSMENT'
        LIMIT 1
      )
    );
  

  insert into applications (
    "id",
    "created_at",
    "created_by_user_id",
    "crn",
    "data",
    "document",
    "schema_version",
    "service",
    "submitted_at"
  )
  values
    (
      '1ba0d70b-36f9-4b3c-bfcd-ecf474e5ec6a',
      CURRENT_DATE + 6,
      '531455f4-c76f-4943-b4eb-3c02d8fefa69',
      '8LO3HSH',
      applicationData,
      applicationDocument,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_APPLICATION'
        LIMIT 1
      ),
      'approved-premises',
      CURRENT_DATE + 5
    );
  

  insert into approved_premises_applications (
      "conviction_id",
      "event_number",
      "id",
      "is_pipe_application",
      "is_womens_application",
      "offence_id",
      "risk_ratings"
    )
  values
    (
      '2500295345',
      '2',
      '1ba0d70b-36f9-4b3c-bfcd-ecf474e5ec6a',
      false,
      false,
      'M2500295343',
      '{"roshRisks":{"status":"Error","value":null},"mappa":{"status":"Retrieved","value":{"level":"CAT M2/LEVEL M2","lastUpdated":[2021,2,1]}},"tier":{"status":"Retrieved","value":{"level":"D2","lastUpdated":[2022,9,5]}},"flags":{"status":"Retrieved","value":["Risk to Known Adult"]}}'
    );
  

  INSERT into
  assessments (
    "id",
    "application_id",
    "allocated_to_user_id",
    "created_at",
    "allocated_at",
    "schema_version"
  )
  VALUES
    (
      '281dcf30-756a-4ae6-bbbd-730380e3bc63',
      '1ba0d70b-36f9-4b3c-bfcd-ecf474e5ec6a',
      '531455f4-c76f-4943-b4eb-3c02d8fefa69',
      CURRENT_DATE,
      CURRENT_DATE,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_ASSESSMENT'
        LIMIT 1
      )
    );
  

  insert into applications (
    "id",
    "created_at",
    "created_by_user_id",
    "crn",
    "data",
    "document",
    "schema_version",
    "service",
    "submitted_at"
  )
  values
    (
      '0ecff4e1-6851-4893-a779-5ef695caabdf',
      CURRENT_DATE + 12,
      '68715a03-06af-49ee-bae5-039c824ab9af',
      'BWEFOI7',
      applicationData,
      applicationDocument,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_APPLICATION'
        LIMIT 1
      ),
      'approved-premises',
      CURRENT_DATE + 18
    );
  

  insert into approved_premises_applications (
      "conviction_id",
      "event_number",
      "id",
      "is_pipe_application",
      "is_womens_application",
      "offence_id",
      "risk_ratings"
    )
  values
    (
      '2500295345',
      '2',
      '0ecff4e1-6851-4893-a779-5ef695caabdf',
      false,
      false,
      'M2500295343',
      '{"roshRisks":{"status":"Error","value":null},"mappa":{"status":"Retrieved","value":{"level":"CAT M2/LEVEL M2","lastUpdated":[2021,2,1]}},"tier":{"status":"Retrieved","value":{"level":"D2","lastUpdated":[2022,9,5]}},"flags":{"status":"Retrieved","value":["Risk to Known Adult"]}}'
    );
  

  INSERT into
  assessments (
    "id",
    "application_id",
    "allocated_to_user_id",
    "created_at",
    "allocated_at",
    "schema_version"
  )
  VALUES
    (
      'c26644e7-337e-43af-a57c-bc40a25823e3',
      '0ecff4e1-6851-4893-a779-5ef695caabdf',
      '6dcd2559-2d14-4feb-8faf-89ad30dfa765',
      CURRENT_DATE,
      CURRENT_DATE,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_ASSESSMENT'
        LIMIT 1
      )
    );
  

  insert into applications (
    "id",
    "created_at",
    "created_by_user_id",
    "crn",
    "data",
    "document",
    "schema_version",
    "service",
    "submitted_at"
  )
  values
    (
      '18387705-a22a-4dc7-ae39-9a0bd95e5735',
      CURRENT_DATE + 5,
      '6dcd2559-2d14-4feb-8faf-89ad30dfa765',
      'GSR1T2F',
      applicationData,
      applicationDocument,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_APPLICATION'
        LIMIT 1
      ),
      'approved-premises',
      CURRENT_DATE + 11
    );
  

  insert into approved_premises_applications (
      "conviction_id",
      "event_number",
      "id",
      "is_pipe_application",
      "is_womens_application",
      "offence_id",
      "risk_ratings"
    )
  values
    (
      '2500295345',
      '2',
      '18387705-a22a-4dc7-ae39-9a0bd95e5735',
      false,
      false,
      'M2500295343',
      '{"roshRisks":{"status":"Error","value":null},"mappa":{"status":"Retrieved","value":{"level":"CAT M2/LEVEL M2","lastUpdated":[2021,2,1]}},"tier":{"status":"Retrieved","value":{"level":"D2","lastUpdated":[2022,9,5]}},"flags":{"status":"Retrieved","value":["Risk to Known Adult"]}}'
    );
  

  INSERT into
  assessments (
    "id",
    "application_id",
    "allocated_to_user_id",
    "created_at",
    "allocated_at",
    "schema_version"
  )
  VALUES
    (
      'ae60a9dd-e542-4777-8764-0874d8610b48',
      '18387705-a22a-4dc7-ae39-9a0bd95e5735',
      '68715a03-06af-49ee-bae5-039c824ab9af',
      CURRENT_DATE,
      CURRENT_DATE,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_ASSESSMENT'
        LIMIT 1
      )
    );
  

  insert into applications (
    "id",
    "created_at",
    "created_by_user_id",
    "crn",
    "data",
    "document",
    "schema_version",
    "service",
    "submitted_at"
  )
  values
    (
      '4cedfea8-52a7-4846-81b6-e4f768923733',
      CURRENT_DATE + 28,
      '6dcd2559-2d14-4feb-8faf-89ad30dfa765',
      'HRV83TE',
      applicationData,
      applicationDocument,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_APPLICATION'
        LIMIT 1
      ),
      'approved-premises',
      CURRENT_DATE + 20
    );
  

  insert into approved_premises_applications (
      "conviction_id",
      "event_number",
      "id",
      "is_pipe_application",
      "is_womens_application",
      "offence_id",
      "risk_ratings"
    )
  values
    (
      '2500295345',
      '2',
      '4cedfea8-52a7-4846-81b6-e4f768923733',
      false,
      false,
      'M2500295343',
      '{"roshRisks":{"status":"Error","value":null},"mappa":{"status":"Retrieved","value":{"level":"CAT M2/LEVEL M2","lastUpdated":[2021,2,1]}},"tier":{"status":"Retrieved","value":{"level":"D2","lastUpdated":[2022,9,5]}},"flags":{"status":"Retrieved","value":["Risk to Known Adult"]}}'
    );
  

  INSERT into
  assessments (
    "id",
    "application_id",
    "allocated_to_user_id",
    "created_at",
    "allocated_at",
    "schema_version"
  )
  VALUES
    (
      '60195bb3-ca9f-4647-a6ae-c6b44c76ca84',
      '4cedfea8-52a7-4846-81b6-e4f768923733',
      '68715a03-06af-49ee-bae5-039c824ab9af',
      CURRENT_DATE,
      CURRENT_DATE,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_ASSESSMENT'
        LIMIT 1
      )
    );
  

  insert into applications (
    "id",
    "created_at",
    "created_by_user_id",
    "crn",
    "data",
    "document",
    "schema_version",
    "service",
    "submitted_at"
  )
  values
    (
      'cd31a8a9-6f7d-4f41-9b5d-a2306db59133',
      CURRENT_DATE + 8,
      '6dcd2559-2d14-4feb-8faf-89ad30dfa765',
      'HTVI42B',
      applicationData,
      applicationDocument,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_APPLICATION'
        LIMIT 1
      ),
      'approved-premises',
      CURRENT_DATE + 12
    );
  

  insert into approved_premises_applications (
      "conviction_id",
      "event_number",
      "id",
      "is_pipe_application",
      "is_womens_application",
      "offence_id",
      "risk_ratings"
    )
  values
    (
      '2500295345',
      '2',
      'cd31a8a9-6f7d-4f41-9b5d-a2306db59133',
      false,
      false,
      'M2500295343',
      '{"roshRisks":{"status":"Error","value":null},"mappa":{"status":"Retrieved","value":{"level":"CAT M2/LEVEL M2","lastUpdated":[2021,2,1]}},"tier":{"status":"Retrieved","value":{"level":"D2","lastUpdated":[2022,9,5]}},"flags":{"status":"Retrieved","value":["Risk to Known Adult"]}}'
    );
  

  INSERT into
  assessments (
    "id",
    "application_id",
    "allocated_to_user_id",
    "created_at",
    "allocated_at",
    "schema_version"
  )
  VALUES
    (
      'f4a55720-1a9b-4acb-832e-7cec48eaad9f',
      'cd31a8a9-6f7d-4f41-9b5d-a2306db59133',
      '531455f4-c76f-4943-b4eb-3c02d8fefa69',
      CURRENT_DATE,
      CURRENT_DATE,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_ASSESSMENT'
        LIMIT 1
      )
    );
  

  insert into applications (
    "id",
    "created_at",
    "created_by_user_id",
    "crn",
    "data",
    "document",
    "schema_version",
    "service",
    "submitted_at"
  )
  values
    (
      '3041e294-e3c7-4cec-9449-82a301d2af6e',
      CURRENT_DATE + 13,
      '8a39870c-3a1f-4e05-ad45-a450e15b242d',
      'HUN3BN0',
      applicationData,
      applicationDocument,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_APPLICATION'
        LIMIT 1
      ),
      'approved-premises',
      CURRENT_DATE + 24
    );
  

  insert into approved_premises_applications (
      "conviction_id",
      "event_number",
      "id",
      "is_pipe_application",
      "is_womens_application",
      "offence_id",
      "risk_ratings"
    )
  values
    (
      '2500295345',
      '2',
      '3041e294-e3c7-4cec-9449-82a301d2af6e',
      false,
      false,
      'M2500295343',
      '{"roshRisks":{"status":"Error","value":null},"mappa":{"status":"Retrieved","value":{"level":"CAT M2/LEVEL M2","lastUpdated":[2021,2,1]}},"tier":{"status":"Retrieved","value":{"level":"D2","lastUpdated":[2022,9,5]}},"flags":{"status":"Retrieved","value":["Risk to Known Adult"]}}'
    );
  

  INSERT into
  assessments (
    "id",
    "application_id",
    "allocated_to_user_id",
    "created_at",
    "allocated_at",
    "schema_version"
  )
  VALUES
    (
      '0711a132-93f1-49b9-bc1e-79b92566c3f2',
      '3041e294-e3c7-4cec-9449-82a301d2af6e',
      '68715a03-06af-49ee-bae5-039c824ab9af',
      CURRENT_DATE,
      CURRENT_DATE,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_ASSESSMENT'
        LIMIT 1
      )
    );
  

  insert into applications (
    "id",
    "created_at",
    "created_by_user_id",
    "crn",
    "data",
    "document",
    "schema_version",
    "service",
    "submitted_at"
  )
  values
    (
      'e3ef8fd7-9e41-4983-b467-622ace779558',
      CURRENT_DATE + 26,
      '68715a03-06af-49ee-bae5-039c824ab9af',
      'IHGHXYM',
      applicationData,
      applicationDocument,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_APPLICATION'
        LIMIT 1
      ),
      'approved-premises',
      CURRENT_DATE + 29
    );
  

  insert into approved_premises_applications (
      "conviction_id",
      "event_number",
      "id",
      "is_pipe_application",
      "is_womens_application",
      "offence_id",
      "risk_ratings"
    )
  values
    (
      '2500295345',
      '2',
      'e3ef8fd7-9e41-4983-b467-622ace779558',
      false,
      false,
      'M2500295343',
      '{"roshRisks":{"status":"Error","value":null},"mappa":{"status":"Retrieved","value":{"level":"CAT M2/LEVEL M2","lastUpdated":[2021,2,1]}},"tier":{"status":"Retrieved","value":{"level":"D2","lastUpdated":[2022,9,5]}},"flags":{"status":"Retrieved","value":["Risk to Known Adult"]}}'
    );
  

  INSERT into
  assessments (
    "id",
    "application_id",
    "allocated_to_user_id",
    "created_at",
    "allocated_at",
    "schema_version"
  )
  VALUES
    (
      '57082020-4311-4b73-b302-141240ac5100',
      'e3ef8fd7-9e41-4983-b467-622ace779558',
      '68715a03-06af-49ee-bae5-039c824ab9af',
      CURRENT_DATE,
      CURRENT_DATE,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_ASSESSMENT'
        LIMIT 1
      )
    );
  

  insert into applications (
    "id",
    "created_at",
    "created_by_user_id",
    "crn",
    "data",
    "document",
    "schema_version",
    "service",
    "submitted_at"
  )
  values
    (
      '637b7b24-e086-45ec-be3f-d689c7c8b32b',
      CURRENT_DATE + 17,
      '7a424213-3a0c-45b0-9a51-4977243c2b21',
      'JCRH9V5',
      applicationData,
      applicationDocument,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_APPLICATION'
        LIMIT 1
      ),
      'approved-premises',
      CURRENT_DATE + 5
    );
  

  insert into approved_premises_applications (
      "conviction_id",
      "event_number",
      "id",
      "is_pipe_application",
      "is_womens_application",
      "offence_id",
      "risk_ratings"
    )
  values
    (
      '2500295345',
      '2',
      '637b7b24-e086-45ec-be3f-d689c7c8b32b',
      false,
      false,
      'M2500295343',
      '{"roshRisks":{"status":"Error","value":null},"mappa":{"status":"Retrieved","value":{"level":"CAT M2/LEVEL M2","lastUpdated":[2021,2,1]}},"tier":{"status":"Retrieved","value":{"level":"D2","lastUpdated":[2022,9,5]}},"flags":{"status":"Retrieved","value":["Risk to Known Adult"]}}'
    );
  

  INSERT into
  assessments (
    "id",
    "application_id",
    "allocated_to_user_id",
    "created_at",
    "allocated_at",
    "schema_version"
  )
  VALUES
    (
      'f8c4572c-2987-42bc-9f16-cb26354c5ad3',
      '637b7b24-e086-45ec-be3f-d689c7c8b32b',
      '68715a03-06af-49ee-bae5-039c824ab9af',
      CURRENT_DATE,
      CURRENT_DATE,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_ASSESSMENT'
        LIMIT 1
      )
    );
  

  insert into applications (
    "id",
    "created_at",
    "created_by_user_id",
    "crn",
    "data",
    "document",
    "schema_version",
    "service",
    "submitted_at"
  )
  values
    (
      '6e38628a-6ac3-447b-b733-c88677f67f57',
      CURRENT_DATE + 24,
      '7a424213-3a0c-45b0-9a51-4977243c2b21',
      'N6OUTAY',
      applicationData,
      applicationDocument,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_APPLICATION'
        LIMIT 1
      ),
      'approved-premises',
      CURRENT_DATE + 25
    );
  

  insert into approved_premises_applications (
      "conviction_id",
      "event_number",
      "id",
      "is_pipe_application",
      "is_womens_application",
      "offence_id",
      "risk_ratings"
    )
  values
    (
      '2500295345',
      '2',
      '6e38628a-6ac3-447b-b733-c88677f67f57',
      false,
      false,
      'M2500295343',
      '{"roshRisks":{"status":"Error","value":null},"mappa":{"status":"Retrieved","value":{"level":"CAT M2/LEVEL M2","lastUpdated":[2021,2,1]}},"tier":{"status":"Retrieved","value":{"level":"D2","lastUpdated":[2022,9,5]}},"flags":{"status":"Retrieved","value":["Risk to Known Adult"]}}'
    );
  

  INSERT into
  assessments (
    "id",
    "application_id",
    "allocated_to_user_id",
    "created_at",
    "allocated_at",
    "schema_version"
  )
  VALUES
    (
      'beb05e58-35da-4671-a744-38662b2a87b8',
      '6e38628a-6ac3-447b-b733-c88677f67f57',
      '7a424213-3a0c-45b0-9a51-4977243c2b21',
      CURRENT_DATE,
      CURRENT_DATE,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_ASSESSMENT'
        LIMIT 1
      )
    );
  

  insert into applications (
    "id",
    "created_at",
    "created_by_user_id",
    "crn",
    "data",
    "document",
    "schema_version",
    "service",
    "submitted_at"
  )
  values
    (
      '930a0717-ea15-4c4b-8cab-02625ca4e319',
      CURRENT_DATE + 7,
      '6dcd2559-2d14-4feb-8faf-89ad30dfa765',
      'PI251LM',
      applicationData,
      applicationDocument,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_APPLICATION'
        LIMIT 1
      ),
      'approved-premises',
      CURRENT_DATE + 28
    );
  

  insert into approved_premises_applications (
      "conviction_id",
      "event_number",
      "id",
      "is_pipe_application",
      "is_womens_application",
      "offence_id",
      "risk_ratings"
    )
  values
    (
      '2500295345',
      '2',
      '930a0717-ea15-4c4b-8cab-02625ca4e319',
      false,
      false,
      'M2500295343',
      '{"roshRisks":{"status":"Error","value":null},"mappa":{"status":"Retrieved","value":{"level":"CAT M2/LEVEL M2","lastUpdated":[2021,2,1]}},"tier":{"status":"Retrieved","value":{"level":"D2","lastUpdated":[2022,9,5]}},"flags":{"status":"Retrieved","value":["Risk to Known Adult"]}}'
    );
  

  INSERT into
  assessments (
    "id",
    "application_id",
    "allocated_to_user_id",
    "created_at",
    "allocated_at",
    "schema_version"
  )
  VALUES
    (
      '3d6f4507-698c-4a5e-8bad-662b687680b2',
      '930a0717-ea15-4c4b-8cab-02625ca4e319',
      '68715a03-06af-49ee-bae5-039c824ab9af',
      CURRENT_DATE,
      CURRENT_DATE,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_ASSESSMENT'
        LIMIT 1
      )
    );
  

  insert into applications (
    "id",
    "created_at",
    "created_by_user_id",
    "crn",
    "data",
    "document",
    "schema_version",
    "service",
    "submitted_at"
  )
  values
    (
      '2ff19c8b-22ca-4156-ad87-a5c29c68b4d4',
      CURRENT_DATE + 17,
      '7a424213-3a0c-45b0-9a51-4977243c2b21',
      'PR5E5Y2',
      applicationData,
      applicationDocument,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_APPLICATION'
        LIMIT 1
      ),
      'approved-premises',
      CURRENT_DATE + 5
    );
  

  insert into approved_premises_applications (
      "conviction_id",
      "event_number",
      "id",
      "is_pipe_application",
      "is_womens_application",
      "offence_id",
      "risk_ratings"
    )
  values
    (
      '2500295345',
      '2',
      '2ff19c8b-22ca-4156-ad87-a5c29c68b4d4',
      false,
      false,
      'M2500295343',
      '{"roshRisks":{"status":"Error","value":null},"mappa":{"status":"Retrieved","value":{"level":"CAT M2/LEVEL M2","lastUpdated":[2021,2,1]}},"tier":{"status":"Retrieved","value":{"level":"D2","lastUpdated":[2022,9,5]}},"flags":{"status":"Retrieved","value":["Risk to Known Adult"]}}'
    );
  

  INSERT into
  assessments (
    "id",
    "application_id",
    "allocated_to_user_id",
    "created_at",
    "allocated_at",
    "schema_version"
  )
  VALUES
    (
      '2497d891-cb35-45df-a596-1a1bbcfa0a37',
      '2ff19c8b-22ca-4156-ad87-a5c29c68b4d4',
      '68715a03-06af-49ee-bae5-039c824ab9af',
      CURRENT_DATE,
      CURRENT_DATE,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_ASSESSMENT'
        LIMIT 1
      )
    );
  

  insert into applications (
    "id",
    "created_at",
    "created_by_user_id",
    "crn",
    "data",
    "document",
    "schema_version",
    "service",
    "submitted_at"
  )
  values
    (
      'e168a312-1f6f-4b2f-9b84-01f299a698a7',
      CURRENT_DATE + 5,
      '531455f4-c76f-4943-b4eb-3c02d8fefa69',
      'QA93YYK',
      applicationData,
      applicationDocument,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_APPLICATION'
        LIMIT 1
      ),
      'approved-premises',
      CURRENT_DATE + 25
    );
  

  insert into approved_premises_applications (
      "conviction_id",
      "event_number",
      "id",
      "is_pipe_application",
      "is_womens_application",
      "offence_id",
      "risk_ratings"
    )
  values
    (
      '2500295345',
      '2',
      'e168a312-1f6f-4b2f-9b84-01f299a698a7',
      false,
      false,
      'M2500295343',
      '{"roshRisks":{"status":"Error","value":null},"mappa":{"status":"Retrieved","value":{"level":"CAT M2/LEVEL M2","lastUpdated":[2021,2,1]}},"tier":{"status":"Retrieved","value":{"level":"D2","lastUpdated":[2022,9,5]}},"flags":{"status":"Retrieved","value":["Risk to Known Adult"]}}'
    );
  

  INSERT into
  assessments (
    "id",
    "application_id",
    "allocated_to_user_id",
    "created_at",
    "allocated_at",
    "schema_version"
  )
  VALUES
    (
      '8304c24e-4fc4-43ea-ad80-977fa877eba0',
      'e168a312-1f6f-4b2f-9b84-01f299a698a7',
      '6dcd2559-2d14-4feb-8faf-89ad30dfa765',
      CURRENT_DATE,
      CURRENT_DATE,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_ASSESSMENT'
        LIMIT 1
      )
    );
  

  insert into applications (
    "id",
    "created_at",
    "created_by_user_id",
    "crn",
    "data",
    "document",
    "schema_version",
    "service",
    "submitted_at"
  )
  values
    (
      '8eddfe1e-9800-44d2-b977-f93d8bc88072',
      CURRENT_DATE + 0,
      '6dcd2559-2d14-4feb-8faf-89ad30dfa765',
      'XCMSG3I',
      applicationData,
      applicationDocument,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_APPLICATION'
        LIMIT 1
      ),
      'approved-premises',
      CURRENT_DATE + 22
    );
  

  insert into approved_premises_applications (
      "conviction_id",
      "event_number",
      "id",
      "is_pipe_application",
      "is_womens_application",
      "offence_id",
      "risk_ratings"
    )
  values
    (
      '2500295345',
      '2',
      '8eddfe1e-9800-44d2-b977-f93d8bc88072',
      false,
      false,
      'M2500295343',
      '{"roshRisks":{"status":"Error","value":null},"mappa":{"status":"Retrieved","value":{"level":"CAT M2/LEVEL M2","lastUpdated":[2021,2,1]}},"tier":{"status":"Retrieved","value":{"level":"D2","lastUpdated":[2022,9,5]}},"flags":{"status":"Retrieved","value":["Risk to Known Adult"]}}'
    );
  

  INSERT into
  assessments (
    "id",
    "application_id",
    "allocated_to_user_id",
    "created_at",
    "allocated_at",
    "schema_version"
  )
  VALUES
    (
      '6e8b8700-cedc-4b8b-a876-1d837796e2a0',
      '8eddfe1e-9800-44d2-b977-f93d8bc88072',
      '531455f4-c76f-4943-b4eb-3c02d8fefa69',
      CURRENT_DATE,
      CURRENT_DATE,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_ASSESSMENT'
        LIMIT 1
      )
    );
  

  insert into applications (
    "id",
    "created_at",
    "created_by_user_id",
    "crn",
    "data",
    "document",
    "schema_version",
    "service",
    "submitted_at"
  )
  values
    (
      'ffb38b0c-4a89-4b45-a023-51560d65b7fb',
      CURRENT_DATE + 25,
      '7a424213-3a0c-45b0-9a51-4977243c2b21',
      'YRPARSH',
      applicationData,
      applicationDocument,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_APPLICATION'
        LIMIT 1
      ),
      'approved-premises',
      CURRENT_DATE + 21
    );
  

  insert into approved_premises_applications (
      "conviction_id",
      "event_number",
      "id",
      "is_pipe_application",
      "is_womens_application",
      "offence_id",
      "risk_ratings"
    )
  values
    (
      '2500295345',
      '2',
      'ffb38b0c-4a89-4b45-a023-51560d65b7fb',
      false,
      false,
      'M2500295343',
      '{"roshRisks":{"status":"Error","value":null},"mappa":{"status":"Retrieved","value":{"level":"CAT M2/LEVEL M2","lastUpdated":[2021,2,1]}},"tier":{"status":"Retrieved","value":{"level":"D2","lastUpdated":[2022,9,5]}},"flags":{"status":"Retrieved","value":["Risk to Known Adult"]}}'
    );
  

  INSERT into
  assessments (
    "id",
    "application_id",
    "allocated_to_user_id",
    "created_at",
    "allocated_at",
    "schema_version"
  )
  VALUES
    (
      '371321af-c5fe-475a-9865-0d06e8a8933a',
      'ffb38b0c-4a89-4b45-a023-51560d65b7fb',
      '7a424213-3a0c-45b0-9a51-4977243c2b21',
      CURRENT_DATE,
      CURRENT_DATE,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_ASSESSMENT'
        LIMIT 1
      )
    );
  

  insert into applications (
    "id",
    "created_at",
    "created_by_user_id",
    "crn",
    "data",
    "document",
    "schema_version",
    "service",
    "submitted_at"
  )
  values
    (
      'ccb80ad8-f07e-4e18-a0b5-f401d5745cdf',
      CURRENT_DATE + 6,
      '8a39870c-3a1f-4e05-ad45-a450e15b242d',
      'Z33A1BU',
      applicationData,
      applicationDocument,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_APPLICATION'
        LIMIT 1
      ),
      'approved-premises',
      CURRENT_DATE + 8
    );
  

  insert into approved_premises_applications (
      "conviction_id",
      "event_number",
      "id",
      "is_pipe_application",
      "is_womens_application",
      "offence_id",
      "risk_ratings"
    )
  values
    (
      '2500295345',
      '2',
      'ccb80ad8-f07e-4e18-a0b5-f401d5745cdf',
      false,
      false,
      'M2500295343',
      '{"roshRisks":{"status":"Error","value":null},"mappa":{"status":"Retrieved","value":{"level":"CAT M2/LEVEL M2","lastUpdated":[2021,2,1]}},"tier":{"status":"Retrieved","value":{"level":"D2","lastUpdated":[2022,9,5]}},"flags":{"status":"Retrieved","value":["Risk to Known Adult"]}}'
    );
  

  INSERT into
  assessments (
    "id",
    "application_id",
    "allocated_to_user_id",
    "created_at",
    "allocated_at",
    "schema_version"
  )
  VALUES
    (
      '84cd417b-7a49-470e-8f6c-03748cabbe35',
      'ccb80ad8-f07e-4e18-a0b5-f401d5745cdf',
      '68715a03-06af-49ee-bae5-039c824ab9af',
      CURRENT_DATE,
      CURRENT_DATE,
      (
        SELECT
          id
        FROM
          json_schemas
        WHERE
          type = 'APPROVED_PREMISES_ASSESSMENT'
        LIMIT 1
      )
    );
  
END $$;
