package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AppealDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AppealEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomOf
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class AppealEntityFactory : Factory<AppealEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var appealDate: Yielded<LocalDate> = { LocalDate.now().randomDateBefore(14) }
  private var appealDetail: Yielded<String> = { randomStringMultiCaseWithNumbers(20) }
  private var decision: Yielded<String> = { randomOf(AppealDecision.entries).value }
  private var decisionDetail: Yielded<String> = { randomStringMultiCaseWithNumbers(20) }
  private var createdAt: Yielded<OffsetDateTime> = { OffsetDateTime.now() }
  private var application: Yielded<ApprovedPremisesApplicationEntity> = {
    ApprovedPremisesApplicationEntityFactory()
      .withYieldedCreatedByUser(createdBy)
      .produce()
  }
  private var assessment: Yielded<ApprovedPremisesAssessmentEntity> = {
    ApprovedPremisesAssessmentEntityFactory()
      .withYieldedApplication(application)
      .produce()
  }
  private var createdBy: Yielded<UserEntity> = {
    UserEntityFactory()
      .withProbationRegion(
        ProbationRegionEntityFactory()
          .withApArea(
            ApAreaEntityFactory().produce(),
          )
          .produce(),
      )
      .produce()
  }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withAppealDate(appealDate: LocalDate) = apply {
    this.appealDate = { appealDate }
  }

  fun withAppealDetail(appealDetail: String) = apply {
    this.appealDetail = { appealDetail }
  }

  fun withDecision(decision: AppealDecision) = apply {
    this.decision = { decision.value }
  }

  fun withDecisionDetail(decisionDetail: String) = apply {
    this.decisionDetail = { decisionDetail }
  }

  fun withCreatedAt(createdAt: OffsetDateTime) = apply {
    this.createdAt = { createdAt }
  }

  fun withApplication(configuration: ApprovedPremisesApplicationEntityFactory.() -> Unit) = apply {
    this.application = { ApprovedPremisesApplicationEntityFactory().apply(configuration).produce() }
  }

  fun withApplication(application: ApprovedPremisesApplicationEntity) = apply {
    this.application = { application }
  }

  fun withAssessment(configuration: ApprovedPremisesAssessmentEntityFactory.() -> Unit) = apply {
    this.assessment = { ApprovedPremisesAssessmentEntityFactory().apply(configuration).produce() }
  }

  fun withAssessment(assessment: ApprovedPremisesAssessmentEntity) = apply {
    this.assessment = { assessment }
  }

  fun withCreatedBy(configuration: UserEntityFactory.() -> Unit) = apply {
    this.createdBy = { UserEntityFactory().apply(configuration).produce() }
  }

  fun withCreatedBy(createdBy: UserEntity) = apply {
    this.createdBy = { createdBy }
  }

  override fun produce() = AppealEntity(
    id = this.id(),
    appealDate = this.appealDate(),
    appealDetail = this.appealDetail(),
    decision = this.decision(),
    decisionDetail = this.decisionDetail(),
    createdAt = this.createdAt(),
    application = this.application(),
    assessment = this.assessment(),
    createdBy = this.createdBy(),
  )
}
