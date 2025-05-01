package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.Prisoner
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.categoriesChanged
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.InvalidDomainEventException

@Service
class Cas2LocationChangedService(
  private val prisonerSearchClient: PrisonerSearchClient,
  private val applicationService: Cas2ApplicationService,
  private val applicationRepository: Cas2ApplicationRepository,
  private val emailService: Cas2EmailService,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  fun getMostRecentLocationAssignment(application: Cas2ApplicationEntity) = application.applicationAssignments.firstOrNull { it.allocatedPomUser == null }
  fun isFirstValidLocationChangedAssigment(application: Cas2ApplicationEntity, prisoner: Prisoner) = getMostRecentLocationAssignment(application) == null && prisoner.prisonId != application.referringPrisonCode
  fun isValidLocationChangedAssignment(application: Cas2ApplicationEntity, prisoner: Prisoner): Boolean {
    val mostRecentLocationAssignment = getMostRecentLocationAssignment(application)
    return mostRecentLocationAssignment != null && prisoner.prisonId != mostRecentLocationAssignment.prisonCode
  }

  fun isNewPrison(application: Cas2ApplicationEntity, prisoner: Prisoner) = isFirstValidLocationChangedAssigment(application, prisoner) || isValidLocationChangedAssignment(application, prisoner)

  @Transactional
  fun process(event: HmppsDomainEvent) {
    if (event.additionalInformation.categoriesChanged.contains("LOCATION")) {
      val nomsNumber = event.personReference.findNomsNumber()
      val detailUrl = event.detailUrl
      if (nomsNumber == null || detailUrl == null) {
        throw InvalidDomainEventException(event)
      }

      applicationService.findApplicationToAssign(nomsNumber)?.let { application ->
        log.info("Received location change event of interest: \n{}", event)

        val prisoner = when (val result = prisonerSearchClient.getPrisoner(detailUrl)) {
          is ClientResult.Success -> result.body
          is ClientResult.Failure -> throw result.toException()
        }

        if (isNewPrison(application, prisoner)) {
          application.createApplicationAssignment(
            prisonCode = prisoner.prisonId,
            allocatedPomUser = null,
          )

          applicationRepository.save(application)
          log.info("Added application assignment for prisoner: {}", nomsNumber)

          emailService.sendLocationChangedEmails(application = application, prisoner = prisoner)
        } else {
          log.info("Prisoner {} prison location not changed, no action required", nomsNumber)
        }
      }
    }
  }
}
