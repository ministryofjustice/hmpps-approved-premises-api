
-- ${flyway:timestamp}
TRUNCATE TABLE applications CASCADE;
TRUNCATE TABLE assessments CASCADE;


DO $$
DECLARE
   applicationData json := '{"basic-information":{"sentence-type":{"sentenceType":"bailPlacement"},"situation":{"situation":"bailSentence"},"release-date":{"releaseDate-year":"2022","releaseDate-month":"11","releaseDate-day":"14","releaseDate":"2022-11-14","knowReleaseDate":"yes"},"placement-date":{"startDateSameAsReleaseDate":"yes"},"placement-purpose":{"placementPurposes":["drugAlcoholMonitoring","otherReason"],"otherReason":"Reason"}},"type-of-ap":{"ap-type":{"type":"standard"}},"oasys-import":{"optional-oasys-sections":{"needsLinkedToReoffending":[{"section":1,"name":"accommodation","linkedToHarm":false,"linkedToReOffending":true},{"section":2,"name":"relationships","linkedToHarm":false,"linkedToReOffending":true}],"otherNeeds":[{"section":3,"name":"emotional","linkedToHarm":false,"linkedToReOffending":false},{"section":4,"name":"thinking","linkedToHarm":false,"linkedToReOffending":false}]},"rosh-summary":{"roshAnswers":["Some answer for the first RoSH question. With an extra comment 1","Some answer for the second RoSH question. With an extra comment 2","Some answer for the third RoSH question. With an extra comment 3"],"roshSummaries":[{"questionNumber":"1","label":"The first RoSH question","answer":"Some answer for the first RoSH question"},{"questionNumber":"2","label":"The second RoSH question","answer":"Some answer for the second RoSH question"},{"questionNumber":"3","label":"The third RoSH question","answer":"Some answer for the third RoSH question"}]},"offence-details":{"offenceDetailsAnswers":["Some answer for the first offence details question. With an extra comment 1","Some answer for the second offence details question. With an extra comment 2","Some answer for the third offence details question. With an extra comment 3"],"offenceDetailsSummaries":[{"questionNumber":"1","label":"The first offence details question","answer":"Some answer for the first offence details question"},{"questionNumber":"2","label":"The second offence details question","answer":"Some answer for the second offence details question"},{"questionNumber":"3","label":"The third offence details question","answer":"Some answer for the third offence details question"}]},"supporting-information":{"supportingInformationAnswers":["Some answer for the first supporting information question. With an extra comment 1","Some answer for the second supporting information question. With an extra comment 2","Some answer for the third supporting information question. With an extra comment 3"],"supportingInformationSummaries":[{"questionNumber":"1","label":"The first supporting information question","answer":"Some answer for the first supporting information question"},{"questionNumber":"2","label":"The second supporting information question","answer":"Some answer for the second supporting information question"},{"questionNumber":"3","label":"The third supporting information question","answer":"Some answer for the third supporting information question"}]},"risk-management-plan":{"riskManagementAnswers":["Some answer for the first risk management question. With an extra comment 1","Some answer for the second risk management question. With an extra comment 2","Some answer for the third risk management question. With an extra comment 3"],"riskManagementSummaries":[{"questionNumber":"1","label":"The first risk management question","answer":"Some answer for the first risk management question"},{"questionNumber":"2","label":"The second risk management question","answer":"Some answer for the second risk management question"},{"questionNumber":"3","label":"The third risk management question","answer":"Some answer for the third risk management question"}]},"risk-to-self":{"riskToSelfAnswers":["Some answer for the first risk to self question. With an extra comment 1","Some answer for the second risk to self question. With an extra comment 2","Some answer for the third risk to self question. With an extra comment 3"],"riskToSelfSummaries":[{"questionNumber":"1","label":"The first risk to self question","answer":"Some answer for the first risk to self question"},{"questionNumber":"2","label":"The second risk to self question","answer":"Some answer for the second risk to self question"},{"questionNumber":"3","label":"The third risk to self question","answer":"Some answer for the third risk to self question"}]}},"risk-management-features":{"risk-management-features":{"manageRiskDetails":"est vero nesciunt","additionalFeaturesDetails":"deleniti enim necessitatibus"},"convicted-offences":{"response":"yes"},"type-of-convicted-offence":{"offenceConvictions":["arson","sexualOffence","hateCrimes","childNonSexualOffence"]},"date-of-offence":{"arsonOffence":["current"],"hateCrime":["previous"],"inPersonSexualOffence":["current","previous"]},"rehabilitative-interventions":{"rehabilitativeInterventions":["accommodation","drugsAndAlcohol","childrenAndFamilies","health","educationTrainingAndEmployment","financeBenefitsAndDebt","attitudesAndBehaviour","abuse","other"],"otherIntervention":"Another"}},"prison-information":{"case-notes":{"caseNoteIds":["a30173ca-061f-42c9-a1a2-28c70b282d3f","4a477187-b77f-4fcc-a919-43a6633ee868"],"selectedCaseNotes":[{"authorName":"Denise Collins","id":"a30173ca-061f-42c9-a1a2-28c70b282d3f","createdAt":"2022-11-10","occurredAt":"2022-10-19","sensitive":false,"subType":"Ressettlement","type":"Social Care","note":"Note 1"},{"authorName":"Leticia Mann","id":"4a477187-b77f-4fcc-a919-43a6633ee868","createdAt":"2022-07-24","occurredAt":"2022-09-22","sensitive":true,"subType":"Quality Work","type":"General","note":"Note 2"}],"moreDetail":"some details","adjudications":[{"id":69927,"reportedAt":"2022-10-09","establishment":"Hawthorne","offenceDescription":"Nam vel nisi fugiat veniam possimus omnis.","hearingHeld":false,"finding":"NOT_PROVED"},{"id":39963,"reportedAt":"2022-07-10","establishment":"Oklahoma City","offenceDescription":"Illum maxime enim explicabo soluta sequi voluptas.","hearingHeld":true,"finding":"PROVED"},{"id":77431,"reportedAt":"2022-05-30","establishment":"Jurupa Valley","offenceDescription":"Quis porro nemo voluptates doloribus atque quis provident iure.","hearingHeld":false,"finding":"PROVED"}]}},"location-factors":{"describe-location-factors":{"postcodeArea":"SW1","positiveFactors":"Some positive factors","restrictions":"yes","restrictionDetail":"Restrictions go here","alternativeRadiusAccepted":"yes","alternativeRadius":"70","differentPDU":"no"},"pdu-transfer":{"transferStatus":"yes","probationPractitioner":"Probation Practicioner"}},"access-and-healthcare":{"access-needs":{"additionalNeeds":["mobility","learningDisability","neurodivergentConditions"],"religiousOrCulturalNeeds":"yes","religiousOrCulturalNeedsDetails":"sunt at minus","needsInterpreter":"yes","interpreterLanguage":"beatae eligendi explicabo","careActAssessmentCompleted":"yes"},"access-needs-mobility":{"needsWheelchair":"yes","mobilityNeeds":"laboriosam","visualImpairment":"exercitationem"},"covid":{"fullyVaccinated":"yes","highRisk":"yes","additionalCovidInfo":"additional info"}},"further-considerations":{"room-sharing":{"riskToStaff":"no","riskToStaffDetail":"","riskToOthers":"no","riskToOthersDetail":"","sharingConcerns":"yes","sharingConcernsDetail":"Some details here","traumaConcerns":"no","traumaConcernsDetail":"","sharingBenefits":"no","sharingBenefitsDetail":""},"vulnerability":{"exploitable":"no","exploitableDetail":"","exploitOthers":"yes","exploitOthersDetail":"Some details here"},"previous-placements":{"previousPlacement":"yes","previousPlacementDetail":"Some details here"},"complex-case-board":{"complexCaseBoard":"yes","complexCaseBoardDetail":"Some details here"},"catering":{"catering":"yes","cateringDetail":"Some details here"},"arson":{"arson":"yes","arsonDetail":"Some details here"}},"move-on":{"placement-duration":{"duration":"10","durationDetail":"Some more information"},"relocation-region":{"postcodeArea":"XX1"},"plans-in-place":{"arePlansInPlace":"yes"},"type-of-accommodation":{"accommodationType":"foreignNational","otherAccommodationType":""},"foreign-national":{"response":"yes","date-year":"2015","date-month":"1","date-day":"1","date":"2015-01-01"}},"attach-required-documents":{"attach-documents":{"selectedDocuments":[{"id":"aeb43e06-a4a1-460f-9acf-e2495de84604","level":"Offender","fileName":"Random offender document1.pdf","createdAt":"2019-09-10T00:00:00Z","typeCode":"OFFENDER_DOCUMENT","typeDescription":"Offender related","description":"Some Description 1"},{"id":"5f8a2288-0a17-4e45-afd5-5e57794a7b53","level":"Conviction","fileName":"national.asm","createdAt":"2022-09-13","typeCode":"serenade","typeDescription":"porter","description":"Some Description 2"}]}},"check-your-answers":{"review":{"reviewed":"1"}}}';
   applicationDocument json := '{"basic-information":[{"Which of the following best describes the sentence type?":"Standard determinate custody"},{"What type of release will the application support?":"Release on Temporary License (ROTL)"},{"Do you know Mr Smith’s release date?":"Yes","Release Date":"Saturday 1 April 2023"},{"Is Saturday 1 April 2023 the date you want the placement to start?":"Yes"},{"What is the purpose of the AP placement?":"Prevent contact, Help individual readjust to life outside custody"}],"type-of-ap":[{"Which type of AP does Mr Smith require?":"Standard"}],"oasys-import":[{"Needs linked to reoffending":"9. Alcohol","Needs not linked to risk of serious harm or reoffending":"5. Finance, 11. Thinking and behavioural"},{"R10.1":"[R10.1] IN CUSTODY\r\n \r\n KNOWN ADULTS:\r\n Such as Ms Elaine Underhill and any of the victims of the index offence if they are placed close to Mr Smith cell.\r\n \r\n CHILDREN:\r\n Simon Smith, Roger Smith, Lindy Smith","R10.2":"[R10.2] IN CUSTODY:\r\n \r\n KNOWN ADULTS:\r\n Intimidation, threats of violence, use of weapons or boiling water, physical education and violent assault, long term psychological impact as a result of Mr Smith violent behaviour. This harm may be cause in the course of physical altercation due to seeking revenge or holding grudges...","R10.3":"[R10.3] STATIC RISK FACTORS:\r\n \r\n Mr Smith gender - At the time of the offence, Mr Smith was 35 years old and considered to be in the higher risk group.\r\n \r\n STABLE DYNAMIC RISK FACTORS:\r\n Thinking and behaviour - limited ability to manage mood or emotions poor impulse control. Mr Smith demonstrated cognitive deficit in committing the index offences and this still remains a concern...","R10.4":"[R10.4] CIRCUMSTANCES:\r\n A lack of stable accommodation\r\n unemployment\r\n non constructive use of time\r\n breakdown of family support\r\n being unable to access benefits\r\n \r\n UNDERLYING FACTORS:\r\n pro criminal attitudes supporting commission of crime...","R10.5":"[R10.5] Engage with Mental Health Services in prison and in the community.\r\n Maintain emotional stability.\r\n Abstain from alcohol.\r\n Abstain from illegal drugs.\r\n Regular testing for alcohol and drug use, in prison and on Licence.\r\n Maintain stable accommodation..."},{"2.1":"","2.4.1":"","2.4.2":"","2.12":"","2.5":"","2.8.3":"","2.98":""},{"3.9":"[3.9] Mr Smith told me that he was renting a room in a shared house, prior to his current remand...","4.9":"[4.9] He said that he has since learnt to read and write during periods of custody and an initial assessment of his...","5.9":"[5.9] Mr Smith was in receipt of Job Seekers Allowance prior to his current remand in custody...","9.9":"[9.9] Mr Smith told me that he rarely consumes alcohol and has not had any problems associated with excessive drinking in years...","11.9":"[11.9] The circumstances of the current offence suggest that Mr Smith acted ..."},{"RM28":"[RM28] Mr Smith is currently on remand awaiting sentence","RM35":"[RM35] Placeholder content for additional comments","RM34":"[RM34] Discuss any concerns with line manager/SPO Increase frequency of reporting Bring forward MAPPA/Consider Level 2 MAPPA professionals meetings  considered if MAPPA not applicable. Consider referral to Approved Premises Consider motivational interviewing/engagement officer.  Joint interview with police OM or other relevant agencies? To raise concerns Referral to substance misues worker for additional support. Warning letters to be employed Recall/Breach if risk not manageable.","RM33":"[RM33] Undertake sentence planning with OS within 8 or 16 weeks of sentence carats - continue to work with Mr Smith and refer for relevant programmes during any prison sentence imposed. DIP supervise DRR is imposed or provide support post release if conditions/requirements to manage the specific risks.","RM32":"[RM32] Requires referral to community drug DIP upon  release. To link in with employment/training resources such as College and ETEO. Completion of victim empathy module via 1-1 supervision to build upon awareness of impact of offending.   Level setting to be undertaken by Senior Probation Officer and OMU. Multi Agency Public Protection Panel to be convened if level 2 either at start of supervision or 6 months prior to release","RM31":"[RM31] State they will have secure accommodation for Mr Smith  and partner on release, although she too is subject to a DRR at present. Supportive wider family.  Has completed previously Short Duration programme and attended one of the support sessions .  Engaged with CARATS in custody.  Complied with voluntary drug tests in custody. Some motivation to desist from alcohol/drug use in the future, this needs to be encouraged with use of community resources. Completed drug and heroin awareness programmes in custody.","RM30":"[RM30] Probation Officer, Education training and employment Officer, Prison Offender Supervisor","RM28.1":"[RM28.1] Currently on remand at HMP Wandsworth - Management of case under MAPPA - level not yet set."},{"R8.1.1":"Review 06.10.21:\r\n \r\n         There have been numerous ACCTs opened since 2013 and every subsequent year he has been in custody.  In 2021...","R8.2.1":"Has told prison staff that he swallowed batteries on one occasion due to wanting..","R8.3.1":"Review 06.10.21:\r\n \r\n         A previous assessor opined that Mr Smith was displaying all the characteristics of someone..."}],"risk-management-features":[{"Describe why an AP placement is needed to manage the risk of Mr Smith":"Some words","Provide details of any additional measures that will be necessary for the management of risk":"Some words"},{"Has Mr Smith ever been convicted of any arson offences, sexual offences, hate crimes or non-sexual offences against children?":"No"},{"Which rehabilitative interventions will support the person''s Approved Premises (AP) placement?":"Drugs and alcohol, Children and families"}],"prison-information":[{"Selected prison case notes that support this application":["Array"],"Adjudications":["Array"]}],"location-factors":[{"What is the preferred location for the AP placement?":"WS1","Give details of any positive factors for the person in this location.":"Some words","Are there any restrictions linked to placement location?":"No","If an AP Placement is not available in the persons preferred area, would a placement further away be considered?":"No","Is the person moving to a different area where they''ll be managed by a different probation delivery unit (PDU)?":"No"},{"Have you agreed Mr Smith''s transfer/supervision with the receiving PDU?":"No, I still need to make arrangements"}],"access-and-healthcare":[{"Does Mr Smith have any of the following needs?":"Learning disability, hearing impairment","Does Mr Smith have any religious or cultural needs?":"No","Details of religious or cultural needs":"","Does Mr Smith need an interpreter?":"No","Has a care act assessment been completed?":"No"},{"Has Mr Smith been fully vaccinated for COVID-19?":"No","Is Mr Smith at a higher risk from COVID-19?":"No"}],"further-considerations":[{"Is there any evidence that the person may pose a risk to AP staff?":"No","Is there any evidence that the person may pose a risk to other AP residents?":"No","Do you have any concerns about the person sharing a bedroom?":"No","Is there any evidence of previous trauma or significant event in the persons history which would indicate that room share may not be suitable?":"No","Is there potential for the person to benefit from a room share?":"No"},{"Are you aware that Mr Smith is vulnerable to exploitation from others?":"No","Is there any evidence or expectation that Mr Smith may groom, radicalise or exploit others?":"No"},{"Has Mr Smith stayed or been offered a placement in an AP before?":"No"},{"Does Mr Smith''s gender identity require a complex case board to review their application?":"No"},{"Do you have any concerns about Mr Smith catering for themselves?":"No"},{"Does Mr Smith need a specialist arson room?":"No"}],"move-on":[{"What duration of placement do you recommend?":"11 weeks","Provide any additional information":"Some words"},{"Where is Mr Smith most likely to live when they move on from the AP?":"WS13"},{"Are move on arrangements already in place for when the person leaves the AP?":"No"},{"What type of accommodation will Mr Smith have when they leave the AP?":"Supported housing"}],"attach-required-documents":[{"PRE-CONS.pdf":"Previous convictions as of 01/09/2019","Personal circumstances.pdf.pdf":"Personal circumstance of AP - Medication in Posession  - Assessment started on 11/09/2019","NSIOffender.pdf":"Non Statutory Intervention for OPD Community Pathway on 16/10/2019","paroleParom1Report_04092019_122116_OMIC_A_X320741.pdf":"Parole Assessment Report at Berwyn (HMP) requested on 05/09/2019"}],"check-your-answers":[{}]}';
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
      '2f2f0795-30c3-4bbc-af00-12f3373c16b8',
      CURRENT_DATE + 20,
      '68715a03-06af-49ee-bae5-039c824ab9af',
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
      '2f2f0795-30c3-4bbc-af00-12f3373c16b8',
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
      '11c2c5ed-32d6-4239-ba25-6dd8a0d8a13e',
      '2f2f0795-30c3-4bbc-af00-12f3373c16b8',
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
      '70139e7f-5454-4fcd-9682-eff43bec498f',
      CURRENT_DATE + 22,
      '6dcd2559-2d14-4feb-8faf-89ad30dfa765',
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
      '70139e7f-5454-4fcd-9682-eff43bec498f',
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
      '84f503a6-7576-41f1-86fa-44892310fbf3',
      '70139e7f-5454-4fcd-9682-eff43bec498f',
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
      '21821ee4-1100-45e4-b657-f3d96699d89d',
      CURRENT_DATE + 7,
      '531455f4-c76f-4943-b4eb-3c02d8fefa69',
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
      CURRENT_DATE + 26
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
      '21821ee4-1100-45e4-b657-f3d96699d89d',
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
      '66d1189a-1380-4ae7-8384-d5cc218176a0',
      '21821ee4-1100-45e4-b657-f3d96699d89d',
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
      'be56caed-b69e-4b34-ab09-a7c9b0c70f4a',
      CURRENT_DATE + 1,
      '7a424213-3a0c-45b0-9a51-4977243c2b21',
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
      'be56caed-b69e-4b34-ab09-a7c9b0c70f4a',
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
      '6290316a-6d1c-481b-9c1e-40d96be3dcc5',
      'be56caed-b69e-4b34-ab09-a7c9b0c70f4a',
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
      '6afced9a-a0ae-4013-84de-503f25334473',
      CURRENT_DATE + 25,
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
      CURRENT_DATE + 19
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
      '6afced9a-a0ae-4013-84de-503f25334473',
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
      '2ca1cb88-8628-4f9c-82c9-a58accb6c8e5',
      '6afced9a-a0ae-4013-84de-503f25334473',
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
      'f988e450-0017-40f9-9e4d-e27484bc7b66',
      CURRENT_DATE + 21,
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
      'f988e450-0017-40f9-9e4d-e27484bc7b66',
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
      '4343e319-206c-4d5f-9610-26688b80f811',
      'f988e450-0017-40f9-9e4d-e27484bc7b66',
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
      '2d6af1be-cc15-43ed-a940-feb8826953e4',
      CURRENT_DATE + 16,
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
      CURRENT_DATE + 6
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
      '2d6af1be-cc15-43ed-a940-feb8826953e4',
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
      '8c90cb5b-7ca6-41bb-931c-b4fe48a728bf',
      '2d6af1be-cc15-43ed-a940-feb8826953e4',
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
      'b8af221f-61da-4338-88ab-e502e10a3256',
      CURRENT_DATE + 24,
      '531455f4-c76f-4943-b4eb-3c02d8fefa69',
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
      'b8af221f-61da-4338-88ab-e502e10a3256',
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
      '7ef4b33d-11ff-424e-afb1-bb7fffe98240',
      'b8af221f-61da-4338-88ab-e502e10a3256',
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
      '1c64ab71-b389-4fc2-9f55-05523e93f503',
      CURRENT_DATE + 7,
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
      '1c64ab71-b389-4fc2-9f55-05523e93f503',
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
      'a1d3934a-ec76-4555-b652-d8e5f684c8b0',
      '1c64ab71-b389-4fc2-9f55-05523e93f503',
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
      'b155aeef-ce00-4a8c-b700-7cd475d40a2b',
      CURRENT_DATE + 0,
      '7a424213-3a0c-45b0-9a51-4977243c2b21',
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
      CURRENT_DATE + 2
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
      'b155aeef-ce00-4a8c-b700-7cd475d40a2b',
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
      'f4f8c779-84d9-40c1-840b-3bd8a848183d',
      'b155aeef-ce00-4a8c-b700-7cd475d40a2b',
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
      'be86194d-e9dc-4c2c-9149-9f8a8baf08d4',
      CURRENT_DATE + 8,
      '68715a03-06af-49ee-bae5-039c824ab9af',
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
      'be86194d-e9dc-4c2c-9149-9f8a8baf08d4',
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
      'a3aeed66-ac61-47bf-b5bf-8be33e9b0277',
      'be86194d-e9dc-4c2c-9149-9f8a8baf08d4',
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
      'f1dcf109-1d37-4f07-a8cf-0afafea652ac',
      CURRENT_DATE + 17,
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
      'f1dcf109-1d37-4f07-a8cf-0afafea652ac',
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
      '7cfc0ea7-c0fe-4ad3-bc44-cda2f26ded2f',
      'f1dcf109-1d37-4f07-a8cf-0afafea652ac',
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
      '94b6b6ce-0792-4340-a810-b9c6dec0d1c2',
      CURRENT_DATE + 16,
      '6dcd2559-2d14-4feb-8faf-89ad30dfa765',
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
      '94b6b6ce-0792-4340-a810-b9c6dec0d1c2',
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
      'd42744e0-b19b-450a-9934-d7b03844dfab',
      '94b6b6ce-0792-4340-a810-b9c6dec0d1c2',
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
      '53cb87e2-804f-4b69-a50f-923dc753567a',
      CURRENT_DATE + 11,
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
      '53cb87e2-804f-4b69-a50f-923dc753567a',
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
      '4a72af67-fa2f-4e84-914b-53c02373161a',
      '53cb87e2-804f-4b69-a50f-923dc753567a',
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
      '8f788870-e88c-449c-a5e8-194bd63b01d9',
      CURRENT_DATE + 26,
      '7a424213-3a0c-45b0-9a51-4977243c2b21',
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
      '8f788870-e88c-449c-a5e8-194bd63b01d9',
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
      '130ab04e-b0ab-4414-bbef-4141374d6635',
      '8f788870-e88c-449c-a5e8-194bd63b01d9',
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
      '7d7c6eb8-89c8-4723-a255-8220b43fcdc4',
      CURRENT_DATE + 16,
      '68715a03-06af-49ee-bae5-039c824ab9af',
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
      '7d7c6eb8-89c8-4723-a255-8220b43fcdc4',
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
      '04cad38f-f151-48a2-9c45-f54679b30c88',
      '7d7c6eb8-89c8-4723-a255-8220b43fcdc4',
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
      'f6d7032c-8fdb-4706-8c69-0dc2a4074a26',
      CURRENT_DATE + 1,
      '68715a03-06af-49ee-bae5-039c824ab9af',
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
      'f6d7032c-8fdb-4706-8c69-0dc2a4074a26',
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
      '5ede72f0-4637-4ef5-a9c5-a3d2bb19f843',
      'f6d7032c-8fdb-4706-8c69-0dc2a4074a26',
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
      'e2e1841f-a625-42ea-a030-07c120229689',
      CURRENT_DATE + 4,
      '531455f4-c76f-4943-b4eb-3c02d8fefa69',
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
      'e2e1841f-a625-42ea-a030-07c120229689',
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
      'a681cafa-a79b-4ce2-812d-7722691734de',
      'e2e1841f-a625-42ea-a030-07c120229689',
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
      'dfc9e61e-a043-4d7a-9bed-7fe462517f37',
      CURRENT_DATE + 24,
      '68715a03-06af-49ee-bae5-039c824ab9af',
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
      CURRENT_DATE + 9
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
      'dfc9e61e-a043-4d7a-9bed-7fe462517f37',
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
      '32c394d5-9afb-495b-8348-3936a096a341',
      'dfc9e61e-a043-4d7a-9bed-7fe462517f37',
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
      '0ba231be-be19-4dcc-9d76-7a5a9c91dfe4',
      CURRENT_DATE + 6,
      '531455f4-c76f-4943-b4eb-3c02d8fefa69',
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
      '0ba231be-be19-4dcc-9d76-7a5a9c91dfe4',
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
      '4e3785eb-2c3a-4c00-a7f8-4b0fb0e9c7be',
      '0ba231be-be19-4dcc-9d76-7a5a9c91dfe4',
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
  
END $$;
