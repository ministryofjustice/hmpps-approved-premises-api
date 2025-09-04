package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model

import com.fasterxml.jackson.annotation.JsonCreator

// TODO besscerule this is an external facing model so this will impact the frontends as staffIdentifier has changed from Long to String

data class Cas2StaffMember(

  val staffIdentifier: String,

  val name: String,

  val username: String? = null,

  val usertype: Usertype? = null,
) {

  @Suppress("ktlint:standard:enum-entry-name-case", "EnumNaming")
  enum class Usertype(val value: String) {

    nomis("nomis"),
    delius("delius"),

    // TODO besscerule added in auth as we now have it set up with external users
    auth("auth"), ;

    companion object {
      @JvmStatic
      @JsonCreator
      fun forValue(value: String): Usertype = entries.first { it.value == value }
    }
  }
}
