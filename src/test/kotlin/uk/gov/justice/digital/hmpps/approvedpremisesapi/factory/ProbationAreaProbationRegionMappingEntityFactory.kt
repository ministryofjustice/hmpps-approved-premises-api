package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import java.util.UUID
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationAreaProbationRegionMappingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationAreaProbationRegionMappingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase

@Component
class ProbationAreaProbationRegionMappingEntityTestFactory(private val probationAreaProbationRegionMappingRepository: ProbationAreaProbationRegionMappingRepository) {
  fun produceAndPersist(
    probationRegion: ProbationRegionEntity,
    deliusCode: String = randomStringUpperCase(6),
  ): ProbationAreaProbationRegionMappingEntity =
    probationAreaProbationRegionMappingRepository.saveAndFlush(
      ProbationAreaProbationRegionMappingEntityTestTest(
        probationRegion = probationRegion,
        probationAreaDeliusCode = deliusCode,
      ).toEntity(),
    )
}

data class ProbationAreaProbationRegionMappingEntityTestTest(
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
