package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationTimeline
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1TimelineEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1TimelineEventAssociatedUrl
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1TimelineEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1TimelineEventUrlType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NamedId
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.hmppstier.Tier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.oasyscontext.RiskToTheIndividualInner
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.oasyscontext.RisksToTheIndividual
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi.CsraSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas1.Cas1PersonalTimeline
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseAccessFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.MappaDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RegistrationFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RoshRatingsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.InitialiseDatabasePerClassTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas1Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOfflineApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextAddListCaseSummaryToBulkResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextAddResponseToUserAccessCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextEmptyCaseSummaryToBulkResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextMockSuccessfulCaseDetailCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apOASysContextMockRiskToTheIndividual404Call
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apOASysContextMockSuccessfulRiskToTheIndividualCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apOASysContextMockSuccessfulRoshRatingsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.hmppsTierMockSuccessfulTierCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.prisonAPIMockNotFoundCsraSummariesCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.prisonAPIMockSuccessfulCsraSummariesCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.bodyAsObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.collections.listOf

class Cas1PeopleTest : InitialiseDatabasePerClassTestBase() {
  @Autowired
  lateinit var personTransformer: PersonTransformer

  @Nested
  inner class GetOasysRisksToIndividual {
    @Test
    fun `Getting OASys risks to individual without a JWT returns 401`() {
      webTestClient.get()
        .uri("/cas1/people/CRN/oasys/risks-to-individual")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Getting OASys risks to individual returns OK with correct body`() {
      givenAUser(roles = listOf(UserRole.CAS1_MANAGE_RESIDENT)) { _, jwt ->
        val crn = "CRN"
        val risksToTheIndividual = RisksToTheIndividual(
          assessmentId = 1,
          assessmentType = "TYPE",
          dateCompleted = OffsetDateTime.now(),
          assessorSignedDate = OffsetDateTime.now(),
          initiationDate = OffsetDateTime.now(),
          assessmentStatus = "STATUS",
          superStatus = "SUPER",
          limitedAccessOffender = false,
          riskToTheIndividual = RiskToTheIndividualInner(
            currentConcernsSelfHarmSuicide = "currentConcernsSelfHarmSuicide",
            previousConcernsSelfHarmSuicide = "previousConcernsSelfHarmSuicide",
            currentCustodyHostelCoping = "currentCustodyHostelCoping",
            previousCustodyHostelCoping = "previousCustodyHostelCoping",
            currentVulnerability = "currentVulnerability",
            previousVulnerability = "previousVulnerability",
            riskOfSeriousHarm = "riskOfSeriousHarm",
            currentConcernsBreachOfTrustText = "currentConcernsBreachOfTrustText",
            analysisSuicideSelfharm = "analysisSuicideSelfharm",
            analysisCoping = "analysisCoping",
            analysisVulnerabilities = "analysisVulnerabilities",
          ),
        )

        apOASysContextMockSuccessfulRiskToTheIndividualCall(crn, risksToTheIndividual)

        webTestClient.get()
          .uri("/cas1/people/$crn/oasys/risks-to-individual")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(risksToTheIndividual.riskToTheIndividual),
          )
      }
    }

    @Test
    fun `Getting OASys risks to individual returns 404 when OASys returns 404`() {
      givenAUser(roles = listOf(UserRole.CAS1_MANAGE_RESIDENT)) { _, jwt ->
        val crn = "CRN"

        apOASysContextMockRiskToTheIndividual404Call(crn)

        webTestClient.get()
          .uri("/cas1/people/$crn/oasys/risks-to-individual")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isNotFound
      }
    }

    @Test
    fun `Getting OASys risks to individual returns 404 when riskToTheIndividual is null`() {
      givenAUser(roles = listOf(UserRole.CAS1_MANAGE_RESIDENT)) { _, jwt ->
        val crn = "CRN"
        val risksToTheIndividual = RisksToTheIndividual(
          assessmentId = 1,
          assessmentType = "TYPE",
          dateCompleted = OffsetDateTime.now(),
          assessorSignedDate = OffsetDateTime.now(),
          initiationDate = OffsetDateTime.now(),
          assessmentStatus = "STATUS",
          superStatus = "SUPER",
          limitedAccessOffender = false,
          riskToTheIndividual = null,
        )

        apOASysContextMockSuccessfulRiskToTheIndividualCall(crn, risksToTheIndividual)

        webTestClient.get()
          .uri("/cas1/people/$crn/oasys/risks-to-individual")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isNotFound
      }
    }
  }

  @Nested
  inner class GetTimelineForCrn {

    @Test
    fun `Getting a personal timeline for a CRN without a JWT returns 401`() {
      webTestClient.get()
        .uri("/cas1/people/CRN/timeline")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Getting a personal timeline for a CRN that does not exist returns 404`() {
      givenAUser { _, jwt ->
        apDeliusContextEmptyCaseSummaryToBulkResponse("CRN1")

        webTestClient.get()
          .uri("/cas1/people/CRN1/timeline")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isNotFound
      }
    }

    @Test
    fun `Getting a personal timeline for a CRN returns OK with correct body`() {
      givenAUser { userEntity, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          givenAnApplication(
            userEntity,
            crn = offenderDetails.otherIds.crn,
            submittedAt = null,
          )
          givenAnApplication(
            userEntity,
            crn = offenderDetails.otherIds.crn,
            submittedAt = OffsetDateTime.now(),
          ) { submittedApplication ->
            val domainEvents = domainEventFactory.produceAndPersistMultiple(2) {
              withCrn(offenderDetails.otherIds.crn)
              withApplicationId(submittedApplication.id)
            }

            val personInfoResult = PersonInfoResult.Success.Full(
              crn = offenderDetails.otherIds.crn,
              offenderDetailSummary = offenderDetails,
              inmateDetail = inmateDetails,
            )

            val apArea = userEntity.apArea!!

            webTestClient.get()
              .uri("/cas1/people/${offenderDetails.otherIds.crn}/timeline")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectBody()
              .json(
                objectMapper.writeValueAsString(
                  Cas1PersonalTimeline(
                    person = personTransformer.transformModelToPersonApi(personInfoResult),
                    applications = listOf(
                      Cas1ApplicationTimeline(
                        id = submittedApplication.id,
                        createdAt = submittedApplication.createdAt.toInstant(),
                        status = Cas1ApplicationStatus.started,
                        isOfflineApplication = false,
                        createdBy = ApprovedPremisesUser(
                          qualifications = emptyList(),
                          roles = emptyList(),
                          permissions = emptyList(),
                          apArea = ApArea(
                            id = apArea.id,
                            identifier = apArea.identifier,
                            name = apArea.name,
                          ),
                          service = "CAS1",
                          id = userEntity.id,
                          name = userEntity.name,
                          deliusUsername = userEntity.deliusUsername,
                          region = ProbationRegion(
                            id = userEntity.probationRegion.id,
                            name = userEntity.probationRegion.name,
                            hptEmail = userEntity.probationRegion.hptEmail,
                          ),
                          email = userEntity.email,
                          telephoneNumber = userEntity.telephoneNumber,
                          isActive = userEntity.isActive,
                          version = 993,
                          cruManagementArea = NamedId(
                            id = userEntity.cruManagementArea!!.id,
                            name = userEntity.cruManagementArea!!.name,
                          ),
                          cruManagementAreaDefault = NamedId(
                            id = apArea.defaultCruManagementArea.id,
                            name = apArea.defaultCruManagementArea.name,
                          ),
                          cruManagementAreaOverride = null,
                        ),
                        timelineEvents = listOf(
                          Cas1TimelineEvent(
                            type = Cas1TimelineEventType.applicationSubmitted,
                            id = domainEvents[0].id.toString(),
                            occurredAt = domainEvents[0].occurredAt.toInstant(),
                            content = "The application was submitted",
                            associatedUrls = listOf(
                              Cas1TimelineEventAssociatedUrl(
                                type = Cas1TimelineEventUrlType.application,
                                url = "http://frontend/applications/${submittedApplication.id}",
                              ),
                            ),
                          ),
                          Cas1TimelineEvent(
                            type = Cas1TimelineEventType.applicationSubmitted,
                            id = domainEvents[1].id.toString(),
                            occurredAt = domainEvents[1].occurredAt.toInstant(),
                            content = "The application was submitted",
                            associatedUrls = listOf(
                              Cas1TimelineEventAssociatedUrl(
                                type = Cas1TimelineEventUrlType.application,
                                url = "http://frontend/applications/${submittedApplication.id}",
                              ),
                            ),
                          ),
                        ),
                      ),
                    ),
                  ),
                ),
              )
          }
        }
      }
    }

    @Test
    fun `Applications are returned in created at order, descending`() {
      val (user, jwt) = givenAUser()
      val (offenderDetails, _) = givenAnOffender()

      val crn = offenderDetails.otherIds.crn

      val appA = givenACas1Application(
        createdByUser = user,
        crn = crn,
        createdAt = OffsetDateTime.now().minusDays(5),
        submittedAt = OffsetDateTime.now(),
      )

      val appB = givenACas1Application(
        createdByUser = user,
        crn = crn,
        createdAt = OffsetDateTime.now().minusDays(3),
        submittedAt = OffsetDateTime.now(),
      )

      val appC = givenACas1Application(
        createdByUser = user,
        crn = crn,
        createdAt = OffsetDateTime.now().minusDays(7),
        submittedAt = OffsetDateTime.now(),
      )

      val appD = givenAnOfflineApplication(
        crn = offenderDetails.otherIds.crn,
        createdAt = OffsetDateTime.now().minusDays(4),
      )

      val result = webTestClient.get()
        .uri("/cas1/people/${offenderDetails.otherIds.crn}/timeline")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<Cas1PersonalTimeline>()

      assertThat(result.applications.map { it.id }).containsExactly(
        appB.id,
        appD.id,
        appA.id,
        appC.id,
      )
    }

    @Test
    fun `Getting a personal timeline for a CRN is limited to 50 entries`() {
      val (user, jwt) = givenAUser()
      val (offenderDetails, _) = givenAnOffender()

      repeat(60) {
        givenAnApplication(
          user,
          crn = offenderDetails.otherIds.crn,
          submittedAt = OffsetDateTime.now(),
        )
      }

      val result = webTestClient.get()
        .uri("/cas1/people/${offenderDetails.otherIds.crn}/timeline")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsObject<Cas1PersonalTimeline>()

      assertThat(result.applications).hasSize(50)
    }

    @Test
    fun `Getting a personal timeline for a CRN with no applications returns OK with correct body`() {
      givenAUser { _, jwt ->
        givenAnOffender { offenderDetails, inmateDetails ->
          val personInfoResult = PersonInfoResult.Success.Full(
            crn = offenderDetails.otherIds.crn,
            offenderDetailSummary = offenderDetails,
            inmateDetail = inmateDetails,
          )

          webTestClient.get()
            .uri("/cas1/people/${offenderDetails.otherIds.crn}/timeline")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .json(
              objectMapper.writeValueAsString(
                Cas1PersonalTimeline(
                  person = personTransformer.transformModelToPersonApi(personInfoResult),
                  applications = emptyList(),
                ),
              ),
            )
        }
      }
    }

    @Test
    fun `Getting a personal timeline for a CRN without a NomsNumber returns OK with correct body`() {
      givenAUser { userEntity, jwt ->
        givenAnOffender(
          offenderDetailsConfigBlock = {
            withNomsNumber(null)
          },
        ) { offenderDetails, _ ->
          givenAnApplication(
            userEntity,
            crn = offenderDetails.otherIds.crn,
            submittedAt = OffsetDateTime.now(),
          ) { application ->
            val domainEvents = domainEventFactory.produceAndPersistMultiple(2) {
              withCrn(offenderDetails.otherIds.crn)
              withApplicationId(application.id)
            }

            val personInfoResult = PersonInfoResult.Success.Full(
              crn = offenderDetails.otherIds.crn,
              offenderDetailSummary = offenderDetails,
              inmateDetail = null,
            )

            val apArea = userEntity.apArea!!

            webTestClient.get()
              .uri("/cas1/people/${offenderDetails.otherIds.crn}/timeline")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .expectBody()
              .json(
                objectMapper.writeValueAsString(
                  Cas1PersonalTimeline(
                    person = personTransformer.transformModelToPersonApi(personInfoResult),
                    applications = listOf(
                      Cas1ApplicationTimeline(
                        id = application.id,
                        createdAt = application.createdAt.toInstant(),
                        status = Cas1ApplicationStatus.started,
                        isOfflineApplication = false,
                        createdBy = ApprovedPremisesUser(
                          qualifications = emptyList(),
                          roles = emptyList(),
                          permissions = emptyList(),
                          apArea = ApArea(
                            id = apArea.id,
                            identifier = apArea.identifier,
                            name = apArea.name,
                          ),
                          service = "CAS1",
                          id = userEntity.id,
                          name = userEntity.name,
                          deliusUsername = userEntity.deliusUsername,
                          region = ProbationRegion(
                            id = userEntity.probationRegion.id,
                            name = userEntity.probationRegion.name,
                            hptEmail = userEntity.probationRegion.hptEmail,
                          ),
                          email = userEntity.email,
                          telephoneNumber = userEntity.telephoneNumber,
                          isActive = userEntity.isActive,
                          version = 993,
                          cruManagementArea = NamedId(
                            id = userEntity.cruManagementArea!!.id,
                            name = userEntity.cruManagementArea!!.name,
                          ),
                          cruManagementAreaDefault = NamedId(
                            id = apArea.defaultCruManagementArea.id,
                            name = apArea.defaultCruManagementArea.name,
                          ),
                          cruManagementAreaOverride = null,
                        ),
                        timelineEvents = listOf(
                          Cas1TimelineEvent(
                            type = Cas1TimelineEventType.applicationSubmitted,
                            id = domainEvents[0].id.toString(),
                            occurredAt = domainEvents[0].occurredAt.toInstant(),
                            content = "The application was submitted",
                            associatedUrls = listOf(
                              Cas1TimelineEventAssociatedUrl(
                                type = Cas1TimelineEventUrlType.application,
                                url = "http://frontend/applications/${application.id}",
                              ),
                            ),
                          ),
                          Cas1TimelineEvent(
                            type = Cas1TimelineEventType.applicationSubmitted,
                            id = domainEvents[1].id.toString(),
                            occurredAt = domainEvents[1].occurredAt.toInstant(),
                            content = "The application was submitted",
                            associatedUrls = listOf(
                              Cas1TimelineEventAssociatedUrl(
                                type = Cas1TimelineEventUrlType.application,
                                url = "http://frontend/applications/${application.id}",
                              ),
                            ),
                          ),
                        ),
                      ),
                    ),
                  ),
                ),
              )
          }
        }
      }
    }

    @Test
    fun `Getting a personal timeline for a CRN where the Offender is LAO, and the user does have LAO qualification returns OK with correct body`() {
      givenAUser(qualifications = listOf(UserQualification.LAO)) { _, jwt ->
        givenAnOffender(
          offenderDetailsConfigBlock = { withCurrentRestriction(true) },
        ) { offenderDetails, inmateDetails ->
          val personInfoResult = PersonInfoResult.Success.Full(
            crn = offenderDetails.otherIds.crn,
            offenderDetailSummary = offenderDetails,
            inmateDetail = inmateDetails,
          )

          val offlineApplication = offlineApplicationEntityFactory.produceAndPersist {
            withCrn(offenderDetails.otherIds.crn)
          }

          val domainEvents = domainEventFactory.produceAndPersistMultiple(2) {
            withCrn(offenderDetails.otherIds.crn)
            withApplicationId(offlineApplication.id)
          }

          webTestClient.get()
            .uri("/cas1/people/${offenderDetails.otherIds.crn}/timeline")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .json(
              objectMapper.writeValueAsString(
                Cas1PersonalTimeline(
                  person = personTransformer.transformModelToPersonApi(personInfoResult),
                  applications = listOf(
                    Cas1ApplicationTimeline(
                      id = offlineApplication.id,
                      createdAt = offlineApplication.createdAt.toInstant(),
                      status = null,
                      isOfflineApplication = true,
                      createdBy = null,
                      timelineEvents = listOf(
                        Cas1TimelineEvent(
                          type = Cas1TimelineEventType.applicationSubmitted,
                          id = domainEvents[0].id.toString(),
                          occurredAt = domainEvents[0].occurredAt.toInstant(),
                          content = "The application was submitted",
                          associatedUrls = listOf(
                            Cas1TimelineEventAssociatedUrl(
                              type = Cas1TimelineEventUrlType.application,
                              url = "http://frontend/applications/${offlineApplication.id}",
                            ),
                          ),
                        ),
                        Cas1TimelineEvent(
                          type = Cas1TimelineEventType.applicationSubmitted,
                          id = domainEvents[1].id.toString(),
                          occurredAt = domainEvents[1].occurredAt.toInstant(),
                          content = "The application was submitted",
                          associatedUrls = listOf(
                            Cas1TimelineEventAssociatedUrl(
                              type = Cas1TimelineEventUrlType.application,
                              url = "http://frontend/applications/${offlineApplication.id}",
                            ),
                          ),
                        ),
                      ),
                    ),
                  ),
                ),
              ),
            )
        }
      }
    }

    @Test
    fun `Getting a personal timeline for a CRN where the Offender is LAO,but the user does not have LAO qualification returns OK with empty timeline`() {
      givenAUser { _, jwt ->
        givenAnOffender(
          offenderDetailsConfigBlock = { withCurrentRestriction(true) },
        ) { offenderDetails, inmateDetails ->
          val personInfoResult = PersonInfoResult.Success.Restricted(
            crn = offenderDetails.otherIds.crn,
            nomsNumber = offenderDetails.otherIds.nomsNumber!!,
          )

          webTestClient.get()
            .uri("/cas1/people/${offenderDetails.otherIds.crn}/timeline")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .json(
              objectMapper.writeValueAsString(
                Cas1PersonalTimeline(
                  person = personTransformer.transformModelToPersonApi(personInfoResult),
                  applications = emptyList(),
                ),
              ),
            )
        }
      }
    }
  }

