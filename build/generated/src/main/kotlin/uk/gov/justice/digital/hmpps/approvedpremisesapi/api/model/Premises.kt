package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonValue
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Characteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.LocalAuthorityArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PropertyStatus
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 
 * @param service 
 * @param id 
 * @param name 
 * @param addressLine1 
 * @param postcode 
 * @param bedCount 
 * @param availableBedsForToday 
 * @param probationRegion 
 * @param apArea 
 * @param status 
 * @param addressLine2 
 * @param town 
 * @param notes 
 * @param localAuthorityArea 
 * @param characteristics 
 */

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "service", visible = true)
@JsonSubTypes(
      JsonSubTypes.Type(value = ApprovedPremises::class, name = "CAS1"),
      JsonSubTypes.Type(value = TemporaryAccommodationPremises::class, name = "CAS3")
)

interface Premises{
                @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
        val service: kotlin.String

                @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
        val id: java.util.UUID

                @get:Schema(example = "Hope House", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
        val name: kotlin.String

                @get:Schema(example = "one something street", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
        val addressLine1: kotlin.String

                @get:Schema(example = "LS1 3AD", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
        val postcode: kotlin.String

                @get:Schema(example = "22", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
        val bedCount: kotlin.Int

                @get:Schema(example = "20", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
        val availableBedsForToday: kotlin.Int

                @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
        val probationRegion: ProbationRegion

                @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
        val apArea: ApArea

                @get:Schema(example = "null", requiredMode = Schema.RequiredMode.REQUIRED, description = "")
        val status: PropertyStatus

                @get:Schema(example = "Blackmore End", description = "")
        val addressLine2: kotlin.String? 

                @get:Schema(example = "Braintree", description = "")
        val town: kotlin.String? 

                @get:Schema(example = "some notes about this property", description = "")
        val notes: kotlin.String? 

                @get:Schema(example = "null", description = "")
        val localAuthorityArea: LocalAuthorityArea? 

                @get:Schema(example = "null", description = "")
        val characteristics: kotlin.collections.List<Characteristic>? 


}

