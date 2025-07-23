package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationPlaceholderEntity
import java.time.OffsetDateTime
import java.util.UUID

class PlacementApplicationPlaceholderEntityFactory : Factory<PlacementApplicationPlaceholderEntity> {
  private var id = { UUID.randomUUID() }
  private var application = { ApprovedPremisesApplicationEntityFactory().withDefaults().produce() }
  private var submittedAt = { OffsetDateTime.now() }
  private var expectedArrivalDate = { OffsetDateTime.now() }

  fun withApplication(application: ApprovedPremisesApplicationEntity) = apply {
    this.application = { application }
  }

  override fun produce() = PlacementApplicationPlaceholderEntity(
    id(),
    application(),
    submittedAt(),
    expectedArrivalDate(),
  )
}
