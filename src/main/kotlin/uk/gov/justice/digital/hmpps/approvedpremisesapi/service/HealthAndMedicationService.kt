package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.HealthAndMedicationApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.health.DietAndAllergyResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.service.CaseService

@Service
class HealthAndMedicationService(
  private val healthAndMedicationApiClient: HealthAndMedicationApiClient,
  private val caseService: CaseService,
) {

  fun getDietAndAllergyDetails(crn: String): CasResult<DietAndAllergyResponse> {
    val prisonerNumber = caseService.getCase(crn)?.nomsNumber ?: return CasResult.NotFound("DietAndAllergy", crn)

    val dietAndAllergyData = when (val dietAndAllergyDetails = healthAndMedicationApiClient.getDietAndAllergyDetails(prisonerNumber)) {
      is ClientResult.Success -> dietAndAllergyDetails.body
      is ClientResult.Failure.StatusCode -> when (dietAndAllergyDetails.status) {
        HttpStatus.NOT_FOUND -> return CasResult.NotFound("DietAndAllergy", crn)
        HttpStatus.FORBIDDEN -> return CasResult.Unauthorised()
        else -> dietAndAllergyDetails.throwException()
      }

      is ClientResult.Failure -> dietAndAllergyDetails.throwException()
    }
    return CasResult.Success(dietAndAllergyData)
  }
}
