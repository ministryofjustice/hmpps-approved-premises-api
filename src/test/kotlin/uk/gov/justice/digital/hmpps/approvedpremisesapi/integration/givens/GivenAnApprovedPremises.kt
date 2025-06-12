package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PropertyStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesGender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LocalAuthorityAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDouble
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomOf
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomPostCode
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.util.UUID

fun IntegrationTestBase.givenAnApprovedPremises(
  name: String = randomStringMultiCaseWithNumbers(8),
  qCode: String = randomStringUpperCase(4),
  supportsSpaceBookings: Boolean = false,
  region: ProbationRegionEntity? = null,
  localAuthorityArea: LocalAuthorityAreaEntity? = null,
  emailAddress: String? = randomStringUpperCase(10),
  status: PropertyStatus = randomOf(PropertyStatus.entries),
  apCode: String = randomStringUpperCase(10),
  postCode: String = randomPostCode(),
  managerDetails: String = randomStringUpperCase(10),
  latitude: Double = randomDouble(53.50, 54.99),
  longitude: Double = randomDouble(-1.56, 1.10),
  characteristics: List<CharacteristicEntity> = emptyList(),
  gender: ApprovedPremisesGender = ApprovedPremisesGender.MAN,
  id: UUID = UUID.randomUUID(),
): ApprovedPremisesEntity = approvedPremisesEntityFactory
  .produceAndPersist {
    withId(id)
    withName(name)
    withQCode(qCode)
    withEmailAddress(emailAddress)
    withProbationRegion(region ?: givenAProbationRegion())
    withLocalAuthorityArea(localAuthorityArea ?: localAuthorityEntityFactory.produceAndPersist())
    withSupportsSpaceBookings(supportsSpaceBookings)
    withStatus(status)
    withApCode(apCode)
    withPostcode(postCode)
    withManagerDetails(managerDetails)
    withLatitude(latitude)
    withLongitude(longitude)
    withCharacteristics(characteristics.toMutableList())
    withGender(gender)
  }
