package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.every
import io.mockk.mockk
import jakarta.servlet.http.HttpServletRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.RequestContextService

class RequestContextServiceTest {
  val currentRequest = mockk<HttpServletRequest>()

  val service = RequestContextService(currentRequest)

  @Nested
  inner class GetServiceForRequest {

    @Test
    fun `getServiceForRequest CAS1`() {
      every { currentRequest.getHeader("X-Service-Name") } returns ServiceName.approvedPremises.value

      assertThat(service.getServiceForRequest()).isEqualTo(ServiceName.approvedPremises)
    }

    @Test
    fun `getServiceForRequest CAS2`() {
      every { currentRequest.getHeader("X-Service-Name") } returns ServiceName.cas2.value

      assertThat(service.getServiceForRequest()).isEqualTo(ServiceName.cas2)
    }

    @Test
    fun `getServiceForRequest CAS3`() {
      every { currentRequest.getHeader("X-Service-Name") } returns ServiceName.temporaryAccommodation.value

      assertThat(service.getServiceForRequest()).isEqualTo(ServiceName.temporaryAccommodation)
    }

    @Test
    fun `getServiceForRequest unknown`() {
      every { currentRequest.getHeader("X-Service-Name") } returns "unexpected value"

      assertThat(service.getServiceForRequest()).isNull()
    }

    @Test
    fun `getServiceForRequest not defined`() {
      every { currentRequest.getHeader("X-Service-Name") } returns null

      assertThat(service.getServiceForRequest()).isNull()
    }
  }
}
