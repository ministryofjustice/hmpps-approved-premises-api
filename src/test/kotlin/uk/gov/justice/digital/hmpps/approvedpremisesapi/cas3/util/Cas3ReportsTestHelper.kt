package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.util

import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.BookingsReportDataAndPersonInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.reporting.model.PersonInformationReportData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.repository.BookingsReportData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import java.time.Instant
import java.time.LocalDate

fun List<Cas3BookingEntity>.toBookingsReportData(): List<BookingsReportData> = this
  .map {
    val application = it.application as? TemporaryAccommodationApplicationEntity
    object : BookingsReportData {
      override val bookingId: String
        get() = it.id.toString()
      override val referralId: String?
        get() = application?.id?.toString()
      override val referralDate: Instant?
        get() = application?.submittedAt?.toInstant()
      override val riskOfSeriousHarm: String?
        get() = application?.riskRatings?.roshRisks?.value?.overallRisk
      override val registeredSexOffender: Boolean?
        get() = application?.isRegisteredSexOffender
      override val historyOfSexualOffence: Boolean?
        get() = application?.isHistoryOfSexualOffence
      override val concerningSexualBehaviour: Boolean?
        get() = application?.isConcerningSexualBehaviour
      override val needForAccessibleProperty: Boolean?
        get() = application?.needsAccessibleProperty
      override val historyOfArsonOffence: Boolean?
        get() = application?.hasHistoryOfArson
      override val concerningArsonBehaviour: Boolean?
        get() = application?.isConcerningArsonBehaviour
      override val dutyToReferMade: Boolean?
        get() = application?.isDutyToReferSubmitted
      override val dateDutyToReferMade: LocalDate?
        get() = application?.dutyToReferSubmissionDate
      override val referralEligibleForCas3: Boolean?
        get() = application?.isEligible
      override val referralEligibilityReason: String?
        get() = application?.eligibilityReason
      override val probationRegionName: String
        get() = it.bedspace.premises.probationDeliveryUnit.probationRegion.name
      override val localAuthorityAreaName: String?
        get() = it.bedspace.premises.localAuthorityArea?.name
      override val crn: String
        get() = it.crn
      override val confirmationId: String?
        get() = it.confirmation?.id?.toString()
      override val cancellationId: String?
        get() = it.cancellation?.id?.toString()
      override val cancellationReason: String?
        get() = it.cancellation?.reason?.name
      override val startDate: LocalDate?
        get() = it.arrival?.arrivalDate
      override val endDate: LocalDate?
        get() = it.arrival?.expectedDepartureDate
      override val actualEndDate: Instant?
        get() = it.departure?.dateTime?.toInstant()
      override val accommodationOutcome: String?
        get() = it.departure?.moveOnCategory?.name
      override val dutyToReferLocalAuthorityAreaName: String?
        get() = application?.dutyToReferLocalAuthorityAreaName
      override val pdu: String?
        get() = it.bedspace.premises.probationDeliveryUnit.name
      override val town: String?
        get() = it.bedspace.premises.town
      override val postCode: String
        get() = it.bedspace.premises.postcode
    }
  }
  .sortedBy { it.bookingId }

fun List<Cas3BookingEntity>.toBookingsReportDataAndPersonInfo(configuration: (crn: String) -> PersonInformationReportData) = this.toBookingsReportData().map { BookingsReportDataAndPersonInfo(it, configuration(it.crn)) }
