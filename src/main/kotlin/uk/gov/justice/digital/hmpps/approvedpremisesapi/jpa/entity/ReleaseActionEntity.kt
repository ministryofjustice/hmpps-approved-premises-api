package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.javers.core.metamodel.annotation.TypeName
import java.util.UUID
import org.javers.core.metamodel.annotation.Entity as JaversEntity

@Entity
@JaversEntity
@TypeName("ReleaseAction")
@Table(name = "release_action")
class ReleaseActionEntity(
  @Id
  val id: UUID,

  var description: String,
  var actionCadence: String,
  var otherInformation: String? = null,
)
