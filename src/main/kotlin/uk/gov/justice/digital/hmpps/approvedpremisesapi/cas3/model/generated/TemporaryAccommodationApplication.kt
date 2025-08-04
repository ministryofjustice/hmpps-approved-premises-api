package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonRisks

data class TemporaryAccommodationApplication(

    val createdByUserId: java.util.UUID,
    val status: ApplicationStatus,
    val offenceId: kotlin.String,
    override val type: String = "CAS3",
    override val id: java.util.UUID,
    override val person: Person,
    override val createdAt: java.time.Instant,
    val `data`: kotlin.Any? = null,
    val document: kotlin.Any? = null,
    val risks: PersonRisks? = null,
    val submittedAt: java.time.Instant? = null,
    val arrivalDate: java.time.Instant? = null,
    val assessmentId: java.util.UUID? = null,
) : Application
