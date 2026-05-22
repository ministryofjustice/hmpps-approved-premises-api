package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "release_action")
class ReleaseActionEntity(
  @Id
  val id: UUID,

  var description: String,
  var whenField: String,
  var otherInformation: String? = null,
)
