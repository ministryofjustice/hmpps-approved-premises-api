package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.client.apandoasys

import io.github.bluegroundltd.kfactory.Factory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.apandoasys.OASysAssessmentSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomLong
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.time.OffsetDateTime

class OASysAssessmentSummaryFactory : Factory<OASysAssessmentSummary> {

  private var assessmentPk = { randomLong() }
  private var assessmentType = { randomStringUpperCase(6) }
  private var initiationDate = { OffsetDateTime.now() }
  private var status = { randomStringUpperCase(6) }
  private var completedDate = { OffsetDateTime.now() }

  fun withInitiationDate(initiationDate: OffsetDateTime) = apply {
    this.initiationDate = { initiationDate }
  }

  fun withCompletedDate(completedDate: OffsetDateTime) = apply {
    this.completedDate = { completedDate }
  }

  override fun produce() = OASysAssessmentSummary(
    assessmentPk = this.assessmentPk(),
    assessmentType = this.assessmentType(),
    initiationDate = this.initiationDate(),
    status = this.status(),
    completedDate = this.completedDate(),
  )
}
