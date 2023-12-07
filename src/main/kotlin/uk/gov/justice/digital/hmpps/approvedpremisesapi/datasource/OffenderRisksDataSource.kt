package uk.gov.justice.digital.hmpps.approvedpremisesapi.datasource

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApOASysContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.HMPPSTierApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.Mappa
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskTier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskWithStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RoshRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.Registration
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.Registrations

interface OffenderRisksDataSource {
  val name: OffenderRisksDataSourceName

  fun getPersonRisks(crn: String): PersonRisks
}

enum class OffenderRisksDataSourceName {
  COMMUNITY_API,
  AP_DELIUS_CONTEXT_API,
}

@Component
@Primary
class ConfiguredOffenderRisksDataSource(
  dataSources: List<OffenderRisksDataSource>,
  @Value("\${data-sources.offender-risks}")
  private val dataSourceName: OffenderRisksDataSourceName,
) : OffenderRisksDataSource {
  private val log = LoggerFactory.getLogger(this::class.java)

  private lateinit var dataSource: OffenderRisksDataSource

  init {
    log.info("Getting '$dataSourceName' offender risks data source...")
    dataSource = dataSources.first { it.name == dataSourceName }
    log.info("Retrieved instance of type ${dataSource::class}")
  }

  override val name: OffenderRisksDataSourceName
    get() = dataSource.name

  override fun getPersonRisks(crn: String): PersonRisks =
    dataSource.getPersonRisks(crn)
}

@Component
class CommunityApiOffenderRisksDataSource(
  private val communityApiClient: CommunityApiClient,
  private val apOASysContextApiClient: ApOASysContextApiClient,
  private val hmppsTierApiClient: HMPPSTierApiClient,
) : OffenderRisksDataSource {

  private val ignoredRegisterTypesForFlags = listOf("RVHR", "RHRH", "RMRH", "RLRH", "MAPP")

  override val name: OffenderRisksDataSourceName
    get() = OffenderRisksDataSourceName.COMMUNITY_API

  override fun getPersonRisks(crn: String): PersonRisks {
    val registrationsResponse = communityApiClient.getRegistrationsForOffenderCrn(crn)

    return PersonRisks(
      roshRisks = getRoshRisksEnvelope(crn),
      mappa = getMappaEnvelope(registrationsResponse),
      tier = getRiskTierEnvelope(crn),
      flags = getFlagsEnvelope(registrationsResponse),
    )
  }

  private fun getRoshRisksEnvelope(crn: String): RiskWithStatus<RoshRisks> {
    when (val roshRisksResponse = apOASysContextApiClient.getRoshRatings(crn)) {
      is ClientResult.Success -> {
        val summary = roshRisksResponse.body.rosh

        if (summary.anyRisksAreNull()) {
          return RiskWithStatus(
            status = RiskStatus.NotFound,
            value = null,
          )
        }

        return RiskWithStatus(
          status = RiskStatus.Retrieved,
          value = RoshRisks(
            overallRisk = summary.determineOverallRiskLevel().text,
            riskToChildren = summary.riskChildrenCommunity!!.text,
            riskToPublic = summary.riskPublicCommunity!!.text,
            riskToKnownAdult = summary.riskKnownAdultCommunity!!.text,
            riskToStaff = summary.riskStaffCommunity!!.text,
            lastUpdated = roshRisksResponse.body.dateCompleted?.toLocalDate()
              ?: roshRisksResponse.body.initiationDate.toLocalDate(),
          ),
        )
      }
      is ClientResult.Failure.StatusCode -> return if (roshRisksResponse.status == HttpStatus.NOT_FOUND) {
        RiskWithStatus(
          status = RiskStatus.NotFound,
          value = null,
        )
      } else {
        RiskWithStatus(
          status = RiskStatus.Error,
          value = null,
        )
      }
      is ClientResult.Failure -> return RiskWithStatus(
        status = RiskStatus.Error,
        value = null,
      )
    }
  }

  private fun getMappaEnvelope(registrationsResponse: ClientResult<Registrations>): RiskWithStatus<Mappa> {
    when (registrationsResponse) {
      is ClientResult.Success -> {
        val firstMappaRegistration = registrationsResponse.body.registrations.firstOrNull { it.type.code == "MAPP" }

        return if (firstMappaRegistration.isMandatoryPropertyMissing()) {
          RiskWithStatus(status = RiskStatus.Error)
        } else {
          RiskWithStatus(
            value = firstMappaRegistration?.let { registration ->
              Mappa(
                level = "CAT ${registration.registerCategory!!.code}/LEVEL ${registration.registerLevel!!.code}",
                lastUpdated = registration.registrationReviews?.filter { it.completed }?.maxOfOrNull { it.reviewDate } ?: registration.startDate,
              )
            },
          )
        }
      }
      is ClientResult.Failure.StatusCode -> return if (registrationsResponse.status == HttpStatus.NOT_FOUND) {
        RiskWithStatus(status = RiskStatus.NotFound)
      } else {
        RiskWithStatus(status = RiskStatus.Error)
      }
      is ClientResult.Failure -> {
        return RiskWithStatus(status = RiskStatus.Error)
      }
    }
  }

  private fun getRiskTierEnvelope(crn: String): RiskWithStatus<RiskTier> {
    return when (val tierResponse = hmppsTierApiClient.getTier(crn)) {
      is ClientResult.Success -> {
        RiskWithStatus(
          status = RiskStatus.Retrieved,
          value = RiskTier(
            level = tierResponse.body.tierScore,
            lastUpdated = tierResponse.body.calculationDate.toLocalDate(),
          ),
        )
      }
      is ClientResult.Failure.StatusCode -> if (tierResponse.status == HttpStatus.NOT_FOUND) {
        RiskWithStatus(status = RiskStatus.NotFound)
      } else {
        RiskWithStatus(status = RiskStatus.Error)
      }
      is ClientResult.Failure -> {
        RiskWithStatus(status = RiskStatus.Error)
      }
    }
  }

  private fun getFlagsEnvelope(registrationsResponse: ClientResult<Registrations>): RiskWithStatus<List<String>> {
    return when (registrationsResponse) {
      is ClientResult.Success -> {
        RiskWithStatus(
          value = registrationsResponse.body.registrations.filter { !ignoredRegisterTypesForFlags.contains(it.type.code) }.map { it.type.description },
        )
      }

      is ClientResult.Failure.StatusCode -> if (registrationsResponse.status == HttpStatus.NOT_FOUND) {
        RiskWithStatus(status = RiskStatus.NotFound)
      } else {
        RiskWithStatus(status = RiskStatus.Error)
      }

      is ClientResult.Failure -> {
        RiskWithStatus(status = RiskStatus.Error)
      }
    }
  }

  private fun Registration?.isMandatoryPropertyMissing() =
    this != null && (this.registerCategory == null || this.registerLevel == null)
}

@Component
class ApDeliusContextApiOffenderRisksDataSource : OffenderRisksDataSource {
  override val name: OffenderRisksDataSourceName
    get() = OffenderRisksDataSourceName.AP_DELIUS_CONTEXT_API

  override fun getPersonRisks(crn: String): PersonRisks {
    throw NotImplementedError("Getting risks for individual offenders from the AP Delius Context API is not currently supported")
  }
}
