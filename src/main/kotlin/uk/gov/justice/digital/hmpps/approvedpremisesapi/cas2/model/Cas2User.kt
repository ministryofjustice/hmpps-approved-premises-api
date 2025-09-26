package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserType
import java.util.UUID

data class Cas2User(

  val username: String,
  val name: String,
  val email: String?,
  val id: UUID,
  var userType: Cas2UserType,
  var nomisStaffId: Long? = null,
  var activeNomisCaseloadId: String? = null,
  var deliusTeamCodes: List<String>?,
  var deliusStaffCode: String?,
  var isEnabled: Boolean,
  var isActive: Boolean,
  var nomisAccountType: String? = null,
  var externalOrigin: String? = null,
  val applications: MutableList<Cas2ApplicationEntity> = mutableListOf(),
)
