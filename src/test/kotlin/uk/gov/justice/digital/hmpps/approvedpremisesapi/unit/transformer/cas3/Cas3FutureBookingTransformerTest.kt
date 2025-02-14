package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas3

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Bed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.FullPerson
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BedEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RoomEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.BedTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3.Cas3FutureBookingTransformer
import java.time.LocalDate

class Cas3FutureBookingTransformerTest {
  private val mockPersonTransformer = mockk<PersonTransformer>()
  private val mockBedTransformer = mockk<BedTransformer>()
  private val cas3FutureBookingTransformer = Cas3FutureBookingTransformer(mockPersonTransformer, mockBedTransformer)

  @Test
  fun `transformJpaToApi transforms correctly`() {
    val premisesEntity = TemporaryAccommodationPremisesEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
      .produce()

    val roomEntity = RoomEntityFactory()
      .withPremises(premisesEntity)
      .produce()

    val bedEntity = BedEntityFactory()
      .withRoom(roomEntity)
      .produce()

    val bed = Bed(bedEntity.id, name = bedEntity.name, code = bedEntity.code, bedEndDate = bedEntity.endDate)

    val offenderCaseSummary = CaseSummaryFactory().produce()
    val fullPersonSummaryInfo = PersonSummaryInfoResult.Success.Full(
      crn = offenderCaseSummary.crn,
      summary = offenderCaseSummary,
    )

    val person = FullPerson(
      type = PersonType.fullPerson,
      crn = offenderCaseSummary.crn,
      name = "$offenderCaseSummary.name.forename $offenderCaseSummary.name.surname",
      dateOfBirth = LocalDate.parse("1990-09-05"),
      sex = "Male",
      status = PersonStatus.inCustody,
      nomsNumber = offenderCaseSummary.nomsId,
      ethnicity = offenderCaseSummary.profile?.ethnicity,
      nationality = offenderCaseSummary.profile?.nationality,
      religionOrBelief = offenderCaseSummary.profile?.religion,
      genderIdentity = offenderCaseSummary.profile?.genderIdentity,
      prisonName = "HMP Bristol",
      isRestricted = false,
    )

    val bookingEntity = BookingEntityFactory()
      .withPremises(premisesEntity)
      .withBed(bedEntity)
      .withCrn(offenderCaseSummary.crn)
      .withStatus(BookingStatus.departed)
      .withArrivalDate(LocalDate.now().plusDays(5))
      .withDepartureDate(LocalDate.now().plusDays(10))
      .produce()

    every { mockBedTransformer.transformJpaToApi(bedEntity) } returns bed
    every { mockPersonTransformer.transformSummaryToPersonApi(fullPersonSummaryInfo) } returns person

    val result = cas3FutureBookingTransformer.transformJpaToApi(bookingEntity, fullPersonSummaryInfo)

    assertThat(result.id).isEqualTo(bookingEntity.id)
    assertThat(result.person).isEqualTo(person)
    assertThat(result.arrivalDate).isEqualTo(bookingEntity.arrivalDate)
    assertThat(result.departureDate).isEqualTo(bookingEntity.departureDate)
    assertThat(result.bed).isEqualTo(bed)
  }
}
