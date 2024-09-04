package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1KeyWorkerAllocation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NamedId
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.UserTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toLocalDate

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

  fun transformSearchResultToSummary(
    searchResult: Cas1SpaceBookingSearchResult,
    personSummaryInfo: PersonSummaryInfoResult,
  ) = Cas1SpaceBookingSummary(
    id = searchResult.id,
    person = personTransformer.personSummaryInfoToPersonSummary(personSummaryInfo),
    canonicalArrivalDate = searchResult.canonicalArrivalDate,
    canonicalDepartureDate = searchResult.canonicalDepartureDate,
    tier = searchResult.tier,
    keyWorkerAllocation = searchResult.keyWorkerStaffCode?.let { staffCode ->
      Cas1KeyWorkerAllocation(
        allocatedAt = searchResult.keyWorkerAssignedAt!!.toLocalDate(),
        keyWorker = StaffMember(
          code = staffCode,
          keyWorker = true,
          name = searchResult.keyWorkerName!!,
        ),
      )
    },
  )
}
