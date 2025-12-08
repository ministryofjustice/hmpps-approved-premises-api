package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.HMPPSTierApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.CaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1OffenderEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1OffenderRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskTier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1LaoStrategy
import java.time.OffsetDateTime
import java.util.UUID

@Service
class Cas1OffenderService(
  private val cas1OffenderRepository: Cas1OffenderRepository,
  private val offenderService: OffenderService,
  private val hmppsTierApiClient: HMPPSTierApiClient,
) {

  fun getOrCreateOffender(caseSummary: CaseSummary, riskRatings: PersonRisks?): Cas1OffenderEntity {
    val offender = cas1OffenderRepository.findByCrn(caseSummary.crn)?.apply {
      name = "${caseSummary.name.forename.uppercase()} ${caseSummary.name.surname.uppercase()}".trim()
      tier = riskRatings?.tier?.value?.level
      nomsNumber = caseSummary.nomsId
    } ?: Cas1OffenderEntity(
      id = UUID.randomUUID(),
      crn = caseSummary.crn,
      name = "${caseSummary.name.forename.uppercase()} ${caseSummary.name.surname.uppercase()}".trim(),
      tier = riskRatings?.tier?.value?.level,
      nomsNumber = caseSummary.nomsId,
      createdAt = OffsetDateTime.now(),
      lastUpdatedAt = OffsetDateTime.now(),
    )
    return cas1OffenderRepository.saveAndFlush(offender)
  }

  fun getRiskTier(crn: String): CasResult<RiskTier> = when (val tierResponse = hmppsTierApiClient.getTier(crn)) {
    is ClientResult.Success -> CasResult.Success(
      RiskTier(
        level = tierResponse.body.tierScore,
        lastUpdated = tierResponse.body.calculationDate.toLocalDate(),
      ),
    )

    is ClientResult.Failure.StatusCode -> when (tierResponse.status) {
      HttpStatus.NOT_FOUND -> CasResult.NotFound("Risk Tier", crn)
      HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN -> CasResult.Unauthorised("Not authorised to access Risk Tier for CRN: $crn")
      else -> CasResult.GeneralValidationError("Failed to retrieve Risk Tier for CRN: $crn: ${tierResponse.status}")
    }

    is ClientResult.Failure -> CasResult.GeneralValidationError("Error retrieving Risk Tier for CRN: $crn: ${tierResponse.toException().message}")
  }

  fun getCas1PersonSummaryInfoResult(crn: String, cas1LaoStrategy: LaoStrategy): CasResult<PersonSummaryInfoResult> = when (
    val personSummaryInfoResult = offenderService.getPersonSummaryInfoResults(setOf(crn), cas1LaoStrategy).first()
  ) {
    is PersonSummaryInfoResult.NotFound, is PersonSummaryInfoResult.Unknown -> CasResult.NotFound(
      "Person",
      crn,
    )
    is PersonSummaryInfoResult.Success.Full, is PersonSummaryInfoResult.Success.Restricted -> CasResult.Success(personSummaryInfoResult)
  }
}
