package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas2

import com.ninjasquad.springmockk.SpykBean
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.Cas2ApplicationAssignmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.Cas2DomainEventListener
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.ObjectMapperFactory

@ExtendWith(MockKExtension::class)
class Cas2DomainEventListenerTest {

  @MockK
  lateinit var cas2ApplicationAssignmentService: Cas2ApplicationAssignmentService

  @SpykBean
  var objectMapper = ObjectMapperFactory.createRuntimeLikeObjectMapper()

  @InjectMockKs
  lateinit var cas2DomainEventListener: Cas2DomainEventListener

  @Test
  fun `POM Allocation changed message is processed`() {
    every { cas2ApplicationAssignmentService.handleAllocationChangedEvent(any()) } just Runs
    val msg = """{"Message": "{\"eventType\":\"offender-management.allocation.changed\",\"additionalInformation\":{\"staffCode\":123456,\"prisonId\":\"PPP\"}}"}"""
    cas2DomainEventListener.processMessage(msg)
    verify(exactly = 1) { cas2ApplicationAssignmentService.handleAllocationChangedEvent(any()) }
  }

  @Test
  fun `Prisoner updated message is processed`() {
    every { cas2ApplicationAssignmentService.handlePrisonerUpdatedEvent(any()) } just Runs
    val msg =
      """{"Message": "{\"eventType\":\"prisoner-offender-search.prisoner.updated\",\"additionalInformation\":{\"categoriesChanged\": [\"LOCATION\"]},\"version\":\"1\"}"}"""
    cas2DomainEventListener.processMessage(msg)
    verify(exactly = 1) { cas2ApplicationAssignmentService.handlePrisonerUpdatedEvent(any()) }
  }
}
