package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.NullNode
import com.github.tomakehurst.wiremock.client.WireMock
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.web.reactive.server.returnResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Reallocation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.InmateDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffUserDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffUserTeamMembershipFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ApplicationsTransformer
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class ApplicationTest : IntegrationTestBase() {
  @Autowired
  lateinit var applicationsTransformer: ApplicationsTransformer

  private val offenderDetails = OffenderDetailsSummaryFactory()
    .withCrn("CRN123")
    .withNomsNumber("NOMS321")
    .produce()

  private val otherOffenderDetails = OffenderDetailsSummaryFactory()
    .withCrn("OTHERCRN")
    .withNomsNumber("OTHERNOMS")
    .produce()

  @BeforeEach
  fun setup() {
    approvedPremisesApplicationJsonSchemaRepository.deleteAll()

    val inmateDetail = InmateDetailFactory()
      .withOffenderNo("NOMS321")
      .produce()

    val otherInmateDetail = InmateDetailFactory()
      .withOffenderNo("OTHERNOMS")
      .produce()

    mockOffenderDetailsCommunityApiCall(offenderDetails)
    mockInmateDetailPrisonsApiCall(inmateDetail)

    mockOffenderDetailsCommunityApiCall(otherOffenderDetails)
    mockInmateDetailPrisonsApiCall(otherInmateDetail)

    mockClientCredentialsJwtRequest("username", listOf("ROLE_COMMUNITY"), authSource = "delius")
  }

  @Test
  fun `Get all applications without JWT returns 401`() {
    webTestClient.get()
      .uri("/applications")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Get all applications returns 200 with correct body - when user does not have roles returns applications they created`() {
    approvedPremisesApplicationJsonSchemaRepository.deleteAll()

    val newestJsonSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
      withAddedAt(OffsetDateTime.parse("2022-09-21T12:45:00+01:00"))
      withSchema(
        """
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
      )
    }

    val olderJsonSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
      withAddedAt(OffsetDateTime.parse("2022-09-21T09:45:00+01:00"))
      withSchema(
        """
        {
          "${"\$schema"}": "https://json-schema.org/draft/2020-12/schema",
          "${"\$id"}": "https://example.com/product.schema.json",
          "title": "Thing",
          "description": "A thing",
          "type": "object",
          "properties": { }
        }
        """
      )
    }

    val username = "PROBATIONPERSON2"
    val otherUser = userEntityFactory.produceAndPersist()
    val user = userEntityFactory.produceAndPersist { withDeliusUsername(username) }

    val upToDateApplicationEntityCreatedByUser = approvedPremisesApplicationEntityFactory.produceAndPersist {
      withApplicationSchema(newestJsonSchema)
      withCrn(offenderDetails.otherIds.crn)
      withCreatedByUser(user)
      withData(
        """
          {
             "thingId": 123
          }
          """
      )
    }

    val outdatedApplicationEntityCreatedByUser = approvedPremisesApplicationEntityFactory.produceAndPersist {
      withApplicationSchema(olderJsonSchema)
      withCreatedByUser(user)
      withCrn(offenderDetails.otherIds.crn)
      withData("{}")
    }

    val outdatedApplicationEntityNotCreatedByUser = approvedPremisesApplicationEntityFactory.produceAndPersist {
      withApplicationSchema(olderJsonSchema)
      withCreatedByUser(otherUser)
      withCrn(otherOffenderDetails.otherIds.crn)
      withData("{}")
    }

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt(username)

    mockStaffUserInfoCommunityApiCall(
      StaffUserDetailsFactory()
        .withUsername(username)
        .withTeams(
          listOf(
            StaffUserTeamMembershipFactory()
              .withCode("TEAM1")
              .produce()
          )
        )
        .produce()
    )

    mockOffenderUserAccessCommunityApiCall(username, offenderDetails.otherIds.crn, false, false)

    val rawResponseBody = webTestClient.get()
      .uri("/applications")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .returnResult<String>()
      .responseBody
      .blockFirst()

    val responseBody = objectMapper.readValue(rawResponseBody, object : TypeReference<List<Application>>() {})

    assertThat(responseBody).anyMatch {
      outdatedApplicationEntityCreatedByUser.id == it.id &&
        outdatedApplicationEntityCreatedByUser.crn == it.person?.crn &&
        outdatedApplicationEntityCreatedByUser.createdAt.toInstant() == it.createdAt.toInstant() &&
        outdatedApplicationEntityCreatedByUser.createdByUser.id == it.createdByUserId &&
        outdatedApplicationEntityCreatedByUser.submittedAt?.toInstant() == it.submittedAt?.toInstant() &&
        serializableToJsonNode(outdatedApplicationEntityCreatedByUser.data) == serializableToJsonNode(it.data) &&
        olderJsonSchema.id == it.schemaVersion && it.outdatedSchema
    }

    assertThat(responseBody).anyMatch {
      upToDateApplicationEntityCreatedByUser.id == it.id &&
        upToDateApplicationEntityCreatedByUser.crn == it.person?.crn &&
        upToDateApplicationEntityCreatedByUser.createdAt.toInstant() == it.createdAt.toInstant() &&
        upToDateApplicationEntityCreatedByUser.createdByUser.id == it.createdByUserId &&
        upToDateApplicationEntityCreatedByUser.submittedAt?.toInstant() == it.submittedAt?.toInstant() &&
        serializableToJsonNode(upToDateApplicationEntityCreatedByUser.data) == serializableToJsonNode(it.data) &&
        newestJsonSchema.id == it.schemaVersion && !it.outdatedSchema
    }

    assertThat(responseBody).noneMatch {
      outdatedApplicationEntityNotCreatedByUser.id == it.id
    }
  }

  @ParameterizedTest
  @EnumSource(value = UserRole::class, names = [ "WORKFLOW_MANAGER", "ASSESSOR", "MATCHER", "MANAGER" ])
  fun `Get all applications returns 200 with correct body - when user has one of roles WORKFLOW_MANAGER, ASSESSOR, MATCHER, MANAGER returns all applications`(role: UserRole) {
    val username = "PROBATIONPERSON"
    val otherUser = userEntityFactory.produceAndPersist()
    val user = userEntityFactory.produceAndPersist { withDeliusUsername(username) }

    approvedPremisesApplicationJsonSchemaRepository.deleteAll()

    val newestJsonSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
      withAddedAt(OffsetDateTime.parse("2022-09-21T12:45:00+01:00"))
      withSchema(
        """
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
      )
    }

    val olderJsonSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
      withAddedAt(OffsetDateTime.parse("2022-09-21T09:45:00+01:00"))
      withSchema(
        """
        {
          "${"\$schema"}": "https://json-schema.org/draft/2020-12/schema",
          "${"\$id"}": "https://example.com/product.schema.json",
          "title": "Thing",
          "description": "A thing",
          "type": "object",
          "properties": { }
        }
        """
      )
    }

    val upToDateApplicationEntityCreatedByUser = approvedPremisesApplicationEntityFactory.produceAndPersist {
      withApplicationSchema(newestJsonSchema)
      withCrn(offenderDetails.otherIds.crn)
      withCreatedByUser(user)
      withData(
        """
          {
             "thingId": 123
          }
          """
      )
    }

    val outdatedApplicationEntityCreatedByUser = approvedPremisesApplicationEntityFactory.produceAndPersist {
      withApplicationSchema(olderJsonSchema)
      withCreatedByUser(user)
      withCrn(offenderDetails.otherIds.crn)
      withData("{}")
    }

    val outdatedApplicationEntityNotCreatedByUser = approvedPremisesApplicationEntityFactory.produceAndPersist {
      withApplicationSchema(olderJsonSchema)
      withCreatedByUser(otherUser)
      withCrn(otherOffenderDetails.otherIds.crn)
      withData("{}")
    }

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt(username)

    userRoleAssignmentEntityFactory.produceAndPersist {
      withUser(user)
      withRole(role)
    }

    mockStaffUserInfoCommunityApiCall(
      StaffUserDetailsFactory()
        .withUsername(username)
        .withTeams(
          listOf(
            StaffUserTeamMembershipFactory()
              .withCode("TEAM1")
              .produce()
          )
        )
        .produce()
    )

    mockOffenderUserAccessCommunityApiCall(username, offenderDetails.otherIds.crn, false, false)
    mockOffenderUserAccessCommunityApiCall(username, otherOffenderDetails.otherIds.crn, false, false)

    val rawResponseBody = webTestClient.get()
      .uri("/applications")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .returnResult<String>()
      .responseBody
      .blockFirst()

    val responseBody = objectMapper.readValue(rawResponseBody, object : TypeReference<List<Application>>() {})

    assertThat(responseBody).anyMatch {
      outdatedApplicationEntityCreatedByUser.id == it.id &&
        outdatedApplicationEntityCreatedByUser.crn == it.person?.crn &&
        outdatedApplicationEntityCreatedByUser.createdAt.toInstant() == it.createdAt.toInstant() &&
        outdatedApplicationEntityCreatedByUser.createdByUser.id == it.createdByUserId &&
        outdatedApplicationEntityCreatedByUser.submittedAt?.toInstant() == it.submittedAt?.toInstant() &&
        serializableToJsonNode(outdatedApplicationEntityCreatedByUser.data) == serializableToJsonNode(it.data) &&
        olderJsonSchema.id == it.schemaVersion && it.outdatedSchema
    }

    assertThat(responseBody).anyMatch {
      upToDateApplicationEntityCreatedByUser.id == it.id &&
        upToDateApplicationEntityCreatedByUser.crn == it.person?.crn &&
        upToDateApplicationEntityCreatedByUser.createdAt.toInstant() == it.createdAt.toInstant() &&
        upToDateApplicationEntityCreatedByUser.createdByUser.id == it.createdByUserId &&
        upToDateApplicationEntityCreatedByUser.submittedAt?.toInstant() == it.submittedAt?.toInstant() &&
        serializableToJsonNode(upToDateApplicationEntityCreatedByUser.data) == serializableToJsonNode(it.data) &&
        newestJsonSchema.id == it.schemaVersion && !it.outdatedSchema
    }

    assertThat(responseBody).anyMatch {
      outdatedApplicationEntityNotCreatedByUser.id == it.id &&
        outdatedApplicationEntityNotCreatedByUser.crn == it.person?.crn &&
        outdatedApplicationEntityNotCreatedByUser.createdAt.toInstant() == it.createdAt.toInstant() &&
        outdatedApplicationEntityNotCreatedByUser.createdByUser.id == it.createdByUserId &&
        outdatedApplicationEntityNotCreatedByUser.submittedAt?.toInstant() == it.submittedAt?.toInstant() &&
        serializableToJsonNode(outdatedApplicationEntityNotCreatedByUser.data) == serializableToJsonNode(it.data) &&
        olderJsonSchema.id == it.schemaVersion && it.outdatedSchema
    }
  }

  @Test
  fun `Get list of applications returns 500 when a person cannot be found`() {
    val crn = "X1234"
    val username = "PROBATIONPERSON"

    produceAndPersistBasicApplication(crn)
    mockOffenderDetailsCommunityApiCall404(crn)

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt(username)

    mockStaffUserInfoCommunityApiCall(
      StaffUserDetailsFactory()
        .withUsername(username)
        .withTeams(
          listOf(
            StaffUserTeamMembershipFactory()
              .withCode("TEAM1")
              .produce()
          )
        )
        .produce()
    )

    mockOffenderUserAccessCommunityApiCall(username, crn, false, false)

    webTestClient.get()
      .uri("/applications")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .is5xxServerError
      .expectBody()
      .jsonPath("$.detail").isEqualTo("Unable to get Person via crn: $crn")
  }

  @Test
  fun `Get list of applications returns 500 when a person has no NOMS number`() {
    val crn = "X1234"
    val username = "PROBATIONPERSON"

    produceAndPersistBasicApplication(crn)

    val offenderWithoutNomsNumber = OffenderDetailsSummaryFactory()
      .withCrn(crn)
      .withoutNomsNumber()
      .produce()

    mockStaffUserInfoCommunityApiCall(
      StaffUserDetailsFactory()
        .withUsername(username)
        .withTeams(
          listOf(
            StaffUserTeamMembershipFactory()
              .withCode("TEAM1")
              .produce()
          )
        )
        .produce()
    )

    mockOffenderUserAccessCommunityApiCall(username, crn, false, false)
    mockOffenderDetailsCommunityApiCall(offenderWithoutNomsNumber)

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt("PROBATIONPERSON")

    webTestClient.get()
      .uri("/applications")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .is5xxServerError
      .expectBody()
      .jsonPath("$.detail").isEqualTo("No nomsNumber present for CRN")
  }

  @Test
  fun `Get list of applications returns 500 when the person cannot be fetched from the prisons API`() {
    val crn = "X1234"
    val username = "PROBATIONPERSON"

    produceAndPersistBasicApplication(crn)

    val offenderDetails = OffenderDetailsSummaryFactory()
      .withCrn(crn)
      .withNomsNumber("ABC123")
      .produce()

    mockStaffUserInfoCommunityApiCall(
      StaffUserDetailsFactory()
        .withUsername(username)
        .withTeams(
          listOf(
            StaffUserTeamMembershipFactory()
              .withCode("TEAM1")
              .produce()
          )
        )
        .produce()
    )

    mockOffenderUserAccessCommunityApiCall(username, offenderDetails.otherIds.crn, false, false)

    mockOffenderDetailsCommunityApiCall(offenderDetails)
    offenderDetails.otherIds.nomsNumber?.let { mockInmateDetailPrisonsApiCall404(it) }

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt(username)

    webTestClient.get()
      .uri("/applications")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .is5xxServerError
      .expectBody()
      .jsonPath("$.detail").isEqualTo("Unable to get InmateDetail via crn: $crn")
  }

  @Test
  fun `Get single application without JWT returns 401`() {
    webTestClient.get()
      .uri("/applications/9b785e59-b85c-4be0-b271-d9ac287684b6")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Get single application returns 200 with correct body`() {
    val username = "PROBATIONPERSON"

    approvedPremisesApplicationJsonSchemaRepository.deleteAll()

    val newestJsonSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
      withAddedAt(OffsetDateTime.parse("2022-09-21T12:45:00+01:00"))
      withSchema(
        """
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
      )
    }

    val userEntity = userEntityFactory.produceAndPersist { withDeliusUsername("PROBATIONPERSON") }

    val applicationEntity = approvedPremisesApplicationEntityFactory.produceAndPersist {
      withApplicationSchema(newestJsonSchema)
      withCrn(offenderDetails.otherIds.crn)
      withCreatedByUser(userEntity)
      withData(
        """
          {
             "thingId": 123
          }
          """
      )
    }

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt(username)

    mockStaffUserInfoCommunityApiCall(
      StaffUserDetailsFactory()
        .withUsername(username)
        .withTeams(
          listOf(
            StaffUserTeamMembershipFactory()
              .withCode("TEAM1")
              .produce()
          )
        )
        .produce()
    )

    mockOffenderUserAccessCommunityApiCall(username, offenderDetails.otherIds.crn, false, false)

    val rawResponseBody = webTestClient.get()
      .uri("/applications/${applicationEntity.id}")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .returnResult<String>()
      .responseBody
      .blockFirst()

    val responseBody = objectMapper.readValue(rawResponseBody, Application::class.java)

    assertThat(responseBody).matches {
      applicationEntity.id == it.id &&
        applicationEntity.crn == it.person.crn &&
        applicationEntity.createdAt.toInstant() == it.createdAt.toInstant() &&
        applicationEntity.createdByUser.id == it.createdByUserId &&
        applicationEntity.submittedAt?.toInstant() == it.submittedAt?.toInstant() &&
        serializableToJsonNode(applicationEntity.data) == serializableToJsonNode(it.data) &&
        newestJsonSchema.id == it.schemaVersion && !it.outdatedSchema
    }
  }

  @Test
  fun `Get single application returns 403 when caller did not create application and user is not one of roles WORKFLOW_MANAGER, ASSESSOR, MATCHER, MANAGER`() {
    val crn = "X1234"
    val username = "PROBATIONPERSON2"
    val user = userEntityFactory.produceAndPersist {
      withDeliusUsername(username)
    }

    val application = produceAndPersistBasicApplication(crn)
    mockOffenderDetailsCommunityApiCall404(crn)

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt(username)

    mockStaffUserInfoCommunityApiCall(
      StaffUserDetailsFactory()
        .withUsername(username)
        .withTeams(
          listOf(
            StaffUserTeamMembershipFactory()
              .withCode("TEAM1")
              .produce()
          )
        )
        .produce()
    )

    mockOffenderUserAccessCommunityApiCall(username, offenderDetails.otherIds.crn, false, false)

    webTestClient.get()
      .uri("/applications/${application.id}")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Get single application returns 500 when a person has no NOMS number`() {
    val crn = "X1234"
    val username = "PROBATIONPERSON"

    val application = produceAndPersistBasicApplication(crn)

    val offenderWithoutNomsNumber = OffenderDetailsSummaryFactory()
      .withCrn(crn)
      .withoutNomsNumber()
      .produce()

    mockOffenderDetailsCommunityApiCall(offenderWithoutNomsNumber)

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt(username)

    mockStaffUserInfoCommunityApiCall(
      StaffUserDetailsFactory()
        .withUsername(username)
        .withTeams(
          listOf(
            StaffUserTeamMembershipFactory()
              .withCode("TEAM1")
              .produce()
          )
        )
        .produce()
    )

    mockOffenderUserAccessCommunityApiCall(username, crn, false, false)

    webTestClient.get()
      .uri("/applications/${application.id}")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .is5xxServerError
      .expectBody()
      .jsonPath("$.detail").isEqualTo("No nomsNumber present for CRN")
  }

  @Test
  fun `Get single application returns 500 when the person cannot be fetched from the prisons API`() {
    val crn = "X1234"
    val username = "PROBATIONPERSON"

    val application = produceAndPersistBasicApplication(crn)

    val offenderDetails = OffenderDetailsSummaryFactory()
      .withCrn(crn)
      .withNomsNumber("ABC123")
      .produce()

    mockOffenderDetailsCommunityApiCall(offenderDetails)
    offenderDetails.otherIds.nomsNumber?.let { mockInmateDetailPrisonsApiCall404(it) }

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt(username)

    mockStaffUserInfoCommunityApiCall(
      StaffUserDetailsFactory()
        .withUsername(username)
        .withTeams(
          listOf(
            StaffUserTeamMembershipFactory()
              .withCode("TEAM1")
              .produce()
          )
        )
        .produce()
    )

    mockOffenderUserAccessCommunityApiCall(username, offenderDetails.otherIds.crn, false, false)

    webTestClient.get()
      .uri("/applications/${application.id}")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .is5xxServerError
      .expectBody()
      .jsonPath("$.detail").isEqualTo("Unable to get InmateDetail via crn: $crn")
  }

  @Test
  fun `Get single application returns 200 with correct body, non-upgradable outdated application marked as such`() {
    val username = "PROBATIONPERSON"

    approvedPremisesApplicationJsonSchemaRepository.deleteAll()

    approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
      withAddedAt(OffsetDateTime.parse("2022-09-21T12:45:00+01:00"))
      withSchema(
        """
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
      )
    }

    val olderJsonSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
      withAddedAt(OffsetDateTime.parse("2022-09-21T09:45:00+01:00"))
      withSchema(
        """
        {
          "${"\$schema"}": "https://json-schema.org/draft/2020-12/schema",
          "${"\$id"}": "https://example.com/product.schema.json",
          "title": "Thing",
          "description": "A thing",
          "type": "object",
          "properties": { }
        }
        """
      )
    }

    val userEntity = userEntityFactory.produceAndPersist { withDeliusUsername("PROBATIONPERSON") }

    val nonUpgradableApplicationEntity = approvedPremisesApplicationEntityFactory.produceAndPersist {
      withApplicationSchema(olderJsonSchema)
      withCrn(offenderDetails.otherIds.crn)
      withCreatedByUser(userEntity)
      withData("{}")
    }

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt(username)

    mockStaffUserInfoCommunityApiCall(
      StaffUserDetailsFactory()
        .withUsername(username)
        .withTeams(
          listOf(
            StaffUserTeamMembershipFactory()
              .withCode("TEAM1")
              .produce()
          )
        )
        .produce()
    )

    mockOffenderUserAccessCommunityApiCall(username, offenderDetails.otherIds.crn, false, false)

    val rawResponseBody = webTestClient.get()
      .uri("/applications/${nonUpgradableApplicationEntity.id}")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .returnResult<String>()
      .responseBody
      .blockFirst()

    val responseBody = objectMapper.readValue(rawResponseBody, Application::class.java)

    assertThat(responseBody).matches {
      nonUpgradableApplicationEntity.id == it.id &&
        nonUpgradableApplicationEntity.crn == it.person?.crn &&
        nonUpgradableApplicationEntity.createdAt.toInstant() == it.createdAt.toInstant() &&
        nonUpgradableApplicationEntity.createdByUser.id == it.createdByUserId &&
        nonUpgradableApplicationEntity.submittedAt?.toInstant() == it.submittedAt?.toInstant() &&
        serializableToJsonNode(nonUpgradableApplicationEntity.data) == serializableToJsonNode(it.data) &&
        olderJsonSchema.id == it.schemaVersion && it.outdatedSchema
    }
  }

  @Test
  fun `Create new application without JWT returns 401`() {
    webTestClient.post()
      .uri("/applications")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Create new application returns 500 when a person cannot be found`() {
    val crn = "X1234"
    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt("PROBATIONPERSON")

    mockClientCredentialsJwtRequest(username = "username", authSource = "delius")
    mockOffenderDetailsCommunityApiCall404(crn)

    approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
      withAddedAt(OffsetDateTime.now())
      withId(UUID.randomUUID())
    }

    webTestClient.post()
      .uri("/applications")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        NewApplication(
          crn = crn
        )
      )
      .exchange()
      .expectStatus()
      .is5xxServerError
      .expectBody()
      .jsonPath("$.detail").isEqualTo("Unable to get Person via crn: $crn")
  }

  @Test
  fun `Create new application returns 500 when a person has no NOMS number`() {
    val crn = "X1234"
    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt("PROBATIONPERSON")

    mockClientCredentialsJwtRequest(username = "username", authSource = "delius")

    val offenderWithoutNomsNumber = OffenderDetailsSummaryFactory()
      .withCrn(crn)
      .withoutNomsNumber()
      .produce()

    mockOffenderDetailsCommunityApiCall(offenderWithoutNomsNumber)

    approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
      withAddedAt(OffsetDateTime.now())
      withId(UUID.randomUUID())
    }

    webTestClient.post()
      .uri("/applications")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        NewApplication(
          crn = crn
        )
      )
      .exchange()
      .expectStatus()
      .is5xxServerError
      .expectBody()
      .jsonPath("$.detail").isEqualTo("No nomsNumber present for CRN")
  }

  @Test
  fun `Create new application returns 500 when the person cannot be fetched from the prisons API`() {
    val crn = "X1234"
    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt("PROBATIONPERSON")

    mockClientCredentialsJwtRequest(username = "username", authSource = "delius")

    val offenderDetails = OffenderDetailsSummaryFactory()
      .withCrn(crn)
      .withNomsNumber("ABC123")
      .produce()

    mockOffenderDetailsCommunityApiCall(offenderDetails)
    offenderDetails.otherIds.nomsNumber?.let { mockInmateDetailPrisonsApiCall404(it) }

    webTestClient.post()
      .uri("/applications")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        NewApplication(
          crn = crn
        )
      )
      .exchange()
      .expectStatus()
      .is5xxServerError
      .expectBody()
      .jsonPath("$.detail").isEqualTo("Unable to get InmateDetail via crn: $crn")
  }

  @Test
  fun `Create new application returns 201 with correct body and Location header`() {
    val crn = offenderDetails.otherIds.crn
    val username = "PROBATIONPERSON"

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt(username)

    mockClientCredentialsJwtRequest(username = "username", authSource = "delius")
    mockOffenderDetailsCommunityApiCall(
      OffenderDetailsSummaryFactory()
        .withCrn(crn)
        .withDateOfBirth(LocalDate.parse("1985-05-05"))
        .withNomsNumber("NOMS321")
        .withFirstName("James")
        .withLastName("Someone")
        .withGender("Male")
        .withNationality("English")
        .withReligionOrBelief("Judaism")
        .withGenderIdentity("Prefer to self-describe")
        .withSelfDescribedGenderIdentity("This is a self described identity")
        .produce()
    )

    mockStaffUserInfoCommunityApiCall(
      StaffUserDetailsFactory()
        .withUsername("PROBATIONPERSON")
        .withForenames("Jim")
        .withSurname("Jimmerson")
        .withStaffIdentifier(5678)
        .withTeams(
          listOf(
            StaffUserTeamMembershipFactory()
              .withCode("TEAM1")
              .produce()
          )
        )
        .produce()
    )

    val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
      withAddedAt(OffsetDateTime.now())
      withId(UUID.randomUUID())
    }

    val result = webTestClient.post()
      .uri("/applications")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        NewApplication(
          crn = crn,
          convictionId = 123,
          deliusEventNumber = "1",
          offenceId = "789"
        )
      )
      .exchange()
      .expectStatus()
      .isCreated
      .returnResult(Application::class.java)

    assertThat(result.responseHeaders["Location"]).anyMatch {
      it.matches(Regex("/applications/.+"))
    }

    assertThat(result.responseBody.blockFirst()).matches {
      it.person.crn == crn &&
        it.schemaVersion == applicationSchema.id
    }
  }

  @Test
  fun `Update existing application returns 200 with correct body`() {
    val username = "PROBATIONPERSON"
    val crn = offenderDetails.otherIds.crn
    val applicationId = UUID.fromString("22ceda56-98b2-411d-91cc-ace0ab8be872")

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt(username)

    mockClientCredentialsJwtRequest(username = "username", authSource = "delius")
    mockOffenderDetailsCommunityApiCall(
      OffenderDetailsSummaryFactory()
        .withCrn(crn)
        .withDateOfBirth(LocalDate.parse("1985-05-05"))
        .withNomsNumber("NOMS321")
        .withFirstName("James")
        .withLastName("Someone")
        .withGender("Male")
        .withNationality("English")
        .withReligionOrBelief("Judaism")
        .withGenderIdentity("Prefer to self-describe")
        .withSelfDescribedGenderIdentity("This is a self described identity")
        .produce()
    )
    mockStaffUserInfoCommunityApiCall(
      StaffUserDetailsFactory()
        .withUsername("PROBATIONPERSON")
        .withForenames("Jim")
        .withSurname("Jimmerson")
        .withStaffIdentifier(5678)
        .produce()
    )

    val assessor = userEntityFactory.produceAndPersist { withDeliusUsername("ASSESSOR") }

    userRoleAssignmentEntityFactory.produceAndPersist {
      withUser(assessor)
      withRole(UserRole.ASSESSOR)
    }

    userQualificationAssignmentEntityFactory.produceAndPersist {
      withUser(assessor)
      withQualification(UserQualification.PIPE)
    }

    val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
      withAddedAt(OffsetDateTime.now())
      withId(UUID.randomUUID())
      withSchema(
        """
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
      )
    }

    val user = userEntityFactory.produceAndPersist {
      withDeliusUsername(username)
    }

    approvedPremisesApplicationEntityFactory.produceAndPersist {
      withCrn(crn)
      withId(applicationId)
      withApplicationSchema(applicationSchema)
      withCreatedByUser(user)
    }

    val resultBody = webTestClient.put()
      .uri("/applications/$applicationId")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        UpdateApplication(
          data = mapOf("thingId" to 123),
          isWomensApplication = false,
          isPipeApplication = true
        )
      )
      .exchange()
      .expectStatus()
      .isOk
      .returnResult(String::class.java)
      .responseBody
      .blockFirst()

    val result = objectMapper.readValue(resultBody, Application::class.java)

    assertThat(result.person.crn).isEqualTo(crn)
  }

  @Test
  fun `Submit application returns 200, creates and allocates an assessment`() {
    val username = "PROBATIONPERSON"
    val crn = offenderDetails.otherIds.crn
    val applicationId = UUID.fromString("22ceda56-98b2-411d-91cc-ace0ab8be872")

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt(username)

    mockClientCredentialsJwtRequest(username = "username", authSource = "delius")
    mockOffenderDetailsCommunityApiCall(
      OffenderDetailsSummaryFactory()
        .withCrn(crn)
        .withDateOfBirth(LocalDate.parse("1985-05-05"))
        .withNomsNumber("NOMS321")
        .withFirstName("James")
        .withLastName("Someone")
        .withGender("Male")
        .withNationality("English")
        .withReligionOrBelief("Judaism")
        .withGenderIdentity("Prefer to self-describe")
        .withSelfDescribedGenderIdentity("This is a self described identity")
        .produce()
    )
    mockStaffUserInfoCommunityApiCall(
      StaffUserDetailsFactory()
        .withUsername("PROBATIONPERSON")
        .withForenames("Jim")
        .withSurname("Jimmerson")
        .withStaffIdentifier(5678)
        .produce()
    )

    val assessor = userEntityFactory.produceAndPersist { withDeliusUsername("ASSESSOR") }

    userRoleAssignmentEntityFactory.produceAndPersist {
      withUser(assessor)
      withRole(UserRole.ASSESSOR)
    }

    userQualificationAssignmentEntityFactory.produceAndPersist {
      withUser(assessor)
      withQualification(UserQualification.PIPE)
    }

    userQualificationAssignmentEntityFactory.produceAndPersist {
      withUser(assessor)
      withQualification(UserQualification.WOMENS)
    }

    val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
      withAddedAt(OffsetDateTime.now())
      withId(UUID.randomUUID())
      withSchema(
        """
          {
            "${"\$schema"}": "https://json-schema.org/draft/2020-12/schema",
            "${"\$id"}": "https://example.com/product.schema.json",
            "title": "Thing",
            "description": "A thing",
            "type": "object",
            "properties": {
              "isWomensApplication": {
                "description": "whether this is a womens application",
                "type": "boolean"
              },
              "isPipeApplication": {
                "description": "whether this is a PIPE application",
                "type": "boolean"
              }
            },
            "required": [ "isWomensApplication", "isPipeApplication" ]
          }
        """
      )
      withIsPipeJsonLogicRule("""{"var": "isPipeApplication"}""")
      withIsWomensJsonLogicRule("""{"var": "isWomensApplication"}""")
    }

    val user = userEntityFactory.produceAndPersist {
      withDeliusUsername(username)
    }

    approvedPremisesApplicationEntityFactory.produceAndPersist {
      withCrn(crn)
      withId(applicationId)
      withApplicationSchema(applicationSchema)
      withCreatedByUser(user)
      withData(
        """
          {
             "isWomensApplication": true,
             "isPipeApplication": true
          }
        """
      )
    }

    webTestClient.post()
      .uri("/applications/$applicationId/submission")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        SubmitApplication(
          translatedDocument = mapOf("isWomensApplication" to true, "isPipeApplication" to true)
        )
      )
      .exchange()
      .expectStatus()
      .isOk

    val persistedApplication = approvedPremisesApplicationRepository.findByIdOrNull(applicationId)!! as ApprovedPremisesApplicationEntity

    assertThat(persistedApplication.isWomensApplication).isTrue
    assertThat(persistedApplication.isPipeApplication).isTrue

    val createdAssessment = assessmentRepository.findAll().first { it.application.id == applicationId }
    assertThat(createdAssessment.allocatedToUser.id).isEqualTo(assessor.id)
  }

  @Test
  fun `Reallocate application to different assessor without JWT returns 401`() {
    webTestClient.post()
      .uri("/applications/9c7abdf6-fd39-4670-9704-98a5bbfec95e/allocations")
      .bodyValue(
        Reallocation(
          userId = UUID.randomUUID()
        )
      )
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Reallocate application to different assessor without WORKFLOW_MANAGER role returns 403`() {
    val username = "PROBATIONPERSON"
    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt(username)

    mockStaffUserInfoCommunityApiCall(
      StaffUserDetailsFactory()
        .withUsername(username)
        .withForenames("Jim")
        .withSurname("Jimmerson")
        .withStaffIdentifier(5678)
        .produce()
    )

    webTestClient.post()
      .uri("/applications/9c7abdf6-fd39-4670-9704-98a5bbfec95e/allocations")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        Reallocation(
          userId = UUID.randomUUID()
        )
      )
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Reallocate application to different assessor returns 200, creates new assessment, deallocates old one`() {
    val requestUsername = "PROBATIONPERSON"
    val otherUsername = "OTHERUSER"
    val assigneeUsername = "ASSIGNEEUSER"
    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt(requestUsername)

    val requestUser = userEntityFactory.produceAndPersist {
      withDeliusUsername(requestUsername)
    }
    userRoleAssignmentEntityFactory.produceAndPersist {
      withUser(requestUser)
      withRole(UserRole.WORKFLOW_MANAGER)
    }

    mockStaffUserInfoCommunityApiCall(
      StaffUserDetailsFactory()
        .withUsername(requestUsername)
        .produce()
    )

    val otherUser = userEntityFactory.produceAndPersist {
      withDeliusUsername(otherUsername)
    }

    mockStaffUserInfoCommunityApiCall(
      StaffUserDetailsFactory()
        .withUsername(otherUsername)
        .produce()
    )

    val assigneeUser = userEntityFactory.produceAndPersist {
      withDeliusUsername(assigneeUsername)
    }

    mockStaffUserInfoCommunityApiCall(
      StaffUserDetailsFactory()
        .withUsername(assigneeUsername)
        .produce()
    )

    userRoleAssignmentEntityFactory.produceAndPersist {
      withUser(assigneeUser)
      withRole(UserRole.ASSESSOR)
    }

    val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist()
    val assessmentSchema = approvedPremisesAssessmentJsonSchemaEntityFactory.produceAndPersist()

    val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
      withCreatedByUser(otherUser)
      withApplicationSchema(applicationSchema)
    }

    val existingAssessment = assessmentEntityFactory.produceAndPersist {
      withApplication(application)
      withAllocatedToUser(otherUser)
      withAssessmentSchema(assessmentSchema)
    }

    webTestClient.post()
      .uri("/applications/${application.id}/allocations")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        Reallocation(
          userId = assigneeUser.id
        )
      )
      .exchange()
      .expectStatus()
      .isOk

    val assessments = assessmentRepository.findAll()

    assertThat(assessments.first { it.id == existingAssessment.id }.reallocatedAt).isNotNull
    assertThat(assessments).anyMatch { it.application.id == application.id && it.allocatedToUser.id == assigneeUser.id }
  }

  private fun serializableToJsonNode(serializable: Any?): JsonNode {
    if (serializable == null) return NullNode.instance
    if (serializable is String) return objectMapper.readTree(serializable)

    return objectMapper.readTree(objectMapper.writeValueAsString(serializable))
  }

  private fun mockOffenderDetailsCommunityApiCall404(crn: String) = wiremockServer.stubFor(
    WireMock.get(WireMock.urlEqualTo("/secure/offenders/crn/$crn"))
      .willReturn(
        WireMock.aResponse()
          .withHeader("Content-Type", "application/json")
          .withStatus(404)
      )
  )

  private fun mockInmateDetailPrisonsApiCall404(offenderNo: String) = wiremockServer.stubFor(
    WireMock.get(WireMock.urlEqualTo("/api/offenders/$offenderNo"))
      .willReturn(
        WireMock.aResponse()
          .withHeader("Content-Type", "application/json")
          .withStatus(404)
      )
  )

  private fun produceAndPersistBasicApplication(crn: String): ApplicationEntity {
    val jsonSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
      withAddedAt(OffsetDateTime.parse("2022-09-21T12:45:00+01:00"))
      withSchema(
        """
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
      )
    }

    val userEntity = userEntityFactory.produceAndPersist { withDeliusUsername("PROBATIONPERSON") }

    val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
      withApplicationSchema(jsonSchema)
      withCrn(crn)
      withCreatedByUser(userEntity)
      withData(
        """
          {
             "thingId": 123
          }
          """
      )
    }

    return application
  }
}
