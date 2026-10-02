package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.unit.model

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2AssessmentStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ExternalSubmittedApplicationDto
import java.time.OffsetDateTime

class Cas2ExternalDtoTest {

  @Nested
  inner class Cas2ExternalSubmittedApplicationDtoTest {
    @ParameterizedTest
    @EnumSource(Cas2AssessmentStatus::class, mode = EnumSource.Mode.EXCLUDE, names = ["CANCELLED", "OFFER_DECLINED"])
    fun `A non-cancellation or non-offer-declined status never has any reasons - valid`(
      status: Cas2AssessmentStatus,
    ) {
      val thrown = catchThrowable {
        Cas2ExternalSubmittedApplicationDto(
          latestAssessmentStatus = status.apiName,
          offerDeclinedReason = null,
          cancelledReason = null,
          submittedAt = OffsetDateTime.parse("2023-02-01T00:00:00.000Z"),
        )
      }
      assertThat(thrown).isNull()
    }

    @Nested
    inner class CancellationReason {
      @ParameterizedTest
      @EnumSource(Cas2AssessmentStatus::class, mode = EnumSource.Mode.EXCLUDE, names = ["CANCELLED"])
      fun `Only a cancellation status requires a cancellation reason - invalid`(
        status: Cas2AssessmentStatus,
      ) {
        val thrown = catchThrowable {
          Cas2ExternalSubmittedApplicationDto(
            latestAssessmentStatus = status.apiName,
            offerDeclinedReason = null,
            cancelledReason = "a reason",
            submittedAt = OffsetDateTime.parse("2023-02-01T00:00:00.000Z"),
          )
        }
        assertThat(thrown).hasMessage("Cancelled reason must be provided if and only if status is `cancelled`")
      }

      @Test
      fun `A cancellation status requires a cancellation reason - valid`() {
        val thrown = catchThrowable {
          Cas2ExternalSubmittedApplicationDto(
            latestAssessmentStatus = Cas2AssessmentStatus.CANCELLED.apiName,
            offerDeclinedReason = null,
            cancelledReason = "a reason",
            submittedAt = OffsetDateTime.parse("2023-02-01T00:00:00.000Z"),
          )
        }
        assertThat(thrown).isNull()
      }

      @Test
      fun `A CANCELLED status does not have a cancellation reason - invalid`() {
        val thrown = catchThrowable {
          Cas2ExternalSubmittedApplicationDto(
            latestAssessmentStatus = Cas2AssessmentStatus.CANCELLED.apiName,
            offerDeclinedReason = null,
            cancelledReason = null,
            submittedAt = OffsetDateTime.parse("2023-02-01T00:00:00.000Z"),
          )
        }
        assertThat(thrown).hasMessage("Cancelled reason must be provided if and only if status is `cancelled`")
      }
    }

    @Nested
    inner class OfferDeclinedReason {
      @ParameterizedTest
      @EnumSource(Cas2AssessmentStatus::class, mode = EnumSource.Mode.EXCLUDE, names = ["OFFER_DECLINED", "CANCELLED"])
      fun `Only an offer declined status requires a offer declined reason - invalid`(
        status: Cas2AssessmentStatus,
      ) {
        val thrown = catchThrowable {
          Cas2ExternalSubmittedApplicationDto(
            latestAssessmentStatus = status.apiName,
            offerDeclinedReason = "a reason",
            cancelledReason = null,
            submittedAt = OffsetDateTime.parse("2023-02-01T00:00:00.000Z"),
          )
        }
        assertThat(thrown).hasMessage("Offer declined reason must be provided if and only if status is `offerDeclined`")
      }

      // this includes the CANCELLED status, which is handled separately from the other statuses as it will fail on the cancellation reason first
      @Test
      fun `Only an offer declined status requires a offer declined reason - invalid - CANCELLED`() {
        val thrown = catchThrowable {
          Cas2ExternalSubmittedApplicationDto(
            latestAssessmentStatus = Cas2AssessmentStatus.CANCELLED.apiName,
            offerDeclinedReason = "a reason",
            cancelledReason = null,
            submittedAt = OffsetDateTime.parse("2023-02-01T00:00:00.000Z"),
          )
        }
        assertThat(thrown).hasMessage("Cancelled reason must be provided if and only if status is `cancelled`")
      }

      @Test
      fun `An offer declined status requires a offer declined reason - valid`() {
        val thrown = catchThrowable {
          Cas2ExternalSubmittedApplicationDto(
            latestAssessmentStatus = Cas2AssessmentStatus.OFFER_DECLINED.apiName,
            offerDeclinedReason = "a reason",
            cancelledReason = null,
            submittedAt = OffsetDateTime.parse("2023-02-01T00:00:00.000Z"),
          )
        }
        assertThat(thrown).isNull()
      }

      @Test
      fun `An OFFER_DECLINED status does not have a offer declined reason - invalid`() {
        val thrown = catchThrowable {
          Cas2ExternalSubmittedApplicationDto(
            latestAssessmentStatus = Cas2AssessmentStatus.OFFER_DECLINED.apiName,
            offerDeclinedReason = null,
            cancelledReason = null,
            submittedAt = OffsetDateTime.parse("2023-02-01T00:00:00.000Z"),
          )
        }
        assertThat(thrown).hasMessage("Offer declined reason must be provided if and only if status is `offerDeclined`")
      }
    }
  }
}
