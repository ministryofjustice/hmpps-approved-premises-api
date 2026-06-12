package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model

data class Cas2v2UserDto(
  val username: String,
  val type: Cas2v2UserTypeDto,
  val deliusUserInfo: Cas2v2DeliusUserInfoDto? = null,
)

enum class Cas2v2UserTypeDto {
  NOMIS,
  DELIUS,
  EXTERNAL,
}

data class Cas2v2DeliusUserInfoDto(
  val probationArea: ProbationAreaDto,
)

data class ProbationAreaDto(
  val code: String,
  val description: String,
)
