package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.dto.Cas3ExternalSubmittedApplicationDtoFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.dto.Cas3SuitableApplicationFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.TemporaryAccommodationAssessmentStatus

class Cas3ExternalDtoTest {

  @Nested
  inner class Cas3SuitableApplicationTest {

    @CsvSource(
      "rejected,false,false",
      "rejected,true,true",
      "inProgress,false,true",
      "inProgress,true,false",
      "submitted,false,false",
      "submitted,true,true",
      "requestedFurtherInformation,false,false",
      "requestedFurtherInformation,true,true",
    )
    @ParameterizedTest(name = "status {0} has submitted app {1} valid {2}")
    fun `A submitted application is required for any status other than 'inProgress'`(
      status: ApplicationStatus,
      hasSubmittedApplication: Boolean,
      valid: Boolean,
    ) {
      val thrown = catchThrowable {
        Cas3SuitableApplicationFactory()
          .withApplicationStatus(status)
          .withSubmittedApplication(
            if (hasSubmittedApplication) {
              Cas3ExternalSubmittedApplicationDtoFactory().produce()
            } else {
              null
            },
          )
          .produce()
      }

      if (valid) {
        assertThat(thrown).isNull()
      } else {
        assertThat(thrown).hasMessage("A submitted application is required for any status other than 'inProgress'")
      }
    }
  }

  @Nested
  inner class Cas3ExternalSubmittedApplicationDtoTest {

    @CsvSource(
      "unallocated,false,true",
      "unallocated,true,false",
      "inReview,false,true",
      "inReview,true,false",
      "readyToPlace,false,true",
      "readyToPlace,true,false",
      "closed,false,true",
      "closed,true,false",
      "rejected,true,true",
      "rejected,false,false",
    )
    @ParameterizedTest(name = "status {0} has rejection reason {1} valid {2}")
    fun `can only have rejection if status is rejected`(
      status: TemporaryAccommodationAssessmentStatus,
      hasRejectionReason: Boolean,
      valid: Boolean,
    ) {
      val thrown = catchThrowable {
        Cas3ExternalSubmittedApplicationDtoFactory()
          .withAssessmentStatus(status)
          .withAssessmentRejectionReason(
            if (hasRejectionReason) {
              "the rejection reason"
            } else {
              null
            },
          )
          .produce()
      }

      if (valid) {
        assertThat(thrown).isNull()
      } else {
        assertThat(thrown).hasMessage("Assessment rejection reason can only be provided if status is `rejected`")
      }
    }
  }
}
