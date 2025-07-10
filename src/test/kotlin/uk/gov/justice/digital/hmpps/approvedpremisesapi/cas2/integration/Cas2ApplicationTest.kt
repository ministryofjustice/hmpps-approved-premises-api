package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.integration

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.NullNode
import com.ninjasquad.springmockk.SpykBean
import io.mockk.clearMocks
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.test.web.reactive.server.returnResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssignmentType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.FullPerson
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateApplicationType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.Cas2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2StatusUpdateEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.ExternalUserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.NomisUserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2StatusUpdate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.UpdateCas2Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas2Assessor
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas2LicenceCaseAdminUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas2PomUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextEmptyCaseSummaryToBulkResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextMockCaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.prisonAPIMockNotFoundInmateDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.asCaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.bodyAsListOfObjects
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.containsNone
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateAfter
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.math.sign

class Cas2ApplicationTest : IntegrationTestBase() {
  @SpykBean
  lateinit var realApplicationRepository: Cas2ApplicationRepository

  val schema = """
        {
          "${"\$schema"}": "https://json-schema.org/draft/2020-12/schema",
          "${"\$id"}": "https://example.com/product.schema.json",
          "title": "Thing",
          "description": "A thing",
          "type": "object",
          "properties": {
            "thingId": {
              "description": "The unique identifier for a thing",
              "type": "integer"
            }
          },
          "required": [ "thingId" ]
        }
        """

  val data = """
          {
             "thingId": 123
          }
          """

  @AfterEach
  fun afterEach() {
    // SpringMockK does not correctly clear mocks for @SpyKBeans that are also a @Repository, causing mocked behaviour
    // in one test to show up in another (see https://github.com/Ninja-Squad/springmockk/issues/85)
    // Manually clearing after each test seems to fix this.
    clearMocks(realApplicationRepository)
  }

