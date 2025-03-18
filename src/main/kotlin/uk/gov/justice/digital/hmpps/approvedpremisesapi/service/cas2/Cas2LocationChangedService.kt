package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationAssignmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.categoriesChanged
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.InvalidDomainEventException
import java.net.URI
import java.time.OffsetDateTime
import java.util.UUID

@Service
class Cas2LocationChangedService(
  private val prisonerSearchClient: PrisonerSearchClient,
  private val applicationService: Cas2ApplicationService,
  private val applicationRepository: Cas2ApplicationRepository,
  private val emailService: Cas2EmailService,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  @Transactional
  @Suppress("TooGenericExceptionThrown")
  fun process(event: HmppsDomainEvent) {
    if (event.additionalInformation.categoriesChanged.contains("LOCATION")) {
      val nomsNumber = event.personReference.findNomsNumber()
      val detailUrl = event.detailUrl
      if (nomsNumber == null || detailUrl == null) {
        throw InvalidDomainEventException(event)
      }

      applicationService.findMostRecentApplication(nomsNumber)?.let { application ->
        log.info("Received location change event of interest: \n{}", event)

        val prisoner = prisonerSearchClient.getPrisoner(URI.create(detailUrl))!!

        application.applicationAssignments.add(
          Cas2ApplicationAssignmentEntity(
            id = UUID.randomUUID(),
            application = application,
            prisonCode = prisoner.prisonId,
            createdAt = OffsetDateTime.now(),
            allocatedPomUserId = null,
          ),
        )

        applicationRepository.save(application)
        emailService.sendLocationChangedEmailToTransferringPom(application, nomsNumber, prisoner)

        log.info("Added application assignment for prisoner: {}", nomsNumber)
      }
    }
  }
}
