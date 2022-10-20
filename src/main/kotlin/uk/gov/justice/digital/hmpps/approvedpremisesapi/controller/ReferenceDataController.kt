package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.ReferenceDataApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.*
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.*
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.*

@Service
class ReferenceDataController(
  private val departureReasonRepository: DepartureReasonRepository,
  private val moveOnCategoryRepository: MoveOnCategoryRepository,
  private val destinationProviderRepository: DestinationProviderRepository,
  private val cancellationReasonRepository: CancellationReasonRepository,
  private val lostBedReasonRepository: LostBedReasonRepository,
  private val localAuthorityAreaRepository: LocalAuthorityAreaRepository,
  private val departureReasonTransformer: DepartureReasonTransformer,
  private val localAuthorityAreaTransformer: LocalAuthorityAreaTransformer,
  private val moveOnCategoryTransformer: MoveOnCategoryTransformer,
  private val destinationProviderTransformer: DestinationProviderTransformer,
  private val cancellationReasonTransformer: CancellationReasonTransformer,
  private val lostBedReasonTransformer: LostBedReasonTransformer
) : ReferenceDataApiDelegate {

  override fun referenceDataLocalAuthoritiesGet(): ResponseEntity<List<LocalAuthorityArea>> {
    val localAuthorities = localAuthorityAreaRepository.findAll()

    return ResponseEntity.ok(localAuthorities.map(localAuthorityAreaTransformer::transformJpaToApi))
  }

  override fun referenceDataDepartureReasonsGet(): ResponseEntity<List<DepartureReason>> {
    val reasons = departureReasonRepository.findAll()

    return ResponseEntity.ok(reasons.map(departureReasonTransformer::transformJpaToApi))
  }

  override fun referenceDataMoveOnCategoriesGet(): ResponseEntity<List<MoveOnCategory>> {
    val moveOnCategories = moveOnCategoryRepository.findAll()

    return ResponseEntity.ok(moveOnCategories.map(moveOnCategoryTransformer::transformJpaToApi))
  }

  override fun referenceDataDestinationProvidersGet(): ResponseEntity<List<DestinationProvider>> {
    val destinationProviders = destinationProviderRepository.findAll()

    return ResponseEntity.ok(destinationProviders.map(destinationProviderTransformer::transformJpaToApi))
  }

  override fun referenceDataCancellationReasonsGet(): ResponseEntity<List<CancellationReason>> {
    val cancellationReasons = cancellationReasonRepository.findAll()

    return ResponseEntity.ok(cancellationReasons.map(cancellationReasonTransformer::transformJpaToApi))
  }

  override fun referenceDataLostBedReasonsGet(): ResponseEntity<List<LostBedReason>> {
    val lostBedReasons = lostBedReasonRepository.findAll()

    return ResponseEntity.ok(lostBedReasons.map(lostBedReasonTransformer::transformJpaToApi))
  }
}
