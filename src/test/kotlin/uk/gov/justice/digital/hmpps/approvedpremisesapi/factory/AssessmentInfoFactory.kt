package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.oasyscontext.AssessmentInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomInt
import java.time.OffsetDateTime

abstract class AssessmentInfoFactory<T : AssessmentInfo> : Factory<T> {
  protected var assessmentId: Yielded<Long> = { randomInt(1000, 10000).toLong() }
  protected var assessmentType: Yielded<String> = { "LAYER3" }
  protected var dateCompleted: Yielded<OffsetDateTime?> = { null }
  protected var assessorSignedDate: Yielded<OffsetDateTime?> = { null }
  protected var initiationDate: Yielded<OffsetDateTime> = { OffsetDateTime.now().randomDateTimeBefore(7) }
  protected var assessmentStatus: Yielded<String> = { "OPEN" }
  protected var superStatus: Yielded<String> = { "WIP" }
  protected var limitedAccessOffender: Yielded<Boolean> = { false }

  fun withAssessmentId(assessmentId: Long) = apply {
    this.assessmentId = { assessmentId }
  }

  fun withAssessmentType(assessmentType: String) = apply {
    this.assessmentType = { assessmentType }
  }

  fun withDateCompleted(dateCompleted: OffsetDateTime?) = apply {
    this.dateCompleted = { dateCompleted }
  }

  fun withAssessorSignedDate(assessorSignedDate: OffsetDateTime?) = apply {
    this.assessorSignedDate = { assessorSignedDate }
  }

  fun withInitiationDate(initiationDate: OffsetDateTime) = apply {
    this.initiationDate = { initiationDate }
  }

  fun withAssessmentStatus(assessmentStatus: String) = apply {
    this.assessmentStatus = { assessmentStatus }
  }

  fun withSuperStatus(superStatus: String) = apply {
    this.superStatus = { superStatus }
  }

  fun withLimitedAccessOffender(limitedAccessOffender: Boolean) = apply {
    this.limitedAccessOffender = { limitedAccessOffender }
  }
}
