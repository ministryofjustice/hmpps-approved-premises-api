package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import com.github.tomakehurst.wiremock.client.WireMock
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ContentDisposition
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.DocumentFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.GroupedDocumentsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.InmateDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.GroupedDocuments
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.DocumentTransformer
import java.time.LocalDateTime

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
        DocumentFactory()
          .withId("b0df5ec4-5685-4b02-8a95-91b6da80156f")
          .withDocumentName("offender_level_doc.pdf")
          .withTypeCode("TYPE-1")
          .withTypeDescription("Type 1 Description")
          .withCreatedAt(LocalDateTime.parse("2022-12-07T11:40:00"))
          .withExtendedDescription("Extended Description 1")
          .produce()
      )
      .withConvictionLevelDocument(
        "12345",
        DocumentFactory()
          .withId("457af8a5-82b1-449a-ad03-032b39435865")
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

  @Test
  fun `Download document returns 404 when not found in documents meta data`() {
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
        DocumentFactory()
          .withId("b0df5ec4-5685-4b02-8a95-91b6da80156f")
          .withDocumentName("offender_level_doc.pdf")
          .withTypeCode("TYPE-1")
          .withTypeDescription("Type 1 Description")
          .withCreatedAt(LocalDateTime.parse("2022-12-07T11:40:00"))
          .withExtendedDescription("Extended Description 1")
          .produce()
      )
      .withConvictionLevelDocument(
        "12345",
        DocumentFactory()
          .withId("457af8a5-82b1-449a-ad03-032b39435865")
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
      .uri("/documents/${application.crn}/ace0baaf-d7ee-4ea0-9010-da588387c880")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `Download document returns 200 with correct body and headers`() {
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
        DocumentFactory()
          .withId("b0df5ec4-5685-4b02-8a95-91b6da80156f")
          .withDocumentName("offender_level_doc.pdf")
          .withTypeCode("TYPE-1")
          .withTypeDescription("Type 1 Description")
          .withCreatedAt(LocalDateTime.parse("2022-12-07T11:40:00"))
          .withExtendedDescription("Extended Description 1")
          .produce()
      )
      .withConvictionLevelDocument(
        "12345",
        DocumentFactory()
          .withId("457af8a5-82b1-449a-ad03-032b39435865")
          .withDocumentName("conviction_level_doc.pdf")
          .withTypeCode("TYPE-2")
          .withTypeDescription("Type 2 Description")
          .withCreatedAt(LocalDateTime.parse("2022-12-07T10:40:00"))
          .withExtendedDescription("Extended Description 2")
          .produce()
      )
      .produce()

    val offenderDetails = OffenderDetailsSummaryFactory()
      .withCrn("CRN123")
      .withNomsNumber("NOMS321")
      .produce()

    mockClientCredentialsJwtRequest()
    mockOffenderDetailsCommunityApiCall(offenderDetails)
    mockCommunityApiDocumentsCall("CRN123", groupedDocuments)

    val fileContents = this::class.java.classLoader.getResourceAsStream("mock_document.txt").readAllBytes()

    mockCommunityApiDocumentDownloadCall("CRN123", "457af8a5-82b1-449a-ad03-032b39435865", fileContents)

    val result = webTestClient.get()
      .uri("/documents/${application.crn}/457af8a5-82b1-449a-ad03-032b39435865")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectHeader()
      .contentDisposition(ContentDisposition.parse("attachment; filename=\"conviction_level_doc.pdf\""))
      .expectBody()
      .returnResult()

    assertThat(result.responseBody).isEqualTo(fileContents)
  }

  private fun mockCommunityApiDocumentDownloadCall(crn: String, documentId: String, fileContents: ByteArray) = wiremockServer.stubFor(
    WireMock.get(WireMock.urlEqualTo("/secure/offenders/crn/$crn/documents/$documentId"))
      .willReturn(
        WireMock.aResponse()
          .withHeader("Content-Type", "application/octet-stream")
          .withStatus(200)
          .withBody(fileContents)
      )
  )

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
