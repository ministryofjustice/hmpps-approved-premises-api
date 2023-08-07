package uk.gov.justice.digital.hmpps.approvedpremisesapi.repository

import java.util.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentReferralHistoryUserNoteEntity

@Repository
interface AssessmentReferralHistoryUserNoteTestRepository: JpaRepository<AssessmentReferralHistoryUserNoteEntity, UUID>