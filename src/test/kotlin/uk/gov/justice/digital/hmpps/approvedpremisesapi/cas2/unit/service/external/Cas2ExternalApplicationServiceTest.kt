package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.unit.service.external

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ExternalApplicationDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2SuitableApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.external.Cas2ExternalApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.factory.Cas2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.factory.Cas2StatusUpdateEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.factory.Cas2UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2Cohort
import java.time.OffsetDateTime
import java.util.UUID

class Cas2ExternalApplicationServiceTest {
  private val mockCas2ApplicationRepository = mockk<Cas2ApplicationRepository>()

  private val cas2ExternalApplicationService = Cas2ExternalApplicationService(
    mockCas2ApplicationRepository,
    "http://frontend/applications/#id",
    "http://frontend/assess/applications/#applicationId/overview",
  )

  val crn = "ADAFD"
  val id: UUID = UUID.randomUUID()

  @Nested
  inner class GetSuitableApplicationByCrn {
    @Test
    fun `returns latest application (submitted), providing view submitted url`() {
      val status = "finished"
      val user = Cas2UserEntityFactory()
        .produce()
      val cas2applicationEntity = Cas2ApplicationEntityFactory()
        .withCreatedByUser(user)
        .withSubmittedAt(OffsetDateTime.now())
        .withCrn(crn)
        .withId(id)
        .withStatusUpdates(mutableListOf())
        .produce()
      val statusUpdate = Cas2StatusUpdateEntityFactory()
        .withApplication(cas2applicationEntity)
        .withAssessor(user)
        .withLabel(status)
        .produce()
      cas2applicationEntity.statusUpdates!!.add(statusUpdate)

      every { mockCas2ApplicationRepository.findLatestApplication(crn, Cas2Cohort.isr()) } returns cas2applicationEntity
      val result = cas2ExternalApplicationService.getSuitableApplicationByCrn(crn)
      val expected = Cas2SuitableApplication(
        uiUrl = "http://frontend/assess/applications/$id/overview",
        application = Cas2ExternalApplicationDto(
          id = id,
          status = status,
        ),
      )
      assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `returns latest application (draft), providing view draft url`() {
      val status = null
      val expected = Cas2SuitableApplication(
        uiUrl = "http://frontend/applications/$id",
        application = Cas2ExternalApplicationDto(
          id = id,
          status = status,
        ),
      )
      val user = Cas2UserEntityFactory()
        .produce()
      val cas2applicationEntity = Cas2ApplicationEntityFactory()
        .withCreatedByUser(user)
        .withCrn(crn)
        .withId(id)
        .withStatusUpdates(mutableListOf())
        .produce()

      every { mockCas2ApplicationRepository.findLatestApplication(crn, Cas2Cohort.isr()) } returns cas2applicationEntity
      val result = cas2ExternalApplicationService.getSuitableApplicationByCrn(crn)
      assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `returns no application`() {
      every { mockCas2ApplicationRepository.findLatestApplication(crn, Cas2Cohort.isr()) } returns null
      val result = cas2ExternalApplicationService.getSuitableApplicationByCrn(crn)
      assertThat(result).isEqualTo(null)
    }
  }
}
