package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentAcceptance
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentRejection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewClarificationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.InmateDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.AssessmentTransformer
import java.time.OffsetDateTime

class AssessmentTest : IntegrationTestBase() {
  @Autowired
  lateinit var assessmentTransformer: AssessmentTransformer

  private val offenderDetails = OffenderDetailsSummaryFactory()
    .withCrn("CRN123")
    .withNomsNumber("NOMS321")
    .produce()

  @BeforeEach
  fun setup() {
    approvedPremisesAssessmentJsonSchemaRepository.deleteAll()

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

    val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
      withPermissiveSchema()
    }

    val assessmentSchema = approvedPremisesAssessmentJsonSchemaEntityFactory.produceAndPersist {
      withPermissiveSchema()
      withAddedAt(OffsetDateTime.now())
    }

    val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
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

    val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
      withPermissiveSchema()
    }

    val assessmentSchema = approvedPremisesAssessmentJsonSchemaEntityFactory.produceAndPersist {
      withPermissiveSchema()
      withAddedAt(OffsetDateTime.now())
    }

    val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
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

  @Test
  fun `Accept assessment without JWT returns 401`() {
    webTestClient.post()
      .uri("/assessments/6966902f-9b7e-4fc7-96c4-b54ec02d16c9/acceptance")
      .bodyValue(AssessmentAcceptance(document = "{}"))
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Accept assessment returns 200, persists decision`() {
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

    val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
      withPermissiveSchema()
    }

    val assessmentSchema = approvedPremisesAssessmentJsonSchemaEntityFactory.produceAndPersist {
      withPermissiveSchema()
      withAddedAt(OffsetDateTime.now())
    }

    val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
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

    webTestClient.post()
      .uri("/assessments/${assessment.id}/acceptance")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(AssessmentAcceptance(document = mapOf("document" to "value")))
      .exchange()
      .expectStatus()
      .isOk

    val persistedAssessment = assessmentRepository.findByIdOrNull(assessment.id)!!
    assertThat(persistedAssessment.decision).isEqualTo(AssessmentDecision.ACCEPTED)
    assertThat(persistedAssessment.document).isEqualTo("{\"document\":\"value\"}")
    assertThat(persistedAssessment.submittedAt).isNotNull
  }

  @Test
  fun `Reject assessment without JWT returns 401`() {
    webTestClient.post()
      .uri("/assessments/6966902f-9b7e-4fc7-96c4-b54ec02d16c9/rejection")
      .bodyValue(AssessmentRejection(document = "{}", rejectionRationale = "reasoning"))
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Reject assessment returns 200, persists decision`() {
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

    val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
      withPermissiveSchema()
    }

    val assessmentSchema = approvedPremisesAssessmentJsonSchemaEntityFactory.produceAndPersist {
      withPermissiveSchema()
      withAddedAt(OffsetDateTime.now())
    }

    val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
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

    webTestClient.post()
      .uri("/assessments/${assessment.id}/rejection")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(AssessmentRejection(document = mapOf("document" to "value"), rejectionRationale = "reasoning"))
      .exchange()
      .expectStatus()
      .isOk

    val persistedAssessment = assessmentRepository.findByIdOrNull(assessment.id)!!
    assertThat(persistedAssessment.decision).isEqualTo(AssessmentDecision.REJECTED)
    assertThat(persistedAssessment.document).isEqualTo("{\"document\":\"value\"}")
    assertThat(persistedAssessment.submittedAt).isNotNull
  }

  @Test
  fun `Create clarification note returns 200 with correct body`() {
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

    val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
      withPermissiveSchema()
    }

    val assessmentSchema = approvedPremisesAssessmentJsonSchemaEntityFactory.produceAndPersist {
      withPermissiveSchema()
    }

    val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
      withCrn("CRN123")
      withCreatedByUser(user)
      withApplicationSchema(applicationSchema)
    }

    val assessment = assessmentEntityFactory.produceAndPersist {
      withAllocatedToUser(user)
      withApplication(application)
      withAssessmentSchema(assessmentSchema)
    }

    webTestClient.post()
      .uri("/assessments/${assessment.id}/notes")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        NewClarificationNote(
          query = "some text"
        )
      )
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.query").isEqualTo("some text")
  }
}
