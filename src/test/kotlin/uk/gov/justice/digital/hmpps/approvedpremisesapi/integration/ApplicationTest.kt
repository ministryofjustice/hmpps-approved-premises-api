package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import com.ninjasquad.springmockk.SpykBean
import io.mockk.clearMocks
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.returnResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationTimelineNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationTimelinessCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationUserDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewWithdrawal
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReleaseTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SentenceTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateApplicationType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.ManagingTeamsResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.toHttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.Cas1NotifyTemplates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NeedsDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas1CruManagementArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextEmptyCaseSummaryToBulkResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextMockSuccessfulCaseDetailCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextMockSuccessfulTeamsManagingCaseCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apOASysContextMockSuccessfulNeedsDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationTeamCodeEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationTeamCodeRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AutoAllocationDay
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1CruManagementAreaRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.SnsEventPersonReference
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.UUID

class ApplicationTest : IntegrationTestBase() {

  @SpykBean
  lateinit var realApplicationTeamCodeRepository: ApplicationTeamCodeRepository

  @SpykBean
  lateinit var realApplicationRepository: ApplicationRepository

  @AfterEach
  fun afterEach() {
    // SpringMockK does not correctly clear mocks for @SpyKBeans that are also a @Repository, causing mocked behaviour
    // in one test to show up in another (see https://github.com/Ninja-Squad/springmockk/issues/85)
    // Manually clearing after each test seems to fix this.
    clearMocks(realApplicationTeamCodeRepository)
    clearMocks(realApplicationRepository)
  }

