package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository

@Service
class BookingService(private val bookingRepository: BookingRepository) {
  fun createBooking(bookingEntity: BookingEntity): BookingEntity = bookingRepository.save(bookingEntity)
}
