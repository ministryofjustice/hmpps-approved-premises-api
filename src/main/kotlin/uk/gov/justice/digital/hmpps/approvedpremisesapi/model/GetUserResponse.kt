package uk.gov.justice.digital.hmpps.approvedpremisesapi.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity

data class GetUserResponse(
  val user: UserEntity?,
  var staffRecordFound: Boolean,
)
