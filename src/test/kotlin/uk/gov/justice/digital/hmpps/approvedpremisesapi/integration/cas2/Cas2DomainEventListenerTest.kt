package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas2

import com.ninjasquad.springmockk.SpykBean
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NomisStaffInformationFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NomisUserDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas2PomUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.nomisUserRolesMockSuccessfulGetStaffInformationByStaffIdCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.nomisUserRolesMockSuccessfulGetUserByUsernameCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationAssignmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationAssignmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NomisUserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.Cas2DomainEventListener
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomInt
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

const val OCCURRING_AT = "2025-03-25T10:15:23.000+00:00"

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

    val applicationSchema = cas2ApplicationJsonSchemaEntityFactory.produceAndPersist {
      withAddedAt(OffsetDateTime.now())
      withId(UUID.randomUUID())
    }
    givenACas2PomUser { userEntity, _ ->
      givenAnOffender { offenderDetails, _ ->
        val application = createApplicationAndApplicationAssignment(
          allocateApplicationUserAsAssignmentPomUser = true,
          applicationSchema,
          userEntity,
          offenderDetails,
        )

        val url = "/prisoner/${application.nomsNumber}"
        val detailUrl = prisonerSearchBaseUrl + url

        mockSuccessfulGetCallWithJsonResponse(
          url = url,
          responseBody = prisoner,
        )

        val event = stubEvent(eventType, detailUrl, application.nomsNumber)
        publishMessageToTopic(eventType, event)
        await().until { applicationAssignmentRepository.count().toInt() == 2 }
        val locations = applicationAssignmentRepository.findAll()
        assert(locations.last().prisonCode == prisoner.prisonId)
      }
    }
  }

  @ParameterizedTest
  @ValueSource(strings = ["offender-management.allocation.changed"])
  fun `Save new allocation in assignment table`(eventType: String) {
    val applicationSchema = cas2ApplicationJsonSchemaEntityFactory.produceAndPersist {
      withAddedAt(OffsetDateTime.now())
      withId(UUID.randomUUID())
    }
    givenACas2PomUser { userEntity, _ ->
      givenAnOffender { offenderDetails, _ ->
        val application = createApplicationAndApplicationAssignment(
          allocateApplicationUserAsAssignmentPomUser = false,
          applicationSchema,
          userEntity,
          offenderDetails,
        )
        val otherUser = nomisUserEntityFactory.produceAndPersist { withNomisStaffIdentifier(123456L) }
        val (pomAllocation, detailUrl) = wiremockPomAllocation(
          managerCode = otherUser.nomisStaffId,
          application,
        )

        val event = stubEvent(eventType, detailUrl, application.nomsNumber)
        publishMessageToTopic(eventType, event)

        await().until { applicationAssignmentRepository.count().toInt() == 2 }
        val locations = applicationAssignmentRepository.findAll()
        assert(locations.last().prisonCode == pomAllocation.prison.code)
      }
    }
  }

  @ParameterizedTest
  @ValueSource(strings = ["offender-management.allocation.changed"])
  fun `Save new allocation in assignment table where pom allocation manager is not in database`(eventType: String) {
    val applicationSchema = cas2ApplicationJsonSchemaEntityFactory.produceAndPersist {
      withAddedAt(OffsetDateTime.now())
      withId(UUID.randomUUID())
    }
    givenACas2PomUser { userEntity, _ ->
      givenAnOffender { offenderDetails, _ ->
        val application = createApplicationAndApplicationAssignment(
          allocateApplicationUserAsAssignmentPomUser = false,
          applicationSchema,
          userEntity,
          offenderDetails,
        )
        val (pomAllocation, detailUrl) = wiremockPomAllocation(
          managerCode = randomInt(2000, 5000).toLong(),
          application,
        )

        val nomisStaffInformationResponse = NomisStaffInformationFactory().produce()
        nomisUserRolesMockSuccessfulGetStaffInformationByStaffIdCall(
          staffId = pomAllocation.manager.code,
          nomisStaffInformationResponse,
        )

        val nomisUserDetailsResponse = NomisUserDetailFactory()
          .withUsername(nomisStaffInformationResponse.generalAccount.username)
          .produce()

        nomisUserRolesMockSuccessfulGetUserByUsernameCall(
          username = nomisStaffInformationResponse.generalAccount.username,
          nomisUserDetailsResponse,
        )

        assertThat(
          nomisUserRepository.findByNomisUsername(
            nomisUsername = nomisStaffInformationResponse.generalAccount.username,
          ),
        ).isNull()

        val event = stubEvent(eventType, detailUrl, application.nomsNumber)
        publishMessageToTopic(eventType, event)

        await().until { applicationAssignmentRepository.count().toInt() == 2 }
        val locations = applicationAssignmentRepository.findAll()
        assert(locations.last().prisonCode == pomAllocation.prison.code)

        assertThat(
          nomisUserRepository.findByNomisUsername(
            nomisUsername = nomisStaffInformationResponse.generalAccount.username,
          ),
        ).isNotNull
      }
    }
  }

  private fun createApplicationAndApplicationAssignment(
    allocateApplicationUserAsAssignmentPomUser: Boolean,
    applicationSchema: Cas2ApplicationJsonSchemaEntity,
    userEntity: NomisUserEntity,
    offenderDetails: OffenderDetailSummary,
  ): Cas2ApplicationEntity {
    val application = cas2ApplicationEntityFactory.produceAndPersist {
      withApplicationSchema(applicationSchema)
      withCreatedByUser(userEntity)
      withSubmittedAt(OffsetDateTime.now())
      withCrn(offenderDetails.otherIds.crn)
      withCreatedAt(OffsetDateTime.now().minusDays(28))
      withConditionalReleaseDate(LocalDate.now().plusDays(1))
    }

    val allocatedPomUserId = if (allocateApplicationUserAsAssignmentPomUser) {
      application.createdByUser.id
    } else {
      null
    }

    val oldApplicationAssignment = Cas2ApplicationAssignmentEntity(
      id = UUID.randomUUID(),
      application = application,
      prisonCode = "LON",
      allocatedPomUserId = allocatedPomUserId,
      createdAt = OffsetDateTime.parse(OCCURRING_AT),
    )
    applicationAssignmentRepository.deleteAll()
    applicationAssignmentRepository.save(
      oldApplicationAssignment,
    )

    return application
  }

  private fun wiremockPomAllocation(managerCode: Long, application: Cas2ApplicationEntity): Pair<PomAllocation, String> {
    val pomAllocation = PomAllocation(Manager(code = managerCode), Prison("LEI"))
    val url = "/allocation/${application.nomsNumber}/primary_pom"
    val detailUrl = managePomCasesBaseUrl + url
    mockSuccessfulGetCallWithJsonResponse(url, responseBody = pomAllocation)
    return pomAllocation to detailUrl
  }

  private fun stubEvent(eventType: String, detailUrl: String, nomsNumber: String?) = """
    {
       "eventType":"$eventType",
       "description": "Test desc",
       "detailUrl":"$detailUrl",
       "occurredAt":"$OCCURRING_AT",
       "additionalInformation":{
          "categoriesChanged":[
             "LOCATION"
          ]
       },
       "personReference":{
          "identifiers":[
             {
                "type":"NOMS",
                "value":"$nomsNumber"
             }
          ]
       }
    }
  """.trimIndent()
}
