package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NamedId
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.UserTransformer

@Component
class Cas1SpaceBookingTransformer(
  private val personTransformer: PersonTransformer,
  private val spaceBookingRequirementsTransformer: Cas1SpaceBookingRequirementsTransformer,
  private val userTransformer: UserTransformer,
) {
  fun transformJpaToApi(person: PersonInfoResult, jpa: Cas1SpaceBookingEntity) = Cas1SpaceBooking(
    id = jpa.id,
    person = personTransformer.transformModelToPersonApi(person),
    requirements = spaceBookingRequirementsTransformer.transformJpaToApi(jpa.placementRequest.placementRequirements),
    premises = NamedId(
      id = jpa.premises.id,
      name = jpa.premises.name,
    ),
    apArea = jpa.premises.probationRegion.apArea!!.let {
      NamedId(
        id = it.id,
        name = it.name,
      )
    },
    bookedBy = userTransformer.transformJpaToApi(jpa.createdBy, ServiceName.approvedPremises),
    expectedArrivalDate = jpa.expectedArrivalDate,
    expectedDepartureDate = jpa.expectedDepartureDate,
    createdAt = jpa.createdAt.toInstant(),
    tier = null,
    keyWorker = null,
    actualDepartureDate = null,
    actualArrivalDate = null,
    canonicalArrivalDate = jpa.canonicalArrivalDate,
    canonicalDepartureDate = jpa.canonicalDepartureDate,
    otherBookingsInPremises = listOf(),
  )
}
