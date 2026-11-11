package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.external

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ExternalApplicationDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ExternalPlacementV2Dto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.external.Cas1ExternalApplicationV2Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.external.Cas1ExternalCaseService

@ExtendWith(MockKExtension::class)
class Cas1ExternalCaseServiceTest {
  @MockK
  private lateinit var cas1ExternalApplicationV2Service: Cas1ExternalApplicationV2Service

  @MockK
  private lateinit var application: Cas1ExternalApplicationDto

  @MockK
  private lateinit var residence: Cas1ExternalPlacementV2Dto

  private lateinit var service: Cas1ExternalCaseService

  private val crn = "X12345"

  @BeforeEach
  fun setup() {
    service = Cas1ExternalCaseService(cas1ExternalApplicationV2Service)
  }

  @Nested
  inner class GetCaseSummary {
    @Test
    fun `returns null when there is no application and no residence`() {
      every { cas1ExternalApplicationV2Service.getSuitableApplicationByCrn(crn) } returns null
      every { cas1ExternalApplicationV2Service.getCurrentResidentPlacement(crn) } returns null

      assertThat(service.getCaseSummary(crn)).isNull()
    }

    @Test
    fun `returns summary with application only when there is no residence`() {
      every { cas1ExternalApplicationV2Service.getSuitableApplicationByCrn(crn) } returns application
      every { cas1ExternalApplicationV2Service.getCurrentResidentPlacement(crn) } returns null

      val result = service.getCaseSummary(crn)!!

      assertThat(result.currentApplication).isSameAs(application)
      assertThat(result.residence).isNull()
    }

    @Test
    fun `returns summary with residence only when there is no application`() {
      every { cas1ExternalApplicationV2Service.getSuitableApplicationByCrn(crn) } returns null
      every { cas1ExternalApplicationV2Service.getCurrentResidentPlacement(crn) } returns residence

      val result = service.getCaseSummary(crn)!!

      assertThat(result.currentApplication).isNull()
      assertThat(result.residence).isSameAs(residence)
    }

    @Test
    fun `returns summary with both application and residence when both exist`() {
      every { cas1ExternalApplicationV2Service.getSuitableApplicationByCrn(crn) } returns application
      every { cas1ExternalApplicationV2Service.getCurrentResidentPlacement(crn) } returns residence

      val result = service.getCaseSummary(crn)!!

      assertThat(result.currentApplication).isSameAs(application)
      assertThat(result.residence).isSameAs(residence)
    }
  }
}
