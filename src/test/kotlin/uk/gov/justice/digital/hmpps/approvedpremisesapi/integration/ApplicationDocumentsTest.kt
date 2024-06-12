package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ContentDisposition
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.DocumentFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.GroupedDocumentsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Offender`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.communityApiMockSuccessfulDocumentDownloadCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.communityApiMockSuccessfulDocumentsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.DocumentTransformer
import java.time.LocalDateTime

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
  fun `Get application documents returns 200`() {
    `Given a User` { userEntity, jwt ->
      `Given an Offender` { offenderDetails, _ ->
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

        communityApiMockSuccessfulDocumentsCall(offenderDetails.otherIds.crn, groupedDocuments)

        webTestClient.get()
          .uri("/applications/${application.id}/documents")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              documentTransformer.transformToApiUnfiltered(groupedDocuments),
            ),
          )
      }
    }
  }

  @Test
  fun `Get application documents for Temporary Accommodation returns 200`() {
    `Given a User` { userEntity, jwt ->
      `Given an Offender` { offenderDetails, _ ->
        val application = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
          withCreatedByUser(userEntity)
          withCrn(offenderDetails.otherIds.crn)
          withConvictionId(12345)
          withApplicationSchema(temporaryAccommodationApplicationJsonSchemaRepository.findAll().first())
          withProbationRegion(userEntity.probationRegion)
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

        communityApiMockSuccessfulDocumentsCall(offenderDetails.otherIds.crn, groupedDocuments)

        webTestClient.get()
          .uri("/applications/${application.id}/documents")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              documentTransformer.transformToApiUnfiltered(groupedDocuments),
            ),
          )
      }
    }
  }

  @Test
  fun `Download document returns 404 when not found in documents meta data`() {
    `Given a User` { userEntity, jwt ->
      `Given an Offender` { offenderDetails, _ ->
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

        communityApiMockSuccessfulDocumentsCall(offenderDetails.otherIds.crn, groupedDocuments)

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
  fun `Download document returns 200 with correct body and headers`() {
    `Given a User` { userEntity, jwt ->
      `Given an Offender` { offenderDetails, _ ->
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

        communityApiMockSuccessfulDocumentsCall(offenderDetails.otherIds.crn, groupedDocuments)

        val fileContents = this::class.java.classLoader.getResourceAsStream("mock_document.txt").readAllBytes()

        communityApiMockSuccessfulDocumentDownloadCall(offenderDetails.otherIds.crn, "457af8a5-82b1-449a-ad03-032b39435865", fileContents)

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
}
