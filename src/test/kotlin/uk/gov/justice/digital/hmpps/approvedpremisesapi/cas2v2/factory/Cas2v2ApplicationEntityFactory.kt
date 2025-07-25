package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2ApplicationNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2StatusUpdateEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.jpa.entity.Cas2v2UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomInt
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomNumberChars
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class Cas2v2ApplicationEntityFactory : Factory<Cas2v2ApplicationEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var crn: Yielded<String> = { randomStringMultiCaseWithNumbers(8) }
  private var createdByUser: Yielded<Cas2v2UserEntity>? = null
  private var data: Yielded<String?> = { "{}" }
  private var document: Yielded<String?> = { "{}" }
  private var createdAt: Yielded<OffsetDateTime> = { OffsetDateTime.now().randomDateTimeBefore(30) }
  private var submittedAt: Yielded<OffsetDateTime?> = { null }
  private var abandonedAt: Yielded<OffsetDateTime?> = { null }
  private var statusUpdates: Yielded<MutableList<Cas2v2StatusUpdateEntity>> = { mutableListOf() }
  private var eventNumber: Yielded<String> = { randomInt(1, 9).toString() }
  private var nomsNumber: Yielded<String> = { randomStringUpperCase(6) }
  private var telephoneNumber: Yielded<String?> = { randomNumberChars(12) }
  private var notes: Yielded<MutableList<Cas2v2ApplicationNoteEntity>> = { mutableListOf() }
  private var assessment: Yielded<Cas2v2AssessmentEntity?> = { null }
  private var referringPrisonCode: Yielded<String?> = { null }
  private var preferredAreas: Yielded<String?> = { null }
  private var hdcEligibilityDate: Yielded<LocalDate?> = { null }
  private var conditionalReleaseDate: Yielded<LocalDate?> = { null }
  private var applicationOrigin: Yielded<ApplicationOrigin> = { ApplicationOrigin.homeDetentionCurfew }
  private var bailHearingDate: Yielded<LocalDate?> = { null }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withCrn(crn: String) = apply {
    this.crn = { crn }
  }

  fun withNomsNumber(nomsNumber: String) = apply {
    this.nomsNumber = { nomsNumber }
  }

  fun withCreatedByUser(createdByUser: Cas2v2UserEntity) = apply {
    this.createdByUser = { createdByUser }
  }

  fun withYieldedCreatedByUser(createdByUser: Yielded<Cas2v2UserEntity>) = apply {
    this.createdByUser = createdByUser
  }

  fun withData(data: String?) = apply {
    this.data = { data }
  }

  fun withDocument(document: String?) = apply {
    this.document = { document }
  }

  fun withCreatedAt(createdAt: OffsetDateTime) = apply {
    this.createdAt = { createdAt }
  }

  fun withSubmittedAt(submittedAt: OffsetDateTime?) = apply {
    this.submittedAt = { submittedAt }
  }

  fun withAbandonedAt(abandonedAt: OffsetDateTime?) = apply {
    this.abandonedAt = { abandonedAt }
  }

  fun withStatusUpdates(statusUpdates: MutableList<Cas2v2StatusUpdateEntity>) = apply {
    this.statusUpdates = { statusUpdates }
  }

  fun withNotes(notes: MutableList<Cas2v2ApplicationNoteEntity>) = apply {
    this.notes = { notes }
  }

  fun withEventNumber(eventNumber: String) = apply {
    this.eventNumber = { eventNumber }
  }

  fun withAssessment(assessmentEntity: Cas2v2AssessmentEntity) = apply {
    this.assessment = { assessmentEntity }
  }

  fun withReferringPrisonCode(referringPrisonCode: String) = apply {
    this.referringPrisonCode = { referringPrisonCode }
  }

  fun withPreferredAreas(preferredAreas: String) = apply {
    this.preferredAreas = { preferredAreas }
  }

  fun withTelephoneNumber(telephoneNumber: String) = apply {
    this.telephoneNumber = { telephoneNumber }
  }

  fun withHdcEligibilityDate(hdcEligibilityDate: LocalDate) = apply {
    this.hdcEligibilityDate = { hdcEligibilityDate }
  }

  fun withConditionalReleaseDate(conditionalReleaseDate: LocalDate) = apply {
    this.conditionalReleaseDate = { conditionalReleaseDate }
  }

  fun withApplicationOrigin(applicationOrigin: ApplicationOrigin) = apply {
    this.applicationOrigin = { applicationOrigin }
  }

  fun withBailHearingDate(bailHearingDate: LocalDate) = apply {
    this.bailHearingDate = { bailHearingDate }
  }

  @SuppressWarnings("TooGenericExceptionThrown")
  override fun produce(): Cas2v2ApplicationEntity {
    val entity = Cas2v2ApplicationEntity(
      id = this.id(),
      crn = this.crn(),
      createdByUser = this.createdByUser?.invoke() ?: throw RuntimeException("Must provide a createdByUser"),
      data = this.data(),
      document = this.document(),
      createdAt = this.createdAt(),
      submittedAt = this.submittedAt(),
      abandonedAt = this.abandonedAt(),
      statusUpdates = this.statusUpdates(),
      nomsNumber = this.nomsNumber(),
      telephoneNumber = this.telephoneNumber(),
      notes = this.notes(),
      assessment = this.assessment(),
      referringPrisonCode = this.referringPrisonCode(),
      hdcEligibilityDate = this.hdcEligibilityDate(),
      conditionalReleaseDate = this.conditionalReleaseDate(),
      preferredAreas = this.preferredAreas(),
      applicationOrigin = this.applicationOrigin(),
      bailHearingDate = this.bailHearingDate(),
    )

    return entity
  }
}
