package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed

import org.slf4j.LoggerFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesRepository

class ApprovedPremisesRoomsSeedJob(
  fileName: String,
  private val premisesRepository: PremisesRepository,
  private val characteristicRepository: CharacteristicRepository
) : SeedJob<ApprovedPremisesRoomsSeedCsvRow>(
  fileName = fileName,
  requiredColumns = 26
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun verifyPresenceOfRequiredHeaders(headers: Set<String>) {
    val missingHeaders = requiredHeaders() - headers

    if (missingHeaders.any()) {
      throw RuntimeException("required headers: $missingHeaders")
    }
  }

  override fun deserializeRow(columns: Map<String, String>) = ApprovedPremisesRoomsSeedCsvRow(
    apCode = columns["apCode"]!!,
    bedCode = columns["bedCode"]!!,
    roomNumber = columns["roomNumber"]!!,
    bedCount = columns["bedCount"]!!,
    isSingle = parseBooleanStringOrThrow(columns["isSingle"]!!, "isSingle"),
    isGroundFloor = parseBooleanStringOrThrow(columns["isGroundFloor"]!!, "isGroundFloor"),
    isFullyFm = parseBooleanStringOrThrow(columns["isFullyFm"]!!, "isFullyFm"),
    hasCrib7Bedding = parseBooleanStringOrThrow(columns["hasCrib7Bedding"]!!, "hasCrib7Bedding"),
    hasSmokeDetector = parseBooleanStringOrThrow(columns["hasSmokeDetector"]!!, "hasSmokeDetector"),
    isTopFloorVulnerable = parseBooleanStringOrThrow(columns["isTopFloorVulnerable"]!!, "isTopFloorVulnerable"),
    isGroundFloorNrOffice = parseBooleanStringOrThrow(columns["isGroundFloorNrOffice"]!!, "isGroundFloorNrOffice"),
    hasNearbySprinkler = parseBooleanStringOrThrow(columns["hasNearbySprinkler"]!!, "hasNearbySprinkler"),
    isArsonSuitable = parseBooleanStringOrThrow(columns["isArsonSuitable"]!!, "isArsonSuitable"),
    isArsonDesignated = parseBooleanStringOrThrow(columns["isArsonDesignated"]!!, "isArsonDesignated"),
    hasArsonInsuranceConditions = parseBooleanStringOrThrow(columns["hasArsonInsuranceConditions"]!!, "hasArsonInsuranceConditions"),
    isSuitedForSexOffenders = parseBooleanStringOrThrow(columns["isSuitedForSexOffenders"]!!, "isSuitedForSexOffenders"),
    hasEnSuite = parseBooleanStringOrThrow(columns["hasEnSuite"]!!, "hasEnSuite"),
    isWheelchairAccessible = parseBooleanStringOrThrow(columns["isWheelchairAccessible"]!!, "isWheelchairAccessible"),
    hasWideDoor = parseBooleanStringOrThrow(columns["hasWideDoor"]!!, "hasWideDoor"),
    hasStepFreeAccess = parseBooleanStringOrThrow(columns["hasStepFreeAccess"]!!, "hasStepFreeAccess"),
    hasFixedMobilityAids = parseBooleanStringOrThrow(columns["hasFixedMobilityAids"]!!, "hasFixedMobilityAids"),
    hasTurningSpace = parseBooleanStringOrThrow(columns["hasTurningSpace"]!!, "hasTurningSpace"),
    hasCallForAssistance = parseBooleanStringOrThrow(columns["hasCallForAssistance"]!!, "hasCallForAssistance"),
    isWheelchairDesignated = parseBooleanStringOrThrow(columns["isWheelchairDesignated"]!!, "isWheelchairDesignated"),
    isStepFreeDesignated = parseBooleanStringOrThrow(columns["isStepFreeDesignated"]!!, "isStepFreeDesignated"),
    notes = columns["notes"]!!,
  )

  override fun processRow(row: ApprovedPremisesRoomsSeedCsvRow) {
  }

  private fun parseBooleanStringOrThrow(value: String, fieldName: String): String {
    val booleanString = listOf("YES", "NO").find { it == value.trim().uppercase() }
      ?: throw RuntimeException("'$value' is not a recognised boolean for '$fieldName' (use yes | no)")

    return if (booleanString == "YES") "YES" else "NO"
  }
}

private fun requiredHeaders(): Set<String> {
  return setOf(
    "apCode",
    "bedCode",
    "roomNumber",
    "bedCount",
    "isSingle",
    "isGroundFloor",
    "isFullyFm",
    "hasCrib7Bedding",
    "hasSmokeDetector",
    "isTopFloorVulnerable",
    "isGroundFloorNrOffice",
    "hasNearbySprinkler",
    "isArsonSuitable",
    "isArsonDesignated",
    "hasArsonInsuranceConditions",
    "isSuitedForSexOffenders",
    "hasEnSuite",
    "isWheelchairAccessible",
    "hasWideDoor",
    "hasStepFreeAccess",
    "hasFixedMobilityAids",
    "hasTurningSpace",
    "hasCallForAssistance",
    "isWheelchairDesignated",
    "isStepFreeDesignated",
    "notes",
  )
}

data class ApprovedPremisesRoomsSeedCsvRow(
  val apCode: String,
  val bedCode: String,
  val roomNumber: String,
  val bedCount: String,
  val isSingle: String,
  val isGroundFloor: String,
  val isFullyFm: String,
  val hasCrib7Bedding: String,
  val hasSmokeDetector: String,
  val isTopFloorVulnerable: String,
  val isGroundFloorNrOffice: String,
  val hasNearbySprinkler: String,
  val isArsonSuitable: String,
  val isArsonDesignated: String,
  val hasArsonInsuranceConditions: String,
  val isSuitedForSexOffenders: String,
  val hasEnSuite: String,
  val isWheelchairAccessible: String,
  val hasWideDoor: String,
  val hasStepFreeAccess: String,
  val hasFixedMobilityAids: String,
  val hasTurningSpace: String,
  val hasCallForAssistance: String,
  val isWheelchairDesignated: String,
  val isStepFreeDesignated: String,
  val notes: String?
)
