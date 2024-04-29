package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import com.fasterxml.jackson.core.type.TypeReference
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.returnResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationTimelineNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentClarificationNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ApplicationTimelineNoteTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ApplicationTimelineTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.AssessmentClarificationNoteTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.domainevents.DomainEventSummaryImpl
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.createDomainEventOfType

class ApplicationTimelineTest : InitialiseDatabasePerClassTestBase() {
  @Autowired
  lateinit var applicationTimelineTransformer: ApplicationTimelineTransformer

  @Autowired
  lateinit var applicationTimelineNoteTransformer: ApplicationTimelineNoteTransformer

  @Autowired
  lateinit var assessmentClarificationNoteTransformer: AssessmentClarificationNoteTransformer

  lateinit var user: UserEntity
  lateinit var application: ApprovedPremisesApplicationEntity
  lateinit var assessment: ApprovedPremisesAssessmentEntity
  lateinit var domainEvents: List<DomainEventEntity>
  lateinit var notes: List<ApplicationTimelineNoteEntity>
  lateinit var clarificationNotes: List<AssessmentClarificationNoteEntity>

  @BeforeAll
  fun setup() {
    val userArgs = `Given a User`()

    user = userArgs.first

    val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
      withPermissiveSchema()
    }

    val assessmentSchema = approvedPremisesAssessmentJsonSchemaEntityFactory.produceAndPersist {
      withPermissiveSchema()
    }

    application = approvedPremisesApplicationEntityFactory.produceAndPersist {
      withApplicationSchema(applicationSchema)
      withCreatedByUser(user)
    }

    val otherApplication = approvedPremisesApplicationEntityFactory.produceAndPersist {
      withApplicationSchema(applicationSchema)
      withCreatedByUser(user)
    }

    assessment = approvedPremisesAssessmentEntityFactory.produceAndPersist {
      withApplication(application)
      withAllocatedToUser(user)
      withAssessmentSchema(assessmentSchema)
    }

    val otherAssessment = approvedPremisesAssessmentEntityFactory.produceAndPersist {
      withApplication(otherApplication)
      withAllocatedToUser(user)
      withAssessmentSchema(assessmentSchema)
    }

    domainEvents = DomainEventType.entries.filter { it.name.startsWith("APPROVED_PREMISES") }.map {
      createDomainEvent(it, otherApplication, otherAssessment, user)
      return@map createDomainEvent(it, application, assessment, user)
    }

    notes = createTimelineNotes(application, 5)
    createTimelineNotes(otherApplication, 3)

    clarificationNotes = createClarificationNotes(assessment, user, 4)
    createClarificationNotes(otherAssessment, user, 2)
  }

  @Test
  fun `Get application timeline without JWT returns 401`() {
    webTestClient.get()
      .uri("/applications/${application.id}/timeline")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Get application timeline returns 501 not implemented when not approved premises service`() {
    `Given a User`(roles = listOf(UserRole.CAS1_ADMIN)) { _, jwt ->
      webTestClient.get()
        .uri("/applications/${application.id}/timeline")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .exchange()
        .expectStatus()
        .isEqualTo(HttpStatus.NOT_IMPLEMENTED)
    }
  }

  @Test
  fun `Get application timeline returns all expected items`() {
    `Given a User` { _, jwt ->
      val rawResponseBody = webTestClient.get()
        .uri("/applications/${application.id}/timeline")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.approvedPremises.value)
        .exchange()
        .expectStatus()
        .isOk
        .returnResult<String>()
        .responseBody
        .blockFirst()

      val responseBody =
        objectMapper.readValue(
          rawResponseBody,
          object : TypeReference<List<TimelineEvent>>() {},
        )

      val expectedItems = mutableListOf<TimelineEvent>()

      expectedItems.addAll(
        domainEvents.map {
          applicationTimelineTransformer.transformDomainEventSummaryToTimelineEvent(
            DomainEventSummaryImpl(
              it.id.toString(),
              it.type,
              it.occurredAt,
              it.applicationId,
              it.assessmentId,
              null,
              null,
              null,
              user,
            ),
          )
        },
      )
      expectedItems.addAll(notes.map { applicationTimelineNoteTransformer.transformToTimelineEvents(it) })
      expectedItems.addAll(clarificationNotes.map { assessmentClarificationNoteTransformer.transformToTimelineEvent(it) })

      assertThat(responseBody.count()).isEqualTo(expectedItems.count())
      assertThat(responseBody).hasSameElementsAs(expectedItems)
    }
  }

  private fun createDomainEvent(
    type: DomainEventType,
    applicationEntity: ApprovedPremisesApplicationEntity,
    assessmentEntity: ApprovedPremisesAssessmentEntity,
    userEntity: UserEntity,
  ): DomainEventEntity {
    val data = if (type == DomainEventType.APPROVED_PREMISES_ASSESSMENT_INFO_REQUESTED) {
      val clarificationNote = assessmentClarificationNoteEntityFactory.produceAndPersist {
        withAssessment(assessmentEntity)
        withCreatedBy(userEntity)
      }

      createDomainEventOfType(type, clarificationNote.id)
    } else {
      createDomainEventOfType(type)
    }

    return domainEventFactory.produceAndPersist {
      withApplicationId(applicationEntity.id)
      withData(data)
      withTriggeredByUserId(userEntity.id)
    }
  }

  private fun createTimelineNotes(applicationEntity: ApprovedPremisesApplicationEntity, count: Int) = applicationTimelineNoteEntityFactory.produceAndPersistMultiple(count) {
    withApplicationId(applicationEntity.id)
  }.toMutableList()

  private fun createClarificationNotes(
    assessmentEntity: ApprovedPremisesAssessmentEntity,
    userEntity: UserEntity,
    count: Int,
  ) = assessmentClarificationNoteEntityFactory.produceAndPersistMultiple(count) {
    withAssessment(assessmentEntity)
    withCreatedBy(userEntity)
  }.toMutableList()
}
