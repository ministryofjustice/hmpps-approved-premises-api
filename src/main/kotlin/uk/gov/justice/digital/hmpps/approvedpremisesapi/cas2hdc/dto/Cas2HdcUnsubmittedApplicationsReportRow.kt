package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.dto

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationOrigin

class Cas2HdcUnsubmittedApplicationsReportRow(
  val applicationId: String,
  val personCrn: String,
  val personNoms: String,
  val startedAt: String,
  val startedBy: String,
  val applicationOrigin: ApplicationOrigin = ApplicationOrigin.homeDetentionCurfew,
)
