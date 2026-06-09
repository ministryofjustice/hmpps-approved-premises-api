package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.unit.service

import com.fasterxml.jackson.core.JsonParseException
import com.ninjasquad.springmockk.MockkSpyBean
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.service.Cas2HdcAllocationChangedService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.service.Cas2HdcDomainEventListener
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.service.Cas2HdcLocationChangedService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.SentryService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.JsonMapperFactory

@ExtendWith(MockKExtension::class)
class Cas2DomainEventListenerTest {

  @MockK
  lateinit var cas2HdcAllocationChangedService: Cas2HdcAllocationChangedService

  @MockK
  lateinit var cas2HdcLocationChangedService: Cas2HdcLocationChangedService

  @MockK
  lateinit var sentryService: SentryService

  @MockkSpyBean
  var objectMapper = JsonMapperFactory.createJackson2JsonMapper()

  @InjectMockKs
  lateinit var cas2HdcDomainEventListener: Cas2HdcDomainEventListener

  @Test
  fun `POM Allocation changed message is processed`() {
    every { cas2HdcAllocationChangedService.process(any()) } returns Unit
    val msg = """{"Message": "{\"eventType\":\"offender-management.allocation.changed\",\"additionalInformation\":{\"staffCode\":123456,\"prisonId\":\"PPP\"}}"}"""
    cas2HdcDomainEventListener.processMessage(msg)
    verify(exactly = 1) { cas2HdcAllocationChangedService.process(any()) }
  }

  @Test
  fun `Prisoner updated message is processed`() {
    every { cas2HdcLocationChangedService.process(any()) } returns Unit
    val msg =
      """{"Message": "{\"eventType\":\"prisoner-offender-search.prisoner.updated\",\"additionalInformation\":{\"categoriesChanged\": [\"LOCATION\"]},\"version\":\"1\"}"}"""
    cas2HdcDomainEventListener.processMessage(msg)
    verify(exactly = 1) { cas2HdcLocationChangedService.process(any()) }
  }

  @Test
  fun `exceptions are captured by sentry and still thrown`() {
    every { sentryService.captureException(any()) } just Runs
    assertThrows<JsonParseException> { cas2HdcDomainEventListener.processMessage("invalid") }
    verify { sentryService.captureException(any()) }
  }
}
