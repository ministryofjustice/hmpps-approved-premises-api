package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas2

import com.ninjasquad.springmockk.SpykBean
import io.mockk.verify
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.Manager
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PomAllocation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.Prison
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.Prisoner
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas2Assessor
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas2PomUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationAssignmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationAssignmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2StatusUpdateRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.Agency
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EmailNotificationService
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
  private lateinit var emailNotificationService: EmailNotificationService

  @SpykBean
  private lateinit var applicationAssignmentRepository: Cas2ApplicationAssignmentRepository

  @SpykBean
  lateinit var statusUpdateRepository: Cas2StatusUpdateRepository

  private val domainEventsTopic by lazy {
    hmppsQueueService.findByTopicId("domainevents") ?: throw MissingQueueException("HmppsTopic domainevents not found")
  }

  private val domainEventsClient by lazy { domainEventsTopic.snsClient }

  private fun getLink(applicationId: UUID): String = "http://cas2.frontend/applications/#id/overview".replace("#id", applicationId.toString())

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
    val prisoner = Prisoner(prisonId = "LON", prisonName = "LONDON")
    val eventType = "prisoner-offender-search.prisoner.updated"
    val occurredAt = Instant.now().atZone(ZoneId.systemDefault())

    val applicationSchema = cas2ApplicationJsonSchemaEntityFactory.produceAndPersist {
      withAddedAt(OffsetDateTime.now())
      withId(UUID.randomUUID())
    }
    givenACas2PomUser { userEntity, jwt ->
      givenACas2Assessor { assessor, _ ->
        givenAnOffender { offenderDetails, _ ->
          val application = cas2ApplicationEntityFactory.produceAndPersist {
            withApplicationSchema(applicationSchema)
            withCreatedByUser(userEntity)
            withSubmittedAt(OffsetDateTime.now())
            withCrn(offenderDetails.otherIds.crn)
            withCreatedAt(OffsetDateTime.now().minusDays(28))
            withConditionalReleaseDate(LocalDate.now().plusDays(1))
          }

          val cas2StatusUpdateEntity = cas2StatusUpdateEntityFactory.produceAndPersist {
            withApplication(application)
            withLabel("Status Update")
            withAssessor(assessor)
          }
          statusUpdateRepository.save(cas2StatusUpdateEntity)

          val url = "/prisoner/${application.nomsNumber}"
          val detailUrl = prisonerSearchBaseUrl + url
          val olderApplicationAssignment = Cas2ApplicationAssignmentEntity(
            id = UUID.randomUUID(),
            application = application,
            prisonCode = "NEW",
            allocatedPomUserId = null,
            createdAt = occurredAt.toOffsetDateTime().minusDays(1),
          )
          val oldApplicationAssignment = Cas2ApplicationAssignmentEntity(
            id = UUID.randomUUID(),
            application = application,
            prisonCode = "NEW",
            allocatedPomUserId = userEntity.id,
            createdAt = occurredAt.toOffsetDateTime().minusDays(2),
          )
          applicationAssignmentRepository.save(
            olderApplicationAssignment,
          )
          applicationAssignmentRepository.save(
            oldApplicationAssignment,
          )

          mockSuccessfulGetCallWithJsonResponse(
            url = url,
            responseBody = prisoner,
          )

          val agency =
            Agency(agencyId = "NEW", description = "NEWCASTLE", agencyType = "prison")

          mockSuccessfulGetCallWithJsonResponse(
            url = "/api/agencies/${agency.agencyId}",
            responseBody = agency,
          )

          @SuppressWarnings("MaxLineLength")
          val event =
            "{\"eventType\":\"$eventType\",\"detailUrl\":\"$detailUrl\",\"occurredAt\":\"$occurredAt\",\"additionalInformation\": {\"categoriesChanged\": [\"LOCATION\"]},\"personReference\":{\"identifiers\":[{\"type\":\"NOMS\",\"value\":\"${application.nomsNumber}\"}]}}"
          publishMessageToTopic(eventType, event)
          await().until { applicationAssignmentRepository.count().toInt() == 3 }
          val locations = applicationAssignmentRepository.findAll()
          assert(locations.last().prisonCode == prisoner.prisonId)

          verify(exactly = 1, timeout = 5000) {
            emailNotificationService.sendEmail(
              eq(userEntity.email!!),
              eq("5adb6390-0c95-4458-a8b5-3e61ff780715"),
              eq(
                mapOf(
                  "nomsNumber" to application.nomsNumber,
                  "receivingPrisonName" to prisoner.prisonName,
                ),
              ),
              any(),
            )
          }
          verify(exactly = 1, timeout = 5000) {
            emailNotificationService.sendEmail(
              eq("tbc"),
              eq("6b427e8a-eb21-43a3-89c3-f6a147b20c39"),
              eq(
                mapOf(
                  "nomsNumber" to application.nomsNumber,
                  "receivingPrisonName" to prisoner.prisonName,
                ),
              ),
              any(),
            )
          }
          verify(exactly = 1, timeout = 5000) {
            emailNotificationService.sendEmail(
              eq("tbc"),
              eq("1e5d98e4-efdf-428e-bca9-fd5daadd27aa"),
              eq(
                mapOf(
                  "nomsNumber" to application.nomsNumber,
                  "transferringPrisonName" to agency.description,
                  "link" to getLink(application.id),
                  "applicationStatus" to "Status Update",
                ),
              ),
              any(),
            )
          }
          verify(exactly = 1, timeout = 5000) {
            emailNotificationService.sendEmail(
              eq("referrals@nacrocas2.org.uk"),
              eq("e292b246-0d4e-4636-81f0-933bcf4dadd0"),
              eq(
                mapOf(
                  "nomsNumber" to application.nomsNumber,
                  "receivingPrisonName" to prisoner.prisonName,
                  "transferringPrisonName" to agency.description,
                  "link" to getLink(application.id),
                ),
              ),
              any(),
            )
          }
        }
      }
    }
  }

  @Test
  fun `Save new allocation in assignment table`() {
    val eventType = "offender-management.allocation.changed"
    val occurredAt = Instant.now().atZone(ZoneId.systemDefault())
    val applicationSchema = cas2ApplicationJsonSchemaEntityFactory.produceAndPersist {
      withAddedAt(OffsetDateTime.now())
      withId(UUID.randomUUID())
    }
    givenACas2PomUser { oldUserEntity, _ ->
      givenACas2PomUser { newUserEntity, _ ->
        givenACas2Assessor { assessor, _ ->
          givenAnOffender { offenderDetails, _ ->
            val application = cas2ApplicationEntityFactory.produceAndPersist {
              withApplicationSchema(applicationSchema)
              withCreatedByUser(oldUserEntity)
              withSubmittedAt(OffsetDateTime.now())
              withCrn(offenderDetails.otherIds.crn)
              withCreatedAt(OffsetDateTime.now().minusDays(28))
              withConditionalReleaseDate(LocalDate.now().plusDays(1))
            }
            val cas2StatusUpdateEntity = cas2StatusUpdateEntityFactory.produceAndPersist {
              withApplication(application)
              withLabel("Status Update")
              withAssessor(assessor)
            }
            statusUpdateRepository.save(cas2StatusUpdateEntity)

            val oldPrisonCode = "LIV"
            val newPrisonCode = "LON"

            val pomAllocation = PomAllocation(Manager(newUserEntity.nomisStaffId), Prison(newPrisonCode))
            val url = "/allocation/${application.nomsNumber}/primary_pom"
            val detailUrl = managePomCasesBaseUrl + url

            val olderApplicationAssignment = Cas2ApplicationAssignmentEntity(
              id = UUID.randomUUID(),
              application = application,
              prisonCode = oldPrisonCode,
              allocatedPomUserId = oldUserEntity.id,
              createdAt = occurredAt.toOffsetDateTime(),
            )
            val oldApplicationAssignment = Cas2ApplicationAssignmentEntity(
              id = UUID.randomUUID(),
              application = application,
              prisonCode = newPrisonCode,
              allocatedPomUserId = null,
              createdAt = occurredAt.toOffsetDateTime(),
            )

            applicationAssignmentRepository.save(
              oldApplicationAssignment,
            )
            applicationAssignmentRepository.save(
              olderApplicationAssignment,
            )

            mockSuccessfulGetCallWithJsonResponse(
              url = url,
              responseBody = pomAllocation,
            )

            val newAgency =
              Agency(agencyId = newPrisonCode, description = "HMS LONDON", agencyType = "prison")

            mockSuccessfulGetCallWithJsonResponse(
              url = "/api/agencies/${newAgency.agencyId}",
              responseBody = newAgency,
            )

            val oldAgency =
              Agency(agencyId = oldPrisonCode, description = "HMS LIVERPOOL", agencyType = "prison")

            mockSuccessfulGetCallWithJsonResponse(
              url = "/api/agencies/${oldAgency.agencyId}",
              responseBody = oldAgency,
            )

            @SuppressWarnings("MaxLineLength")
            val event =
              "{\"eventType\":\"$eventType\",\"detailUrl\":\"$detailUrl\",\"occurredAt\":\"$occurredAt\",\"additionalInformation\": {\"categoriesChanged\": [\"LOCATION\"]},\"personReference\":{\"identifiers\":[{\"type\":\"NOMS\",\"value\":\"${application.nomsNumber}\"}]}}"
            publishMessageToTopic(eventType, event)
            await().until { applicationAssignmentRepository.count().toInt() == 3 }
            val locations = applicationAssignmentRepository.findAll()
            assert(locations.last().prisonCode == pomAllocation.prison.code)

            verify(exactly = 1, timeout = 5000) {
              emailNotificationService.sendEmail(
                eq("referrals@nacrocas2.org.uk"),
                eq("e36b226e-99f5-4d1f-83d3-12ef9a814a5b"),
                eq(
                  mapOf(
                    "nomsNumber" to application.nomsNumber,
                    "receivingPrisonName" to newAgency.description,
                    "link" to getLink(application.id),
                  ),
                ),
                any(),
              )
            }

            verify(exactly = 1, timeout = 5000) {
              emailNotificationService.sendEmail(
                eq(newUserEntity.email!!),
                eq("289d4004-3c95-4c23-b0fa-9187d9da8eaf"),
                eq(
                  mapOf(
                    "nomsNumber" to application.nomsNumber,
                    "transferringPrisonName" to oldAgency.description,
                    "link" to getLink(application.id),
                    "applicationStatus" to "Status Update",
                  ),
                ),
                any(),
              )
            }
          }
        }
      }
    }
  }
}
