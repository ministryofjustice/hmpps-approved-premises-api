package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NomisUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 *
 * @param id
 * @param person
 * @param createdAt
 * @param schemaVersion
 * @param outdatedSchema
 * @param timelineEvents
 * @param assessment
 * @param isTransferredApplication
 * @param submittedBy
 * @param document Any object
 * @param submittedAt
 * @param telephoneNumber
 * @param allocatedPomName
 * @param currentPrisonName
 * @param allocatedPomEmailAddress
 * @param omuEmailAddress
 * @param assignmentDate
 */
data class Cas2SubmittedApplication(

  val id: UUID,

  val person: Person,

  val createdAt: Instant,

  val timelineEvents: List<Cas2TimelineEvent>,

  val assessment: Cas2Assessment,

  val isTransferredApplication: Boolean,

  val submittedBy: NomisUser? = null,

  val document: Any? = null,

  val submittedAt: Instant? = null,

  val telephoneNumber: String? = null,

  val allocatedPomName: String? = null,

  val currentPrisonName: String? = null,

  val allocatedPomEmailAddress: String? = null,

  val omuEmailAddress: String? = null,

  val assignmentDate: LocalDate? = null,
)
