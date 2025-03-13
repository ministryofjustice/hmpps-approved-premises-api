package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingCharacteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingDaySummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingDaySummarySortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingDaySummarySearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.forCrn
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.CharacteristicService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1SpaceBookingDaySummaryTransformer
import java.time.LocalDate
import java.util.UUID

@Service
class Cas1SpaceBookingDaySummaryService(
  val userAccessService: UserAccessService,
  private val cas1SpaceBookingRepository: Cas1SpaceBookingRepository,
  private val characteristicService: CharacteristicService,
  private val cas1SpaceBookingDaySummaryTransformer: Cas1SpaceBookingDaySummaryTransformer,
  private val offenderService: OffenderService,
  private val userService: UserService,
  private val cas1PremisesService: Cas1PremisesService,
) {

  fun getBookingDaySummaries(
    premisesId: UUID,
    date: LocalDate,
    bookingsCriteriaFilter: List<Cas1SpaceBookingCharacteristic>?,
    bookingsSortBy: Cas1SpaceBookingDaySummarySortField,
    bookingsSortDirection: SortDirection,
    excludeSpaceBookingId: UUID? = null,
  ): CasResult<List<Cas1SpaceBookingDaySummary>> {
    if (cas1PremisesService.findPremiseById(premisesId) == null) return CasResult.NotFound("premises", premisesId.toString())

    val sort = Sort.by(
      when (bookingsSortDirection) {
        SortDirection.desc -> Sort.Direction.DESC
        SortDirection.asc -> Sort.Direction.ASC
      },
      bookingsSortBy.value,
    )

    val spaceBookingsForDate = cas1SpaceBookingRepository.findByPremisesIdAndCriteriaForDate(
      premisesId = premisesId,
      date = date,
      criteria = getBookingCharacteristicIds(bookingsCriteriaFilter),
      sort = sort,
      excludeSpaceBookingId = excludeSpaceBookingId,
    )

    val offenderSummaries = getOffenderSummariesForBookings(spaceBookingsForDate)

    val spaceBookingDaySummaries =
      spaceBookingsForDate.map { bookingSummary ->
        cas1SpaceBookingDaySummaryTransformer.toCas1SpaceBookingDaySummary(
          bookingSummary,
          PersonTransformer()
            .personSummaryInfoToPersonSummary(
              offenderSummaries.forCrn(bookingSummary.crn),
            ),
        )
      }
    return CasResult.Success(
      spaceBookingDaySummaries,
    )
  }

  private fun getBookingCharacteristicIds(bookingsCriteriaFilter: List<Cas1SpaceBookingCharacteristic>?) = bookingsCriteriaFilter?.let { bookingCriteria ->
    val characteristics = bookingCriteria.map { it.value }
    characteristicService.getCharacteristicsByPropertyNames(characteristics, ServiceName.approvedPremises)
      .map { characteristic -> characteristic.id }
  }

  private fun getOffenderSummariesForBookings(spaceBookings: List<Cas1SpaceBookingDaySummarySearchResult>): List<PersonSummaryInfoResult> {
    val user = userService.getUserForRequest()
    return offenderService.getPersonSummaryInfoResults(
      crns = spaceBookings.map { it.crn }.toSet(),
      laoStrategy = user.cas1LaoStrategy(),
    )
  }
}
