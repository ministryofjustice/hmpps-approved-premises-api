package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import com.github.tomakehurst.wiremock.client.WireMock
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ConvictionLevelDocumentFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.GroupedDocumentsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.InmateDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderLevelDocumentFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.GroupedDocuments
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.DocumentTransformer
import java.time.LocalDateTime
import java.util.UUID

class ApplicationDocumentsTest : IntegrationTestBase() {
  @Autowired
  lateinit var documentTransformer: DocumentTransformer

  private val offenderDetails = OffenderDetailsSummaryFactory()
    .withCrn("CRN123")
    .withNomsNumber("NOMS321")
    .produce()

  private val inmateDetail = InmateDetailFactory()
    .withOffenderNo("NOMS321")
    .produce()

  @Test
  fun `Get application documents without JWT returns 401`() {
    webTestClient.get()
      .uri("/applications/1e63735c-64dd-4904-a808-fc11e4feded3/documents")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Get application documents where application was not created by user returns 403`() {
    val user = userEntityFactory.produceAndPersist { withDeliusUsername("PROBATIONPERSON") }
    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt("PROBATIONPERSON")
    val owner = userEntityFactory.produceAndPersist { withDeliusUsername("DIFFERENTPROBATIONPERSON") }

    val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
      withCreatedByUser(owner)
      withCrn("CRN123")
      withApplicationSchema(approvedPremisesApplicationJsonSchemaRepository.findAll().first())
    }

    webTestClient.get()
      .uri("/applications/${application.id}/documents")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Get application documents returns 200`() {
    val user = userEntityFactory.produceAndPersist { withDeliusUsername("PROBATIONPERSON") }
    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt("PROBATIONPERSON")

    val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
      withCreatedByUser(user)
      withCrn("CRN123")
      withConvictionId(12345)
      withApplicationSchema(approvedPremisesApplicationJsonSchemaRepository.findAll().first())
    }

    val groupedDocuments = GroupedDocumentsFactory()
      .withOffenderLevelDocument(
        OffenderLevelDocumentFactory()
          .withId(UUID.fromString("b0df5ec4-5685-4b02-8a95-91b6da80156f").toString())
          .withDocumentName("offender_level_doc.pdf")
          .withTypeCode("TYPE-1")
          .withTypeDescription("Type 1 Description")
          .withCreatedAt(LocalDateTime.parse("2022-12-07T11:40:00"))
          .withExtendedDescription("Extended Description 1")
          .produce()
      )
      .withConvictionLevelDocument(
        "12345",
        ConvictionLevelDocumentFactory()
          .withId(UUID.fromString("457af8a5-82b1-449a-ad03-032b39435865").toString())
          .withDocumentName("conviction_level_doc.pdf")
          .withTypeCode("TYPE-2")
          .withTypeDescription("Type 2 Description")
          .withCreatedAt(LocalDateTime.parse("2022-12-07T10:40:00"))
          .withExtendedDescription("Extended Description 2")
          .produce()
      )
      .produce()

    mockClientCredentialsJwtRequest()
    mockCommunityApiDocumentsCall("CRN123", groupedDocuments)

    webTestClient.get()
      .uri("/applications/${application.id}/documents")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(
        objectMapper.writeValueAsString(
          documentTransformer.transformToApi(groupedDocuments, 12345)
        )
      )
  }

  private fun mockCommunityApiDocumentsCall(crn: String, groupedDocuments: GroupedDocuments) = wiremockServer.stubFor(
    WireMock.get(WireMock.urlEqualTo("/secure/offenders/crn/$crn/documents/grouped"))
      .willReturn(
        WireMock.aResponse()
          .withHeader("Content-Type", "application/json")
          .withStatus(200)
          .withBody(
            objectMapper.writeValueAsString(groupedDocuments)
          )
      )
  )
}
