package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Gender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingNotMadeEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PostCodeDistrictEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class PlacementRequestEntityFactory : Factory<PlacementRequestEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var gender: Yielded<Gender> = { Gender.male }
  private var apType: Yielded<ApType> = { ApType.normal }
  private var expectedArrival: Yielded<LocalDate> = { LocalDate.now() }
  private var duration: Yielded<Int> = { 12 }
  private var postcodeDistrict: Yielded<PostCodeDistrictEntity> = { PostCodeDistrictEntityFactory().produce() }
  private var application: Yielded<ApprovedPremisesApplicationEntity>? = null
  private var assessment: Yielded<AssessmentEntity>? = null
  private var radius: Yielded<Int> = { 50 }
  private var essentialCriteria: Yielded<List<CharacteristicEntity>> = { listOf(CharacteristicEntityFactory().produce()) }
  private var desirableCriteria: Yielded<List<CharacteristicEntity>> = { listOf(CharacteristicEntityFactory().produce(), CharacteristicEntityFactory().produce()) }
  private var createdAt: Yielded<OffsetDateTime> = { OffsetDateTime.now() }
  private var allocatedToUser: Yielded<UserEntity> = { UserEntityFactory().produce() }
  private var booking: Yielded<BookingEntity?> = { null }
  private var bookingNotMades: Yielded<MutableList<BookingNotMadeEntity>> = { mutableListOf() }
  private var reallocatedAt: Yielded<OffsetDateTime?> = { null }
  private var notes: Yielded<String?> = { null }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withAllocatedToUser(user: UserEntity) = apply {
    this.allocatedToUser = { user }
  }

  fun withBooking(booking: BookingEntity) = apply {
    this.booking = { booking }
  }

  fun withApplication(application: ApprovedPremisesApplicationEntity) = apply {
    this.application = { application }
  }

  fun withAssessment(assessment: AssessmentEntity) = apply {
    this.assessment = { assessment }
  }

  fun withReallocatedAt(reallocatedAt: OffsetDateTime) = apply {
    this.reallocatedAt = { reallocatedAt }
  }

  fun withPostcodeDistrict(postCodeDistrictEntity: PostCodeDistrictEntity) = apply {
    this.postcodeDistrict = { postCodeDistrictEntity }
  }

  fun withEssentialCriteria(essentialCriteria: List<CharacteristicEntity>) = apply {
    this.essentialCriteria = { essentialCriteria }
  }

  fun withDesirableCriteria(desirableCriteria: List<CharacteristicEntity>) = apply {
    this.desirableCriteria = { desirableCriteria }
  }

  fun withBookingNotMades(bookingNotMades: MutableList<BookingNotMadeEntity>) = apply {
    this.bookingNotMades = { bookingNotMades }
  }

  fun withNotes(notes: String?) = apply {
    this.notes = { notes }
  }

  override fun produce(): PlacementRequestEntity = PlacementRequestEntity(
    id = this.id(),
    gender = this.gender(),
    apType = this.apType(),
    expectedArrival = this.expectedArrival(),
    duration = this.duration(),
    postcodeDistrict = this.postcodeDistrict(),
    application = this.application?.invoke() ?: throw RuntimeException("Must provide an Application"),
    assessment = this.assessment?.invoke() ?: throw RuntimeException("Must provide an Assessment"),
    radius = this.radius(),
    essentialCriteria = this.essentialCriteria(),
    desirableCriteria = this.desirableCriteria(),
    createdAt = this.createdAt(),
    allocatedToUser = this.allocatedToUser(),
    booking = this.booking(),
    bookingNotMades = this.bookingNotMades(),
    reallocatedAt = this.reallocatedAt(),
    notes = this.notes(),
  )
}
