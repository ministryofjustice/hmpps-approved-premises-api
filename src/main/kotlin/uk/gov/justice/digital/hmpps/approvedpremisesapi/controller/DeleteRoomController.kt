package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.BadRequestProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ConflictProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ParamDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.RoomService
import java.util.UUID

@Controller
class DeleteRoomController(private val roomService: RoomService) {
  @RequestMapping(method = [RequestMethod.DELETE], value = ["/internal/room/{roomId}"])
  fun internalDeletePremises(@PathVariable("roomId") roomId: UUID): ResponseEntity<Unit> {
    throwIfNotLoopbackRequest()

    val room = roomService.getRoom(roomId)
      ?: throw NotFoundProblem(roomId, "Room")

    return when (val result = roomService.deleteRoom(room)) {
      is ValidatableActionResult.ConflictError -> throw ConflictProblem(
        roomId,
        "A room cannot be hard-deleted if it has any bookings associated with it",
      )
      is ValidatableActionResult.FieldValidationError -> throw BadRequestProblem(invalidParams = result.validationMessages.mapValues { ParamDetails(it.value) })
      is ValidatableActionResult.GeneralValidationError -> throw BadRequestProblem(errorDetail = result.message)
      is ValidatableActionResult.Success -> ResponseEntity(HttpStatus.OK)
    }
  }
}
