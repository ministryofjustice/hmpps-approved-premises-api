package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3NewPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.Cas3UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.v2.Cas3v2PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3PremisesTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult


@Cas3Controller
@RequestMapping("/cas3/v2", headers = ["X-Service-Name=temporary-accommodation"])
class Cas3v2PremisesController(
    private val cas3UserAccessService: Cas3UserAccessService,
    private val cas3v2PremisesService: Cas3v2PremisesService,
    private val cas3v2PremisesTransformer: Cas3PremisesTransformer
) {
    @PostMapping("/premises")
    fun createPremises(@RequestBody body: Cas3NewPremises) : ResponseEntity<Cas3Premises> {
        if (!cas3UserAccessService.currentUserCanAccessRegion(body.probationRegionId)) {
            throw ForbiddenProblem()
        }

        val premises = extractEntityFromCasResult(
            cas3v2PremisesService.createNewPremises(
                reference = body.reference,
                addressLine1 = body.addressLine1,
                addressLine2 = body.addressLine2,
                town = body.town,
                postcode = body.postcode,
                localAuthorityAreaId = body.localAuthorityAreaId,
                probationRegionId = body.probationRegionId,
                probationDeliveryUnitId = body.probationDeliveryUnitId,
                characteristicIds = body.characteristicIds,
                notes = body.notes,
                turnaroundWorkingDays = body.turnaroundWorkingDays,
            ),
        )
        return ResponseEntity.noContent().build()

//        return ResponseEntity(cas3v2PremisesTransformer.transformDomainToApi(premises), HttpStatus.CREATED)
    }

}
