package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.ReferenceDataApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.CancellationReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Characteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.DepartureReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.DestinationProvider
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.LocalAuthorityArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.LostBedReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MoveOnCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DestinationProviderRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LocalAuthorityAreaRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LostBedReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MoveOnCategoryRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.CancellationReasonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.CharacteristicTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.DepartureReasonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.DestinationProviderTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.LocalAuthorityAreaTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.LostBedReasonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.MoveOnCategoryTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ProbationRegionTransformer

@Service
class ReferenceDataController(
  private val departureReasonRepository: DepartureReasonRepository,
  private val moveOnCategoryRepository: MoveOnCategoryRepository,
  private val destinationProviderRepository: DestinationProviderRepository,
  private val cancellationReasonRepository: CancellationReasonRepository,
  private val lostBedReasonRepository: LostBedReasonRepository,
  private val localAuthorityAreaRepository: LocalAuthorityAreaRepository,
  private val characteristicRepository: CharacteristicRepository,
  private val probationRegionRepository: ProbationRegionRepository,
  private val departureReasonTransformer: DepartureReasonTransformer,
  private val moveOnCategoryTransformer: MoveOnCategoryTransformer,
  private val destinationProviderTransformer: DestinationProviderTransformer,
  private val cancellationReasonTransformer: CancellationReasonTransformer,
  private val lostBedReasonTransformer: LostBedReasonTransformer,
  private val localAuthorityAreaTransformer: LocalAuthorityAreaTransformer,
  private val characteristicTransformer: CharacteristicTransformer,
  private val probationRegionTransformer: ProbationRegionTransformer,
) : ReferenceDataApiDelegate {

  override fun referenceDataCharacteristicsGet(xServiceName: ServiceName?): ResponseEntity<List<Characteristic>> {

    val characteristics = when (xServiceName != null) {
      true -> characteristicRepository.findAllByServiceScope(xServiceName.value)
      false -> characteristicRepository.findAll()
    }

    return ResponseEntity.ok(characteristics.map(characteristicTransformer::transformJpaToApi))
  }

  override fun referenceDataLocalAuthorityAreasGet(): ResponseEntity<List<LocalAuthorityArea>> {
    val localAuthorities = localAuthorityAreaRepository.findAll()

    return ResponseEntity.ok(localAuthorities.map(localAuthorityAreaTransformer::transformJpaToApi))
  }

  override fun referenceDataDepartureReasonsGet(xServiceName: ServiceName?): ResponseEntity<List<DepartureReason>> {
    val reasons = when (xServiceName != null) {
      true -> departureReasonRepository.findAllByServiceScope(xServiceName.value)
      false -> departureReasonRepository.findAll()
    }

    return ResponseEntity.ok(reasons.map(departureReasonTransformer::transformJpaToApi))
  }

  override fun referenceDataMoveOnCategoriesGet(xServiceName: ServiceName?): ResponseEntity<List<MoveOnCategory>> {
    val moveOnCategories = when (xServiceName != null) {
      true -> moveOnCategoryRepository.findAllByServiceScope(xServiceName.value)
      false -> moveOnCategoryRepository.findAll()
    }

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

  override fun referenceDataProbationRegionsGet(): ResponseEntity<List<ProbationRegion>> {
    val probationRegions = probationRegionRepository.findAll()

    return ResponseEntity.ok(probationRegions.map(probationRegionTransformer::transformJpaToApi))
  }
}
