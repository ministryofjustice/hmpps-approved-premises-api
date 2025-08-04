package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

interface Application {
  val type: String
  val id: java.util.UUID
  val person: Person
  val createdAt: java.time.Instant
}
