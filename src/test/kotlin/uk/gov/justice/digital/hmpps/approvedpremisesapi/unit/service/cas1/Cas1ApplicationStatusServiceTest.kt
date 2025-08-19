package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import io.mockk.Called
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1SpaceBookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationStatusService
import java.time.LocalDate
import java.time.OffsetDateTime

@ExtendWith(MockKExtension::class)
class Cas1ApplicationStatusServiceTest {
  @MockK
  private lateinit var applicationRepository: ApplicationRepository

  @MockK
  private lateinit var cas1SpaceBookingRepository: Cas1SpaceBookingRepository

  @InjectMockKs
  private lateinit var service: Cas1ApplicationStatusService

  @Nested
  inner class UnsubmittedApplicationUpdated {
    @Test
    fun `if is inapplicable, set status to inapplicable`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withDefaults()
        .withStatus(ApprovedPremisesApplicationStatus.STARTED)
        .withIsInapplicable(true)
        .produce()

      every { applicationRepository.save(any()) } returns application

      service.unsubmittedApplicationUpdated(application)

      verify { applicationRepository.save(application) }

      assertThat(application.status).isEqualTo(ApprovedPremisesApplicationStatus.INAPPLICABLE)
    }

    @ParameterizedTest
    @CsvSource("false", "null", nullValues = ["null"])
    fun `if not inapplicable, set status to started`(inapplicable: Boolean?) {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withDefaults()
        .withStatus(ApprovedPremisesApplicationStatus.STARTED)
        .withIsInapplicable(inapplicable)
        .produce()

      every { applicationRepository.save(any()) } returns application

      service.unsubmittedApplicationUpdated(application)

      verify { applicationRepository.save(application) }

      assertThat(application.status).isEqualTo(ApprovedPremisesApplicationStatus.STARTED)
    }
  }

  @Nested
  inner class ApplicationWithdrawn {

    @Test
    fun `set status to WITHDRAWN`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withDefaults()
        .withStatus(ApprovedPremisesApplicationStatus.STARTED)
        .produce()

      every { applicationRepository.save(any()) } returns application

      service.applicationWithdrawn(application)

      verify { applicationRepository.save(application) }

      assertThat(application.status).isEqualTo(ApprovedPremisesApplicationStatus.WITHDRAWN)
    }
  }

  @Nested
  inner class AssessmentCreated {

    @Test
    fun `If not allocated to user, set application status to UNALLOCATED_ASSESSMENT`() {
      val application = ApprovedPremisesApplicationEntityFactory().withDefaults().produce()

      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withDefaults()
        .withApplication(application)
        .withAllocatedToUser(null)
        .produce()

      service.assessmentCreated(assessment)

      assertThat(application.status).isEqualTo(ApprovedPremisesApplicationStatus.UNALLOCATED_ASSESSMENT)
    }

    @Test
    fun `If allocated to user, set application status to AWAITING_ASSESSMENT`() {
      val application = ApprovedPremisesApplicationEntityFactory().withDefaults().produce()

      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withDefaults()
        .withApplication(application)
        .withAllocatedToUser(UserEntityFactory().withDefaults().produce())
        .produce()

      service.assessmentCreated(assessment)

      assertThat(application.status).isEqualTo(ApprovedPremisesApplicationStatus.AWAITING_ASSESSMENT)
    }
  }

  @Nested
  inner class AssessmentUpdated {

    @Test
    fun `If requested further information with no assessment decision, do nothing`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withDefaults()
        .withStatus(ApprovedPremisesApplicationStatus.REQUESTED_FURTHER_INFORMATION)
        .produce()

      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withDefaults()
        .withApplication(application)
        .withDecision(null)
        .produce()

      service.assessmentUpdated(assessment)

      assertThat(application.status).isEqualTo(ApprovedPremisesApplicationStatus.REQUESTED_FURTHER_INFORMATION)
    }

    @Test
    fun `If assessment is withdrawn, do nothing`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withDefaults()
        .withStatus(ApprovedPremisesApplicationStatus.STARTED)
        .produce()

      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withDefaults()
        .withApplication(application)
        .withIsWithdrawn(true)
        .withDecision(AssessmentDecision.ACCEPTED)
        .produce()

      service.assessmentUpdated(assessment)

      assertThat(application.status).isEqualTo(ApprovedPremisesApplicationStatus.STARTED)
    }

    @Test
    fun `If no decision and assessment has data, application status is ASSESSMENT_IN_PROGRESS`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withDefaults()
        .withStatus(ApprovedPremisesApplicationStatus.STARTED)
        .produce()

      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withDefaults()
        .withApplication(application)
        .withDecision(null)
        .withData("{ }")
        .produce()

      service.assessmentUpdated(assessment)

      assertThat(application.status).isEqualTo(ApprovedPremisesApplicationStatus.ASSESSMENT_IN_PROGRESS)
    }

    @Test
    fun `If decision is accepted and application has request for placement, application status is AWAITING_PLACEMENT`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withDefaults()
        .withStatus(ApprovedPremisesApplicationStatus.STARTED)
        .withArrivalDate(OffsetDateTime.now())
        .produce()

      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withDefaults()
        .withApplication(application)
        .withDecision(AssessmentDecision.ACCEPTED)
        .produce()

      service.assessmentUpdated(assessment)

      assertThat(application.status).isEqualTo(ApprovedPremisesApplicationStatus.AWAITING_PLACEMENT)
    }

    @Test
    fun `If decision is accepted and application doesn't have a request for placement, application status is PENDING_PLACEMENT_REQUEST`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withDefaults()
        .withStatus(ApprovedPremisesApplicationStatus.STARTED)
        .withArrivalDate(null)
        .produce()

      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withDefaults()
        .withApplication(application)
        .withDecision(AssessmentDecision.ACCEPTED)
        .produce()

      service.assessmentUpdated(assessment)

      assertThat(application.status).isEqualTo(ApprovedPremisesApplicationStatus.PENDING_PLACEMENT_REQUEST)
    }

    @Test
    fun `If decision is rejected, application status is REJECTED`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withDefaults()
        .withStatus(ApprovedPremisesApplicationStatus.STARTED)
        .produce()

      val assessment = ApprovedPremisesAssessmentEntityFactory()
        .withDefaults()
        .withApplication(application)
        .withDecision(AssessmentDecision.REJECTED)
        .produce()

      service.assessmentUpdated(assessment)

      assertThat(application.status).isEqualTo(ApprovedPremisesApplicationStatus.REJECTED)
    }
  }

  @Nested
  inner class SpaceBookingMade {
    @Test
    fun `if linked to application set status to PLACEMENT_ALLOCATED`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withDefaults()
        .withStatus(ApprovedPremisesApplicationStatus.AWAITING_PLACEMENT)
        .produce()

      val spaceBooking = Cas1SpaceBookingEntityFactory()
        .withApplication(application)
        .produce()

      every { applicationRepository.save(any()) } returns application

      service.spaceBookingMade(spaceBooking)

      verify { applicationRepository.save(application) }

      assertThat(application.status).isEqualTo(ApprovedPremisesApplicationStatus.PLACEMENT_ALLOCATED)
    }
  }

  @Nested
  inner class SpaceBookingCancelled {
    val application = ApprovedPremisesApplicationEntityFactory()
      .withDefaults()
      .withStatus(ApprovedPremisesApplicationStatus.PLACEMENT_ALLOCATED)
      .produce()

    @Test
    fun `if not linked to application, do nothing`() {
      val booking = Cas1SpaceBookingEntityFactory()
        .withApplication(null)
        .produce()

      service.spaceBookingCancelled(booking)

      verify { applicationRepository wasNot Called }
    }

    @Test
    fun `if cancellation not triggered by user request operation do nothing`() {
      val booking = Cas1SpaceBookingEntityFactory()
        .withApplication(application)
        .produce()

      every { cas1SpaceBookingRepository.findAllByApplication(application) } returns listOf(booking)

      service.spaceBookingCancelled(
        spaceBooking = booking,
        isUserRequestedWithdrawal = false,
      )

      verify { applicationRepository wasNot Called }
    }

    @Test
    fun `if there are still live bookings for the application, do nothing`() {
      val booking = Cas1SpaceBookingEntityFactory()
        .withApplication(application)
        .produce()

      every { applicationRepository.save(any()) } returns application
      every { cas1SpaceBookingRepository.findAllByApplication(application) } returns listOf(booking)

      service.spaceBookingCancelled(booking)

      verify { applicationRepository wasNot Called }
    }

    @Test
    fun `if there are no live bookings for the application, change status to AWAITING_PLACEMENT`() {
      val cancelledBooking = Cas1SpaceBookingEntityFactory()
        .withApplication(application)
        .withCancellationOccurredAt(LocalDate.now())
        .produce()

      every { applicationRepository.save(any()) } returns application
      every { cas1SpaceBookingRepository.findAllByApplication(application) } returns listOf(cancelledBooking)

      service.spaceBookingCancelled(cancelledBooking)

      verify { applicationRepository.save(application) }

      assertThat(application.status).isEqualTo(ApprovedPremisesApplicationStatus.AWAITING_PLACEMENT)
    }
  }
}
