package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationOfficerRepository

@Service
class ApplicationService(
  private val probationOfficerRepository: ProbationOfficerRepository,
  private val applicationRepository: ApplicationRepository
) {
  fun getAllApplicationsForUsername(userDistinguishedName: String): List<ApplicationEntity> {
    val probationOfficerEntity = probationOfficerRepository.findByDistinguishedName(userDistinguishedName)
      ?: return emptyList()

    return applicationRepository.findAllByCreatedByProbationOfficer_Id(probationOfficerEntity.id)
      .map {
        // TODO: For any entries where schema version is lower than newest - validate against newest and promote if possible
        it.apply { schemaUpToDate = true }
      }
  }
}
