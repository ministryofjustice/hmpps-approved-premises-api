package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.NullNode
import com.github.tomakehurst.wiremock.client.WireMock
import com.ninjasquad.springmockk.SpykBean
import io.mockk.every
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Reallocation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.InmateDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffUserDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import java.time.OffsetDateTime

class ReallocationAtomicTest : IntegrationTestBase() {
  @SpykBean
  lateinit var realAssessmentRepository: AssessmentRepository

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
  fun `Database exception after setting reallocated on original Assessment results in that change being rolled back`() {
    val requestUsername = "PROBATIONPERSON"
    val otherUsername = "OTHERUSER"
    val assigneeUsername = "ASSIGNEEUSER"
    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt(requestUsername)

    val requestUser = userEntityFactory.produceAndPersist {
      withDeliusUsername(requestUsername)
      withYieldedProbationRegion {
        probationRegionEntityFactory.produceAndPersist {
          withYieldedApArea { apAreaEntityFactory.produceAndPersist() }
        }
      }
    }
    userRoleAssignmentEntityFactory.produceAndPersist {
      withUser(requestUser)
      withRole(UserRole.WORKFLOW_MANAGER)
    }

    mockStaffUserInfoCommunityApiCall(
      StaffUserDetailsFactory()
        .withUsername(requestUsername)
        .produce(),
      false
    )

    val otherUser = userEntityFactory.produceAndPersist {
      withDeliusUsername(otherUsername)
      withYieldedProbationRegion {
        probationRegionEntityFactory.produceAndPersist {
          withYieldedApArea { apAreaEntityFactory.produceAndPersist() }
        }
      }
    }

    mockStaffUserInfoCommunityApiCall(
      StaffUserDetailsFactory()
        .withUsername(otherUsername)
        .produce(),
      false
    )

    val assigneeUser = userEntityFactory.produceAndPersist {
      withDeliusUsername(assigneeUsername)
      withYieldedProbationRegion {
        probationRegionEntityFactory.produceAndPersist {
          withYieldedApArea { apAreaEntityFactory.produceAndPersist() }
        }
      }
    }

    mockStaffUserInfoCommunityApiCall(
      StaffUserDetailsFactory()
        .withUsername(assigneeUsername)
        .produce(),
      false
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

    every { realAssessmentRepository.save(match { it.id != existingAssessment.id }) } throws RuntimeException("I am a database error")

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
      .is5xxServerError

    val assessments = assessmentRepository.findAll()

    Assertions.assertThat(assessments.first { it.id == existingAssessment.id }.reallocatedAt).isNull()
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

    val userEntity = userEntityFactory.produceAndPersist {
      withDeliusUsername("PROBATIONPERSON")
      withYieldedProbationRegion {
        probationRegionEntityFactory.produceAndPersist {
          withYieldedApArea { apAreaEntityFactory.produceAndPersist() }
        }
      }
    }

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
