package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.controller

import jakarta.transaction.Transactional
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.Cas3AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.ensureEntityFromCasResultIsSuccess
import java.util.UUID

@Cas3Controller
class Cas3AssessmentController(
  private val cas3AssessmentService: Cas3AssessmentService,
  private val userService: UserService,
) {
  @DeleteMapping("/assessments/{assessmentId}/allocations")
  @Transactional
  fun deallocateAssessment(@PathVariable assessmentId: UUID): ResponseEntity<Unit> {
    val user = userService.getUserForRequest()

    ensureEntityFromCasResultIsSuccess(cas3AssessmentService.deallocateAssessment(user, assessmentId))

    return ResponseEntity(Unit, HttpStatus.NO_CONTENT)
  }

  @PostMapping("/assessments/{assessmentId}/reallocateToMe")
  @Transactional
  fun reallocateAssessmentToMe(@PathVariable assessmentId: UUID): ResponseEntity<Unit> {
    val user = userService.getUserForRequest()

    ensureEntityFromCasResultIsSuccess(
      cas3AssessmentService.reallocateAssessmentToMe(user, assessmentId),
    )

    return ResponseEntity(HttpStatus.CREATED)
  }
}
