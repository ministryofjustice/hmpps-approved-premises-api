package uk.gov.justice.digital.hmpps.approvedpremisesapi.model

import java.time.LocalDate

data class Availability(
  val date: LocalDate,
  val pendingBookings: Int,
  val arrivedBookings: Int,
  val nonArrivedBookings: Int,
  val cancelledBookings: Int,
  val voidBedspaces: Int,
) {
  fun getFreeCapacity(totalBeds: Int) = ((totalBeds - pendingBookings) - arrivedBookings) - voidBedspaces
}
