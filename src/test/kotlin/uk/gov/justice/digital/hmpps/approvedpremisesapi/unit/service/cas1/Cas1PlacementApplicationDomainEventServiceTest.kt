package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.RequestForPlacementType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementDateEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffUserDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.StaffMemberFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.WithdrawnByFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationWithdrawalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PlacementApplicationDomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawableEntityType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawalContext
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.DomainEventTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.Cas1PlacementApplicationDomainEventServiceTest.TestConstants.CRN
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.Cas1PlacementApplicationDomainEventServiceTest.TestConstants.USERNAME
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.isWithinTheLastMinute
import java.time.LocalDate
import java.time.OffsetDateTime

class Cas1PlacementApplicationDomainEventServiceTest {

  private object TestConstants {
    const val CRN = "CRN123"
    const val USERNAME = "theUserName"
  }

  val domainEventService = mockk<DomainEventService>()
  val domainEventTransformer = mockk<DomainEventTransformer>()
  val communityApiClient = mockk<CommunityApiClient>()

  val service = Cas1PlacementApplicationDomainEventService(
    domainEventService,
    domainEventTransformer,
    communityApiClient,
    applicationUrlTemplate = UrlTemplate("http://frontend/applications/#id"),
  )

  val user = UserEntityFactory()
    .withUnitTestControlProbationRegion()
    .produce()

  val application = ApprovedPremisesApplicationEntityFactory()
    .withCrn(CRN)
    .withCreatedByUser(user)
    .withSubmittedAt(OffsetDateTime.now())
    .produce()

  @Nested
  inner class PlacementApplicationSubmitted {

    @ParameterizedTest
    @CsvSource(
      "ROTL,rotl",
      "RELEASE_FOLLOWING_DECISION,releaseFollowingDecisions",
      "ADDITIONAL_PLACEMENT,additionalPlacement",
    )
    fun `it creates a domain event`(placementType: PlacementType, expectedRequestForPlacementType: RequestForPlacementType) {
      val placementApplication = PlacementApplicationEntityFactory()
        .withApplication(application)
        .withAllocatedToUser(UserEntityFactory().withDefaultProbationRegion().produce())
        .withDecision(null)
        .withCreatedByUser(user)
        .withPlacementType(placementType)
        .produce()

      placementApplication.placementDates = mutableListOf(
        PlacementDateEntityFactory()
          .withPlacementApplication(placementApplication)
          .withExpectedArrival(LocalDate.of(2024, 5, 3))
          .withDuration(7)
          .produce(),
      )

      val staffUserDetails = StaffUserDetailsFactory().produce()
      every { communityApiClient.getStaffUserDetails(USERNAME) } returns ClientResult.Success(
        status = HttpStatus.OK,
        body = staffUserDetails,
      )

      val staffMember = StaffMemberFactory().produce()
      every { domainEventTransformer.toStaffMember(staffUserDetails) } returns staffMember
      every { domainEventService.saveRequestForPlacementCreatedEvent(any(), any()) } returns Unit

      service.placementApplicationSubmitted(placementApplication, USERNAME)

      verify {
        domainEventService.saveRequestForPlacementCreatedEvent(
          withArg {
            assertThat(it.id).isNotNull()
            assertThat(it.applicationId).isEqualTo(application.id)
            assertThat(it.crn).isEqualTo(CRN)
            assertThat(it.occurredAt).isWithinTheLastMinute()

            val eventDetails = it.data.eventDetails
            assertThat(eventDetails.applicationId).isEqualTo(application.id)
            assertThat(eventDetails.applicationUrl).isEqualTo("http://frontend/applications/${application.id}")
            assertThat(eventDetails.requestForPlacementId).isEqualTo(placementApplication.id)
            assertThat(eventDetails.personReference.crn).isEqualTo(CRN)
            assertThat(eventDetails.personReference.noms).isEqualTo(application.nomsNumber)
            assertThat(eventDetails.deliusEventNumber).isEqualTo(application.eventNumber)
            assertThat(eventDetails.createdAt).isWithinTheLastMinute()
            assertThat(eventDetails.createdBy).isEqualTo(staffMember)
            assertThat(eventDetails.expectedArrival).isEqualTo(LocalDate.of(2024, 5, 3))
            assertThat(eventDetails.duration).isEqualTo(7)
            assertThat(eventDetails.requestForPlacementType).isEqualTo(expectedRequestForPlacementType)
          },
          emit = false,
        )
      }
    }
  }

  @Nested
  inner class PlacementApplicationWithdrawn {

    @Test
    fun `it creates a domain event`() {
      val placementApplication = PlacementApplicationEntityFactory()
        .withApplication(application)
        .withAllocatedToUser(UserEntityFactory().withDefaultProbationRegion().produce())
        .withDecision(null)
        .withCreatedByUser(user)
        .withWithdrawalReason(PlacementApplicationWithdrawalReason.ALTERNATIVE_PROVISION_IDENTIFIED)
        .produce()

      placementApplication.placementDates = mutableListOf(
        PlacementDateEntityFactory()
          .withPlacementApplication(placementApplication)
          .withExpectedArrival(LocalDate.of(2024, 5, 3))
          .withDuration(7)
          .produce(),
      )

      val withdrawnBy = WithdrawnByFactory().produce()
      every { domainEventTransformer.toWithdrawnBy(user) } returns withdrawnBy
      every { domainEventService.savePlacementApplicationWithdrawnEvent(any()) } returns Unit

      service.placementApplicationWithdrawn(
        placementApplication,
        withdrawalContext = WithdrawalContext(
          user,
          WithdrawableEntityType.PlacementApplication,
          placementApplication.id,
        ),
      )

      verify(exactly = 1) {
        domainEventService.savePlacementApplicationWithdrawnEvent(
          withArg {
            assertThat(it.id).isNotNull()
            assertThat(it.applicationId).isEqualTo(application.id)
            assertThat(it.crn).isEqualTo(CRN)
            assertThat(it.occurredAt).isWithinTheLastMinute()

            val eventDetails = it.data.eventDetails
            assertThat(eventDetails.applicationId).isEqualTo(application.id)
            assertThat(eventDetails.applicationUrl).isEqualTo("http://frontend/applications/${application.id}")
            assertThat(eventDetails.placementApplicationId).isEqualTo(placementApplication.id)
            assertThat(eventDetails.personReference.crn).isEqualTo(CRN)
            assertThat(eventDetails.personReference.noms).isEqualTo(application.nomsNumber)
            assertThat(eventDetails.deliusEventNumber).isEqualTo(application.eventNumber)
            assertThat(eventDetails.withdrawnBy).isEqualTo(withdrawnBy)
            assertThat(eventDetails.withdrawalReason).isEqualTo("ALTERNATIVE_PROVISION_IDENTIFIED")
            assertThat(eventDetails.placementDates).hasSize(1)
            assertThat(eventDetails.placementDates!![0].startDate).isEqualTo(LocalDate.of(2024, 5, 3))
            assertThat(eventDetails.placementDates!![0].endDate).isEqualTo(LocalDate.of(2024, 5, 10))
          },
        )
      }
    }
  }
}