  @Nested
  inner class GetPersonRiskProfileTest {

    @Test
    fun `Getting a person risk profile for a CRN without a JWT returns 401`() {
      webTestClient.get()
        .uri("/cas1/people/CRN/risk-profile")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Getting a person risk profile for a CRN returns OK with correct body`() {
      val caseDetail = CaseDetailFactory()
        .withMappaDetail(
          MappaDetailFactory()
            .withLevelDescription("Level1 MAPPA")
            .withCategoryDescription("CAT2")
            .produce(),
        )
        .withRegistrations(
          listOf(
            RegistrationFactory().withDescription("Arson").produce(),
            RegistrationFactory().withDescription("Weapon").produce(),

          ),
        )
        .produce()
      val roshRatings = RoshRatingsFactory().produce()
      val tier = Tier(tierScore = "A1", calculationId = UUID.randomUUID(), calculationDate = LocalDateTime.now())
      val (_, jwt) = givenAUser(roles = listOf(UserRole.CAS1_MANAGE_RESIDENT))
      givenAnOffender { offenderDetails, _ ->
        apDeliusContextMockSuccessfulCaseDetailCall(offenderDetails.otherIds.crn, caseDetail)
        apOASysContextMockSuccessfulRoshRatingsCall(offenderDetails.otherIds.crn, roshRatings)
        hmppsTierMockSuccessfulTierCall(
          offenderDetails.otherIds.crn,
          tier,
        )

        val response = webTestClient.get()
          .uri("/cas1/people/${offenderDetails.otherIds.crn}/risk-profile")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .returnResult(PersonRisks::class.java)

        val personRisks = response.responseBody.blockFirst()!!

        assertThat(personRisks).isNotNull
        assertThat(personRisks.tier.value!!.level).isEqualTo("A1")
        assertThat(personRisks.roshRisks.value!!.overallRisk).isEqualTo("Low")
        assertThat(personRisks.mappa.value!!.level).isEqualTo("CAT CAT2/LEVEL Level1 MAPPA")
        assertThat(personRisks.flags.value!![0]).isEqualTo("Arson")
        assertThat(personRisks.flags.value[1]).isEqualTo("Weapon")
      }
    }
  }

  @Nested
  inner class GetCsraSummaries {
    @Test
    fun `Getting CSRA summaries for a CRN without a JWT returns 401`() {
      webTestClient.get()
        .uri("/cas1/people/CRN/csra-summaries")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Getting CSRA summaries for a CRN without the required role returns 403`() {
      val (_, jwt) = givenAUser()
      webTestClient.get()
        .uri("/cas1/people/CRN/csra-summaries")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `Getting CSRA summaries for a CRN returns OK with correct body`() {
      val csraSummaries = listOf(
        CsraSummary(
          bookingId = 123,
          classificationCode = null,
          assessmentCode = "CSR",
          assessmentSeq = 1,
          assessmentDate = LocalDate.now(),
          assessmentAgencyId = null,
          assessmentComment = "comment",
          cellSharingAlertFlag = true,
        ),
      )

      val (user, jwt) = givenAUser(roles = listOf(UserRole.CAS1_MANAGE_RESIDENT))
      givenAnOffender { offenderDetails, _ ->
        prisonAPIMockSuccessfulCsraSummariesCall(offenderDetails.otherIds.nomsNumber!!, csraSummaries)

        webTestClient.get()
          .uri("/cas1/people/${offenderDetails.otherIds.crn}/csra-summaries")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(objectMapper.writeValueAsString(csraSummaries))
      }
    }

    @Test
    fun `Getting CSRA summaries for a CRN where the Offender is LAO, and the user does have LAO qualification returns OK with correct body`() {
      val csraSummaries = listOf(
        CsraSummary(
          bookingId = 123,
          classificationCode = "HI",
          assessmentCode = "STANDARD",
          assessmentSeq = 1,
          assessmentDate = LocalDate.now(),
          assessmentAgencyId = "MDI",
          assessmentComment = "comment",
          cellSharingAlertFlag = true,
        ),
      )

      val (user, jwt) = givenAUser(roles = listOf(UserRole.CAS1_MANAGE_RESIDENT), qualifications = listOf(UserQualification.LAO))
      val crn = "LAO_CRN"
      val nomsNumber = "LAO_NOMS"

      val caseSummary = CaseSummaryFactory()
        .withCrn(crn)
        .withNomsId(nomsNumber)
        .withCurrentRestriction(true)
        .produce()

      val caseAccess = CaseAccessFactory()
        .withCrn(crn)
        .withUserExcluded(false)
        .withUserRestricted(false)
        .produce()

      apDeliusContextAddListCaseSummaryToBulkResponse(listOf(caseSummary))
      apDeliusContextAddResponseToUserAccessCall(listOf(caseAccess), user.deliusUsername)
      prisonAPIMockSuccessfulCsraSummariesCall(nomsNumber, csraSummaries)

      webTestClient.get()
        .uri("/cas1/people/$crn/csra-summaries")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .json(objectMapper.writeValueAsString(csraSummaries))
    }

    @Test
    fun `Getting CSRA summaries for a CRN where the Offender is LAO, but the user does not have LAO qualification returns 403`() {
      val (user, jwt) = givenAUser(roles = listOf(UserRole.CAS1_MANAGE_RESIDENT))
      val crn = "LAO_CRN"
      val nomsNumber = "LAO_NOMS"

      val caseSummary = CaseSummaryFactory()
        .withCrn(crn)
        .withNomsId(nomsNumber)
        .withCurrentRestriction(true)
        .produce()

      val caseAccess = CaseAccessFactory()
        .withCrn(crn)
        .withUserExcluded(false)
        .withUserRestricted(true)
        .produce()

      apDeliusContextAddListCaseSummaryToBulkResponse(listOf(caseSummary))
      apDeliusContextAddResponseToUserAccessCall(listOf(caseAccess), user.deliusUsername)

      webTestClient.get()
        .uri("/cas1/people/$crn/csra-summaries")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `Getting CSRA summaries for a CRN that does not exist returns 404`() {
      val (user, jwt) = givenAUser(roles = listOf(UserRole.CAS1_MANAGE_RESIDENT))
      val crn = "NOT_FOUND_CRN"

      apDeliusContextEmptyCaseSummaryToBulkResponse(crn)

      webTestClient.get()
        .uri("/cas1/people/$crn/csra-summaries")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isNotFound
    }

    @Test
    fun `Getting CSRA summaries for a CRN where Prison API returns 404 returns 404`() {
      val (user, jwt) = givenAUser(roles = listOf(UserRole.CAS1_MANAGE_RESIDENT))
      givenAnOffender { offenderDetails, _ ->
        prisonAPIMockNotFoundCsraSummariesCall(offenderDetails.otherIds.nomsNumber!!)

        webTestClient.get()
          .uri("/cas1/people/${offenderDetails.otherIds.crn}/csra-summaries")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isNotFound
      }
    }
  }
}
