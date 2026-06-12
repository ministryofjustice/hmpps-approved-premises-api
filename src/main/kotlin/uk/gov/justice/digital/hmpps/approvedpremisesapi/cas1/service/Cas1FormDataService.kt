package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.entity.Cas1FormDataEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.entity.Cas1FormDataRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.results.CasResult

@Service
class Cas1FormDataService(
  val cas1FormDataRepository: Cas1FormDataRepository,
) {
  fun get(id: String) = when (val result = cas1FormDataRepository.findByIdOrNull(id)) {
    null -> CasResult.NotFound("Cas1FormData", id)
    else -> CasResult.Success(result.value)
  }

  fun createOrUpdate(id: String, body: String) = cas1FormDataRepository.save(
    Cas1FormDataEntity(
      id,
      body,
    ),
  )
}
