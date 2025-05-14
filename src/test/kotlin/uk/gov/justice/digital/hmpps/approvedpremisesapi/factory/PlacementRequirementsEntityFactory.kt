package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.JpaApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequirementsEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PostCodeDistrictEntity
import java.time.OffsetDateTime
import java.util.UUID

class PlacementRequirementsEntityFactory : Factory<PlacementRequirementsEntity> {

  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var apType: Yielded<JpaApType> = { JpaApType.NORMAL }
  private var postcodeDistrict: Yielded<PostCodeDistrictEntity> = { PostCodeDistrictEntityFactory().produce() }
  private var application: Yielded<ApprovedPremisesApplicationEntity>? = null
  private var assessment: Yielded<ApprovedPremisesAssessmentEntity>? = null
  private var radius: Yielded<Int> = { 50 }
  private var essentialCriteria: Yielded<List<CharacteristicEntity>> = { listOf(CharacteristicEntityFactory().produce()) }
  private var desirableCriteria: Yielded<List<CharacteristicEntity>> = { listOf(CharacteristicEntityFactory().produce(), CharacteristicEntityFactory().produce()) }
  private var createdAt: Yielded<OffsetDateTime> = { OffsetDateTime.now() }

  fun withDefaults() = apply {
    this.application = { ApprovedPremisesApplicationEntityFactory().withDefaults().produce() }
    this.assessment = { ApprovedPremisesAssessmentEntityFactory().withDefaults().produce() }
  }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withApplication(application: ApprovedPremisesApplicationEntity) = apply {
    this.application = { application }
  }

  fun withApType(apType: JpaApType) = apply {
    this.apType = { apType }
  }

  fun withAssessment(assessment: ApprovedPremisesAssessmentEntity) = apply {
    this.assessment = { assessment }
  }

  fun withPostcodeDistrict(postCodeDistrictEntity: PostCodeDistrictEntity) = apply {
    this.postcodeDistrict = { postCodeDistrictEntity }
  }

  fun withYieldedPostcodeDistrict(postcodeDistrict: Yielded<PostCodeDistrictEntity>) = apply {
    this.postcodeDistrict = postcodeDistrict
  }

  fun withEssentialCriteria(essentialCriteria: List<CharacteristicEntity>) = apply {
    this.essentialCriteria = { essentialCriteria }
  }

  fun withDesirableCriteria(desirableCriteria: List<CharacteristicEntity>) = apply {
    this.desirableCriteria = { desirableCriteria }
  }

  fun withCreatedAt(createdAt: OffsetDateTime) = apply {
    this.createdAt = { createdAt }
  }

  override fun produce(): PlacementRequirementsEntity = PlacementRequirementsEntity(
    id = this.id(),
    apType = this.apType(),
    postcodeDistrict = this.postcodeDistrict(),
    application = this.application?.invoke() ?: throw RuntimeException("Must provide an Application"),
    assessment = this.assessment?.invoke() ?: throw RuntimeException("Must provide an Assessment"),
    radius = this.radius(),
    essentialCriteria = this.essentialCriteria(),
    desirableCriteria = this.desirableCriteria(),
    createdAt = this.createdAt(),
  )
}
