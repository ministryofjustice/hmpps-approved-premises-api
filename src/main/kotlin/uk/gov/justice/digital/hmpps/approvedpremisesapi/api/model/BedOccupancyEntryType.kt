package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
*
* Values: booking,lostBed,`open`
*/
enum class BedOccupancyEntryType(val value: kotlin.String) {

  @JsonProperty("booking")
  booking("booking"),

  @JsonProperty("lost_bed")
  lostBed("lost_bed"),

  @JsonProperty("open")
  `open`("open"),
}
