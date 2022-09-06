package uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community

import java.time.LocalDate

data class Registrations(
  val registrations: List<Registration>
)

data class Registration(
  val registrationId: Long,
  val offenderId: Long,
  val register: RegistrationKeyValue,
  val type: RegistrationKeyValue,
  val riskColour: String,
  val startDate: LocalDate,
  val nextReviewDate: LocalDate,
  val reviewPeriodMonths: Int,
  val notes: String?,
  val registeringTeam: RegistrationKeyValue,
  val registeringOfficer: RegistrationStaffHuman,
  val registeringProbationArea: RegistrationKeyValue,
  val registerLevel: RegistrationKeyValue?,
  val registerCategory: RegistrationKeyValue?,
  val warnUser: Boolean,
  val active: Boolean,
  val endDate: LocalDate?,
  val deregisteringOfficer: RegistrationStaffHuman?,
  val deregisteringProbationArea: RegistrationKeyValue?,
  val deregisteringNotes: String?,
  val numberOfPreviousDeregistrations: Int,
  val registrationReviews: List<RegistrationReview>?
)

data class RegistrationKeyValue(
  val code: String,
  val description: String
)

data class RegistrationStaffHuman(
  val code: String,
  val forenames: String,
  val surname: String
)

data class RegistrationReview(
  val reviewDate: LocalDate,
  val reviewDateDue: LocalDate,
  val notes: String?,
  val reviewingTeam: RegistrationKeyValue,
  val reviewingOfficer: RegistrationStaffHuman,
  val completed: Boolean
)
