package uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi

data class BookingDetails(
  val offenderNo: String?,
  val bookingId: Long?,
  val profileInformation: List<ProfileInformation>?,

)

data class ProfileInformation(
  val type: String,
  val question: String?,
  val resultValue: String?,
)
