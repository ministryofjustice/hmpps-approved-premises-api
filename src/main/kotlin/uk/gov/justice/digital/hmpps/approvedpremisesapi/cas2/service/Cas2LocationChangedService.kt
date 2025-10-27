package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.categoriesChanged
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.InvalidDomainEventException

@Service
class Cas2LocationChangedService(
  private val prisonerSearchClient: PrisonerSearchClient,
  private val applicationService: Cas2ApplicationService,
  private val applicationRepository: Cas2ApplicationRepository,
  private val emailService: Cas2EmailService,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  private fun isRelease(prisonCode: String) = prisonCode == "OUT"
  private fun isTransfer(prisonCode: String) = prisonCode == "TRN"

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
        createLocationChangeAssignmentAndSendEmails(application, latestPrisonCode = prisoner.prisonId)
      }
    }
  }

  fun createLocationChangeAssignmentAndSendEmails(
    application: Cas2ApplicationEntity,
    latestPrisonCode: String,
  ): Cas2ApplicationEntity {
    if (!application.isLocationChange(latestPrisonCode) ||
      isRelease(latestPrisonCode) ||
      isTransfer(latestPrisonCode)
    ) {
      log.info("Location change not required for ${application.nomsNumber}. Current prison code: ${application.currentPrisonCode}. Latest prison code: $latestPrisonCode")
      return application
    }

    val transferringFromPomId = application.currentPomUserId

    application.createApplicationAssignment(
      prisonCode = latestPrisonCode,
      allocatedPomUser = null,
    )

    val application = applicationRepository.save(application)
    emailService.sendLocationChangedEmails(application, latestPrisonCode, transferringFromPomId)
    return application
  }
}
