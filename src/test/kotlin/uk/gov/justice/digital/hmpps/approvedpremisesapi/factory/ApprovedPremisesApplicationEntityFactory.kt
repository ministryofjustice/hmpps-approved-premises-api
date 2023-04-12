package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationTeamCodeEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.JsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomInt
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.OffsetDateTime
import java.util.UUID

class ApprovedPremisesApplicationEntityFactory : Factory<ApprovedPremisesApplicationEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var crn: Yielded<String> = { randomStringMultiCaseWithNumbers(8) }
  private var createdByUser: Yielded<UserEntity>? = null
  private var data: Yielded<String?> = { "{}" }
  private var document: Yielded<String?> = { "{}" }
  private var applicationSchema: Yielded<JsonSchemaEntity> = {
    ApprovedPremisesApplicationJsonSchemaEntityFactory().produce()
  }
  private var createdAt: Yielded<OffsetDateTime> = { OffsetDateTime.now().randomDateTimeBefore(30) }
  private var submittedAt: Yielded<OffsetDateTime?> = { null }
  private var isWomensApplication: Yielded<Boolean?> = { null }
  private var isPipeApplication: Yielded<Boolean?> = { null }
  private var convictionId: Yielded<Long> = { randomInt(0, 1000).toLong() }
  private var eventNumber: Yielded<String> = { randomInt(1, 9).toString() }
  private var offenceId: Yielded<String> = { randomStringMultiCaseWithNumbers(5) }
  private var riskRatings: Yielded<PersonRisks> = { PersonRisksFactory().produce() }
  private var assessments: Yielded<MutableList<AssessmentEntity>> = { mutableListOf<AssessmentEntity>() }
  private var teamCodes: Yielded<MutableList<ApplicationTeamCodeEntity>> = { mutableListOf() }
  private var placementRequests: Yielded<MutableList<PlacementRequestEntity>> = { mutableListOf() }
  private var releaseType: Yielded<String?> = { null }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withCrn(crn: String) = apply {
    this.crn = { crn }
  }

  fun withCreatedByUser(createdByUser: UserEntity) = apply {
    this.createdByUser = { createdByUser }
  }

  fun withYieldedCreatedByUser(createdByUser: Yielded<UserEntity>) = apply {
    this.createdByUser = createdByUser
  }

  fun withData(data: String?) = apply {
    this.data = { data }
  }

  fun withDocument(document: String?) = apply {
    this.document = { document }
  }

  fun withApplicationSchema(applicationSchema: JsonSchemaEntity) = apply {
    this.applicationSchema = { applicationSchema }
  }

  fun withYieldedApplicationSchema(applicationSchema: Yielded<JsonSchemaEntity>) = apply {
    this.applicationSchema = applicationSchema
  }

  fun withCreatedAt(createdAt: OffsetDateTime) = apply {
    this.createdAt = { createdAt }
  }

  fun withSubmittedAt(submittedAt: OffsetDateTime?) = apply {
    this.submittedAt = { submittedAt }
  }

  fun withIsWomensApplication(isWomensApplication: Boolean) = apply {
    this.isWomensApplication = { isWomensApplication }
  }

  fun withIsPipeApplication(isPipeApplication: Boolean) = apply {
    this.isPipeApplication = { isPipeApplication }
  }

  fun withConvictionId(convictionId: Long) = apply {
    this.convictionId = { convictionId }
  }

  fun withEventNumber(eventNumber: String) = apply {
    this.eventNumber = { eventNumber }
  }

  fun withOffenceId(offenceId: String) = apply {
    this.offenceId = { offenceId }
  }

  fun withAssessments(assessments: MutableList<AssessmentEntity>) = apply {
    this.assessments = { assessments }
  }

  fun withTeamCodes(teamCodes: MutableList<ApplicationTeamCodeEntity>) = apply {
    this.teamCodes = { teamCodes }
  }

  fun withPlacementRequests(placementRequests: MutableList<PlacementRequestEntity>) = apply {
    this.placementRequests = { placementRequests }
  }

  fun withReleaseType(releaseType: String) = apply {
    this.releaseType = { releaseType }
  }

  override fun produce(): ApprovedPremisesApplicationEntity = ApprovedPremisesApplicationEntity(
    id = this.id(),
    crn = this.crn(),
    createdByUser = this.createdByUser?.invoke() ?: throw RuntimeException("Must provide a createdByUser"),
    data = this.data(),
    document = this.document(),
    schemaVersion = this.applicationSchema(),
    createdAt = this.createdAt(),
    submittedAt = this.submittedAt(),
    isWomensApplication = this.isWomensApplication(),
    isPipeApplication = this.isPipeApplication(),
    convictionId = this.convictionId(),
    eventNumber = this.eventNumber(),
    offenceId = this.offenceId(),
    schemaUpToDate = false,
    riskRatings = this.riskRatings(),
    assessments = this.assessments(),
    teamCodes = this.teamCodes(),
    placementRequests = this.placementRequests(),
    releaseType = this.releaseType(),
  )
}
