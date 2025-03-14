package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas1

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1.ReferenceDataCas1Delegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ChangeRequestType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1CruManagementArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OutOfServiceBedReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.DepartureReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MoveOnCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NamedId
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NonArrivalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1CruManagementAreaRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MoveOnCategoryRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NonArrivalReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1ChangeRequestReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.ChangeRequestType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.DepartureReasonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.MoveOnCategoryTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.NonArrivalReasonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1CruManagementAreaTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1OutOfServiceBedReasonTransformer

@Service
class Cas1ReferenceDataController(
  private val cas1OutOfServiceBedReasonTransformer: Cas1OutOfServiceBedReasonTransformer,
  private val cas1OutOfServiceBedReasonRepository: Cas1OutOfServiceBedReasonRepository,
  private val cas1CruManagementAreaTransformer: Cas1CruManagementAreaTransformer,
  private val cas1CruManagementAreaRepository: Cas1CruManagementAreaRepository,
  private val departureReasonRepository: DepartureReasonRepository,
  private val departureReasonTransformer: DepartureReasonTransformer,
  private val nonArrivalReasonRepository: NonArrivalReasonRepository,
  private val nonArrivalReasonTransformer: NonArrivalReasonTransformer,
  private val moveOnCategoryRepository: MoveOnCategoryRepository,
  private val moveOnCategoryTransformer: MoveOnCategoryTransformer,
  private val changeRequestReasonRepository: Cas1ChangeRequestReasonRepository,
) : ReferenceDataCas1Delegate {

  override fun getChangeRequestReasons(changeRequestType: Cas1ChangeRequestType): ResponseEntity<List<NamedId>> = ResponseEntity.ok(
    changeRequestReasonRepository.findByChangeRequestTypeAndArchivedIsFalse(
      when (changeRequestType) {
        Cas1ChangeRequestType.APPEAL -> ChangeRequestType.APPEAL
        Cas1ChangeRequestType.EXTENSION -> ChangeRequestType.EXTENSION
        Cas1ChangeRequestType.PLANNED_TRANSFER -> ChangeRequestType.PLANNED_TRANSFER
      },
    ).map { NamedId(it.id, it.code) },
  )

  override fun getChangeRequestRejectionReasons(changeRequestType: Cas1ChangeRequestType): ResponseEntity<List<NamedId>> = super.getChangeRequestRejectionReasons(changeRequestType)

  override fun getOutOfServiceBedReasons(): ResponseEntity<List<Cas1OutOfServiceBedReason>> = ResponseEntity.ok(
    cas1OutOfServiceBedReasonRepository.findActive().map { reason ->
      cas1OutOfServiceBedReasonTransformer.transformJpaToApi(reason)
    },
  )

  override fun getCruManagementAreas(): ResponseEntity<List<Cas1CruManagementArea>> = ResponseEntity.ok(
    cas1CruManagementAreaRepository.findAll()
      .map { cas1CruManagementAreaTransformer.transformJpaToApi(it) },
  )

  override fun getDepartureReasons(): ResponseEntity<List<DepartureReason>> = ResponseEntity.ok(
    departureReasonRepository.findActiveForCas1()
      .map { departureReasonTransformer.transformJpaToApi(it) },
  )

  override fun getMoveOnCategories(): ResponseEntity<List<MoveOnCategory>> = ResponseEntity.ok(
    moveOnCategoryRepository.findActiveForCas1()
      .map(moveOnCategoryTransformer::transformJpaToApi),
  )

  override fun getNonArrivalReasons(): ResponseEntity<List<NonArrivalReason>> = ResponseEntity.ok(
    nonArrivalReasonRepository.findAllActiveReasons()
      .map(nonArrivalReasonTransformer::transformJpaToApi),
  )
}
