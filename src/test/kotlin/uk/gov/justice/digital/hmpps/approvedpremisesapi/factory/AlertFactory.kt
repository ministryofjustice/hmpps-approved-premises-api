package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.Alert
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomInt
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.time.LocalDate

class AlertFactory : Factory<Alert> {
  private var alertId: Yielded<Long> = { randomInt(0, 1000).toLong() }
  private var bookingId: Yielded<Long> = { randomInt(0, 1000).toLong() }
  private var offenderNo: Yielded<String> = { randomStringUpperCase(6) }
  private var comment: Yielded<String?> = { randomStringMultiCaseWithNumbers(10) }
  private var dateCreated: Yielded<LocalDate> = { LocalDate.now().randomDateBefore(10) }
  private var dateExpires: Yielded<LocalDate?> = { null }
  private var active: Yielded<Boolean> = { true }

  fun withAlertId(alertId: Long) = apply {
    this.alertId = { alertId }
  }

  fun withBookingId(bookingId: Long) = apply {
    this.bookingId = { bookingId }
  }

  fun withOffenderNo(offenderNo: String) = apply {
    this.offenderNo = { offenderNo }
  }

  fun withComment(comment: String?) = apply {
    this.comment = { comment }
  }

  fun withDateCreated(dateCreated: LocalDate) = apply {
    this.dateCreated = { dateCreated }
  }

  fun withDateExpires(dateExpires: LocalDate) = apply {
    this.dateExpires = { dateExpires }
  }

  fun withActive(active: Boolean) = apply {
    this.active = { active }
  }

  override fun produce(): Alert = Alert(
    alertId = this.alertId(),
    bookingId = this.bookingId(),
    offenderNo = this.offenderNo(),
    alertType = "H",
    alertTypeDescription = "Self Harm",
    alertCode = "HA",
    alertCodeDescription = "ACCT Open (HMPS)",
    comment = this.comment(),
    dateCreated = this.dateCreated(),
    dateExpires = this.dateExpires(),
    expired = this.dateExpires() != null,
    active = this.active(),
  )
}
