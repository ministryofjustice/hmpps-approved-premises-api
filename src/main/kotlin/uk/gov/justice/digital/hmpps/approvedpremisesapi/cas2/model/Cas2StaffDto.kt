package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model

data class Cas2StaffDto(
  val name: String,
  val username: String,
  val deliusStaffCode: String?,
  val nomisStaffId: Long?,
  val userType: Cas2UserTypeDto,
)
