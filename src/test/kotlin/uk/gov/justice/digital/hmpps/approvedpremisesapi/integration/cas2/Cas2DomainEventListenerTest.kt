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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.*
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NomisStaffInformationFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NomisUserDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas2Assessor
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas2PomUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.nomisUserRolesMockSuccessfulGetStaffInformationByStaffIdCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.nomisUserRolesMockSuccessfulGetUserByUsernameCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.*
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EmailNotificationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.Cas2DomainEventListener
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomInt
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.UUID

const val OCCURRING_AT = "2025-03-25T10:15:23.000+00:00"
const val OLD_PRISON_CODE = "LIV"
const val NEW_PRISON_CODE = "LON"

class Cas2DomainEventListenerTest : IntegrationTestBase() {

  @Autowired
  private lateinit var hmppsQueueService: HmppsQueueService

  @Value("\${services.manage-pom-cases-api.base-url}")
  lateinit var managePomCasesBaseUrl: String


  @Value("\${services.prisoner-search.base-url}")
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
    val oldPrisonCode = "LIV"

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
            prisonCode = oldPrisonCode,
            allocatedPomUserId = null,
            createdAt = occurredAt.toOffsetDateTime().minusDays(1),
          )
          val oldApplicationAssignment = Cas2ApplicationAssignmentEntity(
            id = UUID.randomUUID(),
            application = application,
            prisonCode = oldPrisonCode,
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

          mockSuccessfulGetCallWithJsonResponse(
            url = "/prisons/id/$oldPrisonCode",
            responseBody = PrisonDto(prisonId = oldPrisonCode, prisonName = "HMS LIVERPOOL"),
          )

          mockSuccessfulGetCallWithJsonResponse(
            url = "/secure/prisons/id/$oldPrisonCode/department/contact-details?departmentType=OFFENDER_MANAGEMENT_UNIT",
            responseBody = OmuContactDetails(emailAddress = "oldOmu@prison.co.uk"),
          )

          mockSuccessfulGetCallWithJsonResponse(
            url = "/secure/prisons/id/${prisoner.prisonId}/department/contact-details?departmentType=OFFENDER_MANAGEMENT_UNIT",
            responseBody = OmuContactDetails(emailAddress = "newOmu@prison.co.uk"),
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
              eq("oldOmu@prison.co.uk"),
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
              eq("newOmu@prison.co.uk"),
              eq("1e5d98e4-efdf-428e-bca9-fd5daadd27aa"),
              eq(
                mapOf(
                  "nomsNumber" to application.nomsNumber,
                  "transferringPrisonName" to "HMS LIVERPOOL",
                  "link" to getLink(application.id),
                  "applicationStatus" to "Status Update",
                ),
              ),
              any(),
            )
          }
          verify(exactly = 1, timeout = 5000) {
            emailNotificationService.sendEmail(
              eq("nacro@example.com"),
              eq("e292b246-0d4e-4636-81f0-933bcf4dadd0"),
              eq(
                mapOf(
                  "nomsNumber" to application.nomsNumber,
                  "receivingPrisonName" to prisoner.prisonName,
                  "transferringPrisonName" to "HMS LIVERPOOL",
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
    val applicationSchema = cas2ApplicationJsonSchemaEntityFactory.produceAndPersist {
      withAddedAt(OffsetDateTime.now())
      withId(UUID.randomUUID())
    }
    givenACas2PomUser { oldUserEntity, _ ->
      givenACas2PomUser { newUserEntity, _ ->
        givenACas2Assessor { assessor, _ ->
          givenAnOffender { offenderDetails, _ ->

            val application = createApplicationAndApplicationAssignments(
              applicationSchema,
              oldUserEntity,
              offenderDetails,
            )
            val cas2StatusUpdateEntity = cas2StatusUpdateEntityFactory.produceAndPersist {
              withApplication(application)
              withLabel("Status Update")
              withAssessor(assessor)
            }
            statusUpdateRepository.save(cas2StatusUpdateEntity)

            val pomAllocation = PomAllocation(Manager(newUserEntity.nomisStaffId), Prison(NEW_PRISON_CODE))
            val url = "/allocation/${application.nomsNumber}/primary_pom"
            val detailUrl = managePomCasesBaseUrl + url

            mockSuccessfulGetCallWithJsonResponse(
              url = url,
              responseBody = pomAllocation,
            )

            mockSuccessfulGetCallWithJsonResponse(
              url = "/prisons/id/$OLD_PRISON_CODE",
              responseBody = PrisonDto(prisonId = OLD_PRISON_CODE, prisonName = "HMS LIVERPOOL"),
            )
            mockSuccessfulGetCallWithJsonResponse(
              url = "/prisons/id/$NEW_PRISON_CODE",
              responseBody = PrisonDto(prisonId = NEW_PRISON_CODE, prisonName = "HMS LONDON"),
            )

            val event = stubEvent(eventType, detailUrl, application.nomsNumber)
            publishMessageToTopic(eventType, event)
            await().until { applicationAssignmentRepository.count().toInt() == 3 }
            val locations = applicationAssignmentRepository.findAll()
            assert(locations.last().prisonCode == pomAllocation.prison.code)

            verify(exactly = 1, timeout = 5000) {
              emailNotificationService.sendEmail(
                eq("nacro@example.com"),
                eq("e36b226e-99f5-4d1f-83d3-12ef9a814a5b"),
                eq(
                  mapOf(
                    "nomsNumber" to application.nomsNumber,
                    "receivingPrisonName" to "HMS LONDON",
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
                    "transferringPrisonName" to "HMS LIVERPOOL",
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

  @ParameterizedTest
  @ValueSource(strings = ["offender-management.allocation.changed"])
  fun `Save new allocation in assignment table where pom allocation manager is not in database`(eventType: String) {
    val applicationSchema = cas2ApplicationJsonSchemaEntityFactory.produceAndPersist {
      withAddedAt(OffsetDateTime.now())
      withId(UUID.randomUUID())
    }
    givenACas2PomUser { oldUserEntity, _ ->
      givenACas2PomUser { newUserEntity, _ ->
        givenACas2Assessor { assessor, _ ->
          givenAnOffender { offenderDetails, _ ->
            val application = createApplicationAndApplicationAssignments(
              applicationSchema,
              oldUserEntity,
              offenderDetails,
            )

            val cas2StatusUpdateEntity = cas2StatusUpdateEntityFactory.produceAndPersist {
              withApplication(application)
              withLabel("Status Update")
              withAssessor(assessor)
            }
            statusUpdateRepository.save(cas2StatusUpdateEntity)

            val nomisStaffInformationResponse = NomisStaffInformationFactory().produce()

            val nomisUserDetailsResponse = NomisUserDetailFactory()
              .withUsername(nomisStaffInformationResponse.generalAccount.username)
              .produce()


            val pomAllocation = PomAllocation(Manager(code = nomisUserDetailsResponse.staffId), Prison(NEW_PRISON_CODE))
            val url = "/allocation/${application.nomsNumber}/primary_pom"
            val detailUrl = managePomCasesBaseUrl + url

            mockSuccessfulGetCallWithJsonResponse(
              url = url,
              responseBody = pomAllocation,
            )
            nomisUserRolesMockSuccessfulGetStaffInformationByStaffIdCall(
              staffId = nomisUserDetailsResponse.staffId,
              nomisStaffInformationResponse,
            )
            nomisUserRolesMockSuccessfulGetUserByUsernameCall(
              username = nomisStaffInformationResponse.generalAccount.username,
              nomisUserDetailsResponse,
            )
            mockSuccessfulGetCallWithJsonResponse(
              url = "/prisons/id/$OLD_PRISON_CODE",
              responseBody = PrisonDto(prisonId = OLD_PRISON_CODE, prisonName = "HMS LIVERPOOL"),
            )
            mockSuccessfulGetCallWithJsonResponse(
              url = "/prisons/id/$NEW_PRISON_CODE",
              responseBody = PrisonDto(prisonId = NEW_PRISON_CODE, prisonName = "HMS LONDON"),
            )

            val event = stubEvent(eventType, detailUrl, application.nomsNumber)
            publishMessageToTopic(eventType, event)
            await().until { applicationAssignmentRepository.count().toInt() == 3 }
            val locations = applicationAssignmentRepository.findAll()
            assert(locations.last().prisonCode == pomAllocation.prison.code)

            verify(exactly = 1, timeout = 5000) {
              emailNotificationService.sendEmail(
                eq("nacro@example.com"),
                eq("e36b226e-99f5-4d1f-83d3-12ef9a814a5b"),
                eq(
                  mapOf(
                    "nomsNumber" to application.nomsNumber,
                    "receivingPrisonName" to "HMS LONDON",
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
                    "transferringPrisonName" to "HMS LIVERPOOL",
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

  private fun createApplicationAndApplicationAssignments(
    applicationSchema: Cas2ApplicationJsonSchemaEntity,
    oldUserEntity: NomisUserEntity,
    offenderDetails: OffenderDetailSummary,
  ): Cas2ApplicationEntity {
    val application = cas2ApplicationEntityFactory.produceAndPersist {
      withApplicationSchema(applicationSchema)
      withCreatedByUser(oldUserEntity)
      withSubmittedAt(OffsetDateTime.now())
      withCrn(offenderDetails.otherIds.crn)
      withCreatedAt(OffsetDateTime.now().minusDays(28))
      withConditionalReleaseDate(LocalDate.now().plusDays(1))
    }

    val olderApplicationAssignment = Cas2ApplicationAssignmentEntity(
      id = UUID.randomUUID(),
      application = application,
      prisonCode = OLD_PRISON_CODE,
      allocatedPomUserId = oldUserEntity.id,
      createdAt = OffsetDateTime.parse(OCCURRING_AT),
    )
    val oldApplicationAssignment = Cas2ApplicationAssignmentEntity(
      id = UUID.randomUUID(),
      application = application,
      prisonCode = NEW_PRISON_CODE,
      allocatedPomUserId = null,
      createdAt = OffsetDateTime.parse(OCCURRING_AT),
    )

    applicationAssignmentRepository.save(
      oldApplicationAssignment,
    )
    applicationAssignmentRepository.save(
      olderApplicationAssignment,
    )
    return application
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
