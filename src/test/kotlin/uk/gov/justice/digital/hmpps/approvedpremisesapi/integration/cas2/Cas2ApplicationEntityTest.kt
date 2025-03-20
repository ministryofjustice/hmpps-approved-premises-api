package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas2

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertTrue
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

        application.createApplicationAssignment(prisonCode = "LON1", allocatedPomUserId = null)
        application.createApplicationAssignment(prisonCode = "LON2", allocatedPomUserId = userEntity.id)
        application.createApplicationAssignment(prisonCode = "LON3", allocatedPomUserId = null)
        application.createApplicationAssignment(prisonCode = "LON4", allocatedPomUserId = null)

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
        assertThat(retrievedApplication.mostRecentPomUserId).isEqualTo(userEntity.id)
      }
    }
  }

  @Test
  fun `test that most recent pom id is given`() {
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
          allocatedPomUserId = userEntity.id,
        )

        val assignment2 = Cas2ApplicationAssignmentEntity(
          id = UUID.randomUUID(),
          application = application,
          prisonCode = "LON2",
          createdAt = OffsetDateTime.now(),
          allocatedPomUserId = null,
        )

        application.applicationAssignments.add(assignment1)
        application.applicationAssignments.add(assignment2)

        cas2ApplicationRepository.save(application)

        val retrievedApplication = cas2ApplicationRepository.findById(application.id).get()
        val assignments = retrievedApplication.applicationAssignments

        assertThat(assignments.size).isEqualTo(2)
        assertThat(assignment2.id).isEqualTo(assignments[0].id)
        assertThat(assignment1.id).isEqualTo(assignments[1].id)
        assertThat(retrievedApplication.mostRecentPomUserId).isEqualTo(assignment1.allocatedPomUserId)
      }
    }
  }
}
