package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.categoriesChanged
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.InvalidDomainEventException

@Service
class Cas2LocationChangedService(
  private val prisonerSearchClient: PrisonerSearchClient,
  private val applicationService: Cas2ApplicationService,
  private val applicationRepository: Cas2ApplicationRepository,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  private fun isNewPrison(currentPrisonCode: String, prisonCodeToCheck: String) = currentPrisonCode != prisonCodeToCheck

  @Transactional
  fun process(event: HmppsDomainEvent) {
    if (event.additionalInformation.categoriesChanged.contains("LOCATION")) {
      val nomsNumber = event.personReference.findNomsNumber()
      val detailUrl = event.detailUrl
      if (nomsNumber == null || detailUrl == null) {
        throw InvalidDomainEventException(event)
      }

      applicationService.findMostRecentApplication(nomsNumber)?.let { application ->
        log.info("Received location change event of interest: \n{}", event)

        val prisoner = when (val result = prisonerSearchClient.getPrisoner(detailUrl)) {
          is ClientResult.Success -> result.body
          is ClientResult.Failure -> throw result.toException()
        }

        if (isNewPrison(application.currentPrisonCode, prisoner.prisonId)) {
          application.createApplicationAssignment(
            prisonCode = prisoner.prisonId,
            allocatedPomUserId = null,
          )

          applicationRepository.save(application)
          log.info("Added application assignment for prisoner: {}", nomsNumber)
        } else {
          log.info("Prisoner {} prison location not changed, no action required", nomsNumber)
        }
      }
    }
  }
}
