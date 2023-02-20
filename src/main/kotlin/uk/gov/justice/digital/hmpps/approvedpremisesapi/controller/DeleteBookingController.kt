package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.BookingService
import java.util.UUID

@Controller
class DeleteBookingController(private val bookingService: BookingService) {
  @RequestMapping(method = [RequestMethod.DELETE], value = ["/internal/booking/{bookingId}"])
  fun internalDeleteBooking(@PathVariable("bookingId") bookingId: UUID): ResponseEntity<Unit> {
    throwIfNotLoopbackRequest()

    val booking = bookingService.getBooking(bookingId)
      ?: throw NotFoundProblem(bookingId, "Booking")

    bookingService.deleteBooking(booking)

    return ResponseEntity(HttpStatus.OK)
  }
}
