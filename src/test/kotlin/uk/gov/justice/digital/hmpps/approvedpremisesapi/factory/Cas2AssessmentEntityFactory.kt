package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2AssessmentEntity
import java.time.OffsetDateTime
import java.util.UUID

class Cas2AssessmentEntityFactory : Factory<Cas2AssessmentEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var createdAt: Yielded<OffsetDateTime> = { OffsetDateTime.now() }
  private var application: Yielded<Cas2ApplicationEntity> = {
    Cas2ApplicationEntityFactory()
      .withCreatedByUser(NomisUserEntityFactory().produce())
      .produce()
  }
  private var nacroReferralId: Yielded<String?> = { null }
  private var assessorName: Yielded<String?> = { null }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withApplication(application: Cas2ApplicationEntity) = apply {
    this.application = { application }
  }

  fun withNacroReferralId(id: String) = apply {
    this.nacroReferralId = { id }
  }

  fun withAssessorName(name: String) = apply {
    this.assessorName = { name }
  }

  override fun produce(): Cas2AssessmentEntity = Cas2AssessmentEntity(
    id = this.id(),
    createdAt = this.createdAt(),
    application = this.application(),
    nacroReferralId = this.nacroReferralId(),
    assessorName = this.assessorName(),
  )
}
