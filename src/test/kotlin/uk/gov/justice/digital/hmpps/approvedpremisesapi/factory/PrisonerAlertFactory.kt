package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisoneralertsapi.Alert
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisoneralertsapi.AlertCodeSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringLowerCase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@SuppressWarnings("LongParameterList")
class PrisonerAlertFactory {
  fun produce(
    alertUuid: UUID = UUID.randomUUID(),
    prisonNumber: String = randomStringMultiCaseWithNumbers(7),
    alertCode: AlertCodeSummary = AlertCodeSummaryFactory().produce(),
    description: String? = randomStringMultiCaseWithNumbers(100),
    authorisedBy: String? = randomStringLowerCase(10),
    activeFrom: LocalDate = LocalDate.now().minusDays(30).randomDateBefore(120),
    activeTo: LocalDate? = null,
    isActive: Boolean = true,
    createdAt: LocalDateTime = LocalDateTime.now().randomDateTimeBefore(120),
    createdBy: String = randomStringUpperCase(6),
    createdByDisplayName: String = randomStringUpperCase(20),
    lastModifiedAt: LocalDateTime? = null,
    lastModifiedBy: String? = null,
    lastModifiedByDisplayName: String? = null,
    activeToLastSetAt: LocalDateTime? = null,
    activeToLastSetBy: String? = null,
    activeToLastSetByDisplayName: String? = null,
  ): Alert = Alert(
    alertUuid = alertUuid,
    prisonNumber = prisonNumber,
    alertCode = alertCode,
    description = description,
    authorisedBy = authorisedBy,
    activeFrom = activeFrom,
    activeTo = activeTo,
    isActive = isActive,
    createdAt = createdAt,
    createdBy = createdBy,
    createdByDisplayName = createdByDisplayName,
    lastModifiedAt = lastModifiedAt,
    lastModifiedBy = lastModifiedBy,
    lastModifiedByDisplayName = lastModifiedByDisplayName,
    activeToLastSetAt = activeToLastSetAt,
    activeToLastSetBy = activeToLastSetBy,
    activeToLastSetByDisplayName = activeToLastSetByDisplayName,
  )
}
