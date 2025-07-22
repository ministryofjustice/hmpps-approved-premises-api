package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas1

import io.github.bluegroundltd.kfactory.Factory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.CandidatePremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesType
import java.util.UUID

class CandidatePremisesFactory : Factory<CandidatePremises> {
  private var id = { UUID.randomUUID() }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  override fun produce() = CandidatePremises(
    id(),
    3.0f,
    ApprovedPremisesType.PIPE,
    "Some AP",
    fullAddress = "the full address",
    "3 The Street",
    null,
    "Townsbury",
    "TB1 2AB",
    UUID.randomUUID(),
    "Some AP Area",
    "Area Code",
    characteristics = emptyList(),
    localRestrictions = emptyList(),
  )
}
