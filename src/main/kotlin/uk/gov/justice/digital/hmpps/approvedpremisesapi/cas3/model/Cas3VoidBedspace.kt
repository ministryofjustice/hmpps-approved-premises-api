package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.LostBedCancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.LostBedReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.LostBedStatus

data class Cas3VoidBedspace(
    val id: java.util.UUID,
    val startDate: java.time.LocalDate,
    val endDate: java.time.LocalDate,
    val bedId: java.util.UUID,
    val bedName: kotlin.String,
    val roomName: kotlin.String,
    val reason: LostBedReason,
    val status: LostBedStatus, val referenceNumber: kotlin.String? = null,
    val notes: kotlin.String? = null,
    val cancellation: LostBedCancellation? = null,
)