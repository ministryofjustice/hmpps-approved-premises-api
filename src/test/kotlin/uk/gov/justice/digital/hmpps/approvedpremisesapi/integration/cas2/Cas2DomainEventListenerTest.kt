package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas2

import com.ninjasquad.springmockk.MockkBean
import com.ninjasquad.springmockk.SpykBean
import io.mockk.every
import io.mockk.verify
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.Prisoner
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas2PomUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2PrisonerLocationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2PrisonerLocationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NomisUserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.Cas2DomainEventListener
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.UUID

class Cas2DomainEventListenerTest : IntegrationTestBase() {

  @Autowired
  private lateinit var hmppsQueueService: HmppsQueueService

  @MockkBean
  private lateinit var prisonerSearchClient: PrisonerSearchClient

  @SpykBean
  private lateinit var domainEventListener: Cas2DomainEventListener

  @SpykBean
  private lateinit var prisonerLocationRepository: Cas2PrisonerLocationRepository

  private val domainEventsTopic by lazy {
    hmppsQueueService.findByTopicId("domainevents") ?: throw MissingQueueException("HmppsTopic domainevents not found")
  }

  private val domainEventsClient by lazy { domainEventsTopic.snsClient }

  private fun publishMessageToTopic(eventType: String, json: String = "{}") {
    val sendMessageRequest = PublishRequest.builder()
      .topicArn(domainEventsTopic.arn)
      .message(json)
      .messageAttributes(
        mapOf(
          "eventType" to MessageAttributeValue.builder().dataType("String").stringValue(eventType).build(),
        ),
      )
      .build()
    domainEventsClient.publish(sendMessageRequest).get()
  }

  @Test
  fun `Start to process Allocation Changed Message on Domain Events Topic`() {
    val eventType = "offender-management.allocation.changed"
    publishMessageToTopic(eventType)
    verify(exactly = 1, timeout = 5000) { domainEventListener.processMessage(any()) }
  }

  @Test
  fun `Start to process Location Changed Message on Domain Events Topic`() {
    val eventType = "prisoner-offender-search.prisoner.updated"
    publishMessageToTopic(eventType)
    verify(exactly = 1, timeout = 5000) { domainEventListener.processMessage(any()) }
  }

  @Test
  fun `Do not process Message that is not a required event type`() {
    val eventType = "unwanted"
    publishMessageToTopic(eventType)
    verify(exactly = 0, timeout = 5000) { domainEventListener.processMessage(any()) }
  }

  @Test
  fun `Save new location in prisoner locations table`() {
    val prisoner = Prisoner(prisonId = "A1234AB")
    val eventType = "prisoner-offender-search.prisoner.updated"
    val occurredAt = Instant.now().atZone(ZoneId.systemDefault())

    val applicationSchema = cas2ApplicationJsonSchemaEntityFactory.produceAndPersist {
      withAddedAt(OffsetDateTime.now())
      withId(UUID.randomUUID())
    }

    fun createApplication(userEntity: NomisUserEntity, offenderDetails: OffenderDetailSummary): Cas2ApplicationEntity {
      return cas2ApplicationEntityFactory.produceAndPersist {
        withApplicationSchema(applicationSchema)
        withCreatedByUser(userEntity)
        withSubmittedAt(OffsetDateTime.now())
        withCrn(offenderDetails.otherIds.crn)
        withCreatedAt(OffsetDateTime.now().minusDays(28))
        withConditionalReleaseDate(LocalDate.now().plusDays(1))
      }
    }

    givenACas2PomUser { userEntity, jwt ->
      givenAnOffender { offenderDetails, _ ->
        val application = createApplication(userEntity, offenderDetails)
        val detailUrl = "http://localhost:8080/api/pom-allocation/${application.nomsNumber}/3"

        val oldPrisonerLocation = Cas2PrisonerLocationEntity(
          id = UUID.randomUUID(),
          application = application,
          prisonCode = "LON",
          staffId = application.createdByUser.id,
          occurredAt = occurredAt.toOffsetDateTime(),
          endDate = null,
        )
        prisonerLocationRepository.deleteAll()
        prisonerLocationRepository.save(
          oldPrisonerLocation,
        )

        every { prisonerSearchClient.getPrisoner(any()) } returns prisoner

        @SuppressWarnings("MaxLineLength")
        val event =
          "{\"eventType\":\"$eventType\",\"detailUrl\":\"$detailUrl\",\"occurredAt\":\"$occurredAt\",\"additionalInformation\": {\"categoriesChanged\": [\"LOCATION\"]},\"personReference\":{\"identifiers\":[{\"type\":\"NOMS\",\"value\":\"${application.nomsNumber}\"}]}}"
        publishMessageToTopic(eventType, event)

        await().until { prisonerLocationRepository.count().toInt() == 2 }

        val locations = prisonerLocationRepository.findAll()

        assert(locations.last().prisonCode == prisoner.prisonId)
      }
    }
  }
}
