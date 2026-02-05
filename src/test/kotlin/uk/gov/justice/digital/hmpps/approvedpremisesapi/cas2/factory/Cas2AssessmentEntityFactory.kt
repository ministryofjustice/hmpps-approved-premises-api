package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2StatusUpdateEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ServiceOrigin
import java.time.OffsetDateTime
import java.util.UUID

class Cas2AssessmentEntityFactory : Factory<Cas2AssessmentEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var createdAt: Yielded<OffsetDateTime> = { OffsetDateTime.now() }
  private var application: Yielded<Cas2ApplicationEntity> = {
    Cas2ApplicationEntityFactory()
      .withCreatedByUser(Cas2UserEntityFactory().produce())
      .produce()
  }
  private var nacroReferralId: String? = null
  private var assessorName: String? = null
  private var statusUpdates: MutableList<Cas2StatusUpdateEntity> = mutableListOf()
  private var serviceOrigin: Yielded<Cas2ServiceOrigin> = { Cas2ServiceOrigin.HDC }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withApplication(application: Cas2ApplicationEntity) = apply {
    this.application = { application }
  }

  fun withStatusUpdates(statusUpdates: MutableList<Cas2StatusUpdateEntity>) = apply {
    this.statusUpdates = statusUpdates
  }

  fun withNacroReferralId(id: String) = apply {
    this.nacroReferralId = id
  }

  fun withAssessorName(name: String) = apply {
    this.assessorName = name
  }

  fun withCreatedAt(createdAt: OffsetDateTime) = apply {
    this.createdAt = { createdAt }
  }

  fun withServiceOrigin(serviceOrigin: Cas2ServiceOrigin) = apply {
    this.serviceOrigin = { serviceOrigin }
  }

  override fun produce(): Cas2AssessmentEntity = Cas2AssessmentEntity(
    id = this.id(),
    createdAt = this.createdAt(),
    application = this.application(),
    nacroReferralId = this.nacroReferralId,
    assessorName = this.assessorName,
    statusUpdates = this.statusUpdates,
    serviceOrigin = this.serviceOrigin(),
  )
}
