package uk.gov.justice.digital.hmpps.approvedpremisesapi.model

import java.time.LocalDate

interface BookingSummaryForAvailability {
  fun getArrivalDate(): LocalDate
  fun getDepartureDate(): LocalDate
  fun getArrived(): Boolean
  fun getIsNotArrived(): Boolean
  fun getCancelled(): Boolean
}
