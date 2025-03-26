package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas2

import com.ninjasquad.springmockk.SpykBean
import io.mockk.verify
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.Manager
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PomAllocation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.Prison
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.Prisoner
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas2PomUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationAssignmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationAssignmentRepository
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

  @Value("\${services.manage-pom-cases-api.base-url}")
  lateinit var managePomCasesBaseUrl: String

  @Value("\${services.prisoner-search-api.base-url}")
  lateinit var prisonerSearchBaseUrl: String

  @SpykBean
  private lateinit var domainEventListener: Cas2DomainEventListener

  @SpykBean
  private lateinit var applicationAssignmentRepository: Cas2ApplicationAssignmentRepository

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
  fun `Save new location in assignment table`() {
    val prisoner = Prisoner(prisonId = "A1234AB")
    val eventType = "prisoner-offender-search.prisoner.updated"
    val occurredAt = Instant.now().atZone(ZoneId.systemDefault())

    val applicationSchema = cas2ApplicationJsonSchemaEntityFactory.produceAndPersist {
      withAddedAt(OffsetDateTime.now())
      withId(UUID.randomUUID())
    }
    givenACas2PomUser { userEntity, jwt ->
      givenAnOffender { offenderDetails, _ ->
        val application = cas2ApplicationEntityFactory.produceAndPersist {
          withApplicationSchema(applicationSchema)
          withCreatedByUser(userEntity)
          withSubmittedAt(OffsetDateTime.now())
          withCrn(offenderDetails.otherIds.crn)
          withCreatedAt(OffsetDateTime.now().minusDays(28))
          withConditionalReleaseDate(LocalDate.now().plusDays(1))
        }
        val url = "/prisoner/${application.nomsNumber}"
        val detailUrl = prisonerSearchBaseUrl + url

        val oldApplicationAssignment = Cas2ApplicationAssignmentEntity(
          id = UUID.randomUUID(),
          application = application,
          prisonCode = "LON",
          allocatedPomUser = application.createdByUser,
          createdAt = occurredAt.toOffsetDateTime(),
        )
        applicationAssignmentRepository.deleteAll()
        applicationAssignmentRepository.save(
          oldApplicationAssignment,
        )

        mockSuccessfulGetCallWithJsonResponse(
          url = url,
          responseBody = prisoner,
        )

        @SuppressWarnings("MaxLineLength")
        val event =
          "{\"description\":\"$eventType\",\"eventType\":\"$eventType\",\"detailUrl\":\"$detailUrl\",\"occurredAt\":\"$occurredAt\",\"additionalInformation\": {\"categoriesChanged\": [\"LOCATION\"]},\"personReference\":{\"identifiers\":[{\"type\":\"NOMS\",\"value\":\"${application.nomsNumber}\"}]}}"
        publishMessageToTopic(eventType, event)
        await().until { applicationAssignmentRepository.count().toInt() == 2 }
        val locations = applicationAssignmentRepository.findAll()
        assert(locations.last().prisonCode == prisoner.prisonId)
      }
    }
  }

  fun String.setNoms(value: String) = replace("%NOMS%", value)
  fun String.setStaffId(value: String) = replace("%STAFFID%", value)

  @ParameterizedTest
  @ValueSource(strings = ["offender-management.allocation.changed"])
  fun `Save new allocation in assignment table`(eventType: String) {
    val occurredAt = OffsetDateTime.now()
    val applicationSchema = cas2ApplicationJsonSchemaEntityFactory.produceAndPersist {
      withAddedAt(OffsetDateTime.now())
      withId(UUID.randomUUID())
    }
    givenACas2PomUser { userEntity, jwt ->
      givenAnOffender { offenderDetails, _ ->
        val application = cas2ApplicationEntityFactory.produceAndPersist {
          withApplicationSchema(applicationSchema)
          withCreatedByUser(userEntity)
          withSubmittedAt(OffsetDateTime.now())
          withCrn(offenderDetails.otherIds.crn)
          withCreatedAt(OffsetDateTime.now().minusDays(28))
          withConditionalReleaseDate(LocalDate.now().plusDays(1))
        }

        val otherUser = nomisUserEntityFactory.produceAndPersist { withNomisStaffIdentifier(123456L) }
        val pomAllocation = PomAllocation(Manager(otherUser.nomisStaffId), Prison("LEI"))

        val url = "/allocation/${application.nomsNumber}/primary_pom"
        val detailUrl = managePomCasesBaseUrl + url

        mockSuccessfulGetCallWithJsonResponse(url = url, responseBody = pomAllocation)

        val oldApplicationAssignment = Cas2ApplicationAssignmentEntity(
          id = UUID.randomUUID(),
          application = application,
          prisonCode = "LON",
          allocatedPomUser = null,
          createdAt = occurredAt,
        )
        applicationAssignmentRepository.save(
          oldApplicationAssignment,
        )

        @SuppressWarnings("MaxLineLength")
        val event =
          "{\"eventType\":\"$eventType\",\"detailUrl\":\"$detailUrl\",\"occurredAt\":\"$occurredAt\",\"additionalInformation\": {\"categoriesChanged\": [\"LOCATION\"]},\"personReference\":{\"identifiers\":[{\"type\":\"NOMS\",\"value\":\"${application.nomsNumber}\"}]}}"
        publishMessageToTopic(eventType, event)
        await().until { applicationAssignmentRepository.count().toInt() == 2 }
        val locations = applicationAssignmentRepository.findAll()
        assert(locations.last().prisonCode == pomAllocation.prison.code)
      }
    }
  }
}
