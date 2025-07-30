package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.events

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.CAS3ReferralSubmittedEventDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.PersonReference
import java.net.URI
import java.util.UUID

class CAS3ReferralSubmittedEventDetailsFactory : Factory<CAS3ReferralSubmittedEventDetails> {
  private var personReference: Yielded<PersonReference> = { PersonReferenceFactory().produce() }
  private var applicationId: Yielded<UUID> = { UUID.randomUUID() }

  fun withPersonReference(configuration: PersonReferenceFactory.() -> Unit) = apply {
    this.personReference = { PersonReferenceFactory().apply(configuration).produce() }
  }

  fun withApplicationId(applicationId: UUID) = apply {
    this.applicationId = { applicationId }
  }

  override fun produce(): CAS3ReferralSubmittedEventDetails {
    val applicationId = this.applicationId()

    return CAS3ReferralSubmittedEventDetails(
      personReference = this.personReference(),
      applicationId = applicationId,
      applicationUrl = URI("http://api/applications/$applicationId"),
    )
  }
}
