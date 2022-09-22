package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationOfficerEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationOfficerRepository
import java.util.UUID

@Service
class ProbationOfficerService(
  private val httpAuthService: HttpAuthService,
  private val probationOfficerRepository: ProbationOfficerRepository
) {
  fun getProbationOfficerForRequestUser(): ProbationOfficerEntity {
    val deliusPrincipal = httpAuthService.getDeliusPrincipalOrThrow()
    val username = deliusPrincipal.name

    return probationOfficerRepository.findByDistinguishedName(username)
      ?: probationOfficerRepository.save(
        ProbationOfficerEntity(
          id = UUID.randomUUID(),
          name = username,
          distinguishedName = username,
          isActive = true,
          applications = mutableListOf()
        )
      )
  }
}
