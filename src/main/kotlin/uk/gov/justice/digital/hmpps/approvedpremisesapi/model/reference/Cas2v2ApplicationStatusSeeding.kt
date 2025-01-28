package uk.gov.justice.digital.hmpps.approvedpremisesapi.model.reference

import java.util.UUID

@SuppressWarnings("LargeClass")
object Cas2v2ApplicationStatusSeeding {

  @SuppressWarnings("LongMethod")
  fun statusList(): List<Cas2v2PersistedApplicationStatus> {
    return listOf(
      Cas2v2PersistedApplicationStatus(
        id = UUID.fromString("f5cd423b-08eb-4efb-96ff-5cc6bb073905"),
        name = "moreInfoRequested",
        label = "More information requested",
        description = "The prison offender manager (POM) must provide information requested for the application to progress.",
        statusDetails = listOf(
          Cas2v2PersistedApplicationStatusDetail(
            id = UUID.fromString("fabbb8c0-344e-4a9d-a964-7987b22d09c6"),
            name = "personalInformation",
            label = "Personal information",
            children = emptyList(),
          ),
          Cas2v2PersistedApplicationStatusDetail(
            id = UUID.fromString("05669c8a-d65c-48d2-a5e4-0c3f6fc8977b"),
            name = "exclusionZonesAndAreas",
            label = "Exclusion zones and preferred areas",
          ),
          Cas2v2PersistedApplicationStatusDetail(
            id = UUID.fromString("831c241d-63f3-4d17-b969-b8154d7e4902"),
            name = "healthNeeds",
            label = "Health needs",
            children = listOf(
              Cas2v2PersistedApplicationStatusDetail(
                id = UUID.fromString("175aeed1-4841-4337-9b03-407b140fa8c5"),
                name = "healthNeedsCommunicationNeeds",
                label = "Communication needs",
                children = listOf(
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("424953de-4b8f-446e-a996-6339ce14d844"),
                    name = "healthNeedsCommunicationNeedsSeverity",
                    label = "Need and severity",
                    children = emptyList(),
                  ),
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("493ae6d6-28b6-472f-b901-b35251f2077c"),
                    name = "healthNeedsCommunicationNeedsIndependentLiving",
                    label = "Info on independent living",
                    children = emptyList(),
                  ),
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("d56f16b6-bf99-4b0d-ae93-968ead98254d"),
                    name = "healthNeedsCommunicationNeedsSupport",
                    label = "Support they have / will require",
                    children = emptyList(),
                  ),
                ),
              ),
              Cas2v2PersistedApplicationStatusDetail(
                id = UUID.fromString("5d2bbca0-af62-4bd4-891b-2e103b359764"),
                name = "healthNeedsLearningDifficulties",
                label = "Learning difficulties",
                children = listOf(
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("3cb2ef46-6674-4828-bf3b-c72ec2346515"),
                    name = "healthNeedsLearningDifficultiesNeeds",
                    label = "Need and effect on everyday life",
                    children = emptyList(),
                  ),
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("0258e157-456e-4ba7-b5c0-b19ee2649069"),
                    name = "healthNeedsLearningDifficultiesMedication",
                    label = "Medication / Treatment",
                    children = emptyList(),
                  ),
                ),
              ),
              Cas2v2PersistedApplicationStatusDetail(
                id = UUID.fromString("2df0bc70-fe83-4d8e-be05-f88e09d0f33e"),
                name = "healthNeedsMobilityNeeds",
                label = "Mobility needs",
                children = listOf(
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("b026391f-5a77-4377-9623-9b7644f77fce"),
                    name = "healthNeedsMobilityNeedsEffect",
                    label = "Need and effect on everyday life",
                    children = emptyList(),
                  ),
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("eaae138e-bc5a-400f-b38d-ed247d1c19f5"),
                    name = "healthNeedsMobilityNeedsStairs",
                    label = "Ability to use stairs",
                    children = emptyList(),
                  ),
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("d7ba6939-463f-4e6c-9ae8-eeb37d016cd4"),
                    name = "healthNeedsMobilityNeedsMedication",
                    label = "Medication / treatment",
                    children = emptyList(),
                  ),
                ),
              ),
              Cas2v2PersistedApplicationStatusDetail(
                id = UUID.fromString("fc83c980-9c69-497e-9ad5-390377b10ee1"),
                name = "healthNeedsEpilepsy",
                label = "Epilepsy",
                children = listOf(
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("6513e192-327d-465e-a14e-59bf91d8e05f"),
                    name = "healthNeedsEpilepsyFrequency",
                    label = "Frequency / recency of seizures",
                    children = emptyList(),
                  ),
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("de14a9bf-fa1f-4535-95b1-22c63b50f626"),
                    name = "healthNeedsEpilepsyWhen",
                    label = "When seizures typically occur (day, night, both)",
                    children = emptyList(),
                  ),
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("6550e638-0b4c-452f-b1a9-570ac94041d5"),
                    name = "healthNeedsEpilepsyMedication",
                    label = "Medication / treatment",
                    children = emptyList(),
                  ),
                ),
              ),
              Cas2v2PersistedApplicationStatusDetail(
                id = UUID.fromString("2ec7f391-2846-491e-a29e-cf05a770ed86"),
                name = "healthNeedsOther",
                label = "Other",
                children = emptyList(),
              ),
            ),
          ),
          Cas2v2PersistedApplicationStatusDetail(
            id = UUID.fromString("7ba5cd7d-8ae3-4fe5-bb27-9367197ea160"),
            name = "riskToSelf",
            label = "Risk to self",
            children = listOf(
              Cas2v2PersistedApplicationStatusDetail(
                id = UUID.fromString("cccc6cfa-37d9-48e6-b044-1008470f8122"),
                name = "riskToSelfSubstanceAbuse",
                label = "Substance or alcohol abuse",
                children = listOf(
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("a1ddd2cf-b849-40e1-ab2c-11d4b590b649"),
                    name = "riskToSelfSubstanceAbuseSubstances",
                    label = "Substances misused and methods",
                    children = emptyList(),
                  ),
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("041f2806-5209-4973-9dd0-4e1cf5d938c0"),
                    name = "riskToSelfSubstanceAbuseAmount",
                    label = "Amount of consumption",
                    children = emptyList(),
                  ),
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("31f3e452-f93a-4b9b-a61a-f29a8a305f0b"),
                    name = "riskToSelfSubstanceAbuseMedication",
                    label = "Medication / treatment",
                    children = emptyList(),
                  ),
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("bf68e880-a7eb-43d5-b880-9f4f188b6fd3"),
                    name = "riskToSelfSubstanceAbuseEngagement",
                    label = "Engagement with support whilst in custody",
                    children = emptyList(),
                  ),
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("247240d3-6e45-45a9-b717-bf785f68dec5"),
                    name = "riskToSelfSubstanceAbuseSupport",
                    label = "Referral for community support",
                    children = emptyList(),
                  ),
                ),
              ),
              Cas2v2PersistedApplicationStatusDetail(
                id = UUID.fromString("3eddedf5-3ec8-4d53-899e-48817ddde975"),
                name = "riskToSelfMentalHealth",
                label = "Mental health",
                children = listOf(
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("1eedc9bb-7383-45d5-bd30-4cba7712d136"),
                    name = "riskToSelfMentalHealthSeverity",
                    label = "Need, severity, and effect on everyday life",
                    children = emptyList(),
                  ),
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("e1f884e7-ff57-457c-a263-33df2c9614a4"),
                    name = "riskToSelfMentalHealthDiagnosis",
                    label = "Status of clinical diagnosis",
                    children = emptyList(),
                  ),
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("b236e12e-4a92-48a2-b56a-419b9bd87821"),
                    name = "riskToSelfMentalHealthIndependence",
                    label = "Ability to live independently / in shared accommodation",
                    children = emptyList(),
                  ),
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("54b9dfc4-350a-4a6f-aaf1-e2c1b3c1d997"),
                    name = "riskToSelfMentalHealthMedication",
                    label = "Medication / treatment",
                    children = emptyList(),
                  ),
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("1798d91e-8c92-4fbc-bff4-6ee615396a40"),
                    name = "riskToSelfMentalHealthEngagement",
                    label = "Engagement with support whilst in custody",
                    children = emptyList(),
                  ),
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("6a3cec4b-11fb-47bf-941d-1c00150f6473"),
                    name = "riskToSelfMentalHealthReferral",
                    label = "Referral for community support",
                    children = emptyList(),
                  ),
                ),
              ),
              Cas2v2PersistedApplicationStatusDetail(
                id = UUID.fromString("c9740f4e-c9d2-4a51-98f7-ef0adbceac33"),
                name = "riskToSelfSelfHarmSuicide",
                label = "Self-harm and suicide",
                children = listOf(
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("c92cea38-dc76-47e8-bcf7-f87988073f23"),
                    name = "riskToSelfSelfHarmSuicideIncidents",
                    label = "Details of incidents (treatment required, methods)",
                    children = emptyList(),
                  ),
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("e317f507-1f86-49e9-8bbc-22e3a8895f0a"),
                    name = "riskToSelfSelfHarmSuicideConcerns",
                    label = "Current concerns",
                    children = emptyList(),
                  ),
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("e33ccef8-0a55-4641-b036-9ca13bbbdeb9"),
                    name = "riskToSelfSelfHarmSuicideACCT",
                    label = "Details of ACCT history (when opened/closed, reasons)",
                    children = emptyList(),
                  ),
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("26484c36-2077-4385-86c0-0e2d847b7c17"),
                    name = "riskToSelfSelfHarmSuicideReferral",
                    label = "Referral for community support",
                    children = emptyList(),
                  ),
                ),
              ),
              Cas2v2PersistedApplicationStatusDetail(
                id = UUID.fromString("be232a6e-a4b0-4830-b5c3-2f8d813f938b"),
                name = "riskToSelfExploitation",
                label = "Exploitation (risk to self)",
                children = listOf(
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("1def2f0b-5611-4834-9b95-ac6bbcae81c5"),
                    name = "riskToSelfExploitationKnownPerpetrator",
                    label = "Known perpetrator(s)",
                    children = emptyList(),
                  ),
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("7dd3d13e-aa22-4d1d-8050-5fea588f1672"),
                    name = "riskToSelfExploitationNature",
                    label = "Nature of exploitation",
                    children = emptyList(),
                  ),
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("4cfb7116-5778-4c8f-a07a-23de032f75de"),
                    name = "riskToSelfExploitationSafeguarding",
                    label = "Safeguarding measures / restraining orders",
                    children = emptyList(),
                  ),
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("2f01d950-f99a-494c-924b-f41e63aa71a4"),
                    name = "riskToSelfExploitationAreas",
                    label = "Areas to avoid",
                    children = emptyList(),
                  ),
                ),
              ),
              Cas2v2PersistedApplicationStatusDetail(
                id = UUID.fromString("84af7e7a-39a4-4295-a07d-4a2ab96d2127"),
                name = "riskToSelfDomesticViolence",
                label = "Domestic violence (victim)",
                children = listOf(
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("f7124945-25d2-4c9c-aee9-2fa92d0a6562"),
                    name = "riskToSelfDomesticViolenceKnown",
                    label = "Known perpetrator(s)",
                    children = emptyList(),
                  ),
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("079fb9e0-5268-4323-9771-315b9ba2683c"),
                    name = "riskToSelfDomesticViolenceIncidents",
                    label = "Details of incidents",
                    children = emptyList(),
                  ),
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("30331141-9583-462c-a364-5df2f2fc36a3"),
                    name = "riskToSelfDomesticViolenceSafeguarding",
                    label = "Safeguarding measures / restraining orders",
                    children = emptyList(),
                  ),
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("46f9e51f-3fd3-407c-bbc9-86121394e0a4"),
                    name = "riskToSelfDomesticViolenceAreas",
                    label = "Areas to avoid",
                    children = emptyList(),
                  ),
                ),
              ),
              Cas2v2PersistedApplicationStatusDetail(
                id = UUID.fromString("13ae59d2-e3d1-48cf-90cb-aa36eccd1de0"),
                name = "riskToSelfSexWork",
                label = "Sex work",
                children = listOf(
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("aa7d07b9-5cde-4362-9a16-d4525e787d4c"),
                    name = "riskToSelfSexWorkIncidents",
                    label = "Details of incidents",
                    children = emptyList(),
                  ),
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("06260768-7769-4ec2-b5c4-7e67bd391746"),
                    name = "riskToSelfSexWorkCommunityRisk",
                    label = "Risk in community",
                    children = emptyList(),
                  ),
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("f11447ad-8dbc-4850-80c5-bdcd166513e3"),
                    name = "riskToSelfSexWorkSafeguarding",
                    label = "Safeguarding measures / restraining orders",
                    children = emptyList(),
                  ),
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("7634a6e0-2f53-482a-b606-200cede0e68c"),
                    name = "riskToSelfSexWorkAreas",
                    label = "Areas to avoid",
                    children = emptyList(),
                  ),
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("4729cae5-e832-4976-9a92-e8548abf393c"),
                    name = "riskToSelfSexWorkSupport",
                    label = "Support they have / will require",
                    children = emptyList(),
                  ),
                ),
              ),
              Cas2v2PersistedApplicationStatusDetail(
                id = UUID.fromString("936bc158-e0f6-497c-88f9-5d8d09447489"),
                name = "riskToSelfMediaInterest",
                label = "Media interest",
                children = listOf(
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("cf0ff1ac-a3e1-4db9-ab1d-b6deac7603aa"),
                    name = "riskToSelfMediaInterestConcerns",
                    label = "Current concerns",
                    children = emptyList(),
                  ),
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("625ccbb9-4008-4822-a99c-1e5e209d2dfc"),
                    name = "riskToSelfMediaInterestAreas",
                    label = "Areas to avoid",
                    children = emptyList(),
                  ),
                ),
              ),
              Cas2v2PersistedApplicationStatusDetail(
                id = UUID.fromString("7036ee50-fae5-4c24-926c-c3195e5daf1d"),
                name = "riskToSelfOther",
                label = "Other",
                children = emptyList(),
              ),
            ),
          ),
          Cas2v2PersistedApplicationStatusDetail(
            id = UUID.fromString("8641d719-7356-42d3-8363-e323bf76caec"),
            name = "riskOfSeriousHarm",
            label = "Risk of serious harm",
            children = listOf(
              Cas2v2PersistedApplicationStatusDetail(
                id = UUID.fromString("48c9e0d2-9eb6-45e0-88aa-035bb83e9639"),
                name = "riskOfSeriousHarmExploitation",
                label = "Exploitation (risk to others)",
                children = listOf(
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("cc6a8694-fb00-4521-834d-939779d75237"),
                    name = "riskOfSeriousHarmSpecific",
                    label = "Specific risk to others",
                    children = listOf(
                      Cas2v2PersistedApplicationStatusDetail(
                        id = UUID.fromString("4ef2dc7c-38a2-409f-b187-826266aef239"),
                        name = "riskOfSeriousHarmSpecificKnown",
                        label = "Known victim(s)",
                        children = emptyList(),
                      ),
                      Cas2v2PersistedApplicationStatusDetail(
                        id = UUID.fromString("3f857028-c970-4fb0-a248-2a24708dbaaf"),
                        name = "riskOfSeriousHarmSpecificPublic",
                        label = "Public",
                        children = emptyList(),
                      ),
                      Cas2v2PersistedApplicationStatusDetail(
                        id = UUID.fromString("88129c59-8d2f-407b-b282-c34006f2de8c"),
                        name = "riskOfSeriousHarmSpecificResidents",
                        label = "Shared accommodation residents",
                        children = emptyList(),
                      ),
                    ),
                  ),
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("a8aa4e34-ad33-4805-8c0a-f34ae16f5868"),
                    name = "riskOfSeriousHarmNature",
                    label = "Nature of exploitation",
                    children = emptyList(),
                  ),
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("d2419410-27e9-4255-b591-2fecff7ca943"),
                    name = "riskOfSeriousHarmSafeguarding",
                    label = "Safeguarding measures / Restraining orders",
                    children = emptyList(),
                  ),
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("e2026f76-438c-4378-8926-247aedd2473d"),
                    name = "riskOfSeriousHarmNatureAreas",
                    label = "Areas to avoid",
                    children = emptyList(),
                  ),
                ),
              ),
              Cas2v2PersistedApplicationStatusDetail(
                id = UUID.fromString("10c52a7f-5509-4ec5-9f6e-cd1f898f1688"),
                name = "riskOfSeriousHarmDomesticViolence",
                label = "Domestic violence (perpetrator)",
                children = listOf(
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("6913f7db-c1a9-4a6e-ae6a-c55c5661fa2a"),
                    name = "riskOfSeriousHarmDomesticViolenceSpecific",
                    label = "Specific risk to others",
                    children = listOf(
                      Cas2v2PersistedApplicationStatusDetail(
                        id = UUID.fromString("60e79479-b093-46f1-95f3-b7f02d6b48f4"),
                        name = "riskOfSeriousHarmDomesticViolenceSpecificKnown",
                        label = "'Known victim(s)",
                        children = emptyList(),
                      ),
                      Cas2v2PersistedApplicationStatusDetail(
                        id = UUID.fromString("208db2c6-4bd1-41ba-8b41-e33653b7537c"),
                        name = "riskOfSeriousHarmDomesticViolenceSpecificPartners",
                        label = "Ex- or future partners",
                        children = emptyList(),
                      ),
                      Cas2v2PersistedApplicationStatusDetail(
                        id = UUID.fromString("5d9cadcd-0424-4ebc-92aa-7e02c5774f27"),
                        name = "riskOfSeriousHarmDomesticViolenceSpecificFamily",
                        label = "Family members",
                        children = emptyList(),
                      ),
                    ),
                  ),
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("98174b0d-9e6c-41c0-baa4-d7d8ef93b1b9"),
                    name = "riskOfSeriousHarmDomesticViolenceIncidents",
                    label = "Details of incidents",
                    children = emptyList(),
                  ),
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("6ed12193-142b-42f1-9b34-5807d353e014"),
                    name = "riskOfSeriousHarmDomesticViolenceSafeguarding",
                    label = "Safeguarding measures / Restraining orders",
                    children = emptyList(),
                  ),
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("42f08bfa-695d-4da5-8a48-979db114ab12"),
                    name = "riskOfSeriousHarmDomesticViolenceAreas",
                    label = "Areas to avoid",
                    children = emptyList(),
                  ),
                ),
              ),
              Cas2v2PersistedApplicationStatusDetail(
                id = UUID.fromString("f43c5a63-4b94-4b31-b163-0b8c162aceec"),
                name = "riskOfSeriousHarmGangInvolvement",
                label = "Gang involvement",
                children = listOf(
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("8b9ffeea-9fd0-4dd2-958a-51f6d3bc7b08"),
                    name = "riskOfSeriousHarmGangInvolvementSpecific",
                    label = "Specific risk to others",
                    children = listOf(
                      Cas2v2PersistedApplicationStatusDetail(
                        id = UUID.fromString("84b97da9-8ce9-4277-8e28-a2d22d4ca7db"),
                        name = "riskOfSeriousHarmGangInvolvementSpecificStaff",
                        label = "Staff",
                        children = emptyList(),
                      ),
                      Cas2v2PersistedApplicationStatusDetail(
                        id = UUID.fromString("a6d6e362-daf6-4589-ac8a-e43b2e3ceec4"),
                        name = "riskOfSeriousHarmGangInvolvementSpecificPublic",
                        label = "Public",
                        children = emptyList(),
                      ),
                      Cas2v2PersistedApplicationStatusDetail(
                        id = UUID.fromString("08931f00-071a-4353-8b52-caeebb99b0c4"),
                        name = "riskOfSeriousHarmGangInvolvementSpecificResidents",
                        label = "Shared accommodation residents",
                        children = emptyList(),
                      ),
                    ),
                  ),
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("693751e1-086f-41cd-afe7-410623bc5d7d"),
                    name = "riskOfSeriousHarmGangInvolvementDetails",
                    label = "Gang details: name, areas and/or rivals",
                    children = emptyList(),
                  ),
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("4c94a7b1-18d7-4f82-91b6-12cdffdcf7c2"),
                    name = "riskOfSeriousHarmGangInvolvementSafeguarding",
                    label = "Safeguarding measures / Restraining orders",
                    children = emptyList(),
                  ),
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("2e31fae3-c109-4543-b729-08cd1c96e572"),
                    name = "riskOfSeriousHarmGangInvolvementAreas",
                    label = "Areas to avoid",
                    children = emptyList(),
                  ),
                ),
              ),
              Cas2v2PersistedApplicationStatusDetail(
                id = UUID.fromString("ba98426a-8d00-422c-9136-db541c5860ec"),
                name = "riskOfSeriousHarmViolent",
                label = "Violent offences",
                children = listOf(
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("100b5514-aad8-4842-b5bb-5579417df253"),
                    name = "riskOfSeriousHarmViolentSpecific",
                    label = "Specific risk to others",
                    children = listOf(
                      Cas2v2PersistedApplicationStatusDetail(
                        id = UUID.fromString("9c817df5-f881-4dc0-a3ba-2208eabcf973"),
                        name = "riskOfSeriousHarmViolentSpecificKnown",
                        label = "Known victim(s)",
                        children = emptyList(),
                      ),
                      Cas2v2PersistedApplicationStatusDetail(
                        id = UUID.fromString("e0cbc641-2af4-4182-b374-fec7acc0a7a3"),
                        name = "riskOfSeriousHarmViolentSpecificPublic",
                        label = "Public",
                        children = emptyList(),
                      ),
                      Cas2v2PersistedApplicationStatusDetail(
                        id = UUID.fromString("17a3277f-003e-47f2-9014-0ed7e3710e65"),
                        name = "riskOfSeriousHarmViolentSpecificResidents",
                        label = "Shared accommodation residents",
                        children = emptyList(),
                      ),
                      Cas2v2PersistedApplicationStatusDetail(
                        id = UUID.fromString("1771e5db-66c8-4337-9b79-bbcedd201a37"),
                        name = "riskOfSeriousHarmViolentSpecificStaff",
                        label = "Staff",
                        children = emptyList(),
                      ),
                    ),
                  ),
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("8aca5100-6eb1-4540-a8be-e1f22fbd7d4f"),
                    name = "riskOfSeriousHarmViolentDetails",
                    label = "Details of incidents",
                    children = emptyList(),
                  ),
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("f80f8a23-c5a7-427a-ab29-9063fced3dff"),
                    name = "riskOfSeriousHarmViolentSafeguarding",
                    label = "Safeguarding measures / Restraining orders",
                    children = emptyList(),
                  ),
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("2d19dc9f-6e76-4da4-9e9f-c25d5f807d70"),
                    name = "riskOfSeriousHarmViolentVictims",
                    label = "Name, relationship or location of victims",
                    children = emptyList(),
                  ),
                ),
              ),
              Cas2v2PersistedApplicationStatusDetail(
                id = UUID.fromString("3fa3f1f1-44be-475a-8e2d-587f6628a07d"),
                name = "riskOfSeriousHarmDrugSupply",
                label = "Drug supply offences",
                children = listOf(
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("64973ee0-5a6b-47bf-b63c-c60fed2db8fe"),
                    name = "riskOfSeriousHarmDrugSupplySpecific",
                    label = "Specific risk to others",
                    children = listOf(
                      Cas2v2PersistedApplicationStatusDetail(
                        id = UUID.fromString("aaefb9d2-0c9d-464c-9dcf-a33ecdff2071"),
                        name = "riskOfSeriousHarmDrugSupplySpecificApplicant",
                        label = "Applicant",
                        children = emptyList(),
                      ),
                      Cas2v2PersistedApplicationStatusDetail(
                        id = UUID.fromString("0f6ce40b-02bc-4558-937d-58776328c060"),
                        name = "riskOfSeriousHarmDrugSupplySpecificPublic",
                        label = "Public",
                        children = emptyList(),
                      ),
                      Cas2v2PersistedApplicationStatusDetail(
                        id = UUID.fromString("abe5165a-f6cd-476a-a799-15d5b2aa7393"),
                        name = "riskOfSeriousHarmDrugSupplySpecificUsers",
                        label = "Drug users",
                        children = emptyList(),
                      ),
                      Cas2v2PersistedApplicationStatusDetail(
                        id = UUID.fromString("10393c93-c9a4-407a-8282-45f2b1be2216"),
                        name = "riskOfSeriousHarmDrugSupplySpecificResidents",
                        label = "Shared accommodation residents",
                        children = emptyList(),
                      ),
                    ),
                  ),
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("f838258a-e872-40f0-b041-b5a02cd7df8c"),
                    name = "riskOfSeriousHarmDrugSupplyIncidents",
                    label = "Details of incidents",
                    children = emptyList(),
                  ),
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("e9812d43-a918-491c-a1b9-41f2740eb0ae"),
                    name = "riskOfSeriousHarmDrugSupplyCountyLines",
                    label = "County lines involvement",
                    children = emptyList(),
                  ),
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("e858de68-977a-4fa3-9f6c-7bb60af8b076"),
                    name = "riskOfSeriousHarmDrugSupplySafeguarding",
                    label = "Safeguarding measures / Restraining orders",
                    children = emptyList(),
                  ),
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("741337cd-139d-4114-b98c-1f7a45e221f3"),
                    name = "riskOfSeriousHarmDrugSupplyAreas",
                    label = "Areas to avoid",
                    children = emptyList(),
                  ),
                ),
              ),
              Cas2v2PersistedApplicationStatusDetail(
                id = UUID.fromString("d36c6487-8b3a-48d3-9603-c33495fa79d8"),
                name = "riskOfSeriousHarmHateRelated",
                label = "Hate-related attitudes",
                children = listOf(
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("728a25f9-5ee2-4db4-8107-4990112818af"),
                    name = "riskOfSeriousHarmHateRelatedSpecific",
                    label = "Specific risk to others",
                    children = listOf(
                      Cas2v2PersistedApplicationStatusDetail(
                        id = UUID.fromString("6475dd76-6c4e-4b53-97f3-c2cedd6bdf0a"),
                        name = "riskOfSeriousHarmHateRelatedSpecificPublic",
                        label = "Public",
                        children = emptyList(),
                      ),
                      Cas2v2PersistedApplicationStatusDetail(
                        id = UUID.fromString("b227964c-d325-40d8-9812-8918200e4eb2"),
                        name = "riskOfSeriousHarmHateRelatedSpecificKnown",
                        label = "Known victim(s)",
                        children = emptyList(),
                      ),
                      Cas2v2PersistedApplicationStatusDetail(
                        id = UUID.fromString("65063077-2842-425c-84ce-5a08e3fcbcec"),
                        name = "riskOfSeriousHarmHateRelatedSpecificStaff",
                        label = "Staff",
                        children = emptyList(),
                      ),
                      Cas2v2PersistedApplicationStatusDetail(
                        id = UUID.fromString("2a3f52a3-84ff-4a0c-9146-45c5175bdb2a"),
                        name = "riskOfSeriousHarmHateRelatedSpecificResidents",
                        label = "Residents",
                        children = emptyList(),
                      ),
                      Cas2v2PersistedApplicationStatusDetail(
                        id = UUID.fromString("ee6fb44a-1c42-4abd-b605-c6b863884741"),
                        name = "riskOfSeriousHarmHateRelatedSpecificMinorities",
                        label = "Ethnic minorities",
                        children = emptyList(),
                      ),
                      Cas2v2PersistedApplicationStatusDetail(
                        id = UUID.fromString("8925830c-626c-446e-9333-d8ffbaa59fc3"),
                        name = "riskOfSeriousHarmHateRelatedSpecificReligious",
                        label = "Religious groups",
                        children = emptyList(),
                      ),
                      Cas2v2PersistedApplicationStatusDetail(
                        id = UUID.fromString("5397bbf5-1acf-4873-9f2e-1f044d7bfc5a"),
                        name = "riskOfSeriousHarmHateRelatedSpecificLgbtqPlus",
                        label = "LGBTQ+ community",
                        children = emptyList(),
                      ),
                      Cas2v2PersistedApplicationStatusDetail(
                        id = UUID.fromString("f5a24d8c-5ff3-4e71-a12f-77a341fbad4b"),
                        name = "riskOfSeriousHarmHateRelatedSpecificWomen",
                        label = "Women",
                        children = emptyList(),
                      ),
                    ),
                  ),
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("b5223744-dd97-4d45-bae4-7207b31ef6bf"),
                    name = "riskOfSeriousHarmHateRelatedIncidents",
                    label = "Details of incidents",
                    children = emptyList(),
                  ),
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("2395a752-0117-4dfe-b30a-2e1a558b8474"),
                    name = "riskOfSeriousHarmHateRelatedCellSharingAssessment",
                    label = "Cell-sharing risk assessment (CSRA)",
                    children = emptyList(),
                  ),
                ),
              ),
              Cas2v2PersistedApplicationStatusDetail(
                id = UUID.fromString("fa77c5e0-e0de-4187-8909-087f37ad9dd1"),
                name = "riskOfSeriousHarmAcquisitiveOffending",
                label = "Acquisitive offending",
                children = listOf(
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("64ec2514-61fe-4011-806f-2b123e41f205"),
                    name = "riskOfSeriousHarmAcquisitiveOffendingSpecific",
                    label = "Specific risk to others",
                    children = listOf(
                      Cas2v2PersistedApplicationStatusDetail(
                        id = UUID.fromString("2bf8cf08-32d8-49d8-aa24-9d7633224d41"),
                        name = "riskOfSeriousHarmAcquisitiveOffendingSpecificPublic",
                        label = "Public",
                        children = emptyList(),
                      ),
                      Cas2v2PersistedApplicationStatusDetail(
                        id = UUID.fromString("c1430f74-f13c-4c2f-abe5-ad2693c7cc36"),
                        name = "riskOfSeriousHarmAcquisitiveOffendingSpecificHomeowners",
                        label = "Homeowners",
                        children = emptyList(),
                      ),
                      Cas2v2PersistedApplicationStatusDetail(
                        id = UUID.fromString("5fc8645d-05aa-4918-b344-b1892fe7b09a"),
                        name = "riskOfSeriousHarmAcquisitiveOffendingSpecificStaff",
                        label = "Shop owners / staff",
                        children = emptyList(),
                      ),
                    ),
                  ),
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("8265b44d-de2d-42f0-b421-9a3d62c4f62b"),
                    name = "riskOfSeriousHarmAcquisitiveOffendingIncidents",
                    label = "Details of incidents",
                    children = emptyList(),
                  ),
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("4704e859-3223-4071-b26e-0a2d4aba4061"),
                    name = "riskOfSeriousHarmAcquisitiveOffendingSafeguarding",
                    label = "Safeguarding measures / Restraining orders",
                    children = emptyList(),
                  ),
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("eb18a4c9-0cb6-41d4-9adc-34320f45bc16"),
                    name = "riskOfSeriousHarmAcquisitiveOffendingAreas",
                    label = "Areas to avoid",
                    children = emptyList(),
                  ),
                ),
              ),
              Cas2v2PersistedApplicationStatusDetail(
                id = UUID.fromString("d7994d11-a8b0-45ff-a4ca-f2c2ba9946df"),
                name = "riskOfSeriousHarmCuckooing",
                label = "Cuckooing",
                children = listOf(
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("6cf2086a-ee15-48c3-b3d3-d1c30d2673e1"),
                    name = "riskOfSeriousHarmCuckooingSpecific",
                    label = "Specific risk to others",
                    children = listOf(
                      Cas2v2PersistedApplicationStatusDetail(
                        id = UUID.fromString("7d44fd72-9dd0-4b05-9515-b03029224f9b"),
                        name = "riskOfSeriousHarmCuckooingSpecificKnown",
                        label = "Known victim(s)",
                        children = emptyList(),
                      ),
                      Cas2v2PersistedApplicationStatusDetail(
                        id = UUID.fromString("5e391696-4af6-43e1-aeeb-f999cb5d27ce"),
                        name = "riskOfSeriousHarmCuckooingSpecificPublic",
                        label = "Public",
                        children = emptyList(),
                      ),
                      Cas2v2PersistedApplicationStatusDetail(
                        id = UUID.fromString("287b638d-d852-43eb-92bc-92d66ff392c1"),
                        name = "riskOfSeriousHarmCuckooingSpecificResidents",
                        label = "Shared accommodation residents",
                        children = emptyList(),
                      ),
                      Cas2v2PersistedApplicationStatusDetail(
                        id = UUID.fromString("81e2ece2-3402-4558-abea-bd4e3db59379"),
                        name = "riskOfSeriousHarmCuckooingSpecificDrugUsers",
                        label = "Drug users",
                        children = emptyList(),
                      ),
                    ),
                  ),
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("3af82085-8bb1-428f-a2c8-8062d419a519"),
                    name = "riskOfSeriousHarmCuckooingIncidents",
                    label = "Details of incidents",
                    children = emptyList(),
                  ),
                ),
              ),
              Cas2v2PersistedApplicationStatusDetail(
                id = UUID.fromString("d677afa8-5a5a-4b0f-9705-b45da7ef2fe8"),
                name = "riskOfSeriousHarmFinancialAbuseFraud",
                label = "Financial abuse or fraud",
                children = listOf(
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("00da44b7-bde4-4077-bc2e-1473e67f9a54"),
                    name = "riskOfSeriousHarmFinancialAbuseFraudSpecific",
                    label = "Specific risk to others",
                    children = listOf(
                      Cas2v2PersistedApplicationStatusDetail(
                        id = UUID.fromString("7ec0cb45-3953-483c-87f5-2772d2c170e1"),
                        name = "riskOfSeriousHarmFinancialAbuseFraudSpecificKnown",
                        label = "Known victim(s)",
                        children = emptyList(),
                      ),
                      Cas2v2PersistedApplicationStatusDetail(
                        id = UUID.fromString("1b778941-9115-491a-a9ee-c311bfc18dd1"),
                        name = "riskOfSeriousHarmFinancialAbuseFraudSpecificVulnerable",
                        label = "Vulnerable adults",
                        children = emptyList(),
                      ),
                      Cas2v2PersistedApplicationStatusDetail(
                        id = UUID.fromString("7c1a7f9a-7be9-40d5-a7d7-edde7c0016b1"),
                        name = "riskOfSeriousHarmFinancialAbuseFraudSpecificResidents",
                        label = "Shared accommodation residents",
                        children = emptyList(),
                      ),
                      Cas2v2PersistedApplicationStatusDetail(
                        id = UUID.fromString("14683b52-16a6-4f9f-8906-a482ecae67e3"),
                        name = "riskOfSeriousHarmFinancialAbuseFraudSpecificPartners",
                        label = "Ex- or future partners",
                        children = emptyList(),
                      ),
                      Cas2v2PersistedApplicationStatusDetail(
                        id = UUID.fromString("b7bb74d8-b0c0-4849-a4a1-c523b005c895"),
                        name = "riskOfSeriousHarmFinancialAbuseFraudSpecificFamily",
                        label = "Family members",
                        children = emptyList(),
                      ),
                    ),
                  ),
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("8ed6a16c-9aaf-4c42-a9e3-0c6660977a62"),
                    name = "riskOfSeriousHarmFinancialAbuseFraudIncidents",
                    label = "Details of incidents",
                    children = emptyList(),
                  ),
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("f4f6697e-dfe4-4dea-bb1c-bd727c36a962"),
                    name = "riskOfSeriousHarmFinancialAbuseFraudSafeguarding",
                    label = "Safeguarding measures / Restraining orders",
                    children = emptyList(),
                  ),
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("fe0f98e9-1693-4fb5-bfe6-563e9e7a5353"),
                    name = "riskOfSeriousHarmFinancialAbuseFraudAreas",
                    label = "Areas to avoid",
                    children = emptyList(),
                  ),
                ),
              ),
              Cas2v2PersistedApplicationStatusDetail(
                id = UUID.fromString("de6bc5de-0454-4e56-a4cd-d21250216dcd"),
                name = "riskOfSeriousHarmArson",
                label = "Arson",
                children = listOf(
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("93a0776b-418c-4f89-9024-7664adf080e8"),
                    name = "riskOfSeriousHarmArsonSpecific",
                    label = "Specific risk to others",
                    children = listOf(
                      Cas2v2PersistedApplicationStatusDetail(
                        id = UUID.fromString("4f0f5fbe-21de-4b15-9b6f-1c3bfec46849"),
                        name = "riskOfSeriousHarmArsonSpecificKnown",
                        label = "Known victim(s)",
                        children = emptyList(),
                      ),
                      Cas2v2PersistedApplicationStatusDetail(
                        id = UUID.fromString("719e1754-6823-4571-a818-ca5462ff24e2"),
                        name = "riskOfSeriousHarmArsonSpecificPublic",
                        label = "Public",
                        children = emptyList(),
                      ),
                      Cas2v2PersistedApplicationStatusDetail(
                        id = UUID.fromString("5f2b9402-535d-47e8-8ab4-8793f84c851a"),
                        name = "riskOfSeriousHarmArsonSpecificResidents",
                        label = "Shared accommodation residents",
                        children = emptyList(),
                      ),
                      Cas2v2PersistedApplicationStatusDetail(
                        id = UUID.fromString("2c94801b-ff56-4793-b4a9-9a06670fa203"),
                        name = "riskOfSeriousHarmArsonSpecificStaff",
                        label = "Staff",
                        children = emptyList(),
                      ),
                      Cas2v2PersistedApplicationStatusDetail(
                        id = UUID.fromString("f654bf8d-0b2a-427d-be11-f8766c3fa922"),
                        name = "riskOfSeriousHarmArsonSpecificPartners",
                        label = "Ex- or future partners",
                        children = emptyList(),
                      ),
                      Cas2v2PersistedApplicationStatusDetail(
                        id = UUID.fromString("fd9f74fd-838f-40d0-a159-0f1e2e3c4ca7"),
                        name = "riskOfSeriousHarmArsonSpecificFamily",
                        label = "Family members",
                        children = emptyList(),
                      ),
                    ),
                  ),
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("42302e61-96a3-4dfc-a492-d41f05f6a5bc"),
                    name = "riskOfSeriousHarmArsonIncidents",
                    label = "Details of incidents",
                    children = emptyList(),
                  ),
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("6237f7a3-c052-4ec3-90dc-f07916bed044"),
                    name = "riskOfSeriousHarmArsonSafeguarding",
                    label = "Safeguarding measures / Restraining orders",
                    children = emptyList(),
                  ),
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("99cf228a-e7f5-4b63-93b4-5a910ddb5b7c"),
                    name = "riskOfSeriousHarmArsonAreas",
                    label = "Areas to avoid",
                    children = emptyList(),
                  ),
                ),
              ),
              Cas2v2PersistedApplicationStatusDetail(
                id = UUID.fromString("0392737b-d4e5-4e23-932d-d7084e2a77f7"),
                name = "riskOfSeriousHarmWeapons",
                label = "Weapons",
                children = listOf(
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("fb5660b0-3463-4afa-9591-5a531f39e145"),
                    name = "riskOfSeriousHarmWeaponsSpecific",
                    label = "Specific risk to others",
                    children = listOf(
                      Cas2v2PersistedApplicationStatusDetail(
                        id = UUID.fromString("23c2b536-4f4d-4257-adbe-beac7bf05eb5"),
                        name = "riskOfSeriousHarmWeaponsSpecificPublic",
                        label = "Public",
                        children = emptyList(),
                      ),
                      Cas2v2PersistedApplicationStatusDetail(
                        id = UUID.fromString("360719dd-a6a7-401f-8ad1-7cab27cb861e"),
                        name = "riskOfSeriousHarmWeaponsSpecificResidents",
                        label = "Shared accommodation residents",
                        children = emptyList(),
                      ),
                      Cas2v2PersistedApplicationStatusDetail(
                        id = UUID.fromString("30bce08f-df96-4b28-97cc-46487fda1a77"),
                        name = "riskOfSeriousHarmWeaponsSpecificStaff",
                        label = "Staff",
                        children = emptyList(),
                      ),
                    ),
                  ),
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("3cecd1b9-f231-4f8e-8022-89f3cebaa509"),
                    name = "riskOfSeriousHarmWeaponsIncidents",
                    label = "Details of incidents",
                    children = emptyList(),
                  ),
                ),
              ),
              Cas2v2PersistedApplicationStatusDetail(
                id = UUID.fromString("22f7a806-5f66-4f0f-b7af-670aef58b2a3"),
                name = "riskOfSeriousHarmRiskToStaff",
                label = "Risk to staff",
                children = listOf(
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("407e87eb-f947-426d-9744-0c1739ed3a2e"),
                    name = "riskOfSeriousHarmRiskToStaffSpecific",
                    label = "Specific risk to others",
                    children = listOf(
                      Cas2v2PersistedApplicationStatusDetail(
                        id = UUID.fromString("b6b364bb-365b-4475-8832-cf9449cf016c"),
                        name = "riskOfSeriousHarmRiskToStaffSpecificStaff",
                        label = "Staff",
                        children = emptyList(),
                      ),
                      Cas2v2PersistedApplicationStatusDetail(
                        id = UUID.fromString("8321e90c-8691-4e1c-837d-ddeaa0579154"),
                        name = "riskOfSeriousHarmRiskToStaffSpecificProbationWorker",
                        label = "Probation worker",
                        children = emptyList(),
                      ),
                      Cas2v2PersistedApplicationStatusDetail(
                        id = UUID.fromString("2106b0f7-ab57-49bb-b4ae-010cf493f51d"),
                        name = "riskOfSeriousHarmRiskToStaffSpecificOther",
                        label = "Other",
                        children = emptyList(),
                      ),
                    ),
                  ),
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("120383e6-2fae-4e12-be45-226726f98d25"),
                    name = "riskOfSeriousHarmRiskToStaffIncidents",
                    label = "Details of incidents",
                    children = emptyList(),
                  ),
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("a37abd93-f4a0-4ea3-9802-8cb415363f07"),
                    name = "riskOfSeriousHarmRiskToStaffInSupported",
                    label = "If incidents occurred in supported accommodation",
                    children = emptyList(),
                  ),
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("c24b93ea-19d2-410e-9dd1-9fa28bb3d261"),
                    name = "riskOfSeriousHarmRiskToStaffBailConditions",
                    label = "License / Bail conditions",
                    children = emptyList(),
                  ),
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("9f7c0e9c-ae76-4592-a79d-49c51181b937"),
                    name = "riskOfSeriousHarmRiskToStaffSafeguarding",
                    label = "Safeguarding measures / restraining orders",
                    children = emptyList(),
                  ),
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("7e21de90-4b23-4eb3-82d9-36545d6fa538"),
                    name = "riskOfSeriousHarmRiskToStaffAreas",
                    label = "Areas to avoid",
                    children = emptyList(),
                  ),
                ),
              ),
              Cas2v2PersistedApplicationStatusDetail(
                id = UUID.fromString("a69c5c6a-7aaf-44a5-9f03-14f54283429b"),
                name = "riskOfSeriousHarmRiskToProperty",
                label = "Risk to property / criminal damage",
                children = listOf(
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("09299055-6482-4cd8-81a6-fc584873d4fc"),
                    name = "riskOfSeriousHarmRiskToPropertySpecific",
                    label = "Specific risk to others",
                    children = listOf(
                      Cas2v2PersistedApplicationStatusDetail(
                        id = UUID.fromString("0ec0f51a-5d23-4618-ae97-34c1c82c8660"),
                        name = "riskOfSeriousHarmRiskToPropertySpecificStaff",
                        label = "Staff",
                        children = emptyList(),
                      ),
                      Cas2v2PersistedApplicationStatusDetail(
                        id = UUID.fromString("81bf41bd-c0f5-43ee-81b6-2931dde0f9ba"),
                        name = "riskOfSeriousHarmRiskToPropertySpecificPublic",
                        label = "Public",
                        children = emptyList(),
                      ),
                      Cas2v2PersistedApplicationStatusDetail(
                        id = UUID.fromString("dd56eee1-0e24-419b-a469-3373fa874344"),
                        name = "riskOfSeriousHarmRiskToPropertySpecificResidents",
                        label = "Shared accommodation residents",
                        children = emptyList(),
                      ),
                    ),
                  ),
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("6ea638d3-de3b-4fc9-adae-230cef35db9c"),
                    name = "riskOfSeriousHarmRiskToPropertyIncidents",
                    label = "Details of incidents",
                    children = emptyList(),
                  ),
                  Cas2v2PersistedApplicationStatusDetail(
                    id = UUID.fromString("60aaa9b6-92b2-4492-8ce9-7ad856a8b46e"),
                    name = "riskOfSeriousHarmRiskToPropertyInSupported",
                    label = "If incidents occurred in supported accomodation",
                    children = emptyList(),
                  ),
                ),
              ),
              Cas2v2PersistedApplicationStatusDetail(
                id = UUID.fromString("6c14e0d6-1cd0-4ba2-8a72-782b7c7dfcd3"),
                name = "riskOfSeriousHarmOther",
                label = "Other",
                children = emptyList(),
              ),

            ),
          ),
          Cas2v2PersistedApplicationStatusDetail(
            id = UUID.fromString("90f075ae-0b9f-445b-a9b5-1095abca87dc"),
            name = "currentOffences",
            label = "Current offences",
          ),
          Cas2v2PersistedApplicationStatusDetail(
            id = UUID.fromString("8ce77ea1-324e-4ac8-be8c-33d6d4d927f8"),
            name = "offendingHistory",
            label = "Offending history",
          ),
          Cas2v2PersistedApplicationStatusDetail(
            id = UUID.fromString("94631a70-6c51-43d6-9112-2b6d042b5aa0"),
            name = "other",
            label = "Other",
          ),
        ),
      ),
      Cas2v2PersistedApplicationStatus(
        id = UUID.fromString("ba4d8432-250b-4ab9-81ec-7eb4b16e5dd1"),
        name = "awaitingDecision",
        label = "Awaiting decision",
        description = "The CAS-2 team has the information they need and will make a decision.",
      ),
      Cas2v2PersistedApplicationStatus(
        id = UUID.fromString("a919097d-b324-471c-9834-756f255e87ea"),
        name = "onWaitingList",
        label = "On waiting list",
        description = "The applicant has been added to the waiting list for Short-Term Accommodation (CAS-2).",
      ),
      Cas2v2PersistedApplicationStatus(
        id = UUID.fromString("176bbda0-0766-4d77-8d56-18ed8f9a4ef2"),
        name = "placeOffered",
        label = "Place offered",
        description = "The applicant has been offered a place for Short-Term Accommodation (CAS-2).",
      ),
      Cas2v2PersistedApplicationStatus(
        id = UUID.fromString("fe254d88-ce1d-4cd8-8bd6-88de88f39019"),
        name = "offerAccepted",
        label = "Offer accepted",
        description = "The accommodation offered has been accepted.",
      ),
      Cas2v2PersistedApplicationStatus(
        id = UUID.fromString("758eee61-2a6d-46b9-8bdd-869536d77f1b"),
        name = "noPlaceOffered",
        label = "Could not be placed",
        description = "The applicant could not be placed in Short-Term Accommodation (CAS-2).",
        isActive = false,
      ),
      Cas2v2PersistedApplicationStatus(
        id = UUID.fromString("4ad9bbfa-e5b0-456f-b746-146f7fd511dd"),
        name = "incomplete",
        label = "Incomplete",
        description = "The application could not progress because the prison offender manager (POM) did not provide the requested information.",
        isActive = false,
      ),
      Cas2v2PersistedApplicationStatus(
        id = UUID.fromString("9a381bc6-22d3-41d6-804d-4e49f428c1de"),
        name = "offerDeclined",
        label = "Offer declined or withdrawn",
        description = "The accommodation offered has been declined or withdrawn.",
        statusDetails = listOf(
          Cas2v2PersistedApplicationStatusDetail(
            id = UUID.fromString("62645779-242d-4601-a8f8-d2cbf1d41dfa"),
            name = "areaUnsuitable",
            label = "Area unsuitable",
          ),
          Cas2v2PersistedApplicationStatusDetail(
            id = UUID.fromString("fc38f750-e9d2-4270-b542-d38286b9855c"),
            name = "changeOfCircumstances",
            label = "Change of circumstances",
          ),
          Cas2v2PersistedApplicationStatusDetail(
            id = UUID.fromString("31122b89-e087-4b5f-b59a-f7ffa0dd3e0c"),
            name = "noResponse",
            label = "No response",
          ),
          Cas2v2PersistedApplicationStatusDetail(
            id = UUID.fromString("32e62af3-6ea5-4496-a82c-b7bad67080a5"),
            name = "offerWithdrawnByNacro",
            label = "Offer withdrawn by Nacro",
          ),
          Cas2v2PersistedApplicationStatusDetail(
            id = UUID.fromString("b5bfbff4-aaa6-4fb0-ba36-5bca58927dc5"),
            name = "propertyUnsuitable",
            label = "Property unsuitable for needs",
          ),
          Cas2v2PersistedApplicationStatusDetail(
            id = UUID.fromString("f58267b8-f91b-4a4f-9aa2-80089ba111e4"),
            name = "withdrawnByReferrer",
            label = "Withdrawn by referrer",
          ),
          Cas2v2PersistedApplicationStatusDetail(
            id = UUID.fromString("ed5d529c-d7d1-4f29-a0c0-89fd104cc320"),
            name = "rehousedByAnotherLandlord",
            label = "Rehoused by another landlord",
          ),
        ),
      ),
      Cas2v2PersistedApplicationStatus(
        id = UUID.fromString("004e2419-9614-4c1e-a207-a8418009f23d"),
        name = "withdrawn",
        label = "Referral withdrawn",
        description = "The prison offender manager (POM) withdrew the application.",
        statusDetails = listOf(
          Cas2v2PersistedApplicationStatusDetail(
            id = UUID.fromString("c5dce0d2-fc05-4a07-8157-25b8821cdb06"),
            name = "governorDecidedUnsuitable",
            label = "Governor - decided unsuitable",
          ),
          Cas2v2PersistedApplicationStatusDetail(
            id = UUID.fromString("3fb37f85-be88-4eee-812d-af122e268eef"),
            name = "governorChosenAlternative",
            label = "Governor - chosen alternative accommodation",
          ),
          Cas2v2PersistedApplicationStatusDetail(
            id = UUID.fromString("8a857a7d-94f9-43ec-963c-2a2528e88a6e"),
            name = "governorOther",
            label = "Governor - other",
          ),
          Cas2v2PersistedApplicationStatusDetail(
            id = UUID.fromString("d3c789b8-947d-4e24-9cef-335545d85abe"),
            name = "withdrewOrDeclinedOffer",
            label = "Withdrew or declined offer",
          ),
          Cas2v2PersistedApplicationStatusDetail(
            id = UUID.fromString("6fc8d3b7-eb53-479d-8903-3880a9ed563f"),
            name = "personTransferredToAnotherPrisonWithdrawal",
            label = "Person transferred to another prison",
          ),
        ),
      ),
      Cas2v2PersistedApplicationStatus(
        id = UUID.fromString("f13bbdd6-44f1-4362-b9d3-e6f1298b1bf9"),
        name = "cancelled",
        label = "Referral cancelled",
        description = "The application has been cancelled.",
        statusDetails = listOf(
          Cas2v2PersistedApplicationStatusDetail(
            id = UUID.fromString("ba46bbe0-8fb6-4539-895d-5586e6bfe8b6"),
            name = "assessedAsHighRisk",
            label = "Assessed as high risk",
          ),
          Cas2v2PersistedApplicationStatusDetail(
            id = UUID.fromString("522bb736-aeb6-480f-a51a-2bf3dcfcd482"),
            name = "notEligible",
            label = "Not eligible",
          ),
          Cas2v2PersistedApplicationStatusDetail(
            id = UUID.fromString("ccf43af1-359b-4a14-8941-85eefa88f016"),
            name = "noRecourseToPublicFunds",
            label = "No recourse to public funds",
          ),
          Cas2v2PersistedApplicationStatusDetail(
            id = UUID.fromString("c149a14d-ba06-420a-b844-5edfc02da6b1"),
            name = "noPropertyAvailable",
            label = "No property available",
          ),
          Cas2v2PersistedApplicationStatusDetail(
            id = UUID.fromString("3fbdccc9-4858-4ae4-abb5-bd2b90d96d96"),
            name = "noFemalePropertyAvailable",
            label = "No female property available",
          ),
          Cas2v2PersistedApplicationStatusDetail(
            id = UUID.fromString("bc539d6d-c353-49fa-847f-6967a148c527"),
            name = "noAdaptedPropertyAvailable",
            label = "No adapted property available",
          ),
          Cas2v2PersistedApplicationStatusDetail(
            id = UUID.fromString("78636840-0155-45d4-971e-fe8d2d6c660c"),
            name = "noSuitablePropertyAvailable",
            label = "No suitable property available",
          ),
          Cas2v2PersistedApplicationStatusDetail(
            id = UUID.fromString("7e8749c9-5254-4dae-90ed-590cf9f59847"),
            name = "incompleteReferral",
            label = "Incomplete referral",
          ),
          Cas2v2PersistedApplicationStatusDetail(
            id = UUID.fromString("d1d96185-d92a-450b-b47f-bcce50356eed"),
            name = "createdInError",
            label = "Created in error",
          ),
          Cas2v2PersistedApplicationStatusDetail(
            id = UUID.fromString("4f1033ab-2dea-47ce-8a86-7c47b3ccadd8"),
            name = "personTransferredToAnotherPrison",
            label = "Person transferred to another prison",
          ),
        ),
      ),
      Cas2v2PersistedApplicationStatus(
        id = UUID.fromString("89458555-3219-44a2-9584-c4f715d6b565"),
        name = "awaitingArrival",
        label = "Awaiting arrival",
        description = "The accommodation is arranged for the agreed dates.",
      ),
    )
  }
}
