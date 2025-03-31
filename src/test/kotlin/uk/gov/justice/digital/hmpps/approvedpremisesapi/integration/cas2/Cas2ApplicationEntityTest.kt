package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas2

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas2PomUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class Cas2ApplicationEntityTest : IntegrationTestBase() {

  @Test
  fun `applicationAssignments are returned sorted by createdAt descending`() {
    val applicationSchema = cas2ApplicationJsonSchemaEntityFactory.produceAndPersist {
      withAddedAt(OffsetDateTime.now())
      withId(UUID.randomUUID())
    }

    givenACas2PomUser { userEntity, jwt ->
      givenAnOffender { offenderDetails, _ ->
        val application = cas2ApplicationEntityFactory.produceAndPersist {
          withApplicationSchema(applicationSchema)
          withCreatedByUser(userEntity)
          withSubmittedAt(OffsetDateTime.now())
          withCrn(offenderDetails.otherIds.crn)
          withCreatedAt(OffsetDateTime.now().minusDays(28))
          withConditionalReleaseDate(LocalDate.now().plusDays(1))
        }

        application.createApplicationAssignment(prisonCode = "LON1", allocatedPomUser = null)
        application.createApplicationAssignment(prisonCode = "LON2", allocatedPomUser = userEntity)
        application.createApplicationAssignment(prisonCode = "LON3", allocatedPomUser = null)
        application.createApplicationAssignment(prisonCode = "LON4", allocatedPomUser = null)

        cas2ApplicationRepository.save(application)

        val retrievedApplication = cas2ApplicationRepository.findById(application.id).get()
        val assignments = retrievedApplication.applicationAssignments

        assertThat(assignments.size).isEqualTo(4)
        // verify list is returned sorted by created at descending
        assertTrue(assignments.map { it.createdAt }.zipWithNext { s1, s2 -> s2 <= s1 }.all { it })
        assertThat(assignments[0].prisonCode).isEqualTo("LON4")
        assertThat(assignments[1].prisonCode).isEqualTo("LON3")
        assertThat(assignments[2].prisonCode).isEqualTo("LON2")
        assertThat(assignments[3].prisonCode).isEqualTo("LON1")
        assertThat(retrievedApplication.currentPrisonCode).isEqualTo("LON4")
        assertThat(retrievedApplication.currentPomUserId).isEqualTo(null)
      }
    }
  }
}
