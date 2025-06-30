package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesType
import java.time.OffsetDateTime

class ApplicationSummaryQueryTest : IntegrationTestBase() {
  @Autowired
  lateinit var realApplicationRepository: ApplicationRepository

  @Test
  fun `findNonWithdrawnApprovedPremisesSummariesForUser query works as described`() {
    givenAUser { user, _ ->
      givenAUser { differentUser, _ ->
        givenAnOffender { offenderDetails, _ ->
          val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          }

          val applicationCreatedByDifferentUser = approvedPremisesApplicationEntityFactory.produceAndPersist {
            withCrn(offenderDetails.otherIds.crn)
            withCreatedByUser(differentUser)
            withApplicationSchema(applicationSchema)
            withApType(ApprovedPremisesType.PIPE)
            withIsWomensApplication(false)
            withReleaseType("rotl")
            withSubmittedAt(null)
          }

          val nonSubmittedApplication = approvedPremisesApplicationEntityFactory.produceAndPersist {
            withCrn(offenderDetails.otherIds.crn)
            withCreatedByUser(user)
            withApplicationSchema(applicationSchema)
            withApType(ApprovedPremisesType.PIPE)
            withIsWomensApplication(false)
            withReleaseType("rotl")
            withSubmittedAt(null)
            withIsInapplicable(false)
            withStatus(ApprovedPremisesApplicationStatus.STARTED)
          }

          val withdrawnApplication = approvedPremisesApplicationEntityFactory.produceAndPersist {
            withCrn(offenderDetails.otherIds.crn)
            withCreatedByUser(user)
            withApplicationSchema(applicationSchema)
            withApType(ApprovedPremisesType.PIPE)
            withIsWomensApplication(false)
            withReleaseType("rotl")
            withIsWithdrawn(true)
            withStatus(ApprovedPremisesApplicationStatus.WITHDRAWN)
          }

          val inapplicableApplication = approvedPremisesApplicationEntityFactory.produceAndPersist {
            withCrn(offenderDetails.otherIds.crn)
            withCreatedByUser(user)
            withApplicationSchema(applicationSchema)
            withIsInapplicable(true)
            withStatus(ApprovedPremisesApplicationStatus.INAPPLICABLE)
          }

          val submittedApplication = approvedPremisesApplicationEntityFactory.produceAndPersist {
            withCrn(offenderDetails.otherIds.crn)
            withCreatedByUser(user)
            withApplicationSchema(applicationSchema)
            withApType(ApprovedPremisesType.PIPE)
            withIsWomensApplication(false)
            withReleaseType("rotl")
            withSubmittedAt(OffsetDateTime.parse("2023-04-19T09:34:00+01:00"))
            withStatus(ApprovedPremisesApplicationStatus.AWAITING_ASSESSMENT)
          }

          val results = realApplicationRepository.findNonWithdrawnApprovedPremisesSummariesForUser(user.id)

          assertThat(results.size).isEqualTo(2)

          assertThat(results).noneMatch { it.getId() == withdrawnApplication.id }

          results.first { it.getId() == nonSubmittedApplication.id }.let {
            assertThat(it.getCrn()).isEqualTo(nonSubmittedApplication.crn)
            assertThat(it.getCreatedByUserId()).isEqualTo(nonSubmittedApplication.createdByUser.id)
            assertThat(it.getCreatedAt()).isEqualTo(nonSubmittedApplication.createdAt.toInstant())
            assertThat(it.getSubmittedAt()).isEqualTo(nonSubmittedApplication.submittedAt?.toInstant())
            assertThat(it.getStatus()).isEqualTo("STARTED")
          }

          results.first { it.getId() == submittedApplication.id }.let {
            assertThat(it.getCrn()).isEqualTo(submittedApplication.crn)
            assertThat(it.getCreatedByUserId()).isEqualTo(submittedApplication.createdByUser.id)
            assertThat(it.getCreatedAt()).isEqualTo(submittedApplication.createdAt.toInstant())
            assertThat(it.getSubmittedAt()).isEqualTo(submittedApplication.submittedAt?.toInstant())
            assertThat(it.getStatus()).isEqualTo("AWAITING_ASSESSMENT")
          }

          assertThat(results).noneMatch {
            it.getId() == applicationCreatedByDifferentUser.id
          }

          assertThat(results).noneMatch {
            it.getId() == inapplicableApplication.id
          }
        }
      }
    }
  }

  @Test
  fun `findAllTemporaryAccommodationSummariesCreatedByUser query works as described`() {
    givenAUser { user, _ ->
      givenAnOffender { offenderDetails, _ ->
        val applicationSchema = temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
          withPermissiveSchema()
        }

        val assessmentSchema = temporaryAccommodationAssessmentJsonSchemaEntityFactory.produceAndPersist {
          withPermissiveSchema()
        }

        val probationRegion = givenAProbationRegion()

        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withProbationRegion(probationRegion)
          withLocalAuthorityArea(localAuthorityEntityFactory.produceAndPersist())
        }

        val room = roomEntityFactory.produceAndPersist {
          withPremises(premises)
        }

        val bed = bedEntityFactory.produceAndPersist {
          withRoom(room)
        }

        val nonSubmittedApplication = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
          withCrn(offenderDetails.otherIds.crn)
          withCreatedByUser(user)
          withApplicationSchema(applicationSchema)
          withSubmittedAt(null)
          withProbationRegion(probationRegion)
        }

        val submittedApplication = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
          withCrn(offenderDetails.otherIds.crn)
          withCreatedByUser(user)
          withApplicationSchema(applicationSchema)
          withSubmittedAt(OffsetDateTime.parse("2023-04-19T09:34:00+01:00"))
          withProbationRegion(probationRegion)
        }

        val deletedApplication = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
          withCrn(offenderDetails.otherIds.crn)
          withCreatedByUser(user)
          withApplicationSchema(applicationSchema)
          withDeletedAt(OffsetDateTime.parse("2025-01-07T17:31:00+01:00"))
          withProbationRegion(probationRegion)
        }

        val assessmentForSubmittedApplication = temporaryAccommodationAssessmentEntityFactory.produceAndPersist {
          withApplication(submittedApplication)
          withAllocatedToUser(user)
          withAssessmentSchema(assessmentSchema)
          withSubmittedAt(OffsetDateTime.parse("2023-04-19T10:15:00+01:00"))
        }

        assessmentClarificationNoteEntityFactory.produceAndPersist {
          withAssessment(assessmentForSubmittedApplication)
          withCreatedBy(user)
          withResponse(null)
        }

        bookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withBed(bed)
          withApplication(submittedApplication)
        }

        val results = realApplicationRepository.findAllTemporaryAccommodationSummariesCreatedByUser(user.id)

        results.first { it.getId() == nonSubmittedApplication.id }.let {
          assertThat(it.getCrn()).isEqualTo(nonSubmittedApplication.crn)
          assertThat(it.getCreatedByUserId()).isEqualTo(nonSubmittedApplication.createdByUser.id)
          assertThat(it.getCreatedAt()).isEqualTo(nonSubmittedApplication.createdAt.toInstant())
          assertThat(it.getSubmittedAt()).isEqualTo(nonSubmittedApplication.submittedAt?.toInstant())
          assertThat(it.getLatestAssessmentSubmittedAt()).isNull()
          assertThat(it.getLatestAssessmentDecision()).isNull()
          assertThat(it.getLatestAssessmentHasClarificationNotesWithoutResponse()).isEqualTo(false)
          assertThat(it.getHasBooking()).isEqualTo(false)
          assertThat(it.getRiskRatings()).isNotBlank()
        }

        results.first { it.getId() == submittedApplication.id }.let {
          assertThat(it.getCrn()).isEqualTo(submittedApplication.crn)
          assertThat(it.getCreatedByUserId()).isEqualTo(submittedApplication.createdByUser.id)
          assertThat(it.getCreatedAt()).isEqualTo(submittedApplication.createdAt.toInstant())
          assertThat(it.getSubmittedAt()).isEqualTo(submittedApplication.submittedAt?.toInstant())
          assertThat(it.getLatestAssessmentSubmittedAt()).isEqualTo(assessmentForSubmittedApplication.submittedAt?.toInstant())
          assertThat(it.getLatestAssessmentDecision()).isEqualTo(assessmentForSubmittedApplication.decision)
          assertThat(it.getLatestAssessmentHasClarificationNotesWithoutResponse()).isEqualTo(true)
          assertThat(it.getHasBooking()).isEqualTo(true)
          assertThat(it.getRiskRatings()).isNotBlank()
        }

        assertThat(results.firstOrNull { it.getId() == deletedApplication.id }).isNull()
      }
    }
  }
}
