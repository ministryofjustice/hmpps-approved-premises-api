package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.integration

import com.ninjasquad.springmockk.SpykBean
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationAssignmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationAssignmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2DomainEventListener
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.Manager
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PomAllocation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.Prison
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.Prisoner
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NomisStaffInformationFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NomisUserDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderManagementUnitEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas2Assessor
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas2PomUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.nomisUserRolesMockSuccessfulGetStaffInformationByStaffIdCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.nomisUserRolesMockSuccessfulGetUserByUsernameCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OffenderManagementUnitEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EmailNotificationService
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
  private lateinit var emailNotificationService: EmailNotificationService

  @SpykBean
  private lateinit var applicationAssignmentRepository: Cas2ApplicationAssignmentRepository

  private val domainEventsTopic by lazy {
    hmppsQueueService.findByTopicId("domainevents") ?: throw MissingQueueException("HmppsTopic domainevents not found")
  }

  private val domainEventsClient by lazy { domainEventsTopic.snsClient }

  private fun getLink(applicationId: UUID): String = "http://cas2.frontend/applications/#id/overview".replace("#id", applicationId.toString())
  private fun getAssessorLink(applicationId: UUID): String = "http://cas2.frontend/assess/applications/#applicationId/overview".replace(
    "#applicationId",
    applicationId.toString(),
  )

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

  private val oldOmu = OffenderManagementUnitEntityFactory().withPrisonCode("LIV").withPrisonName("HMP LIVERPOOL").produce()
  private val newOmu = OffenderManagementUnitEntityFactory().withPrisonCode("LON").withPrisonName("HMP LONDON").produce()

  @ParameterizedTest
  @ValueSource(strings = ["offender-management.allocation.changed", "prisoner-offender-search.prisoner.updated"])
  fun `Start to process messages on Domain Events Topic`(eventType: String) {
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
  fun `Updates current prison code when prisoner changes location`() {
    val prisoner = Prisoner(prisonId = newOmu.prisonCode, prisonName = newOmu.prisonName)
    val eventType = "prisoner-offender-search.prisoner.updated"

    givenACas2PomUser { userEntity, _ ->
      givenACas2Assessor { assessor, _ ->
        givenAnOffender { offenderDetails, _ ->
          val application = cas2ApplicationEntityFactory.produceAndPersist {
            withCreatedByUser(userEntity)
            withReferringPrisonCode(oldOmu.prisonCode)
            withSubmittedAt(OffsetDateTime.now())
            withCrn(offenderDetails.otherIds.crn)
            withCreatedAt(OffsetDateTime.now().minusDays(28))
            withConditionalReleaseDate(LocalDate.now().plusDays(1))
          }

          application.createApplicationAssignment(application.referringPrisonCode!!, userEntity)
          cas2ApplicationRepository.save(application)

          cas2StatusUpdateEntityFactory.produceAndPersist {
            withApplication(application)
            withLabel("Status Update")
            withAssessor(assessor)
          }

          val url = "/prisoner/${application.nomsNumber}"
          val detailUrl = prisonerSearchBaseUrl + url

          mockSuccessfulGetCallWithJsonResponse(
            url = url,
            responseBody = prisoner,
          )

          offenderManagementUnitRepository.save(oldOmu)
          offenderManagementUnitRepository.save(newOmu)

          val event = stubEvent(eventType, detailUrl, application.nomsNumber)
          publishMessageToTopic(eventType, event)

          await().until { applicationAssignmentRepository.count().toInt() == 2 }

          val updatedApplication = cas2ApplicationRepository.findByIdOrNull(application.id)!!
          assertThat(updatedApplication.applicationAssignments.size).isEqualTo(2)
          assertThat(updatedApplication.currentPrisonCode).isEqualTo(newOmu.prisonCode)
          assertThat(updatedApplication.currentPomUserId).isNull()

          verifyEmailsSentForPrisonerUpdatedCase(application, userEntity, oldOmu, newOmu)
        }
      }
    }
  }

  @Test
  fun `Allocation message is successfully processed when location message has been received first`() {
    val eventType = "offender-management.allocation.changed"
    givenACas2PomUser(nomisUserDetailsConfigBlock = { withActiveCaseloadId(oldOmu.prisonCode) }) { createdByUser, _ ->
      givenACas2PomUser(nomisUserDetailsConfigBlock = { withActiveCaseloadId(newOmu.prisonCode) }) { newUserEntity, _ ->
        givenACas2Assessor { assessor, _ ->
          givenAnOffender { offenderDetails, _ ->
            offenderManagementUnitRepository.save(oldOmu)
            offenderManagementUnitRepository.save(newOmu)

            val application = createApplicationAndInitialAssignment(createdByUser, offenderDetails, oldOmu)

            // create the location changed assainment
            application.createApplicationAssignment(newOmu.prisonCode, null)
            cas2ApplicationRepository.saveAndFlush(application)

            cas2StatusUpdateEntityFactory.produceAndPersist {
              withApplication(application)
              withLabel("Status Update")
              withAssessor(assessor)
            }

            val pomAllocation = PomAllocation(Manager(newUserEntity.nomisStaffId!!), Prison(newOmu.prisonCode))
            val url = "/allocation/${application.nomsNumber}/primary_pom"
            val detailUrl = managePomCasesBaseUrl + url
            mockSuccessfulGetCallWithJsonResponse(
              url = url,
              responseBody = pomAllocation,
            )

            val event = stubEvent(eventType, detailUrl, application.nomsNumber)
            publishMessageToTopic(eventType, event)
            await().until { applicationAssignmentRepository.count().toInt() == 3 }

            val updatedApplication = cas2ApplicationRepository.findByIdOrNull(application.id)!!
            assertThat(updatedApplication.applicationAssignments.size).isEqualTo(3)

            assertThat(updatedApplication.applicationAssignments.get(1).allocatedPomUser).isNull()
            assertThat(updatedApplication.applicationAssignments.get(1).prisonCode).isEqualTo(newOmu.prisonCode)

            assertThat(updatedApplication.currentPrisonCode).isEqualTo(newOmu.prisonCode)
            assertThat(updatedApplication.currentPomUserId).isEqualTo(newUserEntity.id)

            verifyEmailsSentForAllocationChangedCase(
              application,
              pomManagerEmail = newUserEntity.email!!,
            )
          }
        }
      }
    }
  }

  @Test
  fun `POM allocation is processed when location change message has not been received`() {
    val eventType = "offender-management.allocation.changed"
    givenACas2PomUser(nomisUserDetailsConfigBlock = { withActiveCaseloadId(oldOmu.prisonCode) }) { oldUserEntity, _ ->
      givenACas2PomUser(nomisUserDetailsConfigBlock = { withActiveCaseloadId(newOmu.prisonCode) }) { newUserEntity, _ ->
        givenACas2Assessor { assessor, _ ->
          givenAnOffender { offenderDetails, _ ->
            offenderManagementUnitRepository.save(oldOmu)
            offenderManagementUnitRepository.save(newOmu)

            // create application with only initial assignment
            val application = createApplicationAndInitialAssignment(oldUserEntity, offenderDetails)

            cas2StatusUpdateEntityFactory.produceAndPersist {
              withApplication(application)
              withLabel("Status Update")
              withAssessor(assessor)
            }

            val pomAllocation = PomAllocation(Manager(newUserEntity.nomisStaffId!!), Prison(newOmu.prisonCode))
            val url = "/allocation/${application.nomsNumber}/primary_pom"
            val detailUrl = managePomCasesBaseUrl + url
            mockSuccessfulGetCallWithJsonResponse(
              url = url,
              responseBody = pomAllocation,
            )

            val event = stubEvent(eventType, detailUrl, application.nomsNumber)
            publishMessageToTopic(eventType, event)

            // pom allocation message should create location change assignment, and pom allocation assignment
            await().until { applicationAssignmentRepository.count().toInt() == 3 }

            val updatedApplication = cas2ApplicationRepository.findByIdOrNull(application.id)!!
            assertThat(updatedApplication.applicationAssignments.size).isEqualTo(3)

            assertThat(updatedApplication.applicationAssignments.get(1).allocatedPomUser).isNull()
            assertThat(updatedApplication.applicationAssignments.get(1).prisonCode).isEqualTo(newOmu.prisonCode)

            assertThat(updatedApplication.currentPrisonCode).isEqualTo(newOmu.prisonCode)
            assertThat(updatedApplication.currentPomUserId).isEqualTo(newUserEntity.id)

            verifyEmailsSentForAllocationChangedCase(
              application,
              pomManagerEmail = newUserEntity.email!!,
            )

            verifyEmailsSentForPrisonerUpdatedCase(application, oldUserEntity, oldOmu, newOmu)
          }
        }
      }
    }
  }

  @Test
  fun `Save new allocation in assignment table but do not send emails if same prison`() {
    val eventType = "offender-management.allocation.changed"
    givenACas2PomUser { oldUserEntity, _ ->
      givenACas2PomUser { newUserEntity, _ ->
        givenAnOffender { offenderDetails, _ ->

          val application = createApplicationAndApplicationAssignmentsWithoutLocationEvent(
            oldUserEntity,
            offenderDetails,
          )

          val pomAllocation = PomAllocation(Manager(newUserEntity.nomisStaffId!!), Prison(oldOmu.prisonCode))
          val url = "/allocation/${application.nomsNumber}/primary_pom"
          val detailUrl = managePomCasesBaseUrl + url

          mockSuccessfulGetCallWithJsonResponse(
            url = url,
            responseBody = pomAllocation,
          )

          val event = stubEvent(eventType, detailUrl, application.nomsNumber)
          publishMessageToTopic(eventType, event)
          await().until { applicationAssignmentRepository.count().toInt() == 2 }
          val locations = applicationAssignmentRepository.findAll()
          assertThat(locations.last().prisonCode).isEqualTo(pomAllocation.prison.code)

          verifyEmailsNotSentForAllocationChangedCase(
            application,
            pomManagerEmail = newUserEntity.email!!,
          )
        }
      }
    }
  }

  @ParameterizedTest
  @ValueSource(strings = ["offender-management.allocation.changed"])
  fun `Save new allocation in assignment table where pom allocation manager is not in database`(eventType: String) {
    givenACas2PomUser(nomisUserDetailsConfigBlock = { withActiveCaseloadId(oldOmu.prisonCode) }) { oldUserEntity, _ ->
      givenACas2Assessor { assessor, _ ->
        givenAnOffender { offenderDetails, _ ->
          offenderManagementUnitRepository.save(oldOmu)
          offenderManagementUnitRepository.save(newOmu)

          val application = createApplicationAndInitialAssignment(oldUserEntity, offenderDetails)

          cas2StatusUpdateEntityFactory.produceAndPersist {
            withApplication(application)
            withLabel("Status Update")
            withAssessor(assessor)
            withCreatedAt(OffsetDateTime.now())
          }

          val nomisStaffInformationResponse = NomisStaffInformationFactory().produce()
          val newUserName = nomisStaffInformationResponse.generalAccount.username
          val nomisUserDetailsResponse = NomisUserDetailFactory()
            .withActiveCaseloadId(newOmu.prisonCode)
            .withUsername(newUserName)
            .produce()

          val pomAllocation = PomAllocation(Manager(code = nomisUserDetailsResponse.staffId), Prison(newOmu.prisonCode))
          val url = "/allocation/${application.nomsNumber}/primary_pom"
          val detailUrl = managePomCasesBaseUrl + url
          mockSuccessfulGetCallWithJsonResponse(
            url = url,
            responseBody = pomAllocation,
          )

          // extra mocks to retrieve pom manager from nomis-user-roles API
          nomisUserRolesMockSuccessfulGetStaffInformationByStaffIdCall(
            staffId = nomisUserDetailsResponse.staffId,
            nomisStaffInformationResponse,
          )
          nomisUserRolesMockSuccessfulGetUserByUsernameCall(
            username = newUserName,
            nomisUserDetailsResponse,
          )

          assertThat(cas2UserRepository.findByUsername(username = newUserName)).isNull()

          val event = stubEvent(eventType, detailUrl, application.nomsNumber)
          publishMessageToTopic(eventType, event)
          await().until { applicationAssignmentRepository.count().toInt() == 3 }

          val updatedApplication = cas2ApplicationRepository.findByIdOrNull(application.id)!!
          assertThat(updatedApplication.applicationAssignments.size).isEqualTo(3)

          assertThat(updatedApplication.applicationAssignments.get(1).allocatedPomUser).isNull()
          assertThat(updatedApplication.applicationAssignments.get(1).prisonCode).isEqualTo(newOmu.prisonCode)

          assertThat(updatedApplication.currentPrisonCode).isEqualTo(newOmu.prisonCode)

          val missingUser = cas2UserRepository.findByUsername(newUserName)
          assertThat(updatedApplication.currentPomUserId).isEqualTo(missingUser!!.id)

          verifyEmailsSentForAllocationChangedCase(
            application,
            pomManagerEmail = nomisUserDetailsResponse.primaryEmail!!,
          )

          assertThat(cas2UserRepository.findByUsername(username = newUserName)).isNotNull
        }
      }
    }
  }

  private fun verifyEmailsSentForPrisonerUpdatedCase(
    application: Cas2ApplicationEntity,
    userEntity: Cas2UserEntity,
    oldOmu: OffenderManagementUnitEntity,
    newOmu: OffenderManagementUnitEntity,
  ) {
    verify(exactly = 1, timeout = 5000) {
      emailNotificationService.sendEmail(
        eq(userEntity.email!!),
        eq("5adb6390-0c95-4458-a8b5-3e61ff780715"),
        eq(
          mapOf(
            "nomsNumber" to application.nomsNumber,
            "receivingPrisonName" to newOmu.prisonName,
          ),
        ),
        any(),
      )
    }
    verify(exactly = 1, timeout = 5000) {
      emailNotificationService.sendEmail(
        eq(oldOmu.email),
        eq("6b427e8a-eb21-43a3-89c3-f6a147b20c39"),
        eq(
          mapOf(
            "nomsNumber" to application.nomsNumber,
            "receivingPrisonName" to newOmu.prisonName,
          ),
        ),
        any(),
      )
    }
    verify(exactly = 1, timeout = 5000) {
      emailNotificationService.sendEmail(
        eq(newOmu.email),
        eq("1e5d98e4-efdf-428e-bca9-fd5daadd27aa"),
        eq(
          mapOf(
            "nomsNumber" to application.nomsNumber,
            "transferringPrisonName" to oldOmu.prisonName,
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
            "receivingPrisonName" to newOmu.prisonName,
            "transferringPrisonName" to oldOmu.prisonName,
            "link" to getAssessorLink(application.id),
          ),
        ),
        any(),
      )
    }
  }

  private fun verifyEmailsSentForAllocationChangedCase(
    application: Cas2ApplicationEntity,
    pomManagerEmail: String,
  ) {
    verify(exactly = 1, timeout = 5000) {
      emailNotificationService.sendEmail(
        eq("nacro@example.com"),
        eq("e36b226e-99f5-4d1f-83d3-12ef9a814a5b"),
        eq(
          mapOf(
            "nomsNumber" to application.nomsNumber,
            "receivingPrisonName" to newOmu.prisonName,
            "link" to getAssessorLink(application.id),
          ),
        ),
        any(),
      )
    }

    verify(exactly = 1, timeout = 5000) {
      emailNotificationService.sendEmail(
        eq(pomManagerEmail),
        eq("289d4004-3c95-4c23-b0fa-9187d9da8eaf"),
        eq(
          mapOf(
            "nomsNumber" to application.nomsNumber,
            "transferringPrisonName" to oldOmu.prisonName,
            "link" to getLink(application.id),
            "applicationStatus" to "Status Update",
          ),
        ),
        any(),
      )
    }
  }

  private fun verifyEmailsNotSentForAllocationChangedCase(
    application: Cas2ApplicationEntity,
    pomManagerEmail: String,
  ) {
    verify(exactly = 0, timeout = 5000) {
      emailNotificationService.sendEmail(
        eq("nacro@example.com"),
        eq("e36b226e-99f5-4d1f-83d3-12ef9a814a5b"),
        eq(
          mapOf(
            "nomsNumber" to application.nomsNumber,
            "receivingPrisonName" to newOmu.prisonName,
            "link" to getAssessorLink(application.id),
          ),
        ),
        any(),
      )
    }

    verify(exactly = 0, timeout = 5000) {
      emailNotificationService.sendEmail(
        eq(pomManagerEmail),
        eq("289d4004-3c95-4c23-b0fa-9187d9da8eaf"),
        eq(
          mapOf(
            "nomsNumber" to application.nomsNumber,
            "transferringPrisonName" to oldOmu.prisonName,
            "link" to getLink(application.id),
            "applicationStatus" to "Status Update",
          ),
        ),
        any(),
      )
    }
  }

  private fun createApplicationAndInitialAssignment(
    allocatedPom: Cas2UserEntity,
    offenderDetails: OffenderDetailSummary,
    omu: OffenderManagementUnitEntity = oldOmu,
  ): Cas2ApplicationEntity {
    val application = cas2ApplicationEntityFactory.produceAndPersist {
      withCreatedByUser(allocatedPom)
      withSubmittedAt(OffsetDateTime.now())
      withCrn(offenderDetails.otherIds.crn)
      withCreatedAt(OffsetDateTime.now().minusDays(28))
      withConditionalReleaseDate(LocalDate.now().plusDays(1))
    }

    application.createApplicationAssignment(omu.prisonCode, allocatedPom)
    return cas2ApplicationRepository.save(application)
  }

  private fun createApplicationAndApplicationAssignmentsWithoutLocationEvent(
    user: Cas2UserEntity,
    offenderDetails: OffenderDetailSummary,
  ): Cas2ApplicationEntity {
    val application = cas2ApplicationEntityFactory.produceAndPersist {
      withCreatedByUser(user)
      withSubmittedAt(OffsetDateTime.now())
      withCrn(offenderDetails.otherIds.crn)
      withCreatedAt(OffsetDateTime.now().minusDays(28))
      withConditionalReleaseDate(LocalDate.now().plusDays(1))
    }

    val olderApplicationAssignment = Cas2ApplicationAssignmentEntity(
      id = UUID.randomUUID(),
      application = application,
      prisonCode = oldOmu.prisonCode,
      allocatedPomUser = user,
      createdAt = OffsetDateTime.parse(OCCURRING_AT),
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
