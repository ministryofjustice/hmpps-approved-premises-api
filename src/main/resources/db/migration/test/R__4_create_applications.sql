
-- ${flyway:timestamp}
TRUNCATE TABLE applications CASCADE;
TRUNCATE TABLE assessments CASCADE;


DO $$
DECLARE
   applicationData json := '{"basic-information":{"sentence-type":{"sentenceType":"bailPlacement"},"situation":{"situation":"bailSentence"},"release-date":{"releaseDate-year":"2022","releaseDate-month":"11","releaseDate-day":"14","releaseDate":"2022-11-14","knowReleaseDate":"yes"},"placement-date":{"startDateSameAsReleaseDate":"yes"},"placement-purpose":{"placementPurposes":["drugAlcoholMonitoring","otherReason"],"otherReason":"Reason"}},"type-of-ap":{"ap-type":{"type":"standard"}},"oasys-import":{"optional-oasys-sections":{"needsLinkedToReoffending":[{"section":1,"name":"accommodation","linkedToHarm":false,"linkedToReOffending":true},{"section":2,"name":"relationships","linkedToHarm":false,"linkedToReOffending":true}],"otherNeeds":[{"section":3,"name":"emotional","linkedToHarm":false,"linkedToReOffending":false},{"section":4,"name":"thinking","linkedToHarm":false,"linkedToReOffending":false}]},"rosh-summary":{"roshAnswers":["Some answer for the first RoSH question. With an extra comment 1","Some answer for the second RoSH question. With an extra comment 2","Some answer for the third RoSH question. With an extra comment 3"],"roshSummaries":[{"questionNumber":"1","label":"The first RoSH question","answer":"Some answer for the first RoSH question"},{"questionNumber":"2","label":"The second RoSH question","answer":"Some answer for the second RoSH question"},{"questionNumber":"3","label":"The third RoSH question","answer":"Some answer for the third RoSH question"}]},"offence-details":{"offenceDetailsAnswers":["Some answer for the first offence details question. With an extra comment 1","Some answer for the second offence details question. With an extra comment 2","Some answer for the third offence details question. With an extra comment 3"],"offenceDetailsSummaries":[{"questionNumber":"1","label":"The first offence details question","answer":"Some answer for the first offence details question"},{"questionNumber":"2","label":"The second offence details question","answer":"Some answer for the second offence details question"},{"questionNumber":"3","label":"The third offence details question","answer":"Some answer for the third offence details question"}]},"supporting-information":{"supportingInformationAnswers":["Some answer for the first supporting information question. With an extra comment 1","Some answer for the second supporting information question. With an extra comment 2","Some answer for the third supporting information question. With an extra comment 3"],"supportingInformationSummaries":[{"questionNumber":"1","label":"The first supporting information question","answer":"Some answer for the first supporting information question"},{"questionNumber":"2","label":"The second supporting information question","answer":"Some answer for the second supporting information question"},{"questionNumber":"3","label":"The third supporting information question","answer":"Some answer for the third supporting information question"}]},"risk-management-plan":{"riskManagementAnswers":["Some answer for the first risk management question. With an extra comment 1","Some answer for the second risk management question. With an extra comment 2","Some answer for the third risk management question. With an extra comment 3"],"riskManagementSummaries":[{"questionNumber":"1","label":"The first risk management question","answer":"Some answer for the first risk management question"},{"questionNumber":"2","label":"The second risk management question","answer":"Some answer for the second risk management question"},{"questionNumber":"3","label":"The third risk management question","answer":"Some answer for the third risk management question"}]},"risk-to-self":{"riskToSelfAnswers":["Some answer for the first risk to self question. With an extra comment 1","Some answer for the second risk to self question. With an extra comment 2","Some answer for the third risk to self question. With an extra comment 3"],"riskToSelfSummaries":[{"questionNumber":"1","label":"The first risk to self question","answer":"Some answer for the first risk to self question"},{"questionNumber":"2","label":"The second risk to self question","answer":"Some answer for the second risk to self question"},{"questionNumber":"3","label":"The third risk to self question","answer":"Some answer for the third risk to self question"}]}},"risk-management-features":{"risk-management-features":{"manageRiskDetails":"est vero nesciunt","additionalFeaturesDetails":"deleniti enim necessitatibus"},"convicted-offences":{"response":"yes"},"type-of-convicted-offence":{"offenceConvictions":["arson","sexualOffence","hateCrimes","childNonSexualOffence"]},"date-of-offence":{"arsonOffence":["current"],"hateCrime":["previous"],"inPersonSexualOffence":["current","previous"]},"rehabilitative-interventions":{"rehabilitativeInterventions":["accommodation","drugsAndAlcohol","childrenAndFamilies","health","educationTrainingAndEmployment","financeBenefitsAndDebt","attitudesAndBehaviour","abuse","other"],"otherIntervention":"Another"}},"prison-information":{"case-notes":{"caseNoteIds":["a30173ca-061f-42c9-a1a2-28c70b282d3f","4a477187-b77f-4fcc-a919-43a6633ee868"],"selectedCaseNotes":[{"authorName":"Denise Collins","id":"a30173ca-061f-42c9-a1a2-28c70b282d3f","createdAt":"2022-11-10","occurredAt":"2022-10-19","sensitive":false,"subType":"Ressettlement","type":"Social Care","note":"Note 1"},{"authorName":"Leticia Mann","id":"4a477187-b77f-4fcc-a919-43a6633ee868","createdAt":"2022-07-24","occurredAt":"2022-09-22","sensitive":true,"subType":"Quality Work","type":"General","note":"Note 2"}],"moreDetail":"some details","adjudications":[{"id":69927,"reportedAt":"2022-10-09","establishment":"Hawthorne","offenceDescription":"Nam vel nisi fugiat veniam possimus omnis.","hearingHeld":false,"finding":"NOT_PROVED"},{"id":39963,"reportedAt":"2022-07-10","establishment":"Oklahoma City","offenceDescription":"Illum maxime enim explicabo soluta sequi voluptas.","hearingHeld":true,"finding":"PROVED"},{"id":77431,"reportedAt":"2022-05-30","establishment":"Jurupa Valley","offenceDescription":"Quis porro nemo voluptates doloribus atque quis provident iure.","hearingHeld":false,"finding":"PROVED"}]}},"location-factors":{"describe-location-factors":{"postcodeArea":"SW1","positiveFactors":"Some positive factors","restrictions":"yes","restrictionDetail":"Restrictions go here","alternativeRadiusAccepted":"yes","alternativeRadius":"70","differentPDU":"no"}},"access-and-healthcare":{"access-needs":{"additionalNeeds":["mobility","learningDisability","neurodivergentConditions"],"religiousOrCulturalNeeds":"yes","religiousOrCulturalNeedsDetails":"sunt at minus","needsInterpreter":"yes","interpreterLanguage":"beatae eligendi explicabo","careActAssessmentCompleted":"yes"},"access-needs-mobility":{"needsWheelchair":"yes","mobilityNeeds":"laboriosam","visualImpairment":"exercitationem"},"covid":{"fullyVaccinated":"yes","highRisk":"yes","additionalCovidInfo":"additional info"}},"further-considerations":{"room-sharing":{"riskToStaff":"no","riskToStaffDetail":"","riskToOthers":"no","riskToOthersDetail":"","sharingConcerns":"yes","sharingConcernsDetail":"Some details here","traumaConcerns":"no","traumaConcernsDetail":"","sharingBenefits":"no","sharingBenefitsDetail":""},"vulnerability":{"exploitable":"no","exploitableDetail":"","exploitOthers":"yes","exploitOthersDetail":"Some details here"},"previous-placements":{"previousPlacement":"yes","previousPlacementDetail":"Some details here"},"complex-case-board":{"complexCaseBoard":"yes","complexCaseBoardDetail":"Some details here"},"catering":{"catering":"yes","cateringDetail":"Some details here"},"arson":{"arson":"yes","arsonDetail":"Some details here"}},"move-on":{"placement-duration":{"duration":"10","durationDetail":"Some more information"},"relocation-region":{"postcodeArea":"XX1"},"plans-in-place":{"arePlansInPlace":"yes"},"type-of-accommodation":{"accommodationType":"foreignNational","otherAccommodationType":""},"foreign-national":{"response":"yes","date-year":"2015","date-month":"1","date-day":"1","date":"2015-01-01"}},"attach-required-documents":{"attach-documents":{"selectedDocuments":[{"id":"aeb43e06-a4a1-460f-9acf-e2495de84604","level":"Offender","fileName":"Random offender document1.pdf","createdAt":"2019-09-10T00:00:00Z","typeCode":"OFFENDER_DOCUMENT","typeDescription":"Offender related","description":"Some Description 1"},{"id":"5f8a2288-0a17-4e45-afd5-5e57794a7b53","level":"Conviction","fileName":"national.asm","createdAt":"2022-09-13","typeCode":"serenade","typeDescription":"porter","description":"Some Description 2"}]}},"check-your-answers":{"review":{"reviewed":"1"}}}';
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
    "submitted_at",
    "noms_number"
  )
  values
    (
      '970a6675-0bc7-4bd1-a279-1ba03e399241',
      CURRENT_DATE + 11,
      'f9ff1c6e-6876-4ba8-8ca9-d7d2c6f673dc',
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
      CURRENT_DATE + 2,
      'M59CS58'
    );
  

  insert into approved_premises_applications (
      "conviction_id",
      "event_number",
      "id",
      "is_pipe_application",
      "is_womens_application",
      "offence_id",
      "is_withdrawn",
      "risk_ratings"
    )
  values
    (
      '2500295345',
      '2',
      '970a6675-0bc7-4bd1-a279-1ba03e399241',
      false,
      false,
      'M2500295343',
      false,
      '{"roshRisks":{"status":"Error","value":null},"mappa":{"status":"Retrieved","value":{"level":"CAT M2/LEVEL M2","lastUpdated":[2021,2,1]}},"tier":{"status":"Retrieved","value":{"level":"D2","lastUpdated":[2022,9,5]}},"flags":{"status":"Retrieved","value":["Risk to Known Adult"]}}'
    );
  

  INSERT into
  assessments (
    "id",
    "application_id",
    "allocated_to_user_id",
    "created_at",
    "schema_version"
  )
  VALUES
    (
      'd1ee7cdb-f50d-46d0-8cdc-be767e021669',
      '970a6675-0bc7-4bd1-a279-1ba03e399241',
      'f9ff1c6e-6876-4ba8-8ca9-d7d2c6f673dc',
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
    "submitted_at",
    "noms_number"
  )
  values
    (
      'b1e38d0c-026c-40cc-987d-6bb3b6f23855',
      CURRENT_DATE + 6,
      'f9ff1c6e-6876-4ba8-8ca9-d7d2c6f673dc',
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
      CURRENT_DATE + 22,
      'XWU5JWQ'
    );
  

  insert into approved_premises_applications (
      "conviction_id",
      "event_number",
      "id",
      "is_pipe_application",
      "is_womens_application",
      "offence_id",
      "is_withdrawn",
      "risk_ratings"
    )
  values
    (
      '2500295345',
      '2',
      'b1e38d0c-026c-40cc-987d-6bb3b6f23855',
      false,
      false,
      'M2500295343',
      false,
      '{"roshRisks":{"status":"Error","value":null},"mappa":{"status":"Retrieved","value":{"level":"CAT M2/LEVEL M2","lastUpdated":[2021,2,1]}},"tier":{"status":"Retrieved","value":{"level":"D2","lastUpdated":[2022,9,5]}},"flags":{"status":"Retrieved","value":["Risk to Known Adult"]}}'
    );
  

  INSERT into
  assessments (
    "id",
    "application_id",
    "allocated_to_user_id",
    "created_at",
    "schema_version"
  )
  VALUES
    (
      '5403568d-4fc1-45b9-8e5b-afbb8e6c13e7',
      'b1e38d0c-026c-40cc-987d-6bb3b6f23855',
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
    "submitted_at",
    "noms_number"
  )
  values
    (
      'a8911538-b822-44d4-8c1e-c9349bb7812f',
      CURRENT_DATE + 8,
      '6dcd2559-2d14-4feb-8faf-89ad30dfa765',
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
      CURRENT_DATE + 17,
      '5A5C8WL'
    );
  

  insert into approved_premises_applications (
      "conviction_id",
      "event_number",
      "id",
      "is_pipe_application",
      "is_womens_application",
      "offence_id",
      "is_withdrawn",
      "risk_ratings"
    )
  values
    (
      '2500295345',
      '2',
      'a8911538-b822-44d4-8c1e-c9349bb7812f',
      false,
      false,
      'M2500295343',
      false,
      '{"roshRisks":{"status":"Error","value":null},"mappa":{"status":"Retrieved","value":{"level":"CAT M2/LEVEL M2","lastUpdated":[2021,2,1]}},"tier":{"status":"Retrieved","value":{"level":"D2","lastUpdated":[2022,9,5]}},"flags":{"status":"Retrieved","value":["Risk to Known Adult"]}}'
    );
  

  INSERT into
  assessments (
    "id",
    "application_id",
    "allocated_to_user_id",
    "created_at",
    "schema_version"
  )
  VALUES
    (
      '7cc5218b-2dfd-4092-8703-8b34153f10c5',
      'a8911538-b822-44d4-8c1e-c9349bb7812f',
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
    "submitted_at",
    "noms_number"
  )
  values
    (
      'f6ecc7cb-d860-4382-86d5-1a25b344f6e5',
      CURRENT_DATE + 5,
      '531455f4-c76f-4943-b4eb-3c02d8fefa69',
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
      CURRENT_DATE + 15,
      '5HBWEG1'
    );
  

  insert into approved_premises_applications (
      "conviction_id",
      "event_number",
      "id",
      "is_pipe_application",
      "is_womens_application",
      "offence_id",
      "is_withdrawn",
      "risk_ratings"
    )
  values
    (
      '2500295345',
      '2',
      'f6ecc7cb-d860-4382-86d5-1a25b344f6e5',
      false,
      false,
      'M2500295343',
      false,
      '{"roshRisks":{"status":"Error","value":null},"mappa":{"status":"Retrieved","value":{"level":"CAT M2/LEVEL M2","lastUpdated":[2021,2,1]}},"tier":{"status":"Retrieved","value":{"level":"D2","lastUpdated":[2022,9,5]}},"flags":{"status":"Retrieved","value":["Risk to Known Adult"]}}'
    );
  

  INSERT into
  assessments (
    "id",
    "application_id",
    "allocated_to_user_id",
    "created_at",
    "schema_version"
  )
  VALUES
    (
      '6db5c3af-a674-40a2-b881-4af2bef892f2',
      'f6ecc7cb-d860-4382-86d5-1a25b344f6e5',
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
    "submitted_at",
    "noms_number"
  )
  values
    (
      '7cfbfd0f-7446-4733-8297-96928a9f6c0a',
      CURRENT_DATE + 23,
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
      CURRENT_DATE + 13,
      '530X5EC'
    );
  

  insert into approved_premises_applications (
      "conviction_id",
      "event_number",
      "id",
      "is_pipe_application",
      "is_womens_application",
      "offence_id",
      "is_withdrawn",
      "risk_ratings"
    )
  values
    (
      '2500295345',
      '2',
      '7cfbfd0f-7446-4733-8297-96928a9f6c0a',
      false,
      false,
      'M2500295343',
      false,
      '{"roshRisks":{"status":"Error","value":null},"mappa":{"status":"Retrieved","value":{"level":"CAT M2/LEVEL M2","lastUpdated":[2021,2,1]}},"tier":{"status":"Retrieved","value":{"level":"D2","lastUpdated":[2022,9,5]}},"flags":{"status":"Retrieved","value":["Risk to Known Adult"]}}'
    );
  

  INSERT into
  assessments (
    "id",
    "application_id",
    "allocated_to_user_id",
    "created_at",
    "schema_version"
  )
  VALUES
    (
      '1c095116-1f67-4d88-939e-68c4a271a06d',
      '7cfbfd0f-7446-4733-8297-96928a9f6c0a',
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
    "submitted_at",
    "noms_number"
  )
  values
    (
      'a999fa4d-1db8-4cd5-af39-99d6ca9d6c1d',
      CURRENT_DATE + 6,
      '8a39870c-3a1f-4e05-ad45-a450e15b242d',
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
      CURRENT_DATE + 28,
      'JD5CLIA'
    );
  

  insert into approved_premises_applications (
      "conviction_id",
      "event_number",
      "id",
      "is_pipe_application",
      "is_womens_application",
      "offence_id",
      "is_withdrawn",
      "risk_ratings"
    )
  values
    (
      '2500295345',
      '2',
      'a999fa4d-1db8-4cd5-af39-99d6ca9d6c1d',
      false,
      false,
      'M2500295343',
      false,
      '{"roshRisks":{"status":"Error","value":null},"mappa":{"status":"Retrieved","value":{"level":"CAT M2/LEVEL M2","lastUpdated":[2021,2,1]}},"tier":{"status":"Retrieved","value":{"level":"D2","lastUpdated":[2022,9,5]}},"flags":{"status":"Retrieved","value":["Risk to Known Adult"]}}'
    );
  

  INSERT into
  assessments (
    "id",
    "application_id",
    "allocated_to_user_id",
    "created_at",
    "schema_version"
  )
  VALUES
    (
      'f976becc-0bf2-46fe-97ec-f3eab7aedff0',
      'a999fa4d-1db8-4cd5-af39-99d6ca9d6c1d',
      'f9ff1c6e-6876-4ba8-8ca9-d7d2c6f673dc',
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
    "submitted_at",
    "noms_number"
  )
  values
    (
      '54430b83-a1b2-4c3b-88d7-3dced8df89a0',
      CURRENT_DATE + 14,
      '6dcd2559-2d14-4feb-8faf-89ad30dfa765',
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
      CURRENT_DATE + 15,
      'SX9UI94'
    );
  

  insert into approved_premises_applications (
      "conviction_id",
      "event_number",
      "id",
      "is_pipe_application",
      "is_womens_application",
      "offence_id",
      "is_withdrawn",
      "risk_ratings"
    )
  values
    (
      '2500295345',
      '2',
      '54430b83-a1b2-4c3b-88d7-3dced8df89a0',
      false,
      false,
      'M2500295343',
      false,
      '{"roshRisks":{"status":"Error","value":null},"mappa":{"status":"Retrieved","value":{"level":"CAT M2/LEVEL M2","lastUpdated":[2021,2,1]}},"tier":{"status":"Retrieved","value":{"level":"D2","lastUpdated":[2022,9,5]}},"flags":{"status":"Retrieved","value":["Risk to Known Adult"]}}'
    );
  

  INSERT into
  assessments (
    "id",
    "application_id",
    "allocated_to_user_id",
    "created_at",
    "schema_version"
  )
  VALUES
    (
      'f287cc27-b4a7-4333-9db8-f07b1d939268',
      '54430b83-a1b2-4c3b-88d7-3dced8df89a0',
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
    "submitted_at",
    "noms_number"
  )
  values
    (
      '516100fb-acd9-4001-b54b-43cbe7f20d6a',
      CURRENT_DATE + 5,
      '7e8d1738-a07d-4ba4-a8a7-9b7d9c9d27b2',
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
      CURRENT_DATE + 29,
      'KXTJQEF'
    );
  

  insert into approved_premises_applications (
      "conviction_id",
      "event_number",
      "id",
      "is_pipe_application",
      "is_womens_application",
      "offence_id",
      "is_withdrawn",
      "risk_ratings"
    )
  values
    (
      '2500295345',
      '2',
      '516100fb-acd9-4001-b54b-43cbe7f20d6a',
      false,
      false,
      'M2500295343',
      false,
      '{"roshRisks":{"status":"Error","value":null},"mappa":{"status":"Retrieved","value":{"level":"CAT M2/LEVEL M2","lastUpdated":[2021,2,1]}},"tier":{"status":"Retrieved","value":{"level":"D2","lastUpdated":[2022,9,5]}},"flags":{"status":"Retrieved","value":["Risk to Known Adult"]}}'
    );
  

  INSERT into
  assessments (
    "id",
    "application_id",
    "allocated_to_user_id",
    "created_at",
    "schema_version"
  )
  VALUES
    (
      '735bc450-a14c-463b-9e10-0e5f93279e31',
      '516100fb-acd9-4001-b54b-43cbe7f20d6a',
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
    "submitted_at",
    "noms_number"
  )
  values
    (
      '2a246c11-c98f-4fa7-9979-b9c4b4e9968b',
      CURRENT_DATE + 5,
      '7a424213-3a0c-45b0-9a51-4977243c2b21',
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
      CURRENT_DATE + 23,
      'N1RRJZU'
    );
  

  insert into approved_premises_applications (
      "conviction_id",
      "event_number",
      "id",
      "is_pipe_application",
      "is_womens_application",
      "offence_id",
      "is_withdrawn",
      "risk_ratings"
    )
  values
    (
      '2500295345',
      '2',
      '2a246c11-c98f-4fa7-9979-b9c4b4e9968b',
      false,
      false,
      'M2500295343',
      false,
      '{"roshRisks":{"status":"Error","value":null},"mappa":{"status":"Retrieved","value":{"level":"CAT M2/LEVEL M2","lastUpdated":[2021,2,1]}},"tier":{"status":"Retrieved","value":{"level":"D2","lastUpdated":[2022,9,5]}},"flags":{"status":"Retrieved","value":["Risk to Known Adult"]}}'
    );
  

  INSERT into
  assessments (
    "id",
    "application_id",
    "allocated_to_user_id",
    "created_at",
    "schema_version"
  )
  VALUES
    (
      '0288eba9-81c2-4274-9379-8ca8f60a5246',
      '2a246c11-c98f-4fa7-9979-b9c4b4e9968b',
      'f9ff1c6e-6876-4ba8-8ca9-d7d2c6f673dc',
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
    "submitted_at",
    "noms_number"
  )
  values
    (
      '3e884906-e5cd-46d8-b2d9-da5cc681f08f',
      CURRENT_DATE + 8,
      '531455f4-c76f-4943-b4eb-3c02d8fefa69',
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
      CURRENT_DATE + 15,
      '6X1DNW1'
    );
  

  insert into approved_premises_applications (
      "conviction_id",
      "event_number",
      "id",
      "is_pipe_application",
      "is_womens_application",
      "offence_id",
      "is_withdrawn",
      "risk_ratings"
    )
  values
    (
      '2500295345',
      '2',
      '3e884906-e5cd-46d8-b2d9-da5cc681f08f',
      false,
      false,
      'M2500295343',
      false,
      '{"roshRisks":{"status":"Error","value":null},"mappa":{"status":"Retrieved","value":{"level":"CAT M2/LEVEL M2","lastUpdated":[2021,2,1]}},"tier":{"status":"Retrieved","value":{"level":"D2","lastUpdated":[2022,9,5]}},"flags":{"status":"Retrieved","value":["Risk to Known Adult"]}}'
    );
  

  INSERT into
  assessments (
    "id",
    "application_id",
    "allocated_to_user_id",
    "created_at",
    "schema_version"
  )
  VALUES
    (
      '5d6c029a-258a-4611-b229-1936ee1dfdb5',
      '3e884906-e5cd-46d8-b2d9-da5cc681f08f',
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
    "submitted_at",
    "noms_number"
  )
  values
    (
      'a6774caf-53ca-497f-996c-ad4ac1219a90',
      CURRENT_DATE + 28,
      '531455f4-c76f-4943-b4eb-3c02d8fefa69',
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
      CURRENT_DATE + 27,
      'YCSW8BD'
    );
  

  insert into approved_premises_applications (
      "conviction_id",
      "event_number",
      "id",
      "is_pipe_application",
      "is_womens_application",
      "offence_id",
      "is_withdrawn",
      "risk_ratings"
    )
  values
    (
      '2500295345',
      '2',
      'a6774caf-53ca-497f-996c-ad4ac1219a90',
      false,
      false,
      'M2500295343',
      false,
      '{"roshRisks":{"status":"Error","value":null},"mappa":{"status":"Retrieved","value":{"level":"CAT M2/LEVEL M2","lastUpdated":[2021,2,1]}},"tier":{"status":"Retrieved","value":{"level":"D2","lastUpdated":[2022,9,5]}},"flags":{"status":"Retrieved","value":["Risk to Known Adult"]}}'
    );
  

  INSERT into
  assessments (
    "id",
    "application_id",
    "allocated_to_user_id",
    "created_at",
    "schema_version"
  )
  VALUES
    (
      '1b8ff903-14f4-4f4b-ae77-daecf886be49',
      'a6774caf-53ca-497f-996c-ad4ac1219a90',
      '7e8d1738-a07d-4ba4-a8a7-9b7d9c9d27b2',
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
    "submitted_at",
    "noms_number"
  )
  values
    (
      '57158b92-b8c2-41e3-bab5-456462032ccc',
      CURRENT_DATE + 11,
      '8a39870c-3a1f-4e05-ad45-a450e15b242d',
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
      CURRENT_DATE + 15,
      'KWAPTC7'
    );
  

  insert into approved_premises_applications (
      "conviction_id",
      "event_number",
      "id",
      "is_pipe_application",
      "is_womens_application",
      "offence_id",
      "is_withdrawn",
      "risk_ratings"
    )
  values
    (
      '2500295345',
      '2',
      '57158b92-b8c2-41e3-bab5-456462032ccc',
      false,
      false,
      'M2500295343',
      false,
      '{"roshRisks":{"status":"Error","value":null},"mappa":{"status":"Retrieved","value":{"level":"CAT M2/LEVEL M2","lastUpdated":[2021,2,1]}},"tier":{"status":"Retrieved","value":{"level":"D2","lastUpdated":[2022,9,5]}},"flags":{"status":"Retrieved","value":["Risk to Known Adult"]}}'
    );
  

  INSERT into
  assessments (
    "id",
    "application_id",
    "allocated_to_user_id",
    "created_at",
    "schema_version"
  )
  VALUES
    (
      '9a6e3663-bab4-46dc-884f-ce9bd84d7584',
      '57158b92-b8c2-41e3-bab5-456462032ccc',
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
    "submitted_at",
    "noms_number"
  )
  values
    (
      'c3f7ee7f-fbf5-4040-a363-a18631c34b3e',
      CURRENT_DATE + 21,
      '68715a03-06af-49ee-bae5-039c824ab9af',
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
      CURRENT_DATE + 9,
      'VTDINBA'
    );
  

  insert into approved_premises_applications (
      "conviction_id",
      "event_number",
      "id",
      "is_pipe_application",
      "is_womens_application",
      "offence_id",
      "is_withdrawn",
      "risk_ratings"
    )
  values
    (
      '2500295345',
      '2',
      'c3f7ee7f-fbf5-4040-a363-a18631c34b3e',
      false,
      false,
      'M2500295343',
      false,
      '{"roshRisks":{"status":"Error","value":null},"mappa":{"status":"Retrieved","value":{"level":"CAT M2/LEVEL M2","lastUpdated":[2021,2,1]}},"tier":{"status":"Retrieved","value":{"level":"D2","lastUpdated":[2022,9,5]}},"flags":{"status":"Retrieved","value":["Risk to Known Adult"]}}'
    );
  

  INSERT into
  assessments (
    "id",
    "application_id",
    "allocated_to_user_id",
    "created_at",
    "schema_version"
  )
  VALUES
    (
      'e988fd99-e834-45e9-9729-8bddc43d7948',
      'c3f7ee7f-fbf5-4040-a363-a18631c34b3e',
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
    "submitted_at",
    "noms_number"
  )
  values
    (
      'f74b1ab2-f194-4b34-9205-e34e75f69a53',
      CURRENT_DATE + 28,
      '68715a03-06af-49ee-bae5-039c824ab9af',
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
      CURRENT_DATE + 13,
      'HIR0PIN'
    );
  

  insert into approved_premises_applications (
      "conviction_id",
      "event_number",
      "id",
      "is_pipe_application",
      "is_womens_application",
      "offence_id",
      "is_withdrawn",
      "risk_ratings"
    )
  values
    (
      '2500295345',
      '2',
      'f74b1ab2-f194-4b34-9205-e34e75f69a53',
      false,
      false,
      'M2500295343',
      false,
      '{"roshRisks":{"status":"Error","value":null},"mappa":{"status":"Retrieved","value":{"level":"CAT M2/LEVEL M2","lastUpdated":[2021,2,1]}},"tier":{"status":"Retrieved","value":{"level":"D2","lastUpdated":[2022,9,5]}},"flags":{"status":"Retrieved","value":["Risk to Known Adult"]}}'
    );
  

  INSERT into
  assessments (
    "id",
    "application_id",
    "allocated_to_user_id",
    "created_at",
    "schema_version"
  )
  VALUES
    (
      'f0a40b46-7079-4ecd-8544-6b7bcc7515e3',
      'f74b1ab2-f194-4b34-9205-e34e75f69a53',
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
    "submitted_at",
    "noms_number"
  )
  values
    (
      '77063f49-e078-416a-a952-7693c4a6cd88',
      CURRENT_DATE + 17,
      'f9ff1c6e-6876-4ba8-8ca9-d7d2c6f673dc',
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
      CURRENT_DATE + 28,
      'QGH3OL6'
    );
  

  insert into approved_premises_applications (
      "conviction_id",
      "event_number",
      "id",
      "is_pipe_application",
      "is_womens_application",
      "offence_id",
      "is_withdrawn",
      "risk_ratings"
    )
  values
    (
      '2500295345',
      '2',
      '77063f49-e078-416a-a952-7693c4a6cd88',
      false,
      false,
      'M2500295343',
      false,
      '{"roshRisks":{"status":"Error","value":null},"mappa":{"status":"Retrieved","value":{"level":"CAT M2/LEVEL M2","lastUpdated":[2021,2,1]}},"tier":{"status":"Retrieved","value":{"level":"D2","lastUpdated":[2022,9,5]}},"flags":{"status":"Retrieved","value":["Risk to Known Adult"]}}'
    );
  

  INSERT into
  assessments (
    "id",
    "application_id",
    "allocated_to_user_id",
    "created_at",
    "schema_version"
  )
  VALUES
    (
      'dea03574-9320-4239-bfb7-6d4268a06aa6',
      '77063f49-e078-416a-a952-7693c4a6cd88',
      '7e8d1738-a07d-4ba4-a8a7-9b7d9c9d27b2',
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
    "submitted_at",
    "noms_number"
  )
  values
    (
      '215da410-44ab-4234-9730-f5c2b49a3384',
      CURRENT_DATE + 11,
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
      CURRENT_DATE + 9,
      'M9XWJ1S'
    );
  

  insert into approved_premises_applications (
      "conviction_id",
      "event_number",
      "id",
      "is_pipe_application",
      "is_womens_application",
      "offence_id",
      "is_withdrawn",
      "risk_ratings"
    )
  values
    (
      '2500295345',
      '2',
      '215da410-44ab-4234-9730-f5c2b49a3384',
      false,
      false,
      'M2500295343',
      false,
      '{"roshRisks":{"status":"Error","value":null},"mappa":{"status":"Retrieved","value":{"level":"CAT M2/LEVEL M2","lastUpdated":[2021,2,1]}},"tier":{"status":"Retrieved","value":{"level":"D2","lastUpdated":[2022,9,5]}},"flags":{"status":"Retrieved","value":["Risk to Known Adult"]}}'
    );
  

  INSERT into
  assessments (
    "id",
    "application_id",
    "allocated_to_user_id",
    "created_at",
    "schema_version"
  )
  VALUES
    (
      '2b21ebe8-c39d-4f30-b356-d44f44113da8',
      '215da410-44ab-4234-9730-f5c2b49a3384',
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
    "submitted_at",
    "noms_number"
  )
  values
    (
      '6f19ef0e-bef1-4b11-a5ed-a51725bef15c',
      CURRENT_DATE + 8,
      '8a39870c-3a1f-4e05-ad45-a450e15b242d',
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
      CURRENT_DATE + 9,
      'YX2YNT2'
    );
  

  insert into approved_premises_applications (
      "conviction_id",
      "event_number",
      "id",
      "is_pipe_application",
      "is_womens_application",
      "offence_id",
      "is_withdrawn",
      "risk_ratings"
    )
  values
    (
      '2500295345',
      '2',
      '6f19ef0e-bef1-4b11-a5ed-a51725bef15c',
      false,
      false,
      'M2500295343',
      false,
      '{"roshRisks":{"status":"Error","value":null},"mappa":{"status":"Retrieved","value":{"level":"CAT M2/LEVEL M2","lastUpdated":[2021,2,1]}},"tier":{"status":"Retrieved","value":{"level":"D2","lastUpdated":[2022,9,5]}},"flags":{"status":"Retrieved","value":["Risk to Known Adult"]}}'
    );
  

  INSERT into
  assessments (
    "id",
    "application_id",
    "allocated_to_user_id",
    "created_at",
    "schema_version"
  )
  VALUES
    (
      '40c27500-6b52-4a0c-b95b-e6d7acab5ff8',
      '6f19ef0e-bef1-4b11-a5ed-a51725bef15c',
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
    "submitted_at",
    "noms_number"
  )
  values
    (
      '5a138e27-f4eb-4b52-958a-d3284987f1db',
      CURRENT_DATE + 13,
      '7e8d1738-a07d-4ba4-a8a7-9b7d9c9d27b2',
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
      CURRENT_DATE + 4,
      'NYVS303'
    );
  

  insert into approved_premises_applications (
      "conviction_id",
      "event_number",
      "id",
      "is_pipe_application",
      "is_womens_application",
      "offence_id",
      "is_withdrawn",
      "risk_ratings"
    )
  values
    (
      '2500295345',
      '2',
      '5a138e27-f4eb-4b52-958a-d3284987f1db',
      false,
      false,
      'M2500295343',
      false,
      '{"roshRisks":{"status":"Error","value":null},"mappa":{"status":"Retrieved","value":{"level":"CAT M2/LEVEL M2","lastUpdated":[2021,2,1]}},"tier":{"status":"Retrieved","value":{"level":"D2","lastUpdated":[2022,9,5]}},"flags":{"status":"Retrieved","value":["Risk to Known Adult"]}}'
    );
  

  INSERT into
  assessments (
    "id",
    "application_id",
    "allocated_to_user_id",
    "created_at",
    "schema_version"
  )
  VALUES
    (
      '41ba2c85-1374-4126-bc10-bb28da8ab933',
      '5a138e27-f4eb-4b52-958a-d3284987f1db',
      '7e8d1738-a07d-4ba4-a8a7-9b7d9c9d27b2',
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
    "submitted_at",
    "noms_number"
  )
  values
    (
      '2a6be6b7-eafa-4f57-82c8-f92263d583e2',
      CURRENT_DATE + 1,
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
      CURRENT_DATE + 19,
      'HBVE0LJ'
    );
  

  insert into approved_premises_applications (
      "conviction_id",
      "event_number",
      "id",
      "is_pipe_application",
      "is_womens_application",
      "offence_id",
      "is_withdrawn",
      "risk_ratings"
    )
  values
    (
      '2500295345',
      '2',
      '2a6be6b7-eafa-4f57-82c8-f92263d583e2',
      false,
      false,
      'M2500295343',
      false,
      '{"roshRisks":{"status":"Error","value":null},"mappa":{"status":"Retrieved","value":{"level":"CAT M2/LEVEL M2","lastUpdated":[2021,2,1]}},"tier":{"status":"Retrieved","value":{"level":"D2","lastUpdated":[2022,9,5]}},"flags":{"status":"Retrieved","value":["Risk to Known Adult"]}}'
    );
  

  INSERT into
  assessments (
    "id",
    "application_id",
    "allocated_to_user_id",
    "created_at",
    "schema_version"
  )
  VALUES
    (
      '0573f229-b245-4e01-b6fd-c2b91eafc975',
      '2a6be6b7-eafa-4f57-82c8-f92263d583e2',
      'f9ff1c6e-6876-4ba8-8ca9-d7d2c6f673dc',
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
    "submitted_at",
    "noms_number"
  )
  values
    (
      '4741af48-aac8-41f1-9b53-56d1e1afe92a',
      CURRENT_DATE + 27,
      'f9ff1c6e-6876-4ba8-8ca9-d7d2c6f673dc',
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
      CURRENT_DATE + 8,
      '6DO89QY'
    );
  

  insert into approved_premises_applications (
      "conviction_id",
      "event_number",
      "id",
      "is_pipe_application",
      "is_womens_application",
      "offence_id",
      "is_withdrawn",
      "risk_ratings"
    )
  values
    (
      '2500295345',
      '2',
      '4741af48-aac8-41f1-9b53-56d1e1afe92a',
      false,
      false,
      'M2500295343',
      false,
      '{"roshRisks":{"status":"Error","value":null},"mappa":{"status":"Retrieved","value":{"level":"CAT M2/LEVEL M2","lastUpdated":[2021,2,1]}},"tier":{"status":"Retrieved","value":{"level":"D2","lastUpdated":[2022,9,5]}},"flags":{"status":"Retrieved","value":["Risk to Known Adult"]}}'
    );
  

  INSERT into
  assessments (
    "id",
    "application_id",
    "allocated_to_user_id",
    "created_at",
    "schema_version"
  )
  VALUES
    (
      '148e0532-aecd-480b-a68e-ecb293d5c828',
      '4741af48-aac8-41f1-9b53-56d1e1afe92a',
      'f9ff1c6e-6876-4ba8-8ca9-d7d2c6f673dc',
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
