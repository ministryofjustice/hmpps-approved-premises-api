package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

import com.fasterxml.jackson.core.type.TypeReference
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.returnResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1TimelineEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.InitialiseDatabasePerClassTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationTimelineNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventCas
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ApplicationTimelineNoteTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1ApplicationTimelineTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.DomainEventSummaryImpl
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.Cas1DomainEventsFactory
import java.time.OffsetDateTime

class Cas1ApplicationTimelineTest : InitialiseDatabasePerClassTestBase() {
  @Autowired
  lateinit var cas1applicationTimelineTransformer: Cas1ApplicationTimelineTransformer

  @Autowired
  lateinit var applicationTimelineNoteTransformer: ApplicationTimelineNoteTransformer

  lateinit var user: UserEntity
  lateinit var application: ApprovedPremisesApplicationEntity
  lateinit var assessment: ApprovedPremisesAssessmentEntity
  lateinit var domainEvents: List<DomainEventEntity>
  lateinit var notes: List<ApplicationTimelineNoteEntity>

  @BeforeAll
  fun setup() {
    val userArgs = givenAUser()

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

    domainEvents = DomainEventType.entries.filter { it.cas == DomainEventCas.CAS1 }.map {
      createDomainEvent(it, otherApplication, otherAssessment, user)
      return@map createDomainEvent(it, application, assessment, user)
    }

    notes = createTimelineNotes(application, 5, isDeleted = false)
    createTimelineNotes(application, 2, isDeleted = true)
    createTimelineNotes(otherApplication, 3, isDeleted = false)
  }

  @Test
  fun `Get application timeline without JWT returns 401`() {
    webTestClient.get()
      .uri("/cas1/applications/${application.id}/timeline")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Get application timeline returns all expected items`() {
    givenAUser { _, jwt ->
      val rawResponseBody = webTestClient.get()
        .uri("/cas1/applications/${application.id}/timeline")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .returnResult<String>()
        .responseBody
        .blockFirst()

      val responseBody =
        objectMapper.readValue(
          rawResponseBody,
          object : TypeReference<List<Cas1TimelineEvent>>() {},
        )

      val expectedItems = mutableListOf<Cas1TimelineEvent>()

      expectedItems.addAll(
        domainEvents.map {
          cas1applicationTimelineTransformer.transformDomainEventSummaryToTimelineEvent(
            DomainEventSummaryImpl(
              it.id.toString(),
              it.type,
              it.occurredAt,
              it.applicationId,
              it.assessmentId,
              null,
              null,
              null,
              null,
              it.triggerSource,
              user,
            ),
          )
        },
      )
      expectedItems.addAll(notes.map { applicationTimelineNoteTransformer.transformToCas1TimelineEvents(it) })

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
    val domainEventsFactory = Cas1DomainEventsFactory(objectMapper)

    val data = if (type == DomainEventType.APPROVED_PREMISES_ASSESSMENT_INFO_REQUESTED) {
      val clarificationNote = assessmentClarificationNoteEntityFactory.produceAndPersist {
        withAssessment(assessmentEntity)
        withCreatedBy(userEntity)
      }

      domainEventsFactory.createEnvelopeForLatestSchemaVersion(type = type, requestId = clarificationNote.id)
    } else {
      domainEventsFactory.createEnvelopeForLatestSchemaVersion(type = type)
    }

    return domainEventFactory.produceAndPersist {
      withApplicationId(applicationEntity.id)
      withData(data)
      withTriggeredByUserId(userEntity.id)
      withTriggerSourceSystem()
    }
  }

  private fun createTimelineNotes(
    applicationEntity: ApprovedPremisesApplicationEntity,
    count: Int,
    isDeleted: Boolean,
  ) = applicationTimelineNoteEntityFactory.produceAndPersistMultiple(count) {
    withApplicationId(applicationEntity.id)
    if (isDeleted) withDeletedAt(OffsetDateTime.now())
  }.toMutableList()
}
