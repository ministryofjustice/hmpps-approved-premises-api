package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ArrivalEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ArrivalRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ExtensionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ExtensionRepository
import java.util.UUID
import javax.transaction.Transactional

@Service
class BookingService(
  private val bookingRepository: BookingRepository,
  private val arrivalRepository: ArrivalRepository,
  private val cancellationRepository: CancellationRepository,
  private val extensionRepository: ExtensionRepository
) {
  fun createBooking(bookingEntity: BookingEntity): BookingEntity = bookingRepository.save(bookingEntity)
  fun updateBooking(bookingEntity: BookingEntity): BookingEntity = bookingRepository.save(bookingEntity)
  fun getBooking(id: UUID) = bookingRepository.findByIdOrNull(id)
  fun createArrival(arrivalEntity: ArrivalEntity): ArrivalEntity = arrivalRepository.save(arrivalEntity)
  fun createCancellation(cancellationEntity: CancellationEntity): CancellationEntity = cancellationRepository.save(cancellationEntity)

  @Transactional
  fun createExtension(bookingEntity: BookingEntity, extensionEntity: ExtensionEntity): ExtensionEntity {
    val extension = extensionRepository.save(extensionEntity)
    bookingEntity.departureDate = extensionEntity.newDepartureDate
    bookingEntity.extensions.add(extension)
    updateBooking(bookingEntity)

    return extensionEntity
  }
}
