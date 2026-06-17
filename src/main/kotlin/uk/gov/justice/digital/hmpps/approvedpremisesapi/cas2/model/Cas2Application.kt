package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model

import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class Cas2Application(

  @get:JsonProperty("createdBy", required = true) val createdBy: Cas2User,

  @get:JsonProperty("status", required = true) val status: ApplicationStatus,

  @get:JsonProperty("applicationOrigin", required = true) val applicationOrigin: ApplicationOrigin = ApplicationOrigin.homeDetentionCurfew,

  @get:JsonProperty("type", required = true) override val type: String,

  @get:JsonProperty("id", required = true) override val id: UUID,

  @get:JsonProperty("person", required = true) override val person: Person,

  @get:JsonProperty("createdAt", required = true) override val createdAt: Instant,

  @get:JsonProperty("data") val `data`: Any? = null,

  @get:JsonProperty("document") val document: Any? = null,

  @get:JsonProperty("submittedAt") val submittedAt: Instant? = null,

  @get:JsonProperty("telephoneNumber") val telephoneNumber: String? = null,

  @get:JsonProperty("assessment") val assessment: Cas2Assessment? = null,

  @get:JsonProperty("timelineEvents") val timelineEvents: List<Cas2TimelineEvent>? = null,

  @get:JsonProperty("bailHearingDate") val bailHearingDate: LocalDate? = null,

  @get:JsonProperty("cohort") val cohort: Cas2CohortDto? = null,

  ) : Application
