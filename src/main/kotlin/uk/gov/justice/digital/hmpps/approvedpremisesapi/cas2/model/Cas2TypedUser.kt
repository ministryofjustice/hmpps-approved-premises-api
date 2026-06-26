package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model

import java.util.UUID

sealed interface Cas2TypedUser {
  val id: UUID
  val username: String
  val name: String
  val email: String?
  val isActive: Boolean
  val isEnabled: Boolean
  val serviceOrigin: Cas2ServiceOrigin
  val userType: Cas2UserTypeDto

  data class Nomis(
    override val id: UUID,
    override val username: String,
    override val name: String,
    override val email: String?,
    override val isActive: Boolean,
    override val isEnabled: Boolean,
    override val serviceOrigin: Cas2ServiceOrigin,
    val nomisStaffId: Long?,
    val activeNomisCaseloadId: String?,
  ) : Cas2TypedUser {
    override val userType = Cas2UserTypeDto.NOMIS
  }

  data class Delius(
    override val id: UUID,
    override val username: String,
    override val name: String,
    override val email: String?,
    override val isActive: Boolean,
    override val isEnabled: Boolean,
    override val serviceOrigin: Cas2ServiceOrigin,
    val deliusStaffCode: String?,
    val deliusTeamCodes: List<String>?,
    val deliusUserInfo: Cas2DeliusUserInfoDto?,
  ) : Cas2TypedUser {
    override val userType = Cas2UserTypeDto.DELIUS
  }

  data class External(
    override val id: UUID,
    override val username: String,
    override val name: String,
    override val email: String?,
    override val isActive: Boolean,
    override val isEnabled: Boolean,
    override val serviceOrigin: Cas2ServiceOrigin,
    val externalType: String?,
  ) : Cas2TypedUser {
    override val userType = Cas2UserTypeDto.EXTERNAL
  }
}
