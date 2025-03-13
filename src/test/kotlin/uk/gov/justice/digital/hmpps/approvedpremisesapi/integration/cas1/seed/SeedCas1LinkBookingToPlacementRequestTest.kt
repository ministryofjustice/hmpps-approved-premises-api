package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1.seed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.seed.SeedTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.CsvBuilder
import java.time.OffsetDateTime

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class SeedCas1LinkBookingToPlacementRequestTest : SeedTestBase() {

  @Test
  fun `Links placement request to adhoc booking on same application`() {
    val (user, _) = givenAUser()
    val (offenderDetails, _) = givenAnOffender()

    val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
      withCrn(offenderDetails.otherIds.crn)
      withCreatedByUser(user)
      withApplicationSchema(
        approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
          withPermissiveSchema()
        },
      )
      withSubmittedAt(OffsetDateTime.now())
      withReleaseType("licence")
    }

    val placementRequest = createPlacementRequest(application, user)
    val booking = createBooking(application)

    seed(
      SeedFileType.approvedPremisesLinkBookingToPlacementRequest,
      CsvBuilder()
        .withUnquotedFields("booking_id")
        .withUnquotedFields("placement_request_id")
        .newRow()
        .withQuotedField(booking.id)
        .withQuotedField(placementRequest.id)
        .newRow()
        .build(),
    )

    val updatedPlacementRequest = placementRequestRepository.findByIdOrNull(placementRequest.id)!!
    assertThat(updatedPlacementRequest.booking!!.id).isEqualTo(booking.id)
  }

  private fun createPlacementRequest(
    application: ApprovedPremisesApplicationEntity,
    user: UserEntity,
  ): PlacementRequestEntity {
    val assessmentSchema = approvedPremisesAssessmentJsonSchemaEntityFactory.produceAndPersist {
      withPermissiveSchema()
    }

    val assessment = approvedPremisesAssessmentEntityFactory.produceAndPersist {
      withAssessmentSchema(assessmentSchema)
      withApplication(application)
      withSubmittedAt(OffsetDateTime.now())
      withAllocatedToUser(user)
      withDecision(AssessmentDecision.ACCEPTED)
    }

    val placementRequirements = placementRequirementsFactory.produceAndPersist {
      withApplication(application)
      withAssessment(assessment)
      withPostcodeDistrict(postCodeDistrictFactory.produceAndPersist())
      withDesirableCriteria(characteristicEntityFactory.produceAndPersistMultiple(5))
      withEssentialCriteria(characteristicEntityFactory.produceAndPersistMultiple(3))
    }

    val placementRequest = placementRequestFactory.produceAndPersist {
      withAllocatedToUser(user)
      withApplication(application)
      withAssessment(assessment)
      withPlacementRequirements(placementRequirements)
    }
    return placementRequest
  }

  private fun createBooking(application: ApprovedPremisesApplicationEntity): BookingEntity {
    val booking = bookingEntityFactory.produceAndPersist {
      withPremises(
        approvedPremisesEntityFactory.produceAndPersist {
          withProbationRegion(givenAProbationRegion())
          withLocalAuthorityArea(localAuthorityEntityFactory.produceAndPersist())
        },
      )
      withAdhoc(true)
      withApplication(application)
    }
    return booking
  }
}
