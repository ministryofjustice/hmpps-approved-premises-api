package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.ReferenceDataApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.health.api.model.CancellationReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.health.api.model.DepartureReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.health.api.model.DestinationProvider
import uk.gov.justice.digital.hmpps.approvedpremisesapi.health.api.model.MoveOnCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DestinationProviderRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MoveOnCategoryRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.CancellationReasonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.DepartureReasonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.DestinationProviderTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.MoveOnCategoryTransformer

@Service
class ReferenceDataController(
  private val departureReasonRepository: DepartureReasonRepository,
  private val moveOnCategoryRepository: MoveOnCategoryRepository,
  private val destinationProviderRepository: DestinationProviderRepository,
  private val cancellationReasonRepository: CancellationReasonRepository,
  private val departureReasonTransformer: DepartureReasonTransformer,
  private val moveOnCategoryTransformer: MoveOnCategoryTransformer,
  private val destinationProviderTransformer: DestinationProviderTransformer,
  private val cancellationReasonTransformer: CancellationReasonTransformer
) : ReferenceDataApiDelegate {
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
}
