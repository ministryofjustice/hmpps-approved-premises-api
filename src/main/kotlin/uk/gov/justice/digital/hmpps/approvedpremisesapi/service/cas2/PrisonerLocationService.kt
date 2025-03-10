package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ManagePomCasesClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2PrisonerLocationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2PrisonerLocationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NomisUserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.IgnorableMessageException
import java.net.URI
import java.time.ZonedDateTime
import java.util.UUID

@Service
class PrisonerLocationService(
  private val prisonerSearchClient: PrisonerSearchClient,
  private val applicationRepository: Cas2ApplicationRepository,
  private val prisonerLocationRepository: Cas2PrisonerLocationRepository,
  private val nomisUserRepository: NomisUserRepository,
  private val managePomCasesClient: ManagePomCasesClient,
) {

  @SuppressWarnings("ThrowsCount")
  fun handleAllocationChangedEvent(event: HmppsDomainEvent) {
    val nomsNumber = event.personReference.findNomsNumber() ?: throw IgnorableMessageException("No nomsNumber found")
    val detailUrl = event.detailUrl ?: throw IgnorableMessageException("No detail URL found")
    val staffCode = event.staffCode ?: throw IgnorableMessageException("No staff code found")

    val applications = applicationRepository.findAllSubmittedApplicationByNomsNumber(nomsNumber)
    if (applications.isEmpty()) {
      return
    }

    val user = nomisUserRepository.findByNomisStaffId(staffCode.toLong())
      ?: throw IgnorableMessageException("No user found for staffCode $staffCode")

    val pomAllocation = managePomCasesClient.getPomAllocation(URI.create(detailUrl))
      ?: throw IgnorableMessageException("No POM allocation found for detailUrl $detailUrl")

    applications.forEach {
      updatePrisonerLocation(
        event.occurredAt,
        Cas2PrisonerLocationEntity(
          id = UUID.randomUUID(),
          application = it,
          prisonCode = pomAllocation.prison.code,
          staffId = user.id,
          occurredAt = event.occurredAt.toOffsetDateTime(),
          endDate = null,
        ),
      )
    }
  }

  @SuppressWarnings("ThrowsCount")
  fun handleLocationChangedEvent(event: HmppsDomainEvent) {
    val nomsNumber = event.personReference.findNomsNumber() ?: throw IgnorableMessageException("No nomsNumber found")
    val detailUrl = event.detailUrl ?: throw IgnorableMessageException("No detail URL found")

    val applications = applicationRepository.findAllSubmittedApplicationByNomsNumber(nomsNumber)
    if (applications.isEmpty()) {
      return
    }

    val prisoner = prisonerSearchClient.getPrisoner(URI.create(detailUrl))
      ?: throw IgnorableMessageException("No prisoner found for detailUrl $detailUrl")

    applications.forEach {
      updatePrisonerLocation(
        event.occurredAt,
        Cas2PrisonerLocationEntity(
          id = UUID.randomUUID(),
          application = it,
          prisonCode = prisoner.prisonId,
          staffId = null,
          occurredAt = event.occurredAt.toOffsetDateTime(),
          endDate = null,
        ),
      )
    }
  }

  fun updatePrisonerLocation(occurredAt: ZonedDateTime, location: Cas2PrisonerLocationEntity) {
    val oldPrisonerLocation = prisonerLocationRepository.findPrisonerLocation(location.application.id)
      ?: throw IgnorableMessageException("No null prisoner location found for applicationId ${location.application.id}")
    prisonerLocationRepository.save(oldPrisonerLocation.copy(endDate = occurredAt.toOffsetDateTime()))
    prisonerLocationRepository.save(location)
  }
}
