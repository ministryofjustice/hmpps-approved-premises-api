package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas1

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1SpaceBookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1ChangeRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1ChangeRequestReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.ChangeRequestDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.ChangeRequestType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomInt
import java.time.OffsetDateTime
import java.util.UUID

class Cas1ChangeRequestEntityFactory : Factory<Cas1ChangeRequestEntity> {
  private var id = { UUID.randomUUID() }
  private var placementRequest = { PlacementRequestEntityFactory().withDefaults().produce() }
  private var spaceBooking = { Cas1SpaceBookingEntityFactory().produce() }
  private var type = { ChangeRequestType.APPEAL }
  private var requestJson = { "{}" }
  private var requestReason = { Cas1ChangeRequestReasonEntityFactory().produce() }
  private var decisionJson = { null }
  private var decision: Yielded<ChangeRequestDecision?> = { ChangeRequestDecision.APPROVED }
  private var rejectionReason = { null }
  private var decisionMadeByUser: Yielded<UserEntity?> = { UserEntityFactory().withDefaults().produce() }
  private var decisionMadeAt: Yielded<OffsetDateTime?> = { null }
  private var createdAt = { OffsetDateTime.now().minusDays(randomInt(0, 365).toLong()) }
  private var updatedAt = { OffsetDateTime.now() }

  fun withDecisionMadeByUser(user: UserEntity?) = apply {
    this.decisionMadeByUser = { user }
  }

  fun withUser(user: UserEntity) = apply {
    this.decisionMadeByUser = { user }
  }

  fun withSpaceBooking(spaceBooking: Cas1SpaceBookingEntity) = apply {
    this.spaceBooking = { spaceBooking }
  }

  fun withChangeRequestReason(changeRequestReason: Cas1ChangeRequestReasonEntity) = apply {
    this.requestReason = { changeRequestReason }
  }

  fun withPlacementRequest(placementRequest: PlacementRequestEntity) = apply {
    this.placementRequest = { placementRequest }
  }

  fun withType(type: ChangeRequestType) = apply {
    this.type = { type }
  }

  fun withDecision(decision: ChangeRequestDecision?) = apply {
    this.decision = { decision }
  }

  override fun produce(): Cas1ChangeRequestEntity {
    val cas1ChangeRequestEntity = Cas1ChangeRequestEntity(
      id = this.id(),
      placementRequest = this.placementRequest(),
      spaceBooking = this.spaceBooking(),
      type = this.type(),
      requestJson = this.requestJson(),
      requestReason = this.requestReason(),
      decisionJson = this.decisionJson(),
      decision = this.decision(),
      decisionMadeAt = this.decisionMadeAt(),
      rejectionReason = this.rejectionReason(),
      decisionMadeByUser = this.decisionMadeByUser(),
      createdAt = this.createdAt(),
      updatedAt = this.updatedAt(),
    )
    return cas1ChangeRequestEntity
  }
}
