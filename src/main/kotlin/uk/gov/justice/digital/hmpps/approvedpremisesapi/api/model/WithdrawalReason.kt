package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
*
* Values: changeInCircumstancesNewApplicationToBeSubmitted,errorInApplication,duplicateApplication,death,otherAccommodationIdentified,other
*/
enum class WithdrawalReason(val value: kotlin.String) {

  @JsonProperty("change_in_circumstances_new_application_to_be_submitted")
  changeInCircumstancesNewApplicationToBeSubmitted("change_in_circumstances_new_application_to_be_submitted"),

  @JsonProperty("error_in_application")
  errorInApplication("error_in_application"),

  @JsonProperty("duplicate_application")
  duplicateApplication("duplicate_application"),

  @JsonProperty("death")
  death("death"),

  @JsonProperty("other_accommodation_identified")
  otherAccommodationIdentified("other_accommodation_identified"),

  @JsonProperty("other")
  other("other"),
}