  @Nested
  inner class ControlsOnExternalUsers {
    @ParameterizedTest
    @ValueSource(strings = ["ROLE_CAS2_ASSESSOR", "ROLE_CAS2_MI"])
    fun `creating an application is forbidden to external users based on role`(role: String) {
      val jwt = jwtAuthHelper.createClientCredentialsJwt(
        username = "username",
        authSource = "nomis",
        roles = listOf(role),
      )

      webTestClient.post()
        .uri("/cas2/applications")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @ParameterizedTest
    @ValueSource(strings = ["ROLE_CAS2_ASSESSOR", "ROLE_CAS2_MI"])
    fun `updating an application is forbidden to external users based on role`(role: String) {
      val jwt = jwtAuthHelper.createClientCredentialsJwt(
        username = "username",
        authSource = "nomis",
        roles = listOf(role),
      )

      webTestClient.put()
        .uri("/cas2/applications/66911cf0-75b1-4361-84bd-501b176fd4fd")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `viewing list of applications is forbidden to external users based on role`() {
      val jwt = jwtAuthHelper.createClientCredentialsJwt(
        username = "username",
        authSource = "nomis",
        roles = listOf("ROLE_CAS2_ASSESSOR"),
      )

      webTestClient.get()
        .uri("/cas2/applications")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `viewing an application is forbidden to external users based on role`() {
      val jwt = jwtAuthHelper.createClientCredentialsJwt(
        username = "username",
        authSource = "nomis",
        roles = listOf("ROLE_CAS2_ASSESSOR"),
      )

      webTestClient.get()
        .uri("/cas2/applications/66911cf0-75b1-4361-84bd-501b176fd4")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

  @Nested
  inner class MissingJwt {
    @Test
    fun `Get all applications without JWT returns 401`() {
      webTestClient.get()
        .uri("/cas2/applications")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Get single application without JWT returns 401`() {
      webTestClient.get()
        .uri("/cas2/applications/9b785e59-b85c-4be0-b271-d9ac287684b6")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Create new application without JWT returns 401`() {
      webTestClient.post()
        .uri("/cas2/applications")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }
  }

  @Nested
  inner class GetApplicationSummariesWithAssignmentType {

    fun abandonedApplication(userEntity: NomisUserEntity, crn: String) = cas2ApplicationEntityFactory.produceAndPersist {
      withCreatedByUser(userEntity)
      withCrn(crn)
      withData("{}")
      withCreatedAt(OffsetDateTime.parse("2024-01-03T16:10:00+01:00"))
      withAbandonedAt(OffsetDateTime.now())
    }

    fun inProgressApplication(userEntity: NomisUserEntity, crn: String) = cas2ApplicationEntityFactory.produceAndPersist {
      withSubmittedAt(null)
      withCreatedByUser(userEntity)
      withCrn(crn)
      withData("{}")
      withCreatedAt(OffsetDateTime.parse("2024-01-03T16:10:00+01:00"))
      withHdcEligibilityDate(LocalDate.now().plusMonths(3))
    }

    fun submittedApplication(userEntity: NomisUserEntity, crn: String): Cas2ApplicationEntity {
      val application = cas2ApplicationEntityFactory.produceAndPersist {
        withCreatedByUser(userEntity)
        withCrn(crn)
        withData("{}")
        withCreatedAt(OffsetDateTime.parse("2024-02-29T09:00:00+01:00"))
        withSubmittedAt(OffsetDateTime.now())
        withConditionalReleaseDate(LocalDate.now())
      }
      application.createApplicationAssignment(
        prisonCode = userEntity.activeCaseloadId!!,
        allocatedPomUser = userEntity,
      )
      return realApplicationRepository.save(application)
    }

    fun oldSubmittedApplication(userEntity: NomisUserEntity, crn: String): Cas2ApplicationEntity {
      val application = cas2ApplicationEntityFactory.produceAndPersist {
        withCreatedByUser(userEntity)
        withCrn(crn)
        withData("{}")
        withCreatedAt(OffsetDateTime.parse("2024-02-29T09:00:00+01:00"))
        withSubmittedAt(OffsetDateTime.now())
        withConditionalReleaseDate(LocalDate.now().minusDays(1))
      }
      application.createApplicationAssignment(
        prisonCode = userEntity.activeCaseloadId!!,
        allocatedPomUser = userEntity,
      )
      return realApplicationRepository.save(application)
    }

    fun transferredOutApplication(userEntity: NomisUserEntity, crn: String): Cas2ApplicationEntity {
      val application = cas2ApplicationEntityFactory.produceAndPersist {
        withCreatedByUser(userEntity)
        withCrn(crn)
        withData("{}")
        withCreatedAt(OffsetDateTime.parse("2024-02-29T09:00:00+01:00"))
        withSubmittedAt(OffsetDateTime.now())
        withConditionalReleaseDate(LocalDate.now())
      }
      application.createApplicationAssignment(
        prisonCode = userEntity.activeCaseloadId!!,
        allocatedPomUser = userEntity,
      )
      application.createApplicationAssignment(prisonCode = "ZZZ", allocatedPomUser = null)
      return realApplicationRepository.save(application)
    }

    fun transferredInApplication(
      transferredToUser: NomisUserEntity?,
      transferredFromUser: NomisUserEntity,
      crn: String,
      isInternalTransfer: Boolean,
      prisonCode: String? = null,
    ): Cas2ApplicationEntity {
      val application = cas2ApplicationEntityFactory.produceAndPersist {
        withCreatedByUser(transferredFromUser)
        withCrn(crn)
        withData("{}")
        withCreatedAt(OffsetDateTime.parse("2024-02-29T09:00:00+01:00"))
        withSubmittedAt(OffsetDateTime.now())
        withConditionalReleaseDate(LocalDate.now())
      }
      if (!isInternalTransfer) {
        application.createApplicationAssignment(
          prisonCode = transferredToUser?.activeCaseloadId ?: prisonCode!!,
          allocatedPomUser = null,
        )
      }
      if (transferredToUser != null) {
        application.createApplicationAssignment(
          prisonCode = transferredToUser.activeCaseloadId!!,
          allocatedPomUser = transferredToUser,
        )
      }

      return realApplicationRepository.save(application)
    }

    fun doRequestAndGetResponse(assignmentType: AssignmentType, jwt: String): List<Cas2ApplicationSummary> = webTestClient.get()
      .uri("/cas2/applications?assignmentType=${assignmentType.name}")
      .header("Authorization", "Bearer $jwt")
      .header("X-Service-Name", ServiceName.cas2.value)
      .exchange()
      .expectStatus()
      .isOk
      .bodyAsListOfObjects<Cas2ApplicationSummary>()

    @Test
    fun `Get all applications with assignmentType returns 200 with correct body`() {
      val prisonCode = "PRI"
      val otherPrisonCode = "OTH"
      givenACas2PomUser(
        mockCallToGetMe = true,
        nomisUserDetailsConfigBlock = { withActiveCaseloadId(prisonCode) },
      ) { userEntity, jwt ->
        givenACas2PomUser(
          mockCallToGetMe = false,
          nomisUserDetailsConfigBlock = { withActiveCaseloadId(prisonCode) },
        ) { otherUser, _ ->
          givenACas2PomUser(
            mockCallToGetMe = false,
            nomisUserDetailsConfigBlock = { withActiveCaseloadId(otherPrisonCode) },
          ) { otherPrisonUser, _ ->
            givenAnOffender { offenderDetails, _ ->

              val abandonedApplication = abandonedApplication(userEntity, offenderDetails.otherIds.crn)
              val inProgressApplication = inProgressApplication(userEntity, offenderDetails.otherIds.crn)
              val submittedApplication = submittedApplication(userEntity, offenderDetails.otherIds.crn)
              val oldSubmittedApplication = oldSubmittedApplication(userEntity, offenderDetails.otherIds.crn)
              val transferredOutApplication = transferredOutApplication(userEntity, offenderDetails.otherIds.crn)
              val transferredInApplication =
                transferredInApplication(
                  transferredToUser = userEntity,
                  transferredFromUser = otherPrisonUser,
                  crn = offenderDetails.otherIds.crn,
                  isInternalTransfer = false,
                )

              val transferredInUnallocatedApplication =
                transferredInApplication(
                  transferredToUser = null,
                  transferredFromUser = otherPrisonUser,
                  crn = offenderDetails.otherIds.crn,
                  isInternalTransfer = false,
                  prisonCode = userEntity.activeCaseloadId!!,
                )

              val internalTransferredApplication = transferredInApplication(
                transferredToUser = userEntity,
                transferredFromUser = otherUser,
                crn = offenderDetails.otherIds.crn,
                isInternalTransfer = true,
              )

              val otherUserSubmittedApplication = submittedApplication(otherUser, offenderDetails.otherIds.crn)
              val otherUserInProgressApplication = inProgressApplication(otherUser, offenderDetails.otherIds.crn)
              val otherPrisonUserInProgressApplication =
                inProgressApplication(otherPrisonUser, offenderDetails.otherIds.crn)
              val otherPrisonSubmittedApplication = submittedApplication(otherPrisonUser, offenderDetails.otherIds.crn)

              val neverReturnedApplications = listOf(
                abandonedApplication,
                oldSubmittedApplication,
                otherPrisonSubmittedApplication,
                otherPrisonUserInProgressApplication,
                otherUserInProgressApplication,
              )

              val inProgressResponse = doRequestAndGetResponse(AssignmentType.IN_PROGRESS, jwt)
              assertInProgressApplications(
                inProgressResponse,
                expectedApplications = listOf(inProgressApplication),
              )

              val allocatedResponse = doRequestAndGetResponse(AssignmentType.ALLOCATED, jwt)
              assertAllocatedApplications(
                allocatedResponse,
                expectedApplications = listOf(
                  submittedApplication,
                  transferredInApplication,
                  internalTransferredApplication,
                ),
              )

              val deallocatedResponse = doRequestAndGetResponse(AssignmentType.DEALLOCATED, jwt)
              assertDeallocatedApplications(
                deallocatedResponse,
                expectedApplications = listOf(
                  transferredOutApplication,
                ),
              )

              val samePrisonResponse = doRequestAndGetResponse(AssignmentType.PRISON, jwt)
              assertSamePrisonApplications(
                samePrisonResponse,
                expectedApplications = listOf(
                  submittedApplication,
                  otherUserSubmittedApplication,
                  transferredInApplication,
                  internalTransferredApplication,
                ),
              )

              val unallocatedResponse = doRequestAndGetResponse(AssignmentType.UNALLOCATED, jwt)
              assertUnallocatedApplications(
                unallocatedResponse,
                expectedApplications = listOf(
                  transferredInUnallocatedApplication,
                ),
              )

              val returnedIds = listOf(
                unallocatedResponse.map { it.id },
                samePrisonResponse.map { it.id },
                deallocatedResponse.map { it.id },
                allocatedResponse.map { it.id },
                inProgressResponse.map { it.id },
              ).flatten()

              assertThat(returnedIds.containsNone(neverReturnedApplications.map { it.id })).isTrue
            }
          }
        }
      }
    }

    @ParameterizedTest
    @EnumSource(AssignmentType::class)
    fun `applications with conditional release date before today not returned`(assignmentType: AssignmentType) {
      givenACas2Assessor { assessor, _ ->
        givenACas2PomUser { userAPrisonA, jwt ->
          givenAnOffender { offenderDetails, _ ->
            cas2ApplicationJsonSchemaRepository.deleteAll()

            val inProgressApplication = Cas2ApplicationEntityFactory()
              .withCreatedByUser(userAPrisonA)
              .withCrn(offenderDetails.otherIds.crn)
              .withData("{}")
              .withCreatedAt(OffsetDateTime.now().minusDays(14))
              .withReferringPrisonCode(userAPrisonA.activeCaseloadId!!)
              .withConditionalReleaseDate(LocalDate.now().minusDays(1))
              .produce()
            cas2ApplicationRepository.saveAndFlush(inProgressApplication)

            val allocatedApplication =
              inProgressApplication.copy(id = UUID.randomUUID(), submittedAt = OffsetDateTime.now())
            cas2ApplicationRepository.saveAndFlush(allocatedApplication)
            allocatedApplication.createApplicationAssignment(userAPrisonA.activeCaseloadId!!, userAPrisonA)

            val deallocatedApplication = allocatedApplication.copy(id = UUID.randomUUID())
            cas2ApplicationRepository.saveAndFlush(deallocatedApplication)
            deallocatedApplication.createApplicationAssignment("OTHER", null)

            val samePrisonApplication =
              deallocatedApplication.copy(id = UUID.randomUUID(), applicationAssignments = mutableListOf())
            val samePrisonCode = userAPrisonA.activeCaseloadId!!
            val userBPrisonA = nomisUserEntityFactory.produceAndPersist {
              withActiveCaseloadId(samePrisonCode)
            }
            samePrisonApplication.createApplicationAssignment(samePrisonCode, userBPrisonA)
            cas2ApplicationRepository.saveAndFlush(samePrisonApplication)

            val unallocatedApplication =
              allocatedApplication.copy(id = UUID.randomUUID(), applicationAssignments = mutableListOf())
            val otherPrisonCode = "OTHERPRISON"
            val userCPrisonB = nomisUserEntityFactory.produceAndPersist {
              withActiveCaseloadId(otherPrisonCode)
            }
            unallocatedApplication.createApplicationAssignment(otherPrisonCode, userCPrisonB)
            unallocatedApplication.createApplicationAssignment(samePrisonCode, null)
            cas2ApplicationRepository.saveAndFlush(unallocatedApplication)

            val responseBody = webTestClient.get()
              .uri("/cas2/applications?assignmentType=${assignmentType.value}")
              .header("Authorization", "Bearer $jwt")
              .header("X-Service-Name", ServiceName.cas2.value)
              .exchange()
              .expectStatus()
              .isOk
              .bodyAsListOfObjects<Cas2ApplicationSummary>()

            assertThat(responseBody).hasSize(0)
          }
        }
      }
    }

    private fun assertInProgressApplications(
      response: List<Cas2ApplicationSummary>,
      expectedApplications: List<Cas2ApplicationEntity>,
    ) {
      assertThat(response.size).isEqualTo(1)
      assertThat(response.map { it.submittedAt }).containsOnlyNulls()
      assertThat(response.map { it.id }).containsExactlyInAnyOrderElementsOf(expectedApplications.map { it.id })
    }

    private fun assertAllocatedApplications(
      response: List<Cas2ApplicationSummary>,
      expectedApplications: List<Cas2ApplicationEntity>,
    ) {
      assertThat(response.size).isEqualTo(3)
      val ids = response.map { it.id }
      assertThat(ids).containsExactlyInAnyOrderElementsOf(expectedApplications.map({ it.id }))
    }

    private fun assertDeallocatedApplications(
      response: List<Cas2ApplicationSummary>,
      expectedApplications: List<Cas2ApplicationEntity>,
    ) {
      assertThat(response.size).isEqualTo(1)
      val ids = response.map { it.id }
      assertThat(ids).containsExactlyInAnyOrderElementsOf(expectedApplications.map({ it.id }))
    }

    private fun assertSamePrisonApplications(
      response: List<Cas2ApplicationSummary>,
      expectedApplications: List<Cas2ApplicationEntity>,
    ) {
      assertThat(response.size).isEqualTo(4)
      val ids = response.map { it.id }
      assertThat(ids).containsExactlyInAnyOrderElementsOf(expectedApplications.map({ it.id }))
    }

    private fun assertUnallocatedApplications(
      response: List<Cas2ApplicationSummary>,
      expectedApplications: List<Cas2ApplicationEntity>,
    ) {
      assertThat(response.size).isEqualTo(1)
      assertThat(response.filter { it.submittedAt != null }.size).isEqualTo(1)
      val ids = response.map { it.id }
      assertThat(ids).containsExactlyInAnyOrderElementsOf(expectedApplications.map({ it.id }))
    }
  }

  @Nested
  inner class GetToIndex {

    @ParameterizedTest
    @EnumSource(AssignmentType::class)
    fun `return unexpired applications when applications GET is requested`(assignmentType: AssignmentType) {
      val unexpiredSubset = setOf(
        Pair("More information requested", UUID.fromString("f5cd423b-08eb-4efb-96ff-5cc6bb073905")),
        Pair("Awaiting decision", UUID.fromString("ba4d8432-250b-4ab9-81ec-7eb4b16e5dd1")),
        Pair("On waiting list", UUID.fromString("a919097d-b324-471c-9834-756f255e87ea")),
        Pair("Place offered", UUID.fromString("176bbda0-0766-4d77-8d56-18ed8f9a4ef2")),
        Pair("Offer accepted", UUID.fromString("fe254d88-ce1d-4cd8-8bd6-88de88f39019")),
        Pair("Could not be placed", UUID.fromString("758eee61-2a6d-46b9-8bdd-869536d77f1b")),
        Pair("Incomplete", UUID.fromString("4ad9bbfa-e5b0-456f-b746-146f7fd511dd")),
        Pair("Offer declined or withdrawn", UUID.fromString("9a381bc6-22d3-41d6-804d-4e49f428c1de")),
      )

      val expiredSubset = setOf(
        Pair("Referral withdrawn", UUID.fromString("004e2419-9614-4c1e-a207-a8418009f23d")),
        Pair("Referral cancelled", UUID.fromString("f13bbdd6-44f1-4362-b9d3-e6f1298b1bf9")),
        Pair("Awaiting arrival", UUID.fromString("89458555-3219-44a2-9584-c4f715d6b565")),
      )

      fun createApplication(
        userEntity: NomisUserEntity,
        offenderDetails: OffenderDetailSummary,
      ): Cas2ApplicationEntity {
        val isSubmitted = assignmentType != AssignmentType.IN_PROGRESS
        val application = cas2ApplicationEntityFactory.produceAndPersist {
          withCreatedByUser(userEntity)
          withCrn(offenderDetails.otherIds.crn)
          withCreatedAt(OffsetDateTime.now().minusDays(28))
          withConditionalReleaseDate(LocalDate.now().plusDays(1))
          if (isSubmitted) {
            withSubmittedAt(OffsetDateTime.now().minusHours(12))
          }
        }
        if (isSubmitted) {
          application.createApplicationAssignment(userEntity.activeCaseloadId!!, userEntity)
        }
        if (assignmentType == AssignmentType.DEALLOCATED) {
          application.createApplicationAssignment("OTHER", null)
        }
        if (assignmentType == AssignmentType.UNALLOCATED) {
          application.createApplicationAssignment(userEntity.activeCaseloadId!!, null)
        }
        cas2ApplicationRepository.save(application)
        return application
      }

      fun createStatusUpdate(status: Pair<String, UUID>, application: Cas2ApplicationEntity): Cas2StatusUpdateEntity = cas2StatusUpdateEntityFactory.produceAndPersist {
        withLabel(status.first)
        withStatusId(status.second)
        withApplication(application)
        withAssessor(externalUserEntityFactory.produceAndPersist())
      }

      fun unexpiredDateTime() = OffsetDateTime.now().randomDateTimeBefore(32)
      fun expiredDateTime() = unexpiredDateTime().minusDays(33)

      val unexpiredApplicationIds = mutableSetOf<UUID>()
      val expiredApplicationIds = mutableSetOf<UUID>()

      givenACas2PomUser { userEntity, jwt ->
        givenAnOffender { offenderDetails, _ ->

          repeat(2) {
            unexpiredApplicationIds.add(createApplication(userEntity, offenderDetails).id)
          }

          unexpiredSubset.union(expiredSubset).forEach {
            val application = createApplication(userEntity, offenderDetails)
            val statusUpdate = createStatusUpdate(it, application)
            statusUpdate.createdAt = unexpiredDateTime()
            cas2StatusUpdateRepository.save(statusUpdate)
            unexpiredApplicationIds.add(application.id)
          }

          unexpiredSubset.forEach {
            val application = createApplication(userEntity, offenderDetails)
            val statusUpdate = createStatusUpdate(it, application)
            statusUpdate.createdAt = unexpiredDateTime()
            cas2StatusUpdateRepository.save(statusUpdate)
            unexpiredApplicationIds.add(application.id)
          }

          expiredSubset.forEach {
            val application = createApplication(userEntity, offenderDetails)
            val statusUpdate = createStatusUpdate(it, application)
            statusUpdate.createdAt = expiredDateTime()
            cas2StatusUpdateRepository.save(statusUpdate)
            expiredApplicationIds.add(application.id)
          }

          val responseBody = webTestClient.get()
            .uri("/cas2/applications?assignmentType=${assignmentType.value}")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.cas2.value)
            .exchange()
            .expectStatus()
            .isOk
            .bodyAsListOfObjects<Cas2ApplicationSummary>()

          val returnedApplicationIds = responseBody.map { it.id }.toSet()

          assertThat(returnedApplicationIds).containsAll(unexpiredApplicationIds)
        }
      }
    }

    @Test
    fun `Get all applications with pagination returns 200 with correct body and header`() {
      givenACas2PomUser { otherUser, _ ->
        givenACas2PomUser { userEntity, jwt ->
          givenAnOffender { offenderDetails, _ ->
            cas2ApplicationJsonSchemaRepository.deleteAll()

            repeat(12) {
              val application = cas2ApplicationEntityFactory.produceAndPersist {
                withCreatedByUser(userEntity)
                withCrn(offenderDetails.otherIds.crn)
                withData("{}")
                withCreatedAt(OffsetDateTime.now().randomDateTimeBefore(14))
                withSubmittedAt(OffsetDateTime.now().minusDays(1))
              }

              application.createApplicationAssignment(userEntity.activeCaseloadId!!, userEntity)
              cas2ApplicationRepository.save(application)
            }

            val rawResponseBodyPage1 = webTestClient.get()
              .uri("/cas2/applications?page=1&assignmentType=ALLOCATED")
              .header("Authorization", "Bearer $jwt")
              .header("X-Service-Name", ServiceName.cas2.value)
              .exchange()
              .expectStatus()
              .isOk
              .expectHeader().valueEquals("X-Pagination-CurrentPage", 1)
              .expectHeader().valueEquals("X-Pagination-TotalPages", 2)
              .expectHeader().valueEquals("X-Pagination-TotalResults", 12)
              .expectHeader().valueEquals("X-Pagination-PageSize", 10)
              .returnResult<String>()
              .responseBody
              .blockFirst()

            val responseBodyPage1 =
              objectMapper.readValue(rawResponseBodyPage1, object : TypeReference<List<Cas2ApplicationSummary>>() {})

            assertThat(responseBodyPage1).size().isEqualTo(10)

            assertThat(isOrderedByCreatedAtDescending(responseBodyPage1)).isTrue()

            val rawResponseBodyPage2 = webTestClient.get()
              .uri("/cas2/applications?page=2&assignmentType=ALLOCATED")
              .header("Authorization", "Bearer $jwt")
              .header("X-Service-Name", ServiceName.cas2.value)
              .exchange()
              .expectStatus()
              .isOk
              .expectHeader().valueEquals("X-Pagination-CurrentPage", 2)
              .expectHeader().valueEquals("X-Pagination-TotalPages", 2)
              .expectHeader().valueEquals("X-Pagination-TotalResults", 12)
              .expectHeader().valueEquals("X-Pagination-PageSize", 10)
              .returnResult<String>()
              .responseBody
              .blockFirst()

            val responseBodyPage2 =
              objectMapper.readValue(rawResponseBodyPage2, object : TypeReference<List<Cas2ApplicationSummary>>() {})

            assertThat(responseBodyPage2).size().isEqualTo(2)
          }
        }
      }
    }

    @Test
    fun `When a person is not found, returns 200 with placeholder text`() {
      givenACas2PomUser { userEntity, jwt ->
        val crn = "X1234"

        apDeliusContextEmptyCaseSummaryToBulkResponse(crn)

        produceAndPersistBasicApplication(crn, userEntity)
        webTestClient.get()
          .uri("/cas2/applications?assignmentType=IN_PROGRESS")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("$[0].personName").isEqualTo("Person Not Found")
      }
    }

    /**
     * Returns true if the list of application summaries is sorted by descending created_at
     * or false if not.
     *
     * Works by calculating the difference in seconds between two dates and using the sign
     * of this difference.  If two dates are descending then the difference will be positive.
     * If two dates are ascending the difference will be negative (which is set to 0).
     *
     * For a list of dates, the cumulative multiple of these signs will be 1 if all
     * dates in the range are descending (= 1 x 1 x 1 etc.).
     *
     * If any dates are ascending the multiple will be 0 ( = 1 x 1 x 0 etc.).
     *
     * If all dates are ascending the multiple will also be 0 ( = 1 x 0 x 0 etc.).
     */
    private fun isOrderedByCreatedAtDescending(responseBody: List<Cas2ApplicationSummary>): Boolean {
      var allDescending = 1
      for (i in 1..(responseBody.size - 1)) {
        val isDescending = (responseBody[i - 1].createdAt.epochSecond - responseBody[i].createdAt.epochSecond).sign
        allDescending *= if (isDescending > 0) 1 else 0
      }
      return allDescending == 1
    }

    @Nested
    inner class ApplicationsInSamePrison {
      @Test
      fun `Get all submitted applications for prison returns 200 with correct body`() {
        givenACas2Assessor { assessor, _ ->
          givenACas2PomUser { userAPrisonA, jwt ->
            givenAnOffender { offenderDetails, _ ->
              cas2ApplicationJsonSchemaRepository.deleteAll()

              repeat(5) {
                cas2ApplicationEntityFactory.produceAndPersist {
                  withCreatedByUser(userAPrisonA)
                  withCrn(offenderDetails.otherIds.crn)
                  withData("{}")
                  withCreatedAt(OffsetDateTime.now().randomDateTimeBefore(14))
                  withReferringPrisonCode(userAPrisonA.activeCaseloadId!!)
                }
              }

              val userBPrisonAApplications = mutableListOf<Cas2ApplicationEntity>()

              val userBPrisonA = nomisUserEntityFactory.produceAndPersist {
                withActiveCaseloadId(userAPrisonA.activeCaseloadId!!)
              }

              // submitted applications with conditional release dates in the future
              repeat(6) {
                val application = cas2ApplicationEntityFactory.produceAndPersist {
                  withCreatedByUser(userBPrisonA)
                  withCrn(offenderDetails.otherIds.crn)
                  withData("{}")
                  withCreatedAt(OffsetDateTime.now().minusDays(it.toLong()))
                  withReferringPrisonCode(userBPrisonA.activeCaseloadId!!)
                  withSubmittedAt(OffsetDateTime.now().minusDays(it.toLong()))
                  withConditionalReleaseDate(LocalDate.now().randomDateAfter(14))
                }
                application.createApplicationAssignment(userBPrisonA.activeCaseloadId!!, userBPrisonA)
                cas2ApplicationRepository.save(application)
                userBPrisonAApplications.add(application)
              }

              // submitted applications with conditional release dates today
              repeat(2) {
                val application = cas2ApplicationEntityFactory.produceAndPersist {
                  withCreatedByUser(userBPrisonA)
                  withCrn(offenderDetails.otherIds.crn)
                  withData("{}")
                  withCreatedAt(OffsetDateTime.now().minusDays(it.toLong() + 6))
                  withReferringPrisonCode(userBPrisonA.activeCaseloadId!!)
                  withSubmittedAt(OffsetDateTime.now().minusDays(it.toLong() + 6))
                  withConditionalReleaseDate(LocalDate.now())
                }
                application.createApplicationAssignment(userBPrisonA.activeCaseloadId!!, userBPrisonA)
                cas2ApplicationRepository.save(application)
                userBPrisonAApplications.add(application)
              }

              addStatusUpdates(userBPrisonAApplications.first().id, assessor)

              // submitted application with a conditional release date before today
              val excludedApplication = cas2ApplicationEntityFactory.produceAndPersist {
                withCreatedByUser(userBPrisonA)
                withCrn(offenderDetails.otherIds.crn)
                withData("{}")
                withCreatedAt(OffsetDateTime.now().minusDays(14))
                withReferringPrisonCode(userBPrisonA.activeCaseloadId!!)
                withSubmittedAt(OffsetDateTime.now())
                withConditionalReleaseDate(LocalDate.now().randomDateBefore(14))
              }
              excludedApplication.createApplicationAssignment(userBPrisonA.activeCaseloadId!!, userBPrisonA)
              cas2ApplicationRepository.save(excludedApplication)

              val userCPrisonB = nomisUserEntityFactory.produceAndPersist {
                withActiveCaseloadId("another prison")
              }

              val otherPrisonApplication = cas2ApplicationEntityFactory.produceAndPersist {
                withCreatedByUser(userCPrisonB)
                withCrn(offenderDetails.otherIds.crn)
                withData("{}")
                withCreatedAt(OffsetDateTime.now().randomDateTimeBefore(14))
                withReferringPrisonCode(userCPrisonB.activeCaseloadId!!)
                withSubmittedAt(OffsetDateTime.now().minusDays(14))
              }
              otherPrisonApplication.createApplicationAssignment(userCPrisonB.activeCaseloadId!!, userCPrisonB)
              cas2ApplicationRepository.save(otherPrisonApplication)

              val responseBody = webTestClient.get()
                .uri("/cas2/applications?assignmentType=PRISON")
                .header("Authorization", "Bearer $jwt")
                .header("X-Service-Name", ServiceName.cas2.value)
                .exchange()
                .expectStatus()
                .isOk
                .bodyAsListOfObjects<Cas2ApplicationSummary>()

              assertThat(responseBody.map { it.id }).doesNotContain(excludedApplication.id)

              val returnedApplicationIds = responseBody.map { it.id }

              assertThat(responseBody).allMatch { it.currentPrisonName == userAPrisonA.activeCaseloadId!! }
              assertThat(returnedApplicationIds).containsAll(userBPrisonAApplications.map { it.id })

              assertThat(responseBody).noneMatch { otherPrisonApplication.id == it.id }

              assertThat(responseBody[0].latestStatusUpdate?.label).isEqualTo("Awaiting decision")
              assertThat(responseBody[0].latestStatusUpdate?.statusId).isEqualTo(UUID.fromString("c74c3e54-52d8-4aa2-86f6-05190985efee"))
            }
          }
        }
      }

      @Test
      fun `Unsubmitted applications for prison not returned`() {
        givenACas2Assessor { assessor, _ ->
          givenACas2PomUser { userAPrisonA, jwt ->
            givenAnOffender { offenderDetails, _ ->
              cas2ApplicationJsonSchemaRepository.deleteAll()

              val userAPrisonAApplicationIds = mutableListOf<UUID>()

              repeat(5) {
                userAPrisonAApplicationIds.add(
                  cas2ApplicationEntityFactory.produceAndPersist {
                    withCreatedByUser(userAPrisonA)
                    withCrn(offenderDetails.otherIds.crn)
                    withData("{}")
                    withCreatedAt(OffsetDateTime.now().minusDays(it.toLong()))
                    withReferringPrisonCode(userAPrisonA.activeCaseloadId!!)
                  }.id,
                )
              }

              val userBPrisonA = nomisUserEntityFactory.produceAndPersist {
                withActiveCaseloadId(userAPrisonA.activeCaseloadId!!)
              }

              val userBPrisonAApplicationIds = mutableListOf<UUID>()

              repeat(6) {
                userBPrisonAApplicationIds.add(
                  cas2ApplicationEntityFactory.produceAndPersist {
                    withCreatedByUser(userBPrisonA)
                    withCrn(offenderDetails.otherIds.crn)
                    withData("{}")
                    withCreatedAt(OffsetDateTime.now().minusDays(it.toLong()))
                    withSubmittedAt(OffsetDateTime.now().randomDateTimeBefore(14))
                    withReferringPrisonCode(userAPrisonA.activeCaseloadId!!)
                    withConditionalReleaseDate(LocalDate.now().randomDateAfter(14))
                  }.id,
                )
              }

              // submitted applications with conditional release dates today
              repeat(2) {
                userBPrisonAApplicationIds.add(
                  cas2ApplicationEntityFactory.produceAndPersist {
                    withCreatedByUser(userBPrisonA)
                    withCrn(offenderDetails.otherIds.crn)
                    withData("{}")
                    withCreatedAt(OffsetDateTime.now().minusDays(it.toLong() + 6))
                    withReferringPrisonCode(userAPrisonA.activeCaseloadId!!)
                    withSubmittedAt(OffsetDateTime.now().minusDays(it.toLong() + 6))
                    withConditionalReleaseDate(LocalDate.now())
                  }.id,
                )
              }

              // submitted application with a conditional release date before today
              val excludedApplicationId = cas2ApplicationEntityFactory.produceAndPersist {
                withCreatedByUser(userBPrisonA)
                withCrn(offenderDetails.otherIds.crn)
                withData("{}")
                withCreatedAt(OffsetDateTime.now().minusDays(14))
                withReferringPrisonCode(userAPrisonA.activeCaseloadId!!)
                withSubmittedAt(OffsetDateTime.now())
                withConditionalReleaseDate(LocalDate.now().randomDateBefore(14))
              }.id

              addStatusUpdates(userBPrisonAApplicationIds.first(), assessor)

              val responseBody = webTestClient.get()
                .uri("/cas2/applications?assignmentType=PRISON")
                .header("Authorization", "Bearer $jwt")
                .header("X-Service-Name", ServiceName.cas2.value)
                .exchange()
                .expectStatus()
                .isOk
                .bodyAsListOfObjects<Cas2ApplicationSummary>()

              assertThat(responseBody).noneMatch {
                excludedApplicationId == it.id
              }

              val returnedApplicationIds = responseBody.map { it.id }.toSet()

              assertThat(returnedApplicationIds.containsNone(userAPrisonAApplicationIds)).isTrue
            }
          }
        }
      }
    }

    @Nested
    inner class AsLicenceCaseAdminUser {
      @Test
      fun `Get all applications for prison returns 200 with correct body`() {
        givenACas2Assessor { assessor, _ ->
          givenACas2LicenceCaseAdminUser { caseAdminPrisonA, jwt ->
            givenAnOffender { offenderDetails, _ ->
              cas2ApplicationJsonSchemaRepository.deleteAll()

              val pomUserPrisonA = nomisUserEntityFactory.produceAndPersist {
                withActiveCaseloadId(caseAdminPrisonA.activeCaseloadId!!)
              }

              val userBPrisonAApplicationIds = mutableListOf<UUID>()

              // submitted applications with conditional release dates in the future
              repeat(6) {
                val application = cas2ApplicationEntityFactory.produceAndPersist {
                  withCreatedByUser(pomUserPrisonA)
                  withCrn(offenderDetails.otherIds.crn)
                  withData("{}")
                  withCreatedAt(OffsetDateTime.now().minusDays(it.toLong()))
                  withSubmittedAt(OffsetDateTime.now().randomDateTimeBefore(14))
                  withReferringPrisonCode(caseAdminPrisonA.activeCaseloadId!!)
                  withConditionalReleaseDate(LocalDate.now().randomDateAfter(14))
                }
                application.createApplicationAssignment(caseAdminPrisonA.activeCaseloadId!!, caseAdminPrisonA)
                cas2ApplicationRepository.save(application)

                userBPrisonAApplicationIds.add(application.id)
              }

              // submitted applications with conditional release date of today
              repeat(2) {
                val application = cas2ApplicationEntityFactory.produceAndPersist {
                  withCreatedByUser(pomUserPrisonA)
                  withCrn(offenderDetails.otherIds.crn)
                  withData("{}")
                  withCreatedAt(OffsetDateTime.now().minusDays(it.toLong() + 6))
                  withSubmittedAt(OffsetDateTime.now().randomDateTimeBefore(14))
                  withReferringPrisonCode(caseAdminPrisonA.activeCaseloadId!!)
                  withConditionalReleaseDate(LocalDate.now())
                }

                application.createApplicationAssignment(caseAdminPrisonA.activeCaseloadId!!, caseAdminPrisonA)
                cas2ApplicationRepository.save(application)
                userBPrisonAApplicationIds.add(application.id)
              }

              // submitted application with a conditional release date before today
              val excludedApplication = cas2ApplicationEntityFactory.produceAndPersist {
                withCreatedByUser(pomUserPrisonA)
                withCrn(offenderDetails.otherIds.crn)
                withData("{}")
                withCreatedAt(OffsetDateTime.now().randomDateTimeBefore(14))
                withSubmittedAt(OffsetDateTime.now().randomDateTimeBefore(14))
                withReferringPrisonCode(caseAdminPrisonA.activeCaseloadId!!)
                withConditionalReleaseDate(LocalDate.now().randomDateBefore(14))
              }

              excludedApplication.createApplicationAssignment(caseAdminPrisonA.activeCaseloadId!!, caseAdminPrisonA)

              val pomUserPrisonB = nomisUserEntityFactory.produceAndPersist {
                withActiveCaseloadId("other_prison")
              }

              val otherPrisonApplication = cas2ApplicationEntityFactory.produceAndPersist {
                withCreatedByUser(pomUserPrisonB)
                withCrn(offenderDetails.otherIds.crn)
                withData("{}")
                withCreatedAt(OffsetDateTime.now())
                withSubmittedAt(OffsetDateTime.now().randomDateTimeBefore(14))
                withReferringPrisonCode("other_prison")
              }

              val responseBody = webTestClient.get()
                .uri("/cas2/applications?assignmentType=PRISON")
                .header("Authorization", "Bearer $jwt")
                .header("X-Service-Name", ServiceName.cas2.value)
                .exchange()
                .expectStatus()
                .isOk
                .bodyAsListOfObjects<Cas2ApplicationSummary>()

              assertThat(responseBody).noneMatch {
                excludedApplication.id == it.id
              }
              val returnedApplicationIds = responseBody.map { it.id }.toSet()

              assertThat(returnedApplicationIds).isEqualTo(userBPrisonAApplicationIds.toSet())
              assertThat(returnedApplicationIds).noneMatch {
                otherPrisonApplication.id == it
              }
            }
          }
        }
      }
    }
  }

  private fun addStatusUpdates(applicationId: UUID, assessor: ExternalUserEntity) {
    cas2StatusUpdateEntityFactory.produceAndPersist {
      withLabel("More information requested")
      withApplication(cas2ApplicationRepository.findById(applicationId).get())
      withAssessor(assessor)
    }
    // this is the one that should be returned as latestStatusUpdate
    cas2StatusUpdateEntityFactory.produceAndPersist {
      withStatusId(UUID.fromString("c74c3e54-52d8-4aa2-86f6-05190985efee"))
      withLabel("Awaiting decision")
      withApplication(cas2ApplicationRepository.findById(applicationId).get())
      withAssessor(assessor)
    }
  }

  @Nested
  inner class GetToIndexUsingIsSubmitted {

    var jwtForUser: String? = null
    val submittedIds = mutableSetOf<UUID>()
    val unSubmittedIds = mutableSetOf<UUID>()
    lateinit var excludedApplicationId: UUID

    @BeforeEach
    fun setup() {
      givenACas2Assessor { assessor, _ ->
        givenACas2PomUser { userEntity, jwt ->
          givenACas2PomUser { otherUser, _ ->
            givenAnOffender { offenderDetails, _ ->
              cas2ApplicationJsonSchemaRepository.deleteAll()

              // create 3 x submitted applications for this user
              // with most recent first and conditional release dates in the future
              repeat(3) {
                val application = cas2ApplicationEntityFactory.produceAndPersist {
                  withCreatedAt(OffsetDateTime.now().minusDays(it.toLong()))
                  withCreatedByUser(userEntity)
                  withCrn(offenderDetails.otherIds.crn)
                  withData("{}")
                  withSubmittedAt(OffsetDateTime.now().minusDays(it.toLong()))
                  withConditionalReleaseDate(LocalDate.now().randomDateAfter(14))
                }
                application.createApplicationAssignment(userEntity.activeCaseloadId!!, userEntity)
                cas2ApplicationRepository.save(application)
                submittedIds.add(application.id)
              }

              // create 2 x submitted applications for this user
              // with most recent first and conditional release dates of today
              repeat(2) {
                val application = cas2ApplicationEntityFactory.produceAndPersist {
                  withCreatedAt(OffsetDateTime.now().minusDays(it.toLong() + 3))
                  withCreatedByUser(userEntity)
                  withCrn(offenderDetails.otherIds.crn)
                  withData("{}")
                  withSubmittedAt(OffsetDateTime.now().minusDays(it.toLong() + 3))
                  withConditionalReleaseDate(LocalDate.now())
                }
                application.createApplicationAssignment(userEntity.activeCaseloadId!!, userEntity)
                cas2ApplicationRepository.save(application)
                submittedIds.add(application.id)
              }

              // submitted application with a conditional release date before today
              val submittedApplicationWithCondionalReleaseDate = cas2ApplicationEntityFactory.produceAndPersist {
                withCreatedAt(OffsetDateTime.now().minusDays(14))
                withCreatedByUser(userEntity)
                withCrn(offenderDetails.otherIds.crn)
                withData("{}")
                withSubmittedAt(OffsetDateTime.now())
                withConditionalReleaseDate(LocalDate.now().randomDateBefore(14))
              }
              submittedApplicationWithCondionalReleaseDate.createApplicationAssignment(
                userEntity.activeCaseloadId!!,
                userEntity,
              )
              cas2ApplicationRepository.save(submittedApplicationWithCondionalReleaseDate)
              excludedApplicationId = submittedApplicationWithCondionalReleaseDate.id

              addStatusUpdates(submittedIds.first(), assessor)

              // create 4 x un-submitted in-progress applications for this user
              repeat(4) {
                unSubmittedIds.add(
                  cas2ApplicationEntityFactory.produceAndPersist {
                    withCreatedByUser(userEntity)
                    withCrn(offenderDetails.otherIds.crn)
                    withData("{}")
                  }.id,
                )
              }

              // create a submitted application by another user which should not be in results
              val otherUserApplication = cas2ApplicationEntityFactory.produceAndPersist {
                withCreatedByUser(otherUser)
                withCrn(offenderDetails.otherIds.crn)
                withData("{}")
                withSubmittedAt(OffsetDateTime.now())
              }

              otherUserApplication.createApplicationAssignment(otherUser.activeCaseloadId!!, otherUser)
              cas2ApplicationRepository.save(otherUserApplication)

              // create an unsubmitted application by another user which should not be in results
              cas2ApplicationEntityFactory.produceAndPersist {
                withCreatedByUser(otherUser)
                withCrn(offenderDetails.otherIds.crn)
                withData("{}")
              }

              jwtForUser = jwt
            }
          }
        }
      }
    }

    @Test
    fun `returns submitted applications for user when assignmentType is ALLOCATED`() {
      val responseBody = webTestClient.get()
        .uri("/cas2/applications?assignmentType=ALLOCATED")
        .header("Authorization", "Bearer $jwtForUser")
        .header("X-Service-Name", ServiceName.cas2.value)
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsListOfObjects<Cas2ApplicationSummary>()

      assertThat(responseBody).noneMatch {
        excludedApplicationId == it.id
      }

      val uuids = responseBody.map { it.id }.toSet()
      assertThat(uuids).isEqualTo(submittedIds)
      assertThat(responseBody[0].latestStatusUpdate?.label).isEqualTo("Awaiting decision")
      assertThat(responseBody[0].latestStatusUpdate?.statusId).isEqualTo(UUID.fromString("c74c3e54-52d8-4aa2-86f6-05190985efee"))
    }

    @Test
    fun `returns submitted applications for user when assignmentType is ALLOCATED and page specified`() {
      val responseBody = webTestClient.get()
        .uri("/cas2/applications?assignmentType=ALLOCATED&page=1")
        .header("Authorization", "Bearer $jwtForUser")
        .header("X-Service-Name", ServiceName.cas2.value)
        .exchange()
        .expectStatus()
        .isOk
        .bodyAsListOfObjects<Cas2ApplicationSummary>()

      val uuids = responseBody.map { it.id }.toSet()
      assertThat(uuids).isEqualTo(submittedIds)
      assertThat(responseBody[0].latestStatusUpdate?.label).isEqualTo("Awaiting decision")
      assertThat(responseBody[0].latestStatusUpdate?.statusId).isEqualTo(UUID.fromString("c74c3e54-52d8-4aa2-86f6-05190985efee"))
    }
  }

  @Nested
  inner class GetToShow {

    @Nested
    inner class WhenCreatedBySameUser {
      // When the application requested was created by the logged-in user
      @Test
      fun `Get single in progress application returns 200 with correct body`() {
        givenACas2PomUser { userEntity, jwt ->
          givenAnOffender { offenderDetails, inmateDetails ->
            cas2ApplicationJsonSchemaRepository.deleteAll()

            val applicationEntity = cas2ApplicationEntityFactory.produceAndPersist {
              withCrn(offenderDetails.otherIds.crn)
              withCreatedByUser(userEntity)
              withData(
                data,
              )
            }

            val rawResponseBody = webTestClient.get()
              .uri("/cas2/applications/${applicationEntity.id}")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .returnResult<String>()
              .responseBody
              .blockFirst()

            val responseBody = objectMapper.readValue(
              rawResponseBody,
              Cas2Application::class.java,
            )

            assertThat(responseBody).matches {
              applicationEntity.id == it.id &&
                applicationEntity.crn == it.person.crn &&
                applicationEntity.createdAt.toInstant() == it.createdAt &&
                applicationEntity.createdByUser.id == it.createdBy.id &&
                applicationEntity.submittedAt?.toInstant() == it.submittedAt &&
                serializableToJsonNode(applicationEntity.data) == serializableToJsonNode(it.data)
            }
          }
        }
      }

      @Test
      fun `Get single application returns successfully when the offender cannot be fetched from the prisons API`() {
        givenACas2PomUser { userEntity, jwt ->
          val crn = "X1234"

          givenAnOffender(
            offenderDetailsConfigBlock = {
              withCrn(crn)
              withNomsNumber("ABC123")
            },
          ) { offenderDetails, _ ->
            val application = produceAndPersistBasicApplication(crn, userEntity)

            prisonAPIMockNotFoundInmateDetailsCall(offenderDetails.otherIds.nomsNumber!!)
            loadPreemptiveCacheForInmateDetails(offenderDetails.otherIds.nomsNumber!!)

            val rawResponseBody = webTestClient.get()
              .uri("/cas2/applications/${application.id}")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .returnResult<String>()
              .responseBody
              .blockFirst()

            val responseBody = objectMapper.readValue(
              rawResponseBody,
              Cas2Application::class.java,
            )

            assertThat(responseBody.person is FullPerson).isTrue

            assertThat(responseBody).matches {
              val person = it.person as FullPerson

              application.id == it.id &&
                application.crn == person.crn &&
                person.nomsNumber == null &&
                person.status == PersonStatus.unknown &&
                person.prisonName == null
            }
          }
        }
      }

      @Test
      fun `Get single submitted application returns 200 with timeline events`() {
        givenACas2PomUser { userEntity, jwt ->
          givenAnOffender { offenderDetails, _ ->
            val prisonName = "PRISON"
            val omuEmail = "test@test.com"
            val applicationEntity =
              setUpSubmittedApplicationWithTimeline(offenderDetails, userEntity, prisonName, omuEmail)

            val rawResponseBody = webTestClient.get()
              .uri("/cas2/applications/${applicationEntity.id}")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .returnResult<String>()
              .responseBody
              .blockFirst()

            val responseBody = objectMapper.readValue(
              rawResponseBody,
              Cas2Application::class.java,
            )

            assertThat(responseBody.assessment!!.statusUpdates).isEqualTo(emptyList<Cas2StatusUpdate>())
            assertThat(responseBody.allocatedPomEmailAddress).isEqualTo(userEntity.email)
            assertThat(responseBody.allocatedPomName).isEqualTo(userEntity.name)
            assertThat(responseBody.assignmentDate).isEqualTo(applicationEntity.currentAssignmentDate)
            assertThat(responseBody.currentPrisonName).isEqualTo(prisonName)
            assertThat(responseBody.isTransferredApplication).isFalse()
            assertThat(responseBody.omuEmailAddress).isEqualTo(omuEmail)

            assertThat(responseBody.timelineEvents!!.map { event -> event.label })
              .isEqualTo(listOf("Application submitted"))
          }
        }
      }

      @Test
      fun `Get single submitted application returns 200 with timeline events when application is transferred in`() {
        givenACas2PomUser { userEntity, jwt ->
          givenACas2PomUser { otherUser, otherJwt ->
            givenAnOffender { offenderDetails, _ ->
              val prisonName = "PRISON"
              val omuEmail = "test@test.com"
              val applicationEntity =
                setUpSubmittedApplicationWithTimeline(offenderDetails, userEntity, prisonName, omuEmail)

              val omuNew = offenderManagementUnitEntityFactory.produceAndPersist {
                withEmail("test2@test.com")
                withPrisonCode(otherUser.activeCaseloadId!!)
                withPrisonName("New Prison")
              }

              applicationEntity.createApplicationAssignment(prisonCode = otherUser.activeCaseloadId!!, allocatedPomUser = null)
              cas2ApplicationRepository.save(applicationEntity)

              val rawResponseBody = webTestClient.get()
                .uri("/cas2/applications/${applicationEntity.id}")
                .header("Authorization", "Bearer $otherJwt")
                .exchange()
                .expectStatus()
                .isOk
                .returnResult<String>()
                .responseBody
                .blockFirst()

              val responseBody = objectMapper.readValue(
                rawResponseBody,
                Cas2Application::class.java,
              )

              assertThat(responseBody.assessment!!.statusUpdates).isEqualTo(emptyList<Cas2StatusUpdate>())
              assertThat(responseBody.allocatedPomEmailAddress).isNull()
              assertThat(responseBody.allocatedPomName).isNull()
              assertThat(responseBody.assignmentDate).isEqualTo(applicationEntity.currentAssignmentDate)
              assertThat(responseBody.currentPrisonName).isEqualTo(omuNew.prisonName)
              assertThat(responseBody.isTransferredApplication).isTrue()
              assertThat(responseBody.omuEmailAddress).isEqualTo(omuNew.email)

              assertThat(responseBody.timelineEvents!!.map { event -> event.label })
                .isEqualTo(listOf("Prison transfer from ${userEntity.activeCaseloadId!!} to ${omuNew.prisonCode}", "Application submitted"))
            }
          }
        }
      }

      @Test
      fun `Get single submitted application returns 403 when application is transferred to different prison`() {
        givenACas2PomUser { userEntity, jwt ->
          givenACas2PomUser(
            mockCallToGetMe = false,
            nomisUserDetailsConfigBlock = {},
          ) { otherUser, otherJwt ->
            givenAnOffender { offenderDetails, _ ->
              val prisonName = "PRISON"
              val omuEmail = "test@test.com"
              val applicationEntity =
                setUpSubmittedApplicationWithTimeline(offenderDetails, userEntity, prisonName, omuEmail)

              offenderManagementUnitEntityFactory.produceAndPersist {
                withEmail("test2@test.com")
                withPrisonCode(otherUser.activeCaseloadId!!)
                withPrisonName("New Prison")
              }

              applicationEntity.createApplicationAssignment(
                prisonCode = otherUser.activeCaseloadId!!,
                allocatedPomUser = null,
              )
              cas2ApplicationRepository.save(applicationEntity)

              webTestClient.get()
                .uri("/cas2/applications/${applicationEntity.id}")
                .header("Authorization", "Bearer $jwt")
                .exchange()
                .expectStatus()
                .isForbidden
            }
          }
        }
      }
    }

    @Nested
    inner class WhenCreatedByDifferentUser {

      @Nested
      inner class WhenDifferentPrison {
        @Test
        fun `Get single submitted application is forbidden`() {
          givenACas2PomUser { userEntity, jwt ->
            givenAnOffender { offenderDetails, inmateDetails ->
              cas2ApplicationJsonSchemaRepository.deleteAll()

              val otherUser = nomisUserEntityFactory.produceAndPersist {
                withActiveCaseloadId("other_caseload")
              }

              val applicationEntity = cas2ApplicationEntityFactory.produceAndPersist {
                withCrn(offenderDetails.otherIds.crn)
                withSubmittedAt(OffsetDateTime.now())
                withCreatedByUser(otherUser)
                withData(
                  data,
                )
              }

              webTestClient.get()
                .uri("/cas2/applications/${applicationEntity.id}")
                .header("Authorization", "Bearer $jwt")
                .exchange()
                .expectStatus()
                .isForbidden
            }
          }
        }
      }

      @Nested
      inner class WhenSamePrison {
        @Test
        fun `Get single submitted application returns 200 with timeline events`() {
          givenACas2PomUser { userEntity, jwt ->
            givenAnOffender { offenderDetails, inmateDetails ->
              cas2ApplicationJsonSchemaRepository.deleteAll()

              val otherUser = nomisUserEntityFactory.produceAndPersist {
                withActiveCaseloadId(userEntity.activeCaseloadId!!)
              }

              val applicationEntity = cas2ApplicationEntityFactory.produceAndPersist {
                withCrn(offenderDetails.otherIds.crn)
                withCreatedByUser(otherUser)
                withSubmittedAt(OffsetDateTime.now().minusDays(1))
                withReferringPrisonCode(userEntity.activeCaseloadId!!)
              }

              cas2AssessmentEntityFactory.produceAndPersist {
                withApplication(applicationEntity)
              }

              val rawResponseBody = webTestClient.get()
                .uri("/cas2/applications/${applicationEntity.id}")
                .header("Authorization", "Bearer $jwt")
                .exchange()
                .expectStatus()
                .isOk
                .returnResult<String>()
                .responseBody
                .blockFirst()

              val responseBody = objectMapper.readValue(
                rawResponseBody,
                Cas2Application::class.java,
              )

              assertThat(responseBody.assessment!!.statusUpdates).isEqualTo(emptyList<Cas2StatusUpdate>())

              assertThat(responseBody.timelineEvents!!.map { event -> event.label })
                .isEqualTo(listOf("Application submitted"))
            }
          }
        }

        @Test
        fun `Get single unsubmitted application returns 403`() {
          givenACas2PomUser { userEntity, jwt ->
            givenAnOffender { offenderDetails, inmateDetails ->
              cas2ApplicationJsonSchemaRepository.deleteAll()

              val otherUser = nomisUserEntityFactory.produceAndPersist {
                withActiveCaseloadId(userEntity.activeCaseloadId!!)
              }

              val applicationEntity = cas2ApplicationEntityFactory.produceAndPersist {
                withCrn(offenderDetails.otherIds.crn)
                withCreatedByUser(otherUser)
                withReferringPrisonCode(userEntity.activeCaseloadId!!)
              }

              webTestClient.get()
                .uri("/cas2/applications/${applicationEntity.id}")
                .header("Authorization", "Bearer $jwt")
                .exchange()
                .expectStatus()
                .isForbidden
            }
          }
        }
      }
    }
  }

  @Nested
  inner class PostToCreate {

    @Nested
    inner class Cas2BailFields {
      @Test
      fun `Create new application for CAS-2 returns 201 with correct body - check that although we pass in prisonBail we will get out homeDetentionCurfew as this not written yet`() {
        givenACas2PomUser { userEntity, jwt ->
          givenAnOffender { offenderDetails, _ ->

            // BAIL-WIP we are passing in ApplicationOrigin.prisonBail BUT WE KNOW that we have not implemented
            // in the cas2 application service this to be set so it will default to homeDetentionCurfew/
            val result = webTestClient.post()
              .uri("/cas2/applications")
              .header("Authorization", "Bearer $jwt")
              .header("X-Service-Name", ServiceName.cas2.value)
              .bodyValue(
                NewApplication(
                  crn = offenderDetails.otherIds.crn,
                  applicationOrigin = ApplicationOrigin.prisonBail,
                ),
              )
              .exchange()
              .expectStatus()
              .isCreated
              .returnResult(Cas2Application::class.java)

            assertThat(result.responseHeaders["Location"]).anyMatch {
              it.matches(Regex("/cas2/applications/.+"))
            }

            assertThat(result.responseBody.blockFirst()).matches {
              it.person.crn == offenderDetails.otherIds.crn &&
                it.applicationOrigin == ApplicationOrigin.homeDetentionCurfew &&
                it.bailHearingDate == null
            }
          }
        }
      }
    }

    @Nested
    inner class PomUsers {
      @Test
      fun `Create new application for CAS-2 returns 201 with correct body and Location header`() {
        givenACas2PomUser { userEntity, jwt ->
          givenAnOffender { offenderDetails, _ ->

            val result = webTestClient.post()
              .uri("/cas2/applications")
              .header("Authorization", "Bearer $jwt")
              .header("X-Service-Name", ServiceName.cas2.value)
              .bodyValue(
                NewApplication(
                  crn = offenderDetails.otherIds.crn,
                ),
              )
              .exchange()
              .expectStatus()
              .isCreated
              .returnResult(Cas2Application::class.java)

            assertThat(result.responseHeaders["Location"]).anyMatch {
              it.matches(Regex("/cas2/applications/.+"))
            }

            assertThat(result.responseBody.blockFirst()).matches {
              it.person.crn == offenderDetails.otherIds.crn
            }
          }
        }
      }

      @Test
      fun `Create new application returns 404 when a person cannot be found`() {
        givenACas2PomUser { userEntity, jwt ->
          val crn = "X1234"

          webTestClient.post()
            .uri("/cas2/applications")
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
      fun `Create new application returns 403 when a person is restricted`() {
        givenACas2PomUser { userEntity, jwt ->

          val crn = "CRNRESTRICTED"
          val noms = "NOMSRESTRICTED"
          val offenderDetails =
            OffenderDetailsSummaryFactory()
              .withCrn(crn)
              .withNomsNumber(noms)
              .withCurrentRestriction(true)
              .withCurrentExclusion(true).produce()

          val caseDetail = offenderDetails.asCaseSummary()
          apDeliusContextMockCaseSummary(caseDetail)
          mockInmateDetailPrisonsApiCall(InmateDetail(caseDetail.nomsId!!, InmateStatus.IN, null))

          webTestClient.post()
            .uri("/cas2/applications")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              NewApplication(
                crn = caseDetail.crn,
              ),
            )
            .exchange()
            .expectStatus()
            .isForbidden
            .expectBody()
            .jsonPath("$.detail").isEqualTo("Offender $crn is Restricted.")
        }
      }
    }

    @Nested
    inner class LicenceCaseAdminUsers {
      @Test
      fun `Create new application for CAS-2 returns 201 with correct body and Location header`() {
        givenACas2LicenceCaseAdminUser { _, jwt ->
          givenAnOffender { offenderDetails, _ ->

            val result = webTestClient.post()
              .uri("/cas2/applications")
              .header("Authorization", "Bearer $jwt")
              .header("X-Service-Name", ServiceName.cas2.value)
              .bodyValue(
                NewApplication(
                  crn = offenderDetails.otherIds.crn,
                ),
              )
              .exchange()
              .expectStatus()
              .isCreated
              .returnResult(Cas2Application::class.java)

            assertThat(result.responseHeaders["Location"]).anyMatch {
              it.matches(Regex("/cas2/applications/.+"))
            }

            assertThat(result.responseBody.blockFirst()).matches {
              it.person.crn == offenderDetails.otherIds.crn
            }
          }
        }
      }

      @Test
      fun `Create new application returns 404 when a person cannot be found`() {
        givenACas2LicenceCaseAdminUser { _, jwt ->
          val crn = "X1234"

          webTestClient.post()
            .uri("/cas2/applications")
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
    }
  }

  @Nested
  inner class PutToUpdate {

    @Nested
    inner class BailFields {
      @Test
      fun `Update existing CAS2 application returns 200 with correct body and bail fields`() {
        givenACas2PomUser { submittingUser, jwt ->
          givenAnOffender { offenderDetails, _ ->
            val applicationId = UUID.fromString("22ceda56-98b2-411d-91cc-ace0ab8be872")

            cas2ApplicationEntityFactory.produceAndPersist {
              withCrn(offenderDetails.otherIds.crn)
              withId(applicationId)
              withCreatedByUser(submittingUser)
            }

            val resultBody = webTestClient.put()
              .uri("/cas2/applications/$applicationId")
              .header("Authorization", "Bearer $jwt")
              .bodyValue(
                UpdateCas2Application(
                  data = mapOf("thingId" to 123),
                  type = UpdateApplicationType.CAS2,
                ),
              )
              .exchange()
              .expectStatus()
              .isOk
              .returnResult(String::class.java)
              .responseBody
              .blockFirst()

            val result = objectMapper.readValue(resultBody, Cas2Application::class.java)

            assertThat(result.person.crn).isEqualTo(offenderDetails.otherIds.crn)
            assertThat(result.applicationOrigin).isEqualTo(ApplicationOrigin.homeDetentionCurfew)
            assertThat(result.bailHearingDate).isNull()
          }
        }
      }
    }

    @Nested
    inner class PomUsers {
      @Test
      fun `Update existing CAS2 application returns 200 with correct body`() {
        givenACas2PomUser { submittingUser, jwt ->
          givenAnOffender { offenderDetails, _ ->
            val applicationId = UUID.fromString("22ceda56-98b2-411d-91cc-ace0ab8be872")

            cas2ApplicationEntityFactory.produceAndPersist {
              withCrn(offenderDetails.otherIds.crn)
              withId(applicationId)
              withCreatedByUser(submittingUser)
            }

            val resultBody = webTestClient.put()
              .uri("/cas2/applications/$applicationId")
              .header("Authorization", "Bearer $jwt")
              .bodyValue(
                UpdateCas2Application(
                  data = mapOf("thingId" to 123),
                  type = UpdateApplicationType.CAS2,
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

    @Nested
    inner class LicenceCaseAdminUsers {
      @Test
      fun `Update existing CAS2 application returns 200 with correct body`() {
        givenACas2LicenceCaseAdminUser { submittingUser, jwt ->
          givenAnOffender { offenderDetails, _ ->
            val applicationId = UUID.fromString("22ceda56-98b2-411d-91cc-ace0ab8be872")

            cas2ApplicationEntityFactory.produceAndPersist {
              withCrn(offenderDetails.otherIds.crn)
              withId(applicationId)
              withCreatedByUser(submittingUser)
            }

            val resultBody = webTestClient.put()
              .uri("/cas2/applications/$applicationId")
              .header("Authorization", "Bearer $jwt")
              .bodyValue(
                UpdateCas2Application(
                  data = mapOf("thingId" to 123),
                  type = UpdateApplicationType.CAS2,
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

  private fun serializableToJsonNode(serializable: Any?): JsonNode {
    if (serializable == null) return NullNode.instance
    if (serializable is String) return objectMapper.readTree(serializable)

    return objectMapper.readTree(objectMapper.writeValueAsString(serializable))
  }

  private fun produceAndPersistBasicApplication(
    crn: String,
    userEntity: NomisUserEntity,
  ): Cas2ApplicationEntity {
    val application = cas2ApplicationEntityFactory.produceAndPersist {
      withCrn(crn)
      withCreatedByUser(userEntity)
      withData(
        data,
      )
    }

    return application
  }

  private fun setUpSubmittedApplicationWithTimeline(
    offenderDetails: OffenderDetailSummary,
    userEntity: NomisUserEntity,
    prisonName: String,
    omuEmail: String,
  ): Cas2ApplicationEntity {
    cas2ApplicationJsonSchemaRepository.deleteAll()

    val omu = offenderManagementUnitEntityFactory.produceAndPersist {
      withPrisonName(prisonName)
      withPrisonCode(userEntity.activeCaseloadId!!)
      withEmail(omuEmail)
    }

    val applicationEntity = cas2ApplicationEntityFactory.produceAndPersist {
      withCrn(offenderDetails.otherIds.crn)
      withCreatedByUser(userEntity)
      withReferringPrisonCode(omu.prisonCode)
      withSubmittedAt(OffsetDateTime.now().minusDays(1))
    }

    applicationEntity.createApplicationAssignment(prisonCode = omu.prisonCode, allocatedPomUser = userEntity)
    cas2ApplicationRepository.save(applicationEntity)

    cas2AssessmentEntityFactory.produceAndPersist {
      withApplication(applicationEntity)
    }

    return applicationEntity
  }
}