  @Nested
  inner class Cas1CreateApplication {

    @Test
    fun `Create new application without JWT returns 401`() {
      webTestClient.post()
        .uri("/applications")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Create new application returns 404 when a person cannot be found`() {
      givenAUser { _, jwt ->
        val crn = "X1234"

        apDeliusContextEmptyCaseSummaryToBulkResponse(crn)

        webTestClient.post()
          .uri("/applications")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            NewApplication(
              crn = crn,
            ),
          )
          .exchange()
          .expectStatus()
          .isNotFound
          .expectBody()
          .jsonPath("$.detail").isEqualTo("No Offender with an ID of $crn could be found")
      }
    }

    @Test
    fun `Create new application returns 500 and does not create Application without team codes when write to team code table fails`() {
      givenAUser { _, jwt ->
        givenAnOffender { offenderDetails, _ ->

          apDeliusContextMockSuccessfulTeamsManagingCaseCall(
            offenderDetails.otherIds.crn,
            ManagingTeamsResponse(
              teamCodes = listOf(offenderDetails.otherIds.crn),
            ),
          )

          every { realApplicationTeamCodeRepository.save(any()) } throws RuntimeException("Database Error")

          webTestClient.post()
            .uri("/applications")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              NewApplication(
                crn = offenderDetails.otherIds.crn,
                convictionId = 123,
                deliusEventNumber = "1",
                offenceId = "789",
              ),
            )
            .exchange()
            .expectStatus()
            .is5xxServerError

          assertThat(approvedPremisesApplicationRepository.findAll().none { it.crn == offenderDetails.otherIds.crn })
        }
      }
    }

    @Test
    fun `Create new application returns 201 with correct body and Location header`() {
      givenAUser { _, jwt ->
        givenAnOffender { offenderDetails, _ ->

          apDeliusContextMockSuccessfulTeamsManagingCaseCall(
            offenderDetails.otherIds.crn,
            ManagingTeamsResponse(
              teamCodes = listOf("TEAM1"),
            ),
          )

          apOASysContextMockSuccessfulNeedsDetailsCall(
            offenderDetails.otherIds.crn,
            NeedsDetailsFactory().produce(),
          )

          val result = webTestClient.post()
            .uri("/applications")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              NewApplication(
                crn = offenderDetails.otherIds.crn,
                convictionId = 123,
                deliusEventNumber = "1",
                offenceId = "789",
              ),
            )
            .exchange()
            .expectStatus()
            .isCreated
            .returnResult(ApprovedPremisesApplication::class.java)

          assertThat(result.responseHeaders["Location"]).anyMatch {
            it.matches(Regex("/applications/.+"))
          }

          assertThat(result.responseBody.blockFirst()).matches {
            it.person.crn == offenderDetails.otherIds.crn &&
              cas1OffenderRepository.findByCrn(it.person.crn) != null
          }
        }
      }
    }

    @Test
    fun `Create new application without risks returns 201 with correct body and Location header`() {
      givenAUser { _, jwt ->
        givenAnOffender { offenderDetails, _ ->

          apDeliusContextMockSuccessfulTeamsManagingCaseCall(
            offenderDetails.otherIds.crn,
            ManagingTeamsResponse(
              teamCodes = listOf("TEAM1"),
            ),
          )

          apOASysContextMockSuccessfulNeedsDetailsCall(
            offenderDetails.otherIds.crn,
            NeedsDetailsFactory().produce(),
          )

          val result = webTestClient.post()
            .uri("/applications?createWithRisks=false")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              NewApplication(
                crn = offenderDetails.otherIds.crn,
                convictionId = 123,
                deliusEventNumber = "1",
                offenceId = "789",
              ),
            )
            .exchange()
            .expectStatus()
            .isCreated
            .returnResult(ApprovedPremisesApplication::class.java)

          assertThat(result.responseHeaders["Location"]).anyMatch {
            it.matches(Regex("/applications/.+"))
          }

          assertThat(result.responseBody.blockFirst()).matches {
            it.person.crn == offenderDetails.otherIds.crn &&
              cas1OffenderRepository.findByCrn(it.person.crn) != null
          }
        }
      }
    }

    @Test
    fun `Create new application returns successfully when a person has no NOMS number`() {
      givenAUser { _, jwt ->
        givenAnOffender(
          offenderDetailsConfigBlock = { withoutNomsNumber() },
        ) { offenderDetails, _ ->
          apDeliusContextMockSuccessfulTeamsManagingCaseCall(
            offenderDetails.otherIds.crn,
            ManagingTeamsResponse(
              teamCodes = listOf(offenderDetails.otherIds.crn),
            ),
          )

          val result = webTestClient.post()
            .uri("/applications")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              NewApplication(
                crn = offenderDetails.otherIds.crn,
                convictionId = 123,
                deliusEventNumber = "1",
                offenceId = "789",
              ),
            )
            .exchange()
            .expectStatus()
            .isCreated
            .returnResult(ApprovedPremisesApplication::class.java)

          assertThat(result.responseHeaders["Location"]).anyMatch {
            it.matches(Regex("/applications/.+"))
          }

          assertThat(result.responseBody.blockFirst()).matches {
            it.person.crn == offenderDetails.otherIds.crn
            cas1OffenderRepository.findByCrn(it.person.crn) != null
          }
        }
      }
    }

    @Test
    fun `Create new application returns successfully when the person cannot be fetched from the prisons API`() {
      givenAUser { userEntity, jwt ->
        givenAnOffender(
          offenderDetailsConfigBlock = {
            withNomsNumber("ABC123")
          },
        ) { offenderDetails, _ ->
          apDeliusContextMockSuccessfulTeamsManagingCaseCall(
            offenderDetails.otherIds.crn,
            ManagingTeamsResponse(
              teamCodes = listOf(offenderDetails.otherIds.crn),
            ),
          )

          val result = webTestClient.post()
            .uri("/applications")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              NewApplication(
                crn = offenderDetails.otherIds.crn,
                convictionId = 123,
                deliusEventNumber = "1",
                offenceId = "789",
              ),
            )
            .exchange()
            .expectStatus()
            .isCreated
            .returnResult(ApprovedPremisesApplication::class.java)

          assertThat(result.responseHeaders["Location"]).anyMatch {
            it.matches(Regex("/applications/.+"))
          }

          assertThat(result.responseBody.blockFirst()).matches {
            it.person.crn == offenderDetails.otherIds.crn
            cas1OffenderRepository.findByCrn(it.person.crn) != null
          }
        }
      }
    }
  }

  @Nested
  inner class Cas1UpdateApplicationCas {
    @Test
    fun `Update existing AP application returns 200 with correct body`() {
      givenAUser { submittingUser, jwt ->
        givenAUser(
          roles = listOf(UserRole.CAS1_ASSESSOR),
          qualifications = listOf(UserQualification.PIPE),
        ) { _, _ ->
          givenAnOffender { offenderDetails, _ ->
            val applicationId = UUID.fromString("22ceda56-98b2-411d-91cc-ace0ab8be872")

            approvedPremisesApplicationEntityFactory.produceAndPersist {
              withCrn(offenderDetails.otherIds.crn)
              withId(applicationId)
              withCreatedByUser(submittingUser)
            }

            val resultBody = webTestClient.put()
              .uri("/applications/$applicationId")
              .header("Authorization", "Bearer $jwt")
              .bodyValue(
                UpdateApprovedPremisesApplication(
                  data = mapOf("thingId" to 123),
                  isWomensApplication = false,
                  apType = ApType.pipe,
                  type = UpdateApplicationType.CAS1,
                ),
              )
              .exchange()
              .expectStatus()
              .isOk
              .returnResult(String::class.java)
              .responseBody
              .blockFirst()

            val result = objectMapper.readValue(resultBody, Application::class.java)

            assertThat(result.person.crn).isEqualTo(offenderDetails.otherIds.crn)
          }
        }
      }
    }
  }

  @Nested
  inner class Cas1SubmitApplication {

    @Test
    fun `Submit standard application does not auto allocate the assessment, sends emails and raises domain events`() {
      val (submittingUser, jwt) = givenAUser(
        probationRegion = givenAProbationRegion(
          apArea = givenAnApArea(
            defaultCruManagementArea = givenACas1CruManagementArea(assessmentAutoAllocationUsername = "DEFAULT_LONDON_ASSESSOR"),
          ),
        ),
      )

      givenAUser(
        roles = listOf(UserRole.CAS1_ASSESSOR),
        staffDetail = StaffDetailFactory.staffDetail(deliusUsername = "DEFAULT_LONDON_ASSESSOR"),
      )

      val (offenderDetails, _) = givenAnOffender()

      val applicationId = approvedPremisesApplicationEntityFactory.produceAndPersist {
        withCrn(offenderDetails.otherIds.crn)
        withCreatedByUser(submittingUser)
      }.id

      apDeliusContextMockSuccessfulCaseDetailCall(
        offenderDetails.otherIds.crn,
        CaseDetailFactory().produce(),
      )

      govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

      webTestClient.post()
        .uri("/applications/$applicationId/submission")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          SubmitApprovedPremisesApplication(
            noticeType = Cas1ApplicationTimelinessCategory.standard,
            apType = ApType.normal,
            translatedDocument = {},
            isWomensApplication = false,
            targetLocation = "SW1A 1AA",
            releaseType = ReleaseTypeOption.licence,
            sentenceType = SentenceTypeOption.nonStatutory,
            type = "CAS1",
            applicantUserDetails = Cas1ApplicationUserDetails(
              "applicantName",
              "applicantEmail",
              "applicationTelephone",
            ),
            caseManagerIsNotApplicant = false,
            caseManagerUserDetails = Cas1ApplicationUserDetails(
              "cmName",
              "cmEmail",
              "cmTelephone",
            ),
            reasonForShortNotice = "reasonForShort",
            reasonForShortNoticeOther = "reasonForShortOther",
            licenseExpiryDate = LocalDate.of(2026, 12, 1),
            arrivalDate = LocalDate.of(2031, 5, 6),
            duration = 52,
          ),
        )
        .exchange()
        .expectStatus()
        .isOk

      val persistedApplication = approvedPremisesApplicationRepository.findByIdOrNull(applicationId)!!

      assertThat(persistedApplication.noticeType).isEqualTo(Cas1ApplicationTimelinessCategory.standard)
      assertThat(persistedApplication.isWomensApplication).isFalse()
      assertThat(persistedApplication.isPipeApplication).isFalse
      assertThat(persistedApplication.targetLocation).isEqualTo("SW1A 1AA")
      assertThat(persistedApplication.sentenceType).isEqualTo(SentenceTypeOption.nonStatutory.toString())
      assertThat(persistedApplication.apArea?.id).isEqualTo(submittingUser.apArea!!.id)
      assertThat(persistedApplication.licenceExpiryDate).isEqualTo(LocalDate.of(2026, 12, 1))

      val createdAssessment =
        approvedPremisesAssessmentRepository.findAll().first { it.application.id == applicationId }
      assertThat(createdAssessment.allocatedToUser).isNull()
      assertThat(createdAssessment.createdFromAppeal).isFalse()

      domainEventAsserter.assertDomainEventStoreCount(applicationId, 1)
      val persistedApplicationSubmittedEvent = domainEventAsserter.assertDomainEventOfTypeStored(
        applicationId,
        DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED,
      )

      assertThat(persistedApplicationSubmittedEvent.crn).isEqualTo(offenderDetails.otherIds.crn)

      val emittedMessage = domainEventAsserter.blockForEmittedDomainEvent(DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED)

      val emittedMessageDescription = "An application has been submitted for an Approved Premises placement"
      assertThat(emittedMessage.description).isEqualTo(emittedMessageDescription)
      assertThat(emittedMessage.detailUrl).matches("http://api/events/application-submitted/${persistedApplicationSubmittedEvent.id}")
      assertThat(emittedMessage.additionalInformation.applicationId).isEqualTo(applicationId)
      assertThat(emittedMessage.personReference.identifiers).containsExactlyInAnyOrder(
        SnsEventPersonReference("CRN", offenderDetails.otherIds.crn),
        SnsEventPersonReference("NOMS", offenderDetails.otherIds.nomsNumber!!),
      )

      emailAsserter.assertEmailsRequestedCount(1)
      emailAsserter.assertEmailRequested(
        toEmailAddress = submittingUser.email!!,
        templateId = Cas1NotifyTemplates.APPLICATION_SUBMITTED,
        replyToEmailId = persistedApplication.cruManagementArea!!.notifyReplyToEmailId,
      )
    }

    @Test
    fun `Submit womens application does not auto allocate the assessment, sends emails and raises domain events`() {
      val (submittingUser, jwt) = givenAUser(
        probationRegion = givenAProbationRegion(
          apArea = givenAnApArea(
            defaultCruManagementArea = givenACas1CruManagementArea(assessmentAutoAllocationUsername = "DEFAULT_LONDON_ASSESSOR"),
          ),
        ),
      )

      givenAUser(
        roles = listOf(UserRole.CAS1_ASSESSOR),
        staffDetail = StaffDetailFactory.staffDetail(deliusUsername = "DEFAULT_LONDON_ASSESSOR"),
      )

      val (offenderDetails, _) = givenAnOffender()

      val applicationId = approvedPremisesApplicationEntityFactory.produceAndPersist {
        withCrn(offenderDetails.otherIds.crn)
        withCreatedByUser(submittingUser)
      }.id

      apDeliusContextMockSuccessfulCaseDetailCall(
        offenderDetails.otherIds.crn,
        CaseDetailFactory().produce(),
      )

      govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

      webTestClient.post()
        .uri("/applications/$applicationId/submission")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          SubmitApprovedPremisesApplication(
            noticeType = Cas1ApplicationTimelinessCategory.standard,
            apType = ApType.normal,
            translatedDocument = {},
            isWomensApplication = true,
            targetLocation = "SW1A 1AA",
            releaseType = ReleaseTypeOption.licence,
            sentenceType = SentenceTypeOption.nonStatutory,
            type = "CAS1",
            applicantUserDetails = Cas1ApplicationUserDetails(
              "applicantName",
              "applicantEmail",
              "applicationTelephone",
            ),
            caseManagerIsNotApplicant = false,
            caseManagerUserDetails = Cas1ApplicationUserDetails(
              "cmName",
              "cmEmail",
              "cmTelephone",
            ),
            reasonForShortNotice = "reasonForShort",
            reasonForShortNoticeOther = "reasonForShortOther",
          ),
        )
        .exchange()
        .expectStatus()
        .isOk

      val persistedApplication = approvedPremisesApplicationRepository.findByIdOrNull(applicationId)!!

      assertThat(persistedApplication.noticeType).isEqualTo(Cas1ApplicationTimelinessCategory.standard)
      assertThat(persistedApplication.isWomensApplication).isTrue()
      assertThat(persistedApplication.isPipeApplication).isFalse
      assertThat(persistedApplication.targetLocation).isEqualTo("SW1A 1AA")
      assertThat(persistedApplication.sentenceType).isEqualTo(SentenceTypeOption.nonStatutory.toString())
      assertThat(persistedApplication.apArea?.id).isEqualTo(submittingUser.apArea!!.id)
      assertThat(persistedApplication.cruManagementArea!!.id).isEqualTo(Cas1CruManagementAreaRepository.WOMENS_ESTATE_ID)

      val createdAssessment =
        approvedPremisesAssessmentRepository.findAll().first { it.application.id == applicationId }
      assertThat(createdAssessment.allocatedToUser).isNull()
      assertThat(createdAssessment.createdFromAppeal).isFalse()

      domainEventAsserter.assertDomainEventStoreCount(applicationId, 1)
      val persistedApplicationSubmittedEvent = domainEventAsserter.assertDomainEventOfTypeStored(
        applicationId,
        DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED,
      )

      assertThat(persistedApplicationSubmittedEvent.crn).isEqualTo(offenderDetails.otherIds.crn)

      val emittedMessage = domainEventAsserter.blockForEmittedDomainEvent(DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED)

      val emittedMessageDescription = "An application has been submitted for an Approved Premises placement"
      assertThat(emittedMessage.description).isEqualTo(emittedMessageDescription)
      assertThat(emittedMessage.detailUrl).matches("http://api/events/application-submitted/${persistedApplicationSubmittedEvent.id}")
      assertThat(emittedMessage.additionalInformation.applicationId).isEqualTo(applicationId)
      assertThat(emittedMessage.personReference.identifiers).containsExactlyInAnyOrder(
        SnsEventPersonReference("CRN", offenderDetails.otherIds.crn),
        SnsEventPersonReference("NOMS", offenderDetails.otherIds.nomsNumber!!),
      )

      emailAsserter.assertEmailsRequestedCount(1)
      emailAsserter.assertEmailRequested(
        toEmailAddress = submittingUser.email!!,
        templateId = Cas1NotifyTemplates.APPLICATION_SUBMITTED,
        replyToEmailId = persistedApplication.cruManagementArea!!.notifyReplyToEmailId,
      )
    }

    @Test
    fun `Submit emergency application auto allocates the assessment, sends emails and raises domain events`() {
      // Thursday
      clock.setNow(LocalDateTime.parse("2025-03-27T10:15:30"))

      val (submittingUser, jwt) = givenAUser(
        probationRegion = givenAProbationRegion(
          apArea = givenAnApArea(
            defaultCruManagementArea = givenACas1CruManagementArea(
              assessmentAutoAllocations = mutableMapOf(
                AutoAllocationDay.THURSDAY to "DEFAULT_LONDON_ASSESSOR",
              ),
            ),
          ),
        ),
      )

      val (assessorUser, _) = givenAUser(
        roles = listOf(UserRole.CAS1_ASSESSOR),
        staffDetail = StaffDetailFactory.staffDetail(deliusUsername = "DEFAULT_LONDON_ASSESSOR"),
      )

      val (offenderDetails, _) = givenAnOffender()

      val applicationId = approvedPremisesApplicationEntityFactory.produceAndPersist {
        withCrn(offenderDetails.otherIds.crn)
        withCreatedByUser(submittingUser)
      }.id

      apDeliusContextMockSuccessfulCaseDetailCall(
        offenderDetails.otherIds.crn,
        CaseDetailFactory().produce(),
      )

      govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

      webTestClient.post()
        .uri("/applications/$applicationId/submission")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          SubmitApprovedPremisesApplication(
            noticeType = Cas1ApplicationTimelinessCategory.emergency,
            apType = ApType.esap,
            translatedDocument = {},
            isWomensApplication = false,
            targetLocation = "SW1A 1AA",
            releaseType = ReleaseTypeOption.licence,
            sentenceType = SentenceTypeOption.nonStatutory,
            type = "CAS1",
            applicantUserDetails = Cas1ApplicationUserDetails(
              "applicantName",
              "applicantEmail",
              "applicationTelephone",
            ),
            caseManagerIsNotApplicant = false,
            caseManagerUserDetails = Cas1ApplicationUserDetails(
              "cmName",
              "cmEmail",
              "cmTelephone",
            ),
            reasonForShortNotice = "reasonForShort",
            reasonForShortNoticeOther = "reasonForShortOther",
          ),
        )
        .exchange()
        .expectStatus()
        .isOk

      val persistedApplication = approvedPremisesApplicationRepository.findByIdOrNull(applicationId)!!

      assertThat(persistedApplication.noticeType).isEqualTo(Cas1ApplicationTimelinessCategory.emergency)

      val createdAssessment =
        approvedPremisesAssessmentRepository.findAll().first { it.application.id == applicationId }
      assertThat(createdAssessment.allocatedToUser!!.id).isEqualTo(assessorUser.id)
      assertThat(createdAssessment.createdFromAppeal).isFalse()

      domainEventAsserter.assertDomainEventStoreCount(applicationId, 2)
      val persistedApplicationSubmittedEvent = domainEventAsserter.assertDomainEventOfTypeStored(
        applicationId,
        DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED,
      )
      val persistedAssessmentAllocatedEvent = domainEventAsserter.assertDomainEventOfTypeStored(
        applicationId,
        DomainEventType.APPROVED_PREMISES_ASSESSMENT_ALLOCATED,
      )

      assertThat(persistedApplicationSubmittedEvent).isNotNull
      assertThat(persistedApplicationSubmittedEvent.crn).isEqualTo(offenderDetails.otherIds.crn)

      assertThat(persistedAssessmentAllocatedEvent).isNotNull
      assertThat(persistedAssessmentAllocatedEvent.crn).isEqualTo(offenderDetails.otherIds.crn)

      domainEventAsserter.blockForEmittedDomainEvent(DomainEventType.APPROVED_PREMISES_ASSESSMENT_ALLOCATED)

      emailAsserter.assertEmailsRequestedCount(2)
      emailAsserter.assertEmailRequested(
        toEmailAddress = createdAssessment.allocatedToUser!!.email!!,
        templateId = Cas1NotifyTemplates.ASSESSMENT_ALLOCATED,
        replyToEmailId = persistedApplication.cruManagementArea!!.notifyReplyToEmailId,
      )
      emailAsserter.assertEmailRequested(
        toEmailAddress = submittingUser.email!!,
        templateId = Cas1NotifyTemplates.APPLICATION_SUBMITTED,
        replyToEmailId = persistedApplication.cruManagementArea!!.notifyReplyToEmailId,
      )
    }

    @Test
    fun `Submit short notice application auto allocates the assessment according to overridden ap area, sends emails and raises domain events`() {
      // Sunday
      clock.setNow(LocalDateTime.parse("2025-03-30T10:15:30"))

      val (submittingUser, jwt) = givenAUser()

      val (assessorUser, _) = givenAUser(
        roles = listOf(UserRole.CAS1_ASSESSOR),
        staffDetail = StaffDetailFactory.staffDetail(deliusUsername = "DEFAULT_WALES_ASSESSOR"),
      )

      val (offenderDetails, _) = givenAnOffender()

      val applicationId = approvedPremisesApplicationEntityFactory.produceAndPersist {
        withCrn(offenderDetails.otherIds.crn)
        withCreatedByUser(submittingUser)
      }.id

      apDeliusContextMockSuccessfulCaseDetailCall(
        offenderDetails.otherIds.crn,
        CaseDetailFactory().produce(),
      )

      govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

      val overriddenApArea = givenAnApArea(
        name = "wales",
        defaultCruManagementArea = givenACas1CruManagementArea(
          assessmentAutoAllocations = mutableMapOf(
            AutoAllocationDay.SUNDAY to "DEFAULT_WALES_ASSESSOR",
          ),
        ),
      )

      webTestClient.post()
        .uri("/applications/$applicationId/submission")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          SubmitApprovedPremisesApplication(
            noticeType = Cas1ApplicationTimelinessCategory.shortNotice,
            apType = ApType.pipe,
            translatedDocument = {},
            isWomensApplication = false,
            targetLocation = "SW1A 1AA",
            releaseType = ReleaseTypeOption.licence,
            sentenceType = SentenceTypeOption.nonStatutory,
            type = "CAS1",
            apAreaId = overriddenApArea.id,
            applicantUserDetails = Cas1ApplicationUserDetails("applicantName", "applicantEmail", "applicationPhone"),
            caseManagerIsNotApplicant = false,
          ),
        )
        .exchange()
        .expectStatus()
        .isOk

      val persistedApplication = approvedPremisesApplicationRepository.findByIdOrNull(applicationId)!!

      assertThat(persistedApplication.noticeType).isEqualTo(Cas1ApplicationTimelinessCategory.shortNotice)
      assertThat(persistedApplication.apArea?.id).isEqualTo(overriddenApArea.id)
      assertThat(persistedApplication.cruManagementArea?.id).isEqualTo(overriddenApArea.defaultCruManagementArea.id)

      val createdAssessment =
        approvedPremisesAssessmentRepository.findAll().first { it.application.id == applicationId }
      assertThat(createdAssessment.allocatedToUser).isNotNull()
      assertThat(createdAssessment.allocatedToUser!!.id).isEqualTo(assessorUser.id)

      domainEventAsserter.assertDomainEventStoreCount(applicationId, 2)
      domainEventAsserter.assertDomainEventOfTypeStored(
        applicationId,
        DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED,
      )
      domainEventAsserter.assertDomainEventOfTypeStored(
        applicationId,
        DomainEventType.APPROVED_PREMISES_ASSESSMENT_ALLOCATED,
      )

      emailAsserter.assertEmailsRequestedCount(2)
      emailAsserter.assertEmailRequested(
        toEmailAddress = createdAssessment.allocatedToUser!!.email!!,
        templateId = Cas1NotifyTemplates.ASSESSMENT_ALLOCATED,
        replyToEmailId = overriddenApArea.defaultCruManagementArea.notifyReplyToEmailId,
      )
      emailAsserter.assertEmailRequested(
        toEmailAddress = submittingUser.email!!,
        templateId = Cas1NotifyTemplates.APPLICATION_SUBMITTED,
        replyToEmailId = persistedApplication.cruManagementArea!!.notifyReplyToEmailId,
      )
    }

    @Test
    fun `Submit esap application auto allocates the assessment according to esap assessor configuration, sends emails and raises domain events`() {
      val (submittingUser, jwt) = givenAUser()

      val (esapAssessorUser, _) = givenAUser(
        roles = listOf(UserRole.CAS1_ASSESSOR),
        staffDetail = StaffDetailFactory.staffDetail(deliusUsername = "ESAP_ASSESSOR"),
      )

      val (offenderDetails, _) = givenAnOffender()

      val applicationId = approvedPremisesApplicationEntityFactory.produceAndPersist {
        withCrn(offenderDetails.otherIds.crn)
        withCreatedByUser(submittingUser)
      }.id

      apDeliusContextMockSuccessfulCaseDetailCall(
        offenderDetails.otherIds.crn,
        CaseDetailFactory().produce(),
      )

      govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

      val overriddenApArea = givenAnApArea()

      webTestClient.post()
        .uri("/applications/$applicationId/submission")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          SubmitApprovedPremisesApplication(
            noticeType = Cas1ApplicationTimelinessCategory.standard,
            apType = ApType.esap,
            translatedDocument = {},
            isWomensApplication = false,
            targetLocation = "SW1A 1AA",
            releaseType = ReleaseTypeOption.licence,
            sentenceType = SentenceTypeOption.nonStatutory,
            type = "CAS1",
            apAreaId = overriddenApArea.id,
            applicantUserDetails = Cas1ApplicationUserDetails("applicantName", "applicantEmail", "applicationPhone"),
            caseManagerIsNotApplicant = false,
          ),
        )
        .exchange()
        .expectStatus()
        .isOk

      val persistedApplication = approvedPremisesApplicationRepository.findByIdOrNull(applicationId)!!

      assertThat(persistedApplication.noticeType).isEqualTo(Cas1ApplicationTimelinessCategory.standard)
      assertThat(persistedApplication.apArea?.id).isEqualTo(overriddenApArea.id)

      val createdAssessment =
        approvedPremisesAssessmentRepository.findAll().first { it.application.id == applicationId }
      assertThat(createdAssessment.allocatedToUser!!.id).isEqualTo(esapAssessorUser.id)

      domainEventAsserter.assertDomainEventStoreCount(applicationId, 2)
      domainEventAsserter.assertDomainEventOfTypeStored(
        applicationId,
        DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED,
      )
      domainEventAsserter.assertDomainEventOfTypeStored(
        applicationId,
        DomainEventType.APPROVED_PREMISES_ASSESSMENT_ALLOCATED,
      )

      emailAsserter.assertEmailsRequestedCount(2)
      emailAsserter.assertEmailRequested(
        toEmailAddress = createdAssessment.allocatedToUser!!.email!!,
        templateId = Cas1NotifyTemplates.ASSESSMENT_ALLOCATED,
        replyToEmailId = persistedApplication.cruManagementArea!!.notifyReplyToEmailId,
      )
      emailAsserter.assertEmailRequested(
        toEmailAddress = submittingUser.email!!,
        templateId = Cas1NotifyTemplates.APPLICATION_SUBMITTED,
        replyToEmailId = persistedApplication.cruManagementArea!!.notifyReplyToEmailId,
      )
    }

    @Test
    fun `When several concurrent submit application requests occur, only one is successful, all others return 400 without persisting domain events`() {
      givenAUser { submittingUser, jwt ->
        givenAUser(
          roles = listOf(UserRole.CAS1_ASSESSOR),
        ) { _, _ ->
          givenAnOffender { offenderDetails, _ ->

            val applicationId = approvedPremisesApplicationEntityFactory.produceAndPersist {
              withCrn(offenderDetails.otherIds.crn)
              withCreatedByUser(submittingUser)
              withData(
                """
              {
                 "isWomensApplication": true,
                 "isPipeApplication": true
              }
            """,
              )
            }.id

            every { realApplicationRepository.save(any()) } answers {
              Thread.sleep(1000)
              it.invocation.args[0] as ApplicationEntity
            }

            apDeliusContextMockSuccessfulCaseDetailCall(
              offenderDetails.otherIds.crn,
              CaseDetailFactory().produce(),
            )

            val responseStatuses = mutableListOf<HttpStatus>()

            govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

            (1..10).map {
              val thread = Thread {
                webTestClient.post()
                  .uri("/applications/$applicationId/submission")
                  .header("Authorization", "Bearer $jwt")
                  .bodyValue(
                    SubmitApprovedPremisesApplication(
                      translatedDocument = {},
                      apType = ApType.pipe,
                      isWomensApplication = true,
                      isEmergencyApplication = true,
                      targetLocation = "SW1A 1AA",
                      releaseType = ReleaseTypeOption.licence,
                      sentenceType = SentenceTypeOption.nonStatutory,
                      type = "CAS1",
                      applicantUserDetails = Cas1ApplicationUserDetails("applicantName", "applicantEmail", "applicationPhone"),
                      caseManagerIsNotApplicant = false,
                    ),
                  )
                  .exchange()
                  .returnResult<String>()
                  .consumeWith {
                    synchronized(responseStatuses) {
                      responseStatuses += it.status.toHttpStatus()
                    }
                  }
              }

              thread.start()

              thread
            }.forEach(Thread::join)

            val persistedDomainEvents = domainEventRepository.findAll().filter { it.applicationId == applicationId }

            val persistedApplicationSubmittedEvents = persistedDomainEvents.filter { it.type == DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED }

            assertThat(persistedApplicationSubmittedEvents).singleElement()
            assertThat(responseStatuses.count { it.value() == 200 }).isEqualTo(1)
            assertThat(responseStatuses.count { it.value() == 400 }).isEqualTo(9)
          }
        }
      }
    }
  }

  @Nested
  inner class PostTimelineNotesForApplication {
    @Test
    fun `post ApplicationTimelineNote without JWT returns 401`() {
      webTestClient.post()
        .uri("/applications/${UUID.randomUUID()}/notes")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `post ApplicationTimelineNote with JWT returns 200`() {
      givenAUser(roles = listOf(UserRole.CAS1_CRU_MEMBER)) { user, jwt ->
        val applicationId = UUID.randomUUID()

        approvedPremisesApplicationEntityFactory.produceAndPersist {
          withId(applicationId)
          withCreatedByUser(user)
        }
        val body = ApplicationTimelineNote(note = "some note")
        webTestClient.post()
          .uri("/applications/$applicationId/notes")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            body,
          )
          .exchange()
          .expectStatus()
          .isOk

        val savedNote =
          applicationTimelineNoteRepository.findApplicationTimelineNoteEntitiesByApplicationIdAndDeletedAtIsNull(applicationId)
        savedNote.map {
          assertThat(it.body).isEqualTo("some note")
          assertThat(it.applicationId).isEqualTo(applicationId)
          assertThat(it.createdBy!!.id).isEqualTo(user.id)
        }
      }
    }
  }

  /**
   * Note - Withdrawal cascading is tested in [uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1.Cas1WithdrawalTest]
   */
  @Nested
  inner class WithdrawApplication {
    @Test
    fun `Withdraw Application without JWT returns 401`() {
      webTestClient.post()
        .uri("/applications/${UUID.randomUUID()}/withdrawal")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Withdraw Application 200 withdraws application and sends an email to application creator`() {
      givenAUser { user, jwt ->
        val application = produceAndPersistBasicApplication(
          crn = "ABC123",
          userEntity = user,
          managingTeamCode = "TEAM",
          submittedAt = OffsetDateTime.now(),
        )

        val assessment = approvedPremisesAssessmentEntityFactory.produceAndPersist {
          withApplication(application)
          withAllocatedToUser(user)
          withSubmittedAt(OffsetDateTime.now())
        }

        webTestClient.post()
          .uri("/applications/${application.id}/withdrawal")
          .header("Authorization", "Bearer $jwt")
          .bodyValue(
            NewWithdrawal(
              reason = WithdrawalReason.duplicateApplication,
            ),
          )
          .exchange()
          .expectStatus()
          .isOk

        val updatedApplication = approvedPremisesApplicationRepository.findByIdOrNull(application.id)!!
        assertThat(updatedApplication.isWithdrawn).isTrue

        val updatedAssessment = approvedPremisesAssessmentRepository.findByIdOrNull(assessment.id)!!
        assertThat(updatedAssessment.isWithdrawn).isTrue

        emailAsserter.assertEmailsRequestedCount(1)
        emailAsserter.assertEmailRequested(
          user.email!!,
          Cas1NotifyTemplates.APPLICATION_WITHDRAWN_V2,
        )
      }
    }

    @Test
    fun `Withdraw Application 200 withdraws application and sends an email to assessor if assessment is pending`() {
      givenAUser { applicant, jwt ->
        givenAUser { assessor, _ ->
          val application = produceAndPersistBasicApplication(
            crn = "ABC123",
            userEntity = applicant,
            managingTeamCode = "TEAM",
            submittedAt = OffsetDateTime.now(),
          )

          approvedPremisesAssessmentEntityFactory.produceAndPersist {
            withApplication(application)
            withAllocatedToUser(assessor)
            withSubmittedAt(null)
          }

          webTestClient.post()
            .uri("/applications/${application.id}/withdrawal")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              NewWithdrawal(
                reason = WithdrawalReason.duplicateApplication,
              ),
            )
            .exchange()
            .expectStatus()
            .isOk

          emailAsserter.assertEmailsRequestedCount(2)
          emailAsserter.assertEmailRequested(
            applicant.email!!,
            Cas1NotifyTemplates.APPLICATION_WITHDRAWN_V2,
          )
          emailAsserter.assertEmailRequested(
            assessor.email!!,
            Cas1NotifyTemplates.ASSESSMENT_WITHDRAWN_V2,
          )
        }
      }
    }
  }

  private fun produceAndPersistBasicApplication(
    crn: String,
    userEntity: UserEntity,
    managingTeamCode: String,
    submittedAt: OffsetDateTime? = null,
  ): ApplicationEntity {
    val application =
      approvedPremisesApplicationEntityFactory.produceAndPersist {
        withCrn(crn)
        withCreatedByUser(userEntity)
        withData(
          """
          {
             "thingId": 123
          }
          """,
        )
        withSubmittedAt(submittedAt)
      }

    application.teamCodes += applicationTeamCodeRepository.save(
      ApplicationTeamCodeEntity(
        id = UUID.randomUUID(),
        application = application,
        teamCode = managingTeamCode,
      ),
    )

    return application
  }
}
