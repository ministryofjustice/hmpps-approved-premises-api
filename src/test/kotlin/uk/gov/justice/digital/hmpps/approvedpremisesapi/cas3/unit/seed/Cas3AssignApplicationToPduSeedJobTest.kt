package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.unit.seed

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.TemporaryAccommodationApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.TemporaryAccommodationAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.seed.Cas3AssignApplicationToPduSeedCsvRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.seed.Cas3AssignApplicationToPduSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitRepository
import java.util.UUID

class Cas3AssignApplicationToPduSeedJobTest {
  private val mockAssessmentRepository = mockk<AssessmentRepository>()
  private val mockApplicationRepository = mockk<ApplicationRepository>()
  private val mockProbationDeliveryUnitRepository = mockk<ProbationDeliveryUnitRepository>()

  private val assessmentId = UUID.randomUUID()

  private val seedJob = Cas3AssignApplicationToPduSeedJob(
    assessmentRepository = mockAssessmentRepository,
    applicationRepository = mockApplicationRepository,
    probationDeliveryUnitRepository = mockProbationDeliveryUnitRepository,
  )

  @Test
  fun `When an assessment doesn't exist expect an error`() {
    every { mockAssessmentRepository.findByIdOrNull(assessmentId) } returns null

    assertThatThrownBy {
      seedJob.processRow(Cas3AssignApplicationToPduSeedCsvRow(assessmentId, "Croydon"))
    }.hasMessage("Assessment with id $assessmentId not found")
  }

  @Test
  fun `When the assessment is not a Temporary Accommodation assessment expect an error`() {
    every { mockAssessmentRepository.findByIdOrNull(assessmentId) } returns
      ApprovedPremisesAssessmentEntityFactory()
        .withApplication(
          ApprovedPremisesApplicationEntityFactory()
            .withDefaults()
            .produce(),
        )
        .produce()

    assertThatThrownBy {
      seedJob.processRow(Cas3AssignApplicationToPduSeedCsvRow(assessmentId, "Croydon"))
    }.hasMessage("Assessment with id $assessmentId is not a temporary accommodation assessment")
  }

  @Test
  fun `When an application doesn't exist expect an error`() {
    val application = TemporaryAccommodationApplicationEntityFactory()
      .withDefaults()
      .produce()

    every { mockAssessmentRepository.findByIdOrNull(assessmentId) } returns
      TemporaryAccommodationAssessmentEntityFactory()
        .withApplication(application)
        .produce()

    every { mockApplicationRepository.findByIdOrNull(application.id) } returns null

    assertThatThrownBy {
      seedJob.processRow(Cas3AssignApplicationToPduSeedCsvRow(assessmentId, "Croydon"))
    }.hasMessage("Application with id ${application.id} not found")
  }

  @Test
  fun `When the application is not a Temporary Accommodation application expect an error`() {
    val application = ApprovedPremisesApplicationEntityFactory()
      .withDefaults()
      .produce()

    every { mockAssessmentRepository.findByIdOrNull(assessmentId) } returns
      TemporaryAccommodationAssessmentEntityFactory()
        .withApplication(application)
        .produce()

    every { mockApplicationRepository.findByIdOrNull(application.id) } returns application

    assertThatThrownBy {
      seedJob.processRow(Cas3AssignApplicationToPduSeedCsvRow(assessmentId, "Croydon"))
    }.hasMessage("Application with id ${application.id} is not a temporary accommodation application")
  }

  @Test
  fun `When the pdu name doesn't exist expect an error`() {
    val application = TemporaryAccommodationApplicationEntityFactory()
      .withDefaults()
      .produce()

    every { mockAssessmentRepository.findByIdOrNull(assessmentId) } returns
      TemporaryAccommodationAssessmentEntityFactory()
        .withApplication(application)
        .produce()

    every { mockApplicationRepository.findByIdOrNull(application.id) } returns application

    every { mockProbationDeliveryUnitRepository.findByName("Croydon") } returns null

    assertThatThrownBy {
      seedJob.processRow(Cas3AssignApplicationToPduSeedCsvRow(assessmentId, "Croydon"))
    }.hasMessage("Probation Delivery Unit with name Croydon not found")
  }
}
