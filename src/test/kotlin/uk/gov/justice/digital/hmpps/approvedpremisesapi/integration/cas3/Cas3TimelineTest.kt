package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas3

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3AssessmentUpdatedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3AssessmentUpdatedField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReferralHistoryNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ReferralHistorySystemNoteType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class Cas3TimelineTest : IntegrationTestBase() {

  @Test
  fun `getTimelineEvents by ID without JWT returns 401`() {
    webTestClient.get()
      .uri("/cas3/timeline/${UUID.randomUUID()}")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `getTimelineEvents with incorrect ID returns 404`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      webTestClient.get()
        .uri("/cas3/timeline/${UUID.randomUUID()}")
        .header("Authorization", "Bearer $jwt")
        .header("X-service-name", "temporary-accommodation")
        .exchange()
        .expectStatus()
        .isNotFound
    }
  }

  @Test
  fun `getTimelineEvents call with CAS3_ASSESSOR ROLE returns all Timeline entries`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      givenAnOffender { offenderDetails, _ ->

        val application = persistApplication(crn = offenderDetails.otherIds.crn, user = userEntity)
        val assessment = persistAssessment(application)

        addReferralHistoryNotes(assessment)
        addDomainEventTimelineEvents(assessment)

        assessment.schemaUpToDate = true

        val response = webTestClient.get()
          .uri("/cas3/timeline/${assessment.id}")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectBodyList(ReferralHistoryNote::class.java)
          .returnResult()
          .responseBody!!

        assertThat(response.size).isEqualTo(9)
        assertThat(response.map { it.type }).containsOnly("system", "domainEvent", "user")
      }
    }
  }

  @Test
  fun `getTimelineEvents returns no user notes with CAS3_REFERRER ROLE`() {
    givenAUser(roles = listOf(UserRole.CAS3_REFERRER)) { userEntity, jwt ->
      givenAnOffender { offenderDetails, _ ->

        val application = persistApplication(crn = offenderDetails.otherIds.crn, user = userEntity)
        val assessment = persistAssessment(application)

        addReferralHistoryNotes(assessment)
        addDomainEventTimelineEvents(assessment)

        assessment.schemaUpToDate = true

        val result = webTestClient.get()
          .uri("/cas3/timeline/${assessment.id}")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectBodyList(ReferralHistoryNote::class.java)
          .returnResult()
          .responseBody!!

        assertThat(result.size).isEqualTo(7)
        assertThat(result.map { it.type }).doesNotContain("user")
        assertThat(result.map { it.type }).containsOnly("system", "domainEvent")
      }
    }
  }

  @Test
  fun `getTimelineEvents does not return referral rejection detail notes when the user has CAS3_REFERRER ROLE`() {
    givenAUser(roles = listOf(UserRole.CAS3_REFERRER)) { userEntity, jwt ->
      givenAnOffender { offenderDetails, _ ->

        val application = persistApplication(crn = offenderDetails.otherIds.crn, user = userEntity)
        val assessment = persistRejectedAssessment(application)

        addReferralHistoryNotes(assessment)
        addDomainEventTimelineEvents(assessment)

        assessment.schemaUpToDate = true

        val result = webTestClient.get()
          .uri("/cas3/timeline/${assessment.id}")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectBodyList(ReferralHistoryNote::class.java)
          .returnResult()
          .responseBody!!

        assertThat(result.size).isEqualTo(7)
        assertThat(result.map { it.type }).doesNotContain("user")
        assertThat(result.map { it.type }).containsOnly("system", "domainEvent")
        assertThat(result.map { it.messageDetails?.rejectionReasonDetails }).containsOnlyNulls()
      }
    }
  }

  private fun persistAssessment(application: TemporaryAccommodationApplicationEntity) = temporaryAccommodationAssessmentEntityFactory.produceAndPersist {
    withAllocatedToUser(application.createdByUser)
    withApplication(application)
    withAssessmentSchema(persistAssessmentSchema())
    withReleaseDate(null)
    withAccommodationRequiredFromDate(null)
  }

  private fun persistRejectedAssessment(application: TemporaryAccommodationApplicationEntity): TemporaryAccommodationAssessmentEntity {
    val referralRejectionReason = referralRejectionReasonEntityFactory.produceAndPersist()
    val referralRejectionReasonDetail = randomStringMultiCaseWithNumbers(15)

    return temporaryAccommodationAssessmentEntityFactory.produceAndPersist {
      withAllocatedToUser(application.createdByUser)
      withApplication(application)
      withAssessmentSchema(persistAssessmentSchema())
      withSubmittedAt(OffsetDateTime.now().minusDays(21))
      withAllocatedAt(OffsetDateTime.now().minusDays(5))
      withReleaseDate(null)
      withAccommodationRequiredFromDate(null)
      withReferralRejectionReason(referralRejectionReason)
      withReferralRejectionReasonDetail(referralRejectionReasonDetail)
      withIsWithdrawn(false)
    }
  }

  private fun persistApplication(crn: String, user: UserEntity) = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
    withCrn(crn)
    withCreatedByUser(user)
    withApplicationSchema(persistApplicationSchema())
    withProbationRegion(user.probationRegion)
    withArrivalDate(LocalDate.now().minusDays(100))
    withPersonReleaseDate(LocalDate.now().minusDays(100))
  }

  private fun persistAssessmentSchema() = temporaryAccommodationAssessmentJsonSchemaEntityFactory.produceAndPersist {
    withPermissiveSchema()
    withAddedAt(OffsetDateTime.now())
  }

  private fun persistApplicationSchema() = temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
    withPermissiveSchema()
  }

  private fun persistCas3UpdatedDomainEvent(id: UUID, assessment: TemporaryAccommodationAssessmentEntity, data: Any) = domainEventFactory.produceAndPersist {
    withId(id)
    withCrn(assessment.application.crn)
    withOccurredAt(OffsetDateTime.now())
    withCreatedAt(OffsetDateTime.now())
    withData(data)
    withAssessmentId(assessment.id)
    withService(ServiceName.temporaryAccommodation)
    withTriggeredByUserId(assessment.allocatedToUser!!.id)
    withType(DomainEventType.CAS3_ASSESSMENT_UPDATED)
  }

  private fun addReferralHistoryNotes(assessment: TemporaryAccommodationAssessmentEntity) {
    assessment.apply {
      this.referralHistoryNotes += assessmentReferralHistoryUserNoteEntityFactory.produceAndPersist {
        withCreatedBy(allocatedToUser!!)
        withMessage("Some user note")
        withAssessment(this@apply)
      }
      ReferralHistorySystemNoteType.entries.forEach {
        this.referralHistoryNotes += assessmentReferralHistorySystemNoteEntityFactory.produceAndPersist {
          withCreatedBy(allocatedToUser!!)
          withType(it)
          withAssessment(this@apply)
          if (ReferralHistorySystemNoteType.REJECTED == it) {
            withCreatedAt(OffsetDateTime.now().minusDays(90))
          }
        }
      }
      this.referralHistoryNotes += assessmentReferralHistoryUserNoteEntityFactory.produceAndPersist {
        withCreatedBy(allocatedToUser!!)
        withMessage("another user note with some super secret data in")
        withAssessment(this@apply)
      }
    }
  }

  private fun addDomainEventTimelineEvents(assessment: TemporaryAccommodationAssessmentEntity) {
    val accommodationReqCAS3AssessmentUpdatedField = CAS3AssessmentUpdatedField(
      fieldName = "accommodationRequiredFromDate",
      updatedFrom = LocalDate.now().plusDays(1).toString(),
      updatedTo = LocalDate.now().plusDays(7).toString(),
    )

    persistCas3UpdatedDomainEvent(
      UUID.randomUUID(),
      assessment,
      CAS3AssessmentUpdatedEvent(
        updatedFields = listOf(accommodationReqCAS3AssessmentUpdatedField),
        id = UUID.randomUUID(),
        timestamp = Instant.now(),
        eventType = EventType.assessmentUpdated,
      ),
    )
  }
}
