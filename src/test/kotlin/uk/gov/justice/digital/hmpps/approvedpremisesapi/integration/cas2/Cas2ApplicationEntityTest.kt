package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas2

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationOrigin
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
          allocatedPomUser = userEntity,
        )

        val assignment2 = Cas2ApplicationAssignmentEntity(
          id = UUID.randomUUID(),
          application = application,
          prisonCode = "LON2",
          createdAt = OffsetDateTime.now(),
          allocatedPomUser = null,
        )

        application.applicationAssignments.add(assignment1)
        application.applicationAssignments.add(assignment2)

        cas2ApplicationRepository.save(application)

        val retrievedApplication = cas2ApplicationRepository.findById(application.id).get()
        val assignments = retrievedApplication.applicationAssignments

        assertThat(assignments.size).isEqualTo(2)
        assertThat(assignment2.id).isEqualTo(assignments[0].id)
        assertThat(assignment1.id).isEqualTo(assignments[1].id)
      }
    }
  }

  @Nested
  inner class BailFields {

    @Test
    fun `test application origin persists`() {
      val applicationSchema = cas2ApplicationJsonSchemaEntityFactory.produceAndPersist {
        withAddedAt(OffsetDateTime.now())
        withId(UUID.randomUUID())
      }

      givenACas2PomUser { userEntity, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val applicationHDC = cas2ApplicationEntityFactory.produceAndPersist {
            withCreatedByUser(userEntity)
            withApplicationSchema(applicationSchema)
          }
          val applicationCourt = cas2ApplicationEntityFactory.produceAndPersist {
            withCreatedByUser(userEntity)
            withApplicationOrigin(ApplicationOrigin.courtBail)
            withApplicationSchema(applicationSchema)
          }
          val applicationPrison = cas2ApplicationEntityFactory.produceAndPersist {
            withCreatedByUser(userEntity)
            withApplicationOrigin(ApplicationOrigin.prisonBail)
            withApplicationSchema(applicationSchema)
          }

          cas2ApplicationRepository.save(applicationHDC)
          cas2ApplicationRepository.save(applicationCourt)
          cas2ApplicationRepository.save(applicationPrison)

          val retrievedApplicationHDC = cas2ApplicationRepository.findById(applicationHDC.id).get()
          val retrievedApplicationCourt = cas2ApplicationRepository.findById(applicationCourt.id).get()
          val retrievedApplicationPrison = cas2ApplicationRepository.findById(applicationPrison.id).get()

          assertThat(retrievedApplicationHDC.applicationOrigin).isEqualTo(ApplicationOrigin.homeDetentionCurfew)
          assertThat(retrievedApplicationCourt.applicationOrigin).isEqualTo(ApplicationOrigin.courtBail)
          assertThat(retrievedApplicationPrison.applicationOrigin).isEqualTo(ApplicationOrigin.prisonBail)
        }
      }
    }

    @Test
    fun `test bail hearing is persisted if present`() {
      val applicationSchema = cas2ApplicationJsonSchemaEntityFactory.produceAndPersist {
        withAddedAt(OffsetDateTime.now())
        withId(UUID.randomUUID())
      }

      val now = OffsetDateTime.now().toLocalDate()

      givenACas2PomUser { userEntity, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val applicationNoBailHearingDate = cas2ApplicationEntityFactory.produceAndPersist {
            withCreatedByUser(userEntity)
            withApplicationSchema(applicationSchema)
          }

          val applicationBailHearingDate = cas2ApplicationEntityFactory.produceAndPersist {
            withCreatedByUser(userEntity)
            withApplicationSchema(applicationSchema)
            withBailHearingDate(now)
          }

          cas2ApplicationRepository.save(applicationNoBailHearingDate)
          cas2ApplicationRepository.save(applicationBailHearingDate)

          val retrievedApplicationNoBailHearingDate = cas2ApplicationRepository.findById(applicationNoBailHearingDate.id).get()
          val retrievedApplicationBailHearingDate = cas2ApplicationRepository.findById(applicationBailHearingDate.id).get()

          assertThat(retrievedApplicationNoBailHearingDate.bailHearingDate).isNull()
          assertThat(retrievedApplicationBailHearingDate.bailHearingDate).isEqualTo(now)
        }
      }
    }
  }

  @Nested
  inner class BailFields {

    @Test
    fun `test application origin persists`() {
      val applicationSchema = cas2ApplicationJsonSchemaEntityFactory.produceAndPersist {
        withAddedAt(OffsetDateTime.now())
        withId(UUID.randomUUID())
      }

      givenACas2PomUser { userEntity, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val applicationHDC = cas2ApplicationEntityFactory.produceAndPersist {
            withCreatedByUser(userEntity)
            withApplicationSchema(applicationSchema)
          }
          val applicationCourt = cas2ApplicationEntityFactory.produceAndPersist {
            withCreatedByUser(userEntity)
            withApplicationOrigin(ApplicationOrigin.courtBail)
            withApplicationSchema(applicationSchema)
          }
          val applicationPrison = cas2ApplicationEntityFactory.produceAndPersist {
            withCreatedByUser(userEntity)
            withApplicationOrigin(ApplicationOrigin.prisonBail)
            withApplicationSchema(applicationSchema)
          }

          cas2ApplicationRepository.save(applicationHDC)
          cas2ApplicationRepository.save(applicationCourt)
          cas2ApplicationRepository.save(applicationPrison)

          val retrievedApplicationHDC = cas2ApplicationRepository.findById(applicationHDC.id).get()
          val retrievedApplicationCourt = cas2ApplicationRepository.findById(applicationCourt.id).get()
          val retrievedApplicationPrison = cas2ApplicationRepository.findById(applicationPrison.id).get()

          assertThat(retrievedApplicationHDC.applicationOrigin).isEqualTo(ApplicationOrigin.homeDetentionCurfew)
          assertThat(retrievedApplicationCourt.applicationOrigin).isEqualTo(ApplicationOrigin.courtBail)
          assertThat(retrievedApplicationPrison.applicationOrigin).isEqualTo(ApplicationOrigin.prisonBail)
        }
      }
    }

    @Test
    fun `test bail hearing is persisted if present`() {
      val applicationSchema = cas2ApplicationJsonSchemaEntityFactory.produceAndPersist {
        withAddedAt(OffsetDateTime.now())
        withId(UUID.randomUUID())
      }

      val now = OffsetDateTime.now().toLocalDate()

      givenACas2PomUser { userEntity, jwt ->
        givenAnOffender { offenderDetails, _ ->
          val applicationNoBailHearingDate = cas2ApplicationEntityFactory.produceAndPersist {
            withCreatedByUser(userEntity)
            withApplicationSchema(applicationSchema)
          }

          val applicationBailHearingDate = cas2ApplicationEntityFactory.produceAndPersist {
            withCreatedByUser(userEntity)
            withApplicationSchema(applicationSchema)
            withBailHearingDate(now)
          }

          cas2ApplicationRepository.save(applicationNoBailHearingDate)
          cas2ApplicationRepository.save(applicationBailHearingDate)

          val retrievedApplicationNoBailHearingDate = cas2ApplicationRepository.findById(applicationNoBailHearingDate.id).get()
          val retrievedApplicationBailHearingDate = cas2ApplicationRepository.findById(applicationBailHearingDate.id).get()

          assertThat(retrievedApplicationNoBailHearingDate.bailHearingDate).isNull()
          assertThat(retrievedApplicationBailHearingDate.bailHearingDate).isEqualTo(now)
        }
      }
    }
  }
}
