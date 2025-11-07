package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

class OfflineApplication(

  override val type: kotlin.String,

  override val id: java.util.UUID,

  override val person: Person,

  override val createdAt: java.time.Instant,
) : Application
