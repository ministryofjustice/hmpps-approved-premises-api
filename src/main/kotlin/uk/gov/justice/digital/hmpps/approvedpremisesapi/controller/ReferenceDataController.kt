package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.ReferenceDataApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.CancellationReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Characteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.DepartureReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.DestinationProvider
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.LocalAuthorityArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.LostBedReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MoveOnCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NonArrivalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PrisonReleaseType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ProbationDeliveryUnit
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReferralRejectionReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApAreaRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DestinationProviderRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LocalAuthorityAreaRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LostBedReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MoveOnCategoryRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NonArrivalReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PrisonReleaseTypeRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ReferralRejectionReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ApAreaTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.CancellationReasonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.CharacteristicTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.DepartureReasonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.DestinationProviderTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.LocalAuthorityAreaTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.LostBedReasonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.MoveOnCategoryTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.NonArrivalReasonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PrisonReleaseTypeTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ProbationDeliveryUnitTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ProbationRegionTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ReferralRejectionReasonTransformer
import java.util.UUID

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
  private val nonArrivalReasonRepository: NonArrivalReasonRepository,
  private val probationDeliveryUnitRepository: ProbationDeliveryUnitRepository,
  private val referralRejectionReasonRepository: ReferralRejectionReasonRepository,
  private val departureReasonTransformer: DepartureReasonTransformer,
  private val moveOnCategoryTransformer: MoveOnCategoryTransformer,
  private val destinationProviderTransformer: DestinationProviderTransformer,
  private val cancellationReasonTransformer: CancellationReasonTransformer,
  private val lostBedReasonTransformer: LostBedReasonTransformer,
  private val localAuthorityAreaTransformer: LocalAuthorityAreaTransformer,
  private val characteristicTransformer: CharacteristicTransformer,
  private val probationRegionTransformer: ProbationRegionTransformer,
  private val nonArrivalReasonTransformer: NonArrivalReasonTransformer,
  private val probationDeliveryUnitTransformer: ProbationDeliveryUnitTransformer,
  private val referralRejectionReasonTransformer: ReferralRejectionReasonTransformer,
  private val prisonReleaseTypeTransformer: PrisonReleaseTypeTransformer,
  private val apAreaRepository: ApAreaRepository,
  private val apAreaTransformer: ApAreaTransformer,
  private val prisonReleaseTypeRepository: PrisonReleaseTypeRepository,
) : ReferenceDataApiDelegate {

  override fun referenceDataCharacteristicsGet(xServiceName: ServiceName?, includeInactive: Boolean?): ResponseEntity<List<Characteristic>> {
    val characteristics = when (xServiceName != null) {
      true -> when (includeInactive) {
        true -> characteristicRepository.findAllByServiceScope(xServiceName.value)
        else -> characteristicRepository.findActiveByServiceScope(xServiceName.value)
      }
      false -> when (includeInactive) {
        true -> characteristicRepository.findAll()
        else -> characteristicRepository.findActive()
      }
    }

    return ResponseEntity.ok(characteristics.map(characteristicTransformer::transformJpaToApi))
  }

  override fun referenceDataLocalAuthorityAreasGet(): ResponseEntity<List<LocalAuthorityArea>> {
    val localAuthorities = localAuthorityAreaRepository.findAll()

    return ResponseEntity.ok(localAuthorities.map(localAuthorityAreaTransformer::transformJpaToApi))
  }

  override fun referenceDataDepartureReasonsGet(
    xServiceName: ServiceName?,
    includeInactive: Boolean?,
  ): ResponseEntity<List<DepartureReason>> {
    val reasons = when (xServiceName != null) {
      true -> when (includeInactive) {
        true -> departureReasonRepository.findAllByServiceScope(xServiceName.value)
        else -> departureReasonRepository.findActiveByServiceScope(xServiceName.value)
      }
      false -> when (includeInactive) {
        true -> departureReasonRepository.findAll()
        else -> departureReasonRepository.findActive()
      }
    }

    return ResponseEntity.ok(reasons.map(departureReasonTransformer::transformJpaToApi))
  }

  override fun referenceDataMoveOnCategoriesGet(
    xServiceName: ServiceName?,
    includeInactive: Boolean?,
  ): ResponseEntity<List<MoveOnCategory>> {
    val moveOnCategories = when (xServiceName != null) {
      true -> when (includeInactive) {
        true -> moveOnCategoryRepository.findAllByServiceScope(xServiceName.value)
        else -> moveOnCategoryRepository.findActiveByServiceScope(xServiceName.value)
      }
      false -> when (includeInactive) {
        true -> moveOnCategoryRepository.findAll()
        else -> moveOnCategoryRepository.findActive()
      }
    }

    return ResponseEntity.ok(moveOnCategories.map(moveOnCategoryTransformer::transformJpaToApi))
  }

  override fun referenceDataDestinationProvidersGet(): ResponseEntity<List<DestinationProvider>> {
    val destinationProviders = destinationProviderRepository.findAll()

    return ResponseEntity.ok(destinationProviders.map(destinationProviderTransformer::transformJpaToApi))
  }

  override fun referenceDataCancellationReasonsGet(xServiceName: ServiceName?): ResponseEntity<List<CancellationReason>> {
    val cancellationReasons = when (xServiceName != null) {
      true -> cancellationReasonRepository.findAllByServiceScope(xServiceName.value)
      false -> cancellationReasonRepository.findAll()
    }

    return ResponseEntity.ok(cancellationReasons.map(cancellationReasonTransformer::transformJpaToApi))
  }

  override fun referenceDataLostBedReasonsGet(xServiceName: ServiceName?): ResponseEntity<List<LostBedReason>> {
    val lostBedReasons = when (xServiceName != null) {
      true -> lostBedReasonRepository.findAllByServiceScope(xServiceName.value)
      false -> lostBedReasonRepository.findAll()
    }

    return ResponseEntity.ok(lostBedReasons.map(lostBedReasonTransformer::transformJpaToApi))
  }

  override fun referenceDataProbationRegionsGet(): ResponseEntity<List<ProbationRegion>> {
    val probationRegions = probationRegionRepository.findAll()

    return ResponseEntity.ok(probationRegions.map(probationRegionTransformer::transformJpaToApi))
  }

  override fun referenceDataApAreasGet(): ResponseEntity<List<ApArea>> {
    val apAreas = apAreaRepository.findAll()

    return ResponseEntity.ok(apAreas.map(apAreaTransformer::transformJpaToApi))
  }

  override fun referenceDataNonArrivalReasonsGet(): ResponseEntity<List<NonArrivalReason>> {
    val reasons = nonArrivalReasonRepository.findAll()

    return ResponseEntity.ok(reasons.map(nonArrivalReasonTransformer::transformJpaToApi))
  }

  override fun referenceDataProbationDeliveryUnitsGet(probationRegionId: UUID?): ResponseEntity<List<ProbationDeliveryUnit>> {
    val probationDeliveryUnits = when (probationRegionId) {
      null -> probationDeliveryUnitRepository.findAll()
      else -> probationDeliveryUnitRepository.findAllByProbationRegionId(probationRegionId)
    }

    return ResponseEntity.ok(probationDeliveryUnits.map(probationDeliveryUnitTransformer::transformJpaToApi))
  }

  override fun referenceDataReferralRejectionReasonsGet(
    xServiceName: ServiceName?,
  ): ResponseEntity<List<ReferralRejectionReason>> {
    val referralRejectionReasons = when (xServiceName == ServiceName.temporaryAccommodation) {
      true -> referralRejectionReasonRepository.findAllByServiceScope(xServiceName.value)
      else -> throw ForbiddenProblem()
    }

    return ResponseEntity.ok(referralRejectionReasons.map(referralRejectionReasonTransformer::transformJpaToApi))
  }

  override fun referenceDataPrisonReleaseTypesGet(
    xServiceName: ServiceName?,
  ): ResponseEntity<List<PrisonReleaseType>> {
    val prisonReleaseTypes = when (xServiceName == ServiceName.temporaryAccommodation) {
      true -> prisonReleaseTypeRepository.findAllByServiceScope(xServiceName.value)
      else -> throw ForbiddenProblem()
    }

    return ResponseEntity.ok(prisonReleaseTypes.map(prisonReleaseTypeTransformer::transformJpaToApi))
  }
}
