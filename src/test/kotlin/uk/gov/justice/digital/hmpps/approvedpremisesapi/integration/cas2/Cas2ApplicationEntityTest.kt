package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas2

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas2PomUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationAssignmentEntity
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class Cas2ApplicationEntityTest : IntegrationTestBase() {

  @Test
  fun `test SQLOrder on applicationAssignments`() {
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

        val assignment1 = Cas2ApplicationAssignmentEntity(
          id = UUID.randomUUID(),
          application = application,
          prisonCode = "LON1",
          createdAt = OffsetDateTime.now().minusDays(3),
          allocatedPomUserId = null,
        )

        val assignment2 = Cas2ApplicationAssignmentEntity(
          id = UUID.randomUUID(),
          application = application,
          prisonCode = "LON2",
          createdAt = OffsetDateTime.now(),
          allocatedPomUserId = userEntity.id,
        )

        val assignment3 = Cas2ApplicationAssignmentEntity(
          id = UUID.randomUUID(),
          application = application,
          prisonCode = "LON3",
          createdAt = OffsetDateTime.now().minusDays(1),
          allocatedPomUserId = null,
        )
        val assignment4 = Cas2ApplicationAssignmentEntity(
          id = UUID.randomUUID(),
          application = application,
          prisonCode = "LON4",
          createdAt = OffsetDateTime.now().minusDays(2),
          allocatedPomUserId = null,
        )

        application.applicationAssignments.add(assignment1)
        application.applicationAssignments.add(assignment2)
        application.applicationAssignments.add(assignment3)
        application.applicationAssignments.add(assignment4)

        cas2ApplicationRepository.save(application)

        val retrievedApplication = cas2ApplicationRepository.findById(application.id).get()
        val assignments = retrievedApplication.applicationAssignments

        assertThat(assignments.size).isEqualTo(4)
        assertThat(assignment2.id).isEqualTo(assignments[0].id)
        assertThat(assignment3.id).isEqualTo(assignments[1].id)
        assertThat(assignment4.id).isEqualTo(assignments[2].id)
        assertThat(assignment1.id).isEqualTo(assignments[3].id)
        assertThat(retrievedApplication.currentPrisonCode).isEqualTo(assignment2.prisonCode)
        assertThat(retrievedApplication.currentPomUserId).isEqualTo(assignment2.allocatedPomUserId)
      }
    }
  }
}
