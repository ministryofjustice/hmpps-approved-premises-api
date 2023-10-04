package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas3

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.mockk.mockk
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.DomainEventBuilder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.DomainEventService
import uk.gov.justice.hmpps.sqs.HmppsQueueService

class DomainEventServiceTest {
  private val domainEventRepositoryMock = mockk<DomainEventRepository>()
  private val domainEventBuilderMock = mockk<DomainEventBuilder>()
  private val hmppsQueueServiceMock = mockk<HmppsQueueService>()

  private val objectMapper = ObjectMapper().apply {
    registerModule(Jdk8Module())
    registerModule(JavaTimeModule())
    registerKotlinModule()
  }

  private val domainEventService = DomainEventService(
    objectMapper = objectMapper,
    domainEventRepository = domainEventRepositoryMock,
    domainEventBuilder = domainEventBuilderMock,
    hmppsQueueService = hmppsQueueServiceMock,
    emitDomainEventsEnabled = true,
    personArrivedDetailUrlTemplate = "http://api/events/cas3/person-arrived/#eventId",
  )
}
