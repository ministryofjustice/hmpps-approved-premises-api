package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationOfficerRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.AuthorisableActionResult
import java.util.UUID

@Service
class ApplicationService(
  private val probationOfficerRepository: ProbationOfficerRepository,
  private val applicationRepository: ApplicationRepository,
  private val jsonSchemaService: JsonSchemaService
) {
  fun getAllApplicationsForUsername(userDistinguishedName: String): List<ApplicationEntity> {
    val probationOfficerEntity = probationOfficerRepository.findByDistinguishedName(userDistinguishedName)
      ?: return emptyList()

    return applicationRepository.findAllByCreatedByProbationOfficer_Id(probationOfficerEntity.id)
      .map(jsonSchemaService::attemptSchemaUpgrade)
  }

  fun getApplicationForUsername(applicationId: UUID, userDistinguishedName: String): AuthorisableActionResult<ApplicationEntity> {
    val applicationEntity = applicationRepository.findByIdOrNull(applicationId)
      ?: return AuthorisableActionResult.NotFound()

    val probationOfficerEntity = probationOfficerRepository.findByDistinguishedName(userDistinguishedName)

    if (probationOfficerEntity != applicationEntity.createdByProbationOfficer) {
      return AuthorisableActionResult.Unauthorised()
    }

    return AuthorisableActionResult.Success(jsonSchemaService.attemptSchemaUpgrade(applicationEntity))
  }
}
