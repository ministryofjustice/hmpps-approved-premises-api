package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.FurtherInformationRequestedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentClarificationNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.MigrationJobService

class Cas1InformationReceivedMigrationJobTest : IntegrationTestBase() {
  lateinit var applicationSchema: ApprovedPremisesApplicationJsonSchemaEntity
  lateinit var assessmentSchema: ApprovedPremisesAssessmentJsonSchemaEntity
  lateinit var user: UserEntity

  @Autowired
  lateinit var migrationJobService: MigrationJobService

  @BeforeEach
  fun setup() {
    val userArgs = `Given a User`()
    user = userArgs.first

    applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
      withPermissiveSchema()
    }

    assessmentSchema = approvedPremisesAssessmentJsonSchemaEntityFactory.produceAndPersist {
      withPermissiveSchema()
    }
  }

  @Test
  fun `All information received notes have an associated domain event created`() {
    val clarificationNotesWithoutDomainEvents = (0..6).map { createClarificationNote(false) }
    val clarificationNotesWithDomainEvents = (0..4).map { createClarificationNote(true) }

    migrationJobService.runMigrationJob(MigrationJobType.informationReceivedDomainEvents, 1)

    val domainEvents = domainEventRepository.findAll().map {
      it.toDomainEvent<FurtherInformationRequestedEnvelope>(objectMapper)
    }

    clarificationNotesWithoutDomainEvents.forEach {
      val updatedNote = assessmentClarificationNoteRepository.findByIdOrNull(it.id)!!
      assertThat(updatedNote.hasDomainEvent).isTrue()

      val domainEvent = domainEvents.find { domainEvent -> domainEvent.data.eventDetails.requestId == it.id }

      assertThat(domainEvent).isNotNull
    }

    // Confirm that the events that already have domain events have not been touched
    // In reality, they'd have domain events, but the absence of domain events here
    // confirms the job hasn't created a domain even for these clarification notes
    clarificationNotesWithDomainEvents.forEach {
      val domainEvent = domainEvents.find { domainEvent -> domainEvent.data.eventDetails.requestId == it.id }
      assertThat(domainEvent).isNull()
    }
  }

  private fun createClarificationNote(hasDomainEvent: Boolean): AssessmentClarificationNoteEntity {
    val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
      withApplicationSchema(applicationSchema)
      withCreatedByUser(user)
    }

    val assessment = approvedPremisesAssessmentEntityFactory.produceAndPersist {
      withApplication(application)
      withAllocatedToUser(user)
      withAssessmentSchema(assessmentSchema)
    }

    return assessmentClarificationNoteEntityFactory.produceAndPersist {
      withCreatedBy(user)
      withAssessment(assessment)
      withHasDomainEvent(hasDomainEvent)
    }
  }
}
