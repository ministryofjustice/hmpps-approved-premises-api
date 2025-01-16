package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas1

import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceCharacteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.FullPersonSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingDaySummarySearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1SpaceBookingDaySummaryTransformer
import java.time.LocalDate
import java.util.UUID

@ExtendWith(MockKExtension::class)
class Cas1SpaceBookingDaySummaryTransformerTest {

  private val releaseTypeToBeDetermined = "TBD"
  private val transformer = Cas1SpaceBookingDaySummaryTransformer()

  @ParameterizedTest
  @EnumSource(value = Cas1SpaceCharacteristic::class)
  fun `toCas1SpaceBookingDaySummary`(characteristic: Cas1SpaceCharacteristic) {
    val spaceBookingSummary =
      Cas1SpaceBookingDaySummarySearchResultImpl(
        UUID.randomUUID(),
        "crn1",
        LocalDate.now().minusDays(3),
        LocalDate.now().plusDays(3),
        tier = "A1",
        releaseType = "rotl",
        characteristicsPropertyNames = characteristic.value,
      )
    val personSummary = PersonTransformer()
      .personSummaryInfoToPersonSummary(
        PersonSummaryInfoResult.Success.Full("crn1", CaseSummaryFactory().produce()),
      )

    val result = transformer.toCas1SpaceBookingDaySummary(
      spaceBookingSummary,
      personSummary,
    )

    assertThat(result.id).isEqualTo(spaceBookingSummary.id)
    assertThat(result.canonicalArrivalDate).isEqualTo(spaceBookingSummary.canonicalArrivalDate)
    assertThat(result.canonicalDepartureDate).isEqualTo(spaceBookingSummary.canonicalDepartureDate)
    assertThat(result.tier).isEqualTo(spaceBookingSummary.tier)
    assertThat(result.releaseType).isEqualTo(releaseTypeToBeDetermined)
    assertThat(result.essentialCharacteristics.size).isEqualTo(1)
    assertThat(result.essentialCharacteristics[0]).isEqualTo(characteristic)
    val offender = result.person as FullPersonSummary
    assertThat(offender.crn).isEqualTo(personSummary.crn)
    assertThat(offender.name).isEqualTo((personSummary as FullPersonSummary).name)
  }

  @Test
  fun `toCas1SpaceBookingDaySummary with multiple characteristics`() {
    val spaceBookingSummary =
      Cas1SpaceBookingDaySummarySearchResultImpl(
        UUID.randomUUID(),
        "crn1",
        LocalDate.now().minusDays(3),
        LocalDate.now().plusDays(3),
        tier = "A1",
        releaseType = "rotl",
        characteristicsPropertyNames = "isArsonSuitable,hasEnSuite,isSingle,isStepFreeDesignated,isSuitedForSexOffenders,isWheelchairDesignated,isCatered",
      )
    val personSummary = PersonTransformer()
      .personSummaryInfoToPersonSummary(
        PersonSummaryInfoResult.Success.Full("crn1", CaseSummaryFactory().produce()),
      )

    val result = transformer.toCas1SpaceBookingDaySummary(
      spaceBookingSummary,
      personSummary,
    )

    assertThat(result.essentialCharacteristics.size).isEqualTo(7)
    assertThat(result.essentialCharacteristics[0]).isEqualTo(Cas1SpaceCharacteristic.isArsonSuitable)
    assertThat(result.essentialCharacteristics[1]).isEqualTo(Cas1SpaceCharacteristic.hasEnSuite)
    assertThat(result.essentialCharacteristics[2]).isEqualTo(Cas1SpaceCharacteristic.isSingle)
    assertThat(result.essentialCharacteristics[3]).isEqualTo(Cas1SpaceCharacteristic.isStepFreeDesignated)
    assertThat(result.essentialCharacteristics[4]).isEqualTo(Cas1SpaceCharacteristic.isSuitedForSexOffenders)
    assertThat(result.essentialCharacteristics[5]).isEqualTo(Cas1SpaceCharacteristic.isWheelchairDesignated)
    assertThat(result.essentialCharacteristics[6]).isEqualTo(Cas1SpaceCharacteristic.isCatered)
  }
}

data class Cas1SpaceBookingDaySummarySearchResultImpl(
  override val id: UUID,
  override val crn: String,
  override val canonicalArrivalDate: LocalDate,
  override val canonicalDepartureDate: LocalDate,
  override val tier: String,
  override val releaseType: String,
  override val characteristicsPropertyNames: String,
) : Cas1SpaceBookingDaySummarySearchResult
