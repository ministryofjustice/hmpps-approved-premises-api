package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model

data class Cas2UserDto(
  val username: String,
  val type: Cas2UserTypeDto,
  val deliusUserInfo: Cas2DeliusUserInfoDto? = null,
)

enum class Cas2UserTypeDto {
  NOMIS,
  DELIUS,
  EXTERNAL,
}

data class Cas2DeliusUserInfoDto(
  val probationArea: ProbationAreaDto,
)

data class ProbationAreaDto(
  val code: String,
  val description: String,
)
