package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingCharacteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingDaySummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingDaySummarySearchResult

@Component
class Cas1SpaceBookingDaySummaryTransformer {

  private val releaseTypeToBeDetermined = "TBD"

  fun toCas1SpaceBookingDaySummary(
    jpa: Cas1SpaceBookingDaySummarySearchResult,
    personSummary: PersonSummary,
  ) = Cas1SpaceBookingDaySummary(
    id = jpa.id,
    canonicalArrivalDate = jpa.canonicalArrivalDate,
    canonicalDepartureDate = jpa.canonicalDepartureDate,
    tier = jpa.tier,
    releaseType = releaseTypeToBeDetermined,
    essentialCharacteristics = characteristicEntityToCharacteristic(jpa.characteristicsPropertyNames),
    person = personSummary,
  )

  private fun characteristicEntityToCharacteristic(characteristics: String?): List<Cas1SpaceBookingCharacteristic> =
    characteristics?.let { characteristicList ->
      characteristicList.split(",").map { characteristic ->
        Cas1SpaceBookingCharacteristic.entries.first { it.value == characteristic }
      }
    } ?: emptyList()
}
