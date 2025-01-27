package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationAreaProbationRegionMappingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationAreaProbationRegionMappingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.util.UUID

@Component
class ProbationAreaProbationRegionMappingFactory(private val probationAreaProbationRegionMappingRepository: ProbationAreaProbationRegionMappingRepository) {
  fun produceAndPersist(
    probationRegion: ProbationRegionEntity,
    probationAreaDeliusCode: String = randomStringUpperCase(6),
  ): ProbationAreaProbationRegionMappingEntity =
    probationAreaProbationRegionMappingRepository.saveAndFlush(
      ProbationAreaProbationRegionMappingModel(
        probationRegion = probationRegion,
        probationAreaDeliusCode = probationAreaDeliusCode,
      ).toEntity(),
    )
}

data class ProbationAreaProbationRegionMappingModel(
  val id: UUID = UUID.randomUUID(),
  val probationAreaDeliusCode: String = randomStringUpperCase(6),
  val probationRegion: ProbationRegionEntity,
) {
  fun toEntity() = ProbationAreaProbationRegionMappingEntity(
    id = this.id,
    probationAreaDeliusCode = this.probationAreaDeliusCode,
    probationRegion = this.probationRegion,
  )
}
