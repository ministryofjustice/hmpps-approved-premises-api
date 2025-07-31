package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationAssessmentEntity
import java.util.UUID

@Repository
interface TemporaryAccommodationAssessmentTestRepository : JpaRepository<TemporaryAccommodationAssessmentEntity, UUID>
