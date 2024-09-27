package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApAreaRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.StaffDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.InternalServerErrorProblem

@Component
class Cas1ApAreaMappingService(
  private val apAreaRepository: ApAreaRepository,
) {
  private val deliusTeamMappings = listOf(
    DeliusTeamMapping("N07CEU", "LON"),
    DeliusTeamMapping("N43CP1", "NE"),
    DeliusTeamMapping("N43CP2", "NE"),
    DeliusTeamMapping("N43CPP", "NE"),
    DeliusTeamMapping("N43LKS", "LON"),
    DeliusTeamMapping("N43MID", "Mids"),
    DeliusTeamMapping("N43NTH", "NW"),
    DeliusTeamMapping("N43SCE", "SEE"),
    DeliusTeamMapping("N43WSW", "SWSC"),
    DeliusTeamMapping("N43OMD", "Mids"),
    DeliusTeamMapping("N43OLK", "LON"),
    DeliusTeamMapping("N43ONM", "NW"),
    DeliusTeamMapping("N43OSE", "SEE"),
    DeliusTeamMapping("N43OWS", "SWSC"),
    DeliusTeamMapping("N41CRU", "NE"),
    DeliusTeamMapping("N41EFT", "NE"),
    DeliusTeamMapping("N41EP2", "NE"),
    DeliusTeamMapping("N41EXM", "NE"),
    DeliusTeamMapping("N41EP4", "NE"),
    DeliusTeamMapping("XXXNAT", "NE"),
    DeliusTeamMapping("XXXYOS", "NE"),
  )

  private val noTeamsApArea = "NE"

  data class DeliusTeamMapping(
    val deliusTeamCode: String,
    val apAreaCode: String,
  )

  fun determineApArea(
    usersProbationRegion: ProbationRegionEntity,
    deliusUser: StaffDetail,
  ): ApAreaEntity = determineApArea(usersProbationRegion, deliusUser.teamCodes(), deliusUser.username)

  fun determineApArea(
    usersProbationRegion: ProbationRegionEntity,
    deliusUserTeamCodes: List<String>,
    username: String,
  ): ApAreaEntity {
    if (usersProbationRegion.apArea != null) {
      return usersProbationRegion.apArea!!
    } else {
      if (deliusUserTeamCodes.isEmpty()) {
        return apAreaForCode(noTeamsApArea)
      }

      val deliusTeamMapping = deliusTeamMappings.firstOrNull { deliusTeamMapping ->
        deliusUserTeamCodes.contains(deliusTeamMapping.deliusTeamCode)
      }

      if (deliusTeamMapping == null) {
        throw InternalServerErrorProblem(
          "Could not find a delius team mapping for delius user $username with " +
            "probation region ${usersProbationRegion.deliusCode} and teams $deliusUserTeamCodes",
        )
      }

      return apAreaForCode(deliusTeamMapping.apAreaCode)
    }
  }

  private fun apAreaForCode(apAreaCode: String) = apAreaRepository.findByIdentifier(apAreaCode)
    ?: throw InternalServerErrorProblem("Could not find AP Area for code $apAreaCode")
}
