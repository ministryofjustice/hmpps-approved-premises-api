package uk.gov.justice.digital.hmpps.approvedpremisesapi.client.nonassociations

import java.time.LocalDateTime

data class NonAssociationsPage(
  val prisonerNumber: String,
  val firstName: String,
  val lastName: String,
  val prisonId: String?,
  val prisonName: String?,
  val cellLocation: String?,
  val openCount: Int,
  val closedCount: Int,
  val nonAssociations: List<PrisonerNonAssociation>,
)

data class PrisonerNonAssociation(
  val id: Long,
  val role: Role,
  val roleDescription: String,
  val reason: Reason,
  val reasonDescription: String,
  val restrictionType: RestrictionType,
  val restrictionTypeDescription: String,
  val comment: String,
  val authorisedBy: String,
  val whenCreated: LocalDateTime,
  val whenUpdated: LocalDateTime,
  val updatedBy: String,
  val isClosed: Boolean = false,
  val closedBy: String? = null,
  val closedReason: String? = null,
  val closedAt: LocalDateTime? = null,
  val otherPrisonerDetails: OtherPrisonerDetails,
)

enum class Role(
  val description: String,
) {
  VICTIM("Victim"),
  PERPETRATOR("Perpetrator"),
  NOT_RELEVANT("Not relevant"),
  UNKNOWN("Unknown"),
}

enum class Reason(
  val description: String,
) {
  BULLYING("Bullying"),
  GANG_RELATED("Gang related"),
  ORGANISED_CRIME("Organised crime"),
  LEGAL_REQUEST("Police or legal request"),
  THREAT("Threat"),
  VIOLENCE("Violence"),
  OTHER("Other"),
}

enum class RestrictionType(
  val description: String,
) {
  CELL("Cell only"),
  LANDING("Cell and landing"),
  WING("Cell, landing and wing"),
  ;

  fun toLegacyRestrictionType() = when (this) {
    CELL -> LegacyRestrictionType.CELL
    LANDING -> LegacyRestrictionType.LAND
    WING -> LegacyRestrictionType.WING
  }
}

enum class LegacyRestrictionType(
  val description: String,
) {
  CELL("Do Not Locate in Same Cell"),
  LAND("Do Not Locate on Same Landing"),
  WING("Do Not Locate on Same Wing"),
}

data class OtherPrisonerDetails(
  val prisonerNumber: String,
  val role: Role,
  val roleDescription: String,
  val firstName: String,
  val lastName: String,
  val prisonId: String?,
  val prisonName: String?,
  val cellLocation: String?,
)
