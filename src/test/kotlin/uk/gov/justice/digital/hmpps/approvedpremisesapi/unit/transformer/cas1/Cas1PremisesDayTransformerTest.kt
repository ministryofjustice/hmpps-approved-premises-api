package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas1

import io.mockk.impl.annotations.InjectMockKs
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OutOfServiceBedReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OutOfServiceBedReasonReferenceType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OutOfServiceBedSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceCharacteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NamedId
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonSummaryDiscriminator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RestrictedPersonSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1PremisesDayTransformer
import java.time.LocalDate
import java.util.UUID

@ExtendWith(MockKExtension::class)
class Cas1PremisesDayTransformerTest {

  @InjectMockKs
  lateinit var transformer: Cas1PremisesDayTransformer

  @Test
  fun toCas1PremisesDaySummary() {
    val currentSearchDay = LocalDate.now()

    val spaceBookingSummaries = listOf(
      Cas1SpaceBookingSummary(
        id = UUID.randomUUID(),
        person = RestrictedPersonSummary(
          crn = "crn",
          personType = PersonSummaryDiscriminator.restrictedPersonSummary,
        ),
        canonicalArrivalDate = currentSearchDay.minusDays(1),
        canonicalDepartureDate = currentSearchDay.plusDays(1),
        tier = "Tier 1",
        characteristics = listOf(
          Cas1SpaceCharacteristic.isSingle,
          Cas1SpaceCharacteristic.hasEnSuite,
          Cas1SpaceCharacteristic.isIAP,
        ),
        premises = NamedId(UUID.randomUUID(), "premisesName"),
        expectedArrivalDate = currentSearchDay.minusDays(1),
        expectedDepartureDate = currentSearchDay.plusDays(1),
        isCancelled = false,
        openChangeRequestTypes = emptyList(),
        actualArrivalDate = null,
        actualDepartureDate = null,
        isNonArrival = false,
        keyWorkerAllocation = null,
        deliusEventNumber = UUID.randomUUID().toString(),
        plannedTransferRequested = false,
        appealRequested = false,
      ),
    )

    val outOfServiceBeds = listOf(
      Cas1OutOfServiceBedSummary(
        id = UUID.randomUUID(),
        bedId = UUID.randomUUID(),
        startDate = LocalDate.now().minusDays(5),
        endDate = LocalDate.now().plusDays(5),
        reason = Cas1OutOfServiceBedReason(
          UUID.randomUUID(),
          "reason",
          true,
          Cas1OutOfServiceBedReasonReferenceType.WORK_ORDER,
        ),
        characteristics = listOf(Cas1SpaceCharacteristic.isSingle),
      ),
    )

    val result = transformer.toCas1PremisesDaySummary(
      currentSearchDay,
      outOfServiceBeds,
      spaceBookingSummaries,
    )

    assertThat(result.forDate).isEqualTo(currentSearchDay)
    assertThat(result.previousDate).isEqualTo(currentSearchDay.minusDays(1))
    assertThat(result.nextDate).isEqualTo(currentSearchDay.plusDays(1))
    assertThat(result.outOfServiceBeds).isEqualTo(outOfServiceBeds)
    assertThat(result.spaceBookingSummaries).isEqualTo(spaceBookingSummaries)
  }
}
