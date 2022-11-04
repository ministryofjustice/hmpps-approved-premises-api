package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.InmateDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.JsonSchemaType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.AssessmentTransformer

class AssessmentTest : IntegrationTestBase() {
  @Autowired
  lateinit var assessmentTransformer: AssessmentTransformer

  private val offenderDetails = OffenderDetailsSummaryFactory()
    .withCrn("CRN123")
    .withNomsNumber("NOMS321")
    .produce()

  @BeforeEach
  fun setup() {
    jsonSchemaRepository.deleteAll()

    val inmateDetail = InmateDetailFactory()
      .withOffenderNo("NOMS321")
      .produce()

    mockOffenderDetailsCommunityApiCall(offenderDetails)
    mockInmateDetailPrisonsApiCall(inmateDetail)

    mockClientCredentialsJwtRequest("username", listOf("ROLE_COMMUNITY"), authSource = "delius")
  }

  @Test
  fun `Get all assessments without JWT returns 401`() {
    webTestClient.get()
      .uri("/assessments")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Get all assessments returns 200 with correct body`() {
    val user = userEntityFactory.produceAndPersist {
      withDeliusUsername("PROBATIONPERSON")
    }

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt("PROBATIONPERSON")

    val offenderDetails = OffenderDetailsSummaryFactory()
      .withCrn("CRN123")
      .withNomsNumber("NOMS321")
      .produce()

    mockOffenderDetailsCommunityApiCall(offenderDetails)

    val inmateDetails = InmateDetailFactory()
      .withOffenderNo("NOMS321")
      .produce()

    mockInmateDetailPrisonsApiCall(inmateDetails)

    val applicationSchema = jsonSchemaEntityFactory.produceAndPersist {
      withType(JsonSchemaType.APPLICATION)
      withPermissiveSchema()
    }

    val assessmentSchema = jsonSchemaEntityFactory.produceAndPersist {
      withType(JsonSchemaType.ASSESSMENT)
      withPermissiveSchema()
    }

    val application = applicationEntityFactory.produceAndPersist {
      withCrn("CRN123")
      withCreatedByUser(user)
      withApplicationSchema(applicationSchema)
    }

    val assessment = assessmentEntityFactory.produceAndPersist {
      withAllocatedToUser(user)
      withApplication(application)
      withAssessmentSchema(assessmentSchema)
    }

    assessment.schemaUpToDate = true

    webTestClient.get()
      .uri("/assessments")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(
        objectMapper.writeValueAsString(
          listOf(
            assessmentTransformer.transformJpaToApi(assessment, offenderDetails, inmateDetails)
          )
        )
      )
  }

  @Test
  fun `Get assessment by ID without JWT returns 401`() {
    webTestClient.get()
      .uri("/assessments/6966902f-9b7e-4fc7-96c4-b54ec02d16c9")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Get assessment by ID returns 200 with correct body`() {
    val user = userEntityFactory.produceAndPersist {
      withDeliusUsername("PROBATIONPERSON")
    }

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt("PROBATIONPERSON")

    val offenderDetails = OffenderDetailsSummaryFactory()
      .withCrn("CRN123")
      .withNomsNumber("NOMS321")
      .produce()

    mockOffenderDetailsCommunityApiCall(offenderDetails)

    val inmateDetails = InmateDetailFactory()
      .withOffenderNo("NOMS321")
      .produce()

    mockInmateDetailPrisonsApiCall(inmateDetails)

    val applicationSchema = jsonSchemaEntityFactory.produceAndPersist {
      withType(JsonSchemaType.APPLICATION)
      withPermissiveSchema()
    }

    val assessmentSchema = jsonSchemaEntityFactory.produceAndPersist {
      withType(JsonSchemaType.ASSESSMENT)
      withPermissiveSchema()
    }

    val application = applicationEntityFactory.produceAndPersist {
      withCrn("CRN123")
      withCreatedByUser(user)
      withApplicationSchema(applicationSchema)
    }

    val assessment = assessmentEntityFactory.produceAndPersist {
      withAllocatedToUser(user)
      withApplication(application)
      withAssessmentSchema(assessmentSchema)
    }

    assessment.schemaUpToDate = true

    webTestClient.get()
      .uri("/assessments/${assessment.id}")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(
        objectMapper.writeValueAsString(
          assessmentTransformer.transformJpaToApi(assessment, offenderDetails, inmateDetails)
        )
      )
  }
}
