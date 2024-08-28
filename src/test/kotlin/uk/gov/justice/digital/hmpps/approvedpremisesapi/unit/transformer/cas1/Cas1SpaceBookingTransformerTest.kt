package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas1

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingRequirements
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Gender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RestrictedPerson
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1SpaceBookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.UserTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1SpaceBookingRequirementsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1SpaceBookingTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.asOffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.roundNanosToMillisToAccountForLossOfPrecisionInPostgres
import java.time.OffsetDateTime
import java.util.UUID

@ExtendWith(MockKExtension::class)
class Cas1SpaceBookingTransformerTest {
  @MockK
  private lateinit var personTransformer: PersonTransformer

  @MockK
  private lateinit var requirementsTransformer: Cas1SpaceBookingRequirementsTransformer

  @MockK
  private lateinit var userTransformer: UserTransformer

  @InjectMockKs
  private lateinit var transformer: Cas1SpaceBookingTransformer

  @Test
  fun `Space booking is transformed correctly`() {
    val personInfo = PersonInfoResult.Success.Full(
      "SOMECRN",
      CaseSummaryFactory().produce().asOffenderDetailSummary(),
      null,
    )

    val expectedPerson = RestrictedPerson(
      "SOMECRN",
      PersonType.restrictedPerson,
    )

    val spaceBooking = Cas1SpaceBookingEntityFactory()
      .withCreatedAt(OffsetDateTime.now().roundNanosToMillisToAccountForLossOfPrecisionInPostgres())
      .produce()

    val expectedRequirements = Cas1SpaceBookingRequirements(
      apType = ApType.pipe,
      gender = Gender.female,
      essentialCharacteristics = listOf(),
      desirableCharacteristics = listOf(),
    )

    val expectedUser = ApprovedPremisesUser(
      qualifications = listOf(),
      roles = listOf(),
      apArea = ApArea(
        id = UUID.randomUUID(),
        identifier = "SOMEAPA",
        name = "Some AP Area",
      ),
      service = ServiceName.approvedPremises.value,
      id = spaceBooking.createdBy.id,
      name = spaceBooking.createdBy.name,
      deliusUsername = spaceBooking.createdBy.deliusUsername,
      region = ProbationRegion(
        id = UUID.randomUUID(),
        name = "Some Probation Region",
      ),
    )

    every { personTransformer.transformModelToPersonApi(personInfo) } returns expectedPerson
    every { requirementsTransformer.transformJpaToApi(spaceBooking.placementRequest.placementRequirements) } returns expectedRequirements
    every { userTransformer.transformJpaToApi(spaceBooking.createdBy, ServiceName.approvedPremises) } returns expectedUser

    val result = transformer.transformJpaToApi(personInfo, spaceBooking)

    assertThat(result.id).isEqualTo(spaceBooking.id)
    assertThat(result.person).isEqualTo(expectedPerson)
    assertThat(result.requirements).isEqualTo(expectedRequirements)
    assertThat(result.premises.id).isEqualTo(spaceBooking.premises.id)
    assertThat(result.premises.name).isEqualTo(spaceBooking.premises.name)
    assertThat(result.apArea.id).isEqualTo(spaceBooking.premises.probationRegion.apArea!!.id)
    assertThat(result.apArea.name).isEqualTo(spaceBooking.premises.probationRegion.apArea!!.name)
    assertThat(result.bookedBy).isEqualTo(expectedUser)
    assertThat(result.expectedArrivalDate).isEqualTo(spaceBooking.arrivalDate)
    assertThat(result.expectedDepartureDate).isEqualTo(spaceBooking.departureDate)
    assertThat(result.canonicalArrivalDate).isEqualTo(spaceBooking.arrivalDate)
    assertThat(result.canonicalDepartureDate).isEqualTo(spaceBooking.departureDate)
    assertThat(result.createdAt).isEqualTo(spaceBooking.createdAt.toInstant())
  }
}
