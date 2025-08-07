package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.APDeliusDocument
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.APDeliusDocumentFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.InitialiseDatabasePerClassTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextMockSuccessfulDocumentsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.DocumentTransformer
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

class Cas1ApplicationDocumentsTest : InitialiseDatabasePerClassTestBase() {
  @Autowired
  lateinit var documentTransformer: DocumentTransformer

  @Nested
  inner class GetDocuments {

    @Test
    fun `Get application documents without JWT returns 401`() {
      webTestClient.get()
        .uri("/cas1/applications/1e63735c-64dd-4904-a808-fc11e4feded3/documents")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Get application documents - returns 200`() {
      givenAUser { userEntity, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
            withCreatedByUser(userEntity)
            withCrn(offenderDetails.otherIds.crn)
            withConvictionId(12345)
          }

          val convictionLevelDocId = UUID.randomUUID()
          val documents = stubDocumentsFromDelius(convictionLevelDocId)

          apDeliusContextMockSuccessfulDocumentsCall(offenderDetails.otherIds.crn, documents)

          webTestClient.get()
            .uri("/cas1/applications/${application.id}/documents")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .json(objectMapper.writeValueAsString(documentTransformer.transformToApi(documents)))
        }
      }
    }
  }

  private fun stubDocumentsFromDelius(convictionLevelDocId: UUID): List<APDeliusDocument> = listOf(
    APDeliusDocumentFactory()
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
    APDeliusDocumentFactory()
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
