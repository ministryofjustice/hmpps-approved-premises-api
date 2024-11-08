package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ContentDisposition
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.DocumentFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.DocumentFromDeliusApiFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.GroupedDocumentsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextMockSuccessfulDocumentDownloadCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextMockSuccessfulDocumentsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.communityAPIMockSuccessfulDocumentDownloadCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.communityAPIMockSuccessfulDocumentsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.APDeliusDocument
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.DocumentTransformer
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

class ApplicationDocumentsTest : InitialiseDatabasePerClassTestBase() {
  @Autowired
  lateinit var documentTransformer: DocumentTransformer

  @Test
  fun `Get application documents without JWT returns 401`() {
    webTestClient.get()
      .uri("/applications/1e63735c-64dd-4904-a808-fc11e4feded3/documents")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Get application documents - with get-documents-from-ap-delius feature-flag off - returns 200`() {
    givenAUser { userEntity, jwt ->
      givenAnOffender { offenderDetails, _ ->
        val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
          withCreatedByUser(userEntity)
          withCrn(offenderDetails.otherIds.crn)
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
              .produce(),
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
              .produce(),
          )
          .produce()

        communityAPIMockSuccessfulDocumentsCall(offenderDetails.otherIds.crn, groupedDocuments)

        webTestClient.get()
          .uri("/applications/${application.id}/documents")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              documentTransformer.transformToApi(groupedDocuments),
            ),
          )
      }
    }
  }

  @Test
  fun `Get application documents - with get-documents-from-ap-delius feature-flag on - returns 200`() {
    mockFeatureFlagService.setFlag("get-documents-from-ap-delius", true)

    givenAUser { userEntity, jwt ->
      givenAnOffender { offenderDetails, _ ->
        val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
          withCreatedByUser(userEntity)
          withCrn(offenderDetails.otherIds.crn)
          withConvictionId(12345)
          withApplicationSchema(approvedPremisesApplicationJsonSchemaRepository.findAll().first())
        }

        val convictionLevelDocId = UUID.randomUUID()
        val documents = stubDocumentsFromDelius(convictionLevelDocId)

        apDeliusContextMockSuccessfulDocumentsCall(offenderDetails.otherIds.crn, documents)

        webTestClient.get()
          .uri("/applications/${application.id}/documents")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(objectMapper.writeValueAsString(documentTransformer.transformToApi(documents)))
      }
    }
  }

  @Test
  fun `Download document - with get-documents-from-ap-delius feature-flag off - returns 404 when not found in documents meta data`() {
    givenAUser { userEntity, jwt ->
      givenAnOffender { offenderDetails, _ ->
        val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
          withCreatedByUser(userEntity)
          withCrn(offenderDetails.otherIds.crn)
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
              .produce(),
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
              .produce(),
          )
          .produce()

        communityAPIMockSuccessfulDocumentsCall(offenderDetails.otherIds.crn, groupedDocuments)

        webTestClient.get()
          .uri("/documents/${application.crn}/ace0baaf-d7ee-4ea0-9010-da588387c880")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isNotFound
      }
    }
  }

  @Test
  fun `Download document - with get-documents-from-ap-delius feature-flag off - returns 200 with correct body and headers`() {
    givenAUser { userEntity, jwt ->
      givenAnOffender { offenderDetails, _ ->
        val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
          withCreatedByUser(userEntity)
          withCrn(offenderDetails.otherIds.crn)
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
              .produce(),
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
              .produce(),
          )
          .produce()

        communityAPIMockSuccessfulDocumentsCall(offenderDetails.otherIds.crn, groupedDocuments)

        val fileContents = this::class.java.classLoader.getResourceAsStream("mock_document.txt").readAllBytes()

        communityAPIMockSuccessfulDocumentDownloadCall(offenderDetails.otherIds.crn, "457af8a5-82b1-449a-ad03-032b39435865", fileContents)

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
    }
  }

  @Test
  fun `Download document - with get-documents-from-ap-delius feature-flag on - returns 404 when not found in documents meta data`() {
    mockFeatureFlagService.setFlag("get-documents-from-ap-delius", true)

    givenAUser { userEntity, jwt ->
      givenAnOffender { offenderDetails, _ ->
        val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
          withCreatedByUser(userEntity)
          withCrn(offenderDetails.otherIds.crn)
          withConvictionId(12345)
          withApplicationSchema(approvedPremisesApplicationJsonSchemaRepository.findAll().first())
        }

        val convictionLevelDocId = UUID.randomUUID()
        val documents = stubDocumentsFromDelius(convictionLevelDocId)

        apDeliusContextMockSuccessfulDocumentsCall(offenderDetails.otherIds.crn, documents)

        val notFoundDocId = UUID.randomUUID()
        webTestClient.get()
          .uri("/documents/${application.crn}/$notFoundDocId")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isNotFound
      }
    }
  }

  @Test
  fun `Download document - with get-documents-from-ap-delius feature-flag on - returns 200 with correct body and headers`() {
    mockFeatureFlagService.setFlag("get-documents-from-ap-delius", true)

    givenAUser { userEntity, jwt ->
      givenAnOffender { offenderDetails, _ ->
        val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
          withCreatedByUser(userEntity)
          withCrn(offenderDetails.otherIds.crn)
          withConvictionId(12345)
          withApplicationSchema(approvedPremisesApplicationJsonSchemaRepository.findAll().first())
        }
        val convictionLevelDocId = UUID.randomUUID()
        val documents = stubDocumentsFromDelius(convictionLevelDocId)
        val docFileContents = this::class.java.classLoader.getResourceAsStream("mock_document.txt").readAllBytes()

        apDeliusContextMockSuccessfulDocumentsCall(offenderDetails.otherIds.crn, documents)
        apDeliusContextMockSuccessfulDocumentDownloadCall(offenderDetails.otherIds.crn, convictionLevelDocId, docFileContents)

        val result = webTestClient.get()
          .uri("/documents/${application.crn}/$convictionLevelDocId")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectHeader()
          .contentDisposition(ContentDisposition.parse("attachment; filename=\"conviction_level_doc.pdf\""))
          .expectBody()
          .returnResult()

        assertThat(result.responseBody).isEqualTo(docFileContents)
      }
    }
  }

  private fun stubDocumentsFromDelius(convictionLevelDocId: UUID): List<APDeliusDocument> {
    return listOf(
      DocumentFromDeliusApiFactory()
        .withId(UUID.randomUUID().toString())
        .withDescription("Offender level doc description")
        .withLevel("LEVEL-1")
        .withEventNumber("2")
        .withFilename("offender_level_doc.pdf")
        .withTypeCode("TYPE-1")
        .withTypeDescription("Type 1 Description")
        .withDateSaved(LocalDateTime.parse("2024-03-18T06:00:00").atZone(ZoneId.systemDefault()))
        .withDateCreated(LocalDateTime.parse("2024-03-02T15:20:00").atZone(ZoneId.systemDefault()))
        .produce(),
      DocumentFromDeliusApiFactory()
        .withId(convictionLevelDocId.toString())
        .withDescription("Conviction level doc description")
        .withLevel("LEVEL-2")
        .withEventNumber("1")
        .withFilename("conviction_level_doc.pdf")
        .withTypeCode("TYPE-2")
        .withTypeDescription("Type 2 Description")
        .withDateSaved(LocalDateTime.parse("2024-10-05T13:12:00").atZone(ZoneId.systemDefault()))
        .withDateCreated(LocalDateTime.parse("2024-10-02T10:40:00").atZone(ZoneId.systemDefault()))
        .produce(),
    )
  }
}
