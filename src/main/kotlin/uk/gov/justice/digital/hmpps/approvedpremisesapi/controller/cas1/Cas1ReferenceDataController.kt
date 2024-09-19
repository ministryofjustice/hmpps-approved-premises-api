package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas1

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1.ReferenceDataCas1Delegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1CruManagementArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OutOfServiceBedReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1CruManagementAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1CruManagementAreaRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.FeatureFlagService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1OutOfServiceBedReasonTransformer

@Service
class Cas1ReferenceDataController(
  private val cas1OutOfServiceBedReasonTransformer: Cas1OutOfServiceBedReasonTransformer,
  private val cas1OutOfServiceBedReasonRepository: Cas1OutOfServiceBedReasonRepository,
  private val cas1CruManagementAreaRepository: Cas1CruManagementAreaRepository,
  private val featureFlagService: FeatureFlagService,
) : ReferenceDataCas1Delegate {

  override fun getOutOfServiceBedReasons(): ResponseEntity<List<Cas1OutOfServiceBedReason>> {
    return ResponseEntity.ok(
      cas1OutOfServiceBedReasonRepository.findActive().map {
          reason ->
        cas1OutOfServiceBedReasonTransformer.transformJpaToApi(reason)
      },
    )
  }

  override fun getCruManagementAreas(): ResponseEntity<List<Cas1CruManagementArea>> {
    val womensEstateEnabled = featureFlagService.getBooleanFlag("cas1-womens-estate-enabled")
    return ResponseEntity.ok(
      cas1CruManagementAreaRepository.findAll().map {
        Cas1CruManagementArea(
          id = it.id,
          name = it.name,
        )
      }.filter {
        womensEstateEnabled || it.id != Cas1CruManagementAreaEntity.WOMENS_ESTATE_ID
      },
    )
  }
}
