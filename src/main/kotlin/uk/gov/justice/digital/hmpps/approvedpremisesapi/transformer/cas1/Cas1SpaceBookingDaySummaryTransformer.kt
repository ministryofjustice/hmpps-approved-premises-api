package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingCharacteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingDaySummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingDaySummarySearchResult

@Component
class Cas1SpaceBookingDaySummaryTransformer {

  fun toCas1SpaceBookingDaySummary(
    jpa: Cas1SpaceBookingDaySummarySearchResult,
    personSummary: PersonSummary,
  ) = Cas1SpaceBookingDaySummary(
    id = jpa.id,
    canonicalArrivalDate = jpa.canonicalArrivalDate,
    canonicalDepartureDate = jpa.canonicalDepartureDate,
    tier = jpa.tier,
    releaseType = jpa.releaseType,
    essentialCharacteristics = characteristicEntityToCharacteristic(jpa.characteristics),
    person = personSummary,
  )

  private fun characteristicEntityToCharacteristic(characteristics: List<String>?): List<Cas1SpaceBookingCharacteristic> {
    val sbCharacteristics = mutableListOf<Cas1SpaceBookingCharacteristic>()
    characteristics?.map {
      it
      when (it) {
        "isSingle" -> sbCharacteristics += Cas1SpaceBookingCharacteristic.IS_SINGLE
        "hasEnSuite" -> sbCharacteristics += Cas1SpaceBookingCharacteristic.HAS_EN_SUITE
        "isArsonSuitable" -> sbCharacteristics += Cas1SpaceBookingCharacteristic.IS_ARSON_SUITABLE
        "isStepFreeDesignated" -> sbCharacteristics += Cas1SpaceBookingCharacteristic.IS_STEP_FREE_DESIGNATED
        "isWheelchairDesignated" -> sbCharacteristics += Cas1SpaceBookingCharacteristic.IS_WHEELCHAIR_DESIGNATED
        "isSuitedForSexOffenders" -> sbCharacteristics += Cas1SpaceBookingCharacteristic.IS_SUITED_FOR_SEX_OFFENDERS
        else -> {}
      }
    }
    return sbCharacteristics
  }
}
