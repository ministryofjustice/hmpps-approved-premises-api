package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas2

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a CAS2 User`
import java.time.OffsetDateTime
import java.util.*

class Cas2NotesTest : IntegrationTestBase() {

  @Nested
  inner class PostToCreate {
    @Test
    fun `create note returns 201`() {
      val applicationId = UUID.fromString("22ceda56-98b2-411d-91cc-ace0ab8be872")

      `Given a CAS2 User` { applicant, _ ->
        val jsonSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist()
        val application = cas2ApplicationEntityFactory.produceAndPersist {
          withId(applicationId)
          withCreatedByUser(applicant)
          withApplicationSchema(jsonSchema)
          withSubmittedAt(OffsetDateTime.now())
        }
      }
    }
  }
}
