package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed

import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PropertyStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LocalAuthorityAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LocalAuthorityAreaRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionRepository
import java.util.UUID

class ApprovedPremisesSeedJob(
  fileName: String,
  private val premisesRepository: PremisesRepository,
  private val probationRegionRepository: ProbationRegionRepository,
  private val localAuthorityAreaRepository: LocalAuthorityAreaRepository,
  private val characteristicRepository: CharacteristicRepository
) : SeedJob<ApprovedPremisesSeedCsvRow>(
  id = UUID.randomUUID(),
  fileName = fileName,
  requiredColumns = 12
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun verifyPresenceOfRequiredHeaders(headers: Set<String>) {
    val missingHeaders = requiredHeaders() - headers

    if (missingHeaders.any()) {
      throw RuntimeException("required headers: $missingHeaders")
    }
  }

  override fun deserializeRow(columns: Map<String, String>) = ApprovedPremisesSeedCsvRow(
    id = UUID.fromString(columns["id"]!!),
    name = columns["name"]!!,
    addressLine1 = columns["addressLine1"]!!,
    postcode = columns["postcode"]!!,
    totalBeds = Integer.parseInt(columns["totalBeds"]!!),
    notes = columns["notes"]!!,
    probationRegion = columns["probationRegion"]!!,
    localAuthorityArea = columns["localAuthorityArea"]!!,
    characteristics = columns["characteristics"]!!.split(",").filter { it.isNotBlank() }.map { it.trim() },
    isIAP = parseBooleanStringOrThrow(columns["isIAP"]!!, "isIAP"),
    isPIPE = parseBooleanStringOrThrow(columns["isPIPE"]!!, "isPIPE"),
    isESAP = parseBooleanStringOrThrow(columns["isESAP"]!!, "isESAP"),
    isSemiSpecialistMentalHealth = parseBooleanStringOrThrow(columns["isSemiSpecialistMentalHealth"]!!, "isSemiSpecialistMentalHealth"),
    isRecoveryFocussed = parseBooleanStringOrThrow(columns["isRecoveryFocussed"]!!, "isRecoveryFocussed"),
    isSuitableForVulnerable = parseBooleanStringOrThrow(columns["isSuitableForVulnerable"]!!, "isSuitableForVulnerable"),
    acceptsSexOffenders = parseBooleanStringOrThrow(columns["acceptsSexOffenders"]!!, "acceptsSexOffenders"),
    acceptsChildSexOffenders = parseBooleanStringOrThrow(columns["acceptsChildSexOffenders"]!!, "acceptsChildSexOffenders"),
    acceptsNonSexualChildOffenders = parseBooleanStringOrThrow(columns["acceptsNonSexualChildOffenders"]!!, "acceptsNonSexualChildOffenders"),
    acceptsHateCrimeOffenders = parseBooleanStringOrThrow(columns["acceptsHateCrimeOffenders"]!!, "acceptsHateCrimeOffenders"),
    isCatered = parseBooleanStringOrThrow(columns["isCatered"]!!, "isCatered"),
    hasWideStepFreeAccess = parseBooleanStringOrThrow(columns["hasWideStepFreeAccess"]!!, "hasWideStepFreeAccess"),
    hasWideAccessToCommunalAreas = parseBooleanStringOrThrow(columns["hasWideAccessToCommunalAreas"]!!, "hasWideAccessToCommunalAreas"),
    hasStepFreeAccessToCommunalAreas = parseBooleanStringOrThrow(columns["hasStepFreeAccessToCommunalAreas"]!!, "hasStepFreeAccessToCommunalAreas"),
    hasWheelChairAccessibleBathrooms = parseBooleanStringOrThrow(columns["hasWheelChairAccessibleBathrooms"]!!, "hasWheelChairAccessibleBathrooms"),
    hasLift = parseBooleanStringOrThrow(columns["hasLift"]!!, "hasLift"),
    hasTactileFlooring = parseBooleanStringOrThrow(columns["hasTactileFlooring"]!!, "hasTactileFlooring"),
    hasBrailleSignage = parseBooleanStringOrThrow(columns["hasBrailleSignage"]!!, "hasBrailleSignage"),
    hasHearingLoop = parseBooleanStringOrThrow(columns["hasHearingLoop"]!!, "hasHearingLoop"),
    status = PropertyStatus.valueOf(columns["status"]!!),
    apCode = columns["apCode"]!!,
    qCode = columns["qCode"]!!,
    latitude = columns["latitude"]!!.toDoubleOrNull(),
    longitude = columns["longitude"]!!.toDoubleOrNull()
  )

  override fun processRow(row: ApprovedPremisesSeedCsvRow) {
    val existingPremises = premisesRepository.findByIdOrNull(row.id)

    if (existingPremises != null && existingPremises !is ApprovedPremisesEntity) {
      throw RuntimeException("Premises ${row.id} is of type ${existingPremises::class.qualifiedName}, cannot be updated with Approved Premises Seed Job")
    }

    val probationRegion = probationRegionRepository.findByName(row.probationRegion)
      ?: throw RuntimeException("Probation Region ${row.probationRegion} does not exist")

    val localAuthorityArea = localAuthorityAreaRepository.findByName(row.localAuthorityArea)
      ?: throw RuntimeException("Local Authority Area ${row.localAuthorityArea} does not exist")

    val characteristics = characteristicsFromRow(row)

    if (existingPremises != null) {
      updateExistingApprovedPremises(row, existingPremises as ApprovedPremisesEntity, probationRegion, localAuthorityArea, characteristics)
    } else {
      createNewApprovedPremises(row, probationRegion, localAuthorityArea, characteristics)
    }
  }

  private fun characteristicsFromRow(row: ApprovedPremisesSeedCsvRow): List<CharacteristicEntity> {
    return listOf(
      CharacteristicValue("isIAP", castBooleanString(row.isIAP)),
      CharacteristicValue("isPIPE", castBooleanString(row.isPIPE)),
      CharacteristicValue("isESAP", castBooleanString(row.isESAP)),
      CharacteristicValue("isSemiSpecialistMentalHealth", castBooleanString(row.isSemiSpecialistMentalHealth)),
      CharacteristicValue("isRecoveryFocussed", castBooleanString(row.isRecoveryFocussed)),
      CharacteristicValue("isSuitableForVulnerable", castBooleanString(row.isSuitableForVulnerable)),
      CharacteristicValue("acceptsSexOffenders", castBooleanString(row.acceptsSexOffenders)),
      CharacteristicValue("acceptsChildSexOffenders", castBooleanString(row.acceptsChildSexOffenders)),
      CharacteristicValue("acceptsNonSexualChildOffenders", castBooleanString(row.acceptsNonSexualChildOffenders)),
      CharacteristicValue("acceptsHateCrimeOffenders", castBooleanString(row.acceptsHateCrimeOffenders)),
      CharacteristicValue("isCatered", castBooleanString(row.isCatered)),
      CharacteristicValue("hasWideStepFreeAccess", castBooleanString(row.hasWideStepFreeAccess)),
      CharacteristicValue("hasWideAccessToCommunalAreas", castBooleanString(row.hasWideAccessToCommunalAreas)),
      CharacteristicValue("hasStepFreeAccessToCommunalAreas", castBooleanString(row.hasStepFreeAccessToCommunalAreas)),
      CharacteristicValue("hasWheelChairAccessibleBathrooms", castBooleanString(row.hasWheelChairAccessibleBathrooms)),
      CharacteristicValue("hasLift", castBooleanString(row.hasLift)),
      CharacteristicValue("hasTactileFlooring", castBooleanString(row.hasTactileFlooring)),
      CharacteristicValue("hasBrailleSignage", castBooleanString(row.hasBrailleSignage)),
      CharacteristicValue("hasHearingLoop", castBooleanString(row.hasHearingLoop)),
    ).filter { it.value }
      .map {
        characteristicRepository.findByPropertyNameAndScopes(propertyName = it.propertyName, serviceName = "approved-premises", modelName = "premises")
          ?: throw RuntimeException("Characteristic '${it.propertyName}' does not exist for AP premises")
      }
  }

  private fun createNewApprovedPremises(
    row: ApprovedPremisesSeedCsvRow,
    probationRegion: ProbationRegionEntity,
    localAuthorityArea: LocalAuthorityAreaEntity,
    characteristics: List<CharacteristicEntity>
  ) {
    log.info("Creating new Approved Premises: ${row.id}")

    val approvedPremises = premisesRepository.save(
      ApprovedPremisesEntity(
        id = row.id,
        name = row.name,
        addressLine1 = row.addressLine1,
        addressLine2 = null,
        town = null,
        postcode = row.postcode,
        totalBeds = row.totalBeds,
        notes = row.notes,
        probationRegion = probationRegion,
        localAuthorityArea = localAuthorityArea,
        bookings = mutableListOf(),
        lostBeds = mutableListOf(),
        apCode = row.apCode,
        qCode = row.qCode,
        rooms = mutableListOf(),
        characteristics = mutableListOf(),
        status = row.status,
        longitude = row.longitude,
        latitude = row.latitude
      )
    )

    approvedPremises.characteristics.addAll(characteristics)

    premisesRepository.save(approvedPremises)
  }

  private fun parseBooleanStringOrThrow(value: String, fieldName: String): String {
    val booleanString = listOf("YES", "NO").find { it == value.trim().uppercase() }
      ?: throw RuntimeException("'$value' is not a recognised boolean for '$fieldName' (use yes | no)")

    return if (booleanString == "YES") "YES" else "NO"
  }

  private fun castBooleanString(booleanString: String): Boolean {
    return booleanString == "YES"
  }

  private fun updateExistingApprovedPremises(
    row: ApprovedPremisesSeedCsvRow,
    existingApprovedPremises: ApprovedPremisesEntity,
    probationRegion: ProbationRegionEntity,
    localAuthorityArea: LocalAuthorityAreaEntity,
    characteristics: List<CharacteristicEntity>
  ) {
    log.info("Updating existing Approved Premises: ${row.id}")

    existingApprovedPremises.apply {
      this.name = row.name
      this.apCode = row.apCode
      this.qCode = row.qCode
      this.addressLine1 = row.addressLine1
      this.postcode = row.postcode
      this.longitude = row.longitude
      this.latitude = row.latitude
      this.totalBeds = row.totalBeds
      this.notes = row.notes
      this.probationRegion = probationRegion
      this.localAuthorityArea = localAuthorityArea
      this.status = row.status
    }

    characteristics.forEach {
      if (existingApprovedPremises.characteristics.none { existingCharacteristic -> existingCharacteristic.id == it.id }) {
        existingApprovedPremises.characteristics.add(it)
      }
    }

    premisesRepository.save(existingApprovedPremises)
  }
}

private fun requiredHeaders(): Set<String> {
  return setOf(
    "id",
    "name",
    "addressLine1",
    "postcode",
    "totalBeds",
    "notes",
    "probationRegion",
    "localAuthorityArea",
    "characteristics",
    "isIAP",
    "isPIPE",
    "isESAP",
    "isSemiSpecialistMentalHealth",
    "isRecoveryFocussed",
    "isSuitableForVulnerable",
    "acceptsSexOffenders",
    "acceptsChildSexOffenders",
    "acceptsNonSexualChildOffenders",
    "acceptsHateCrimeOffenders",
    "isCatered",
    "hasWideStepFreeAccess",
    "hasWideAccessToCommunalAreas",
    "hasStepFreeAccessToCommunalAreas",
    "hasWheelChairAccessibleBathrooms",
    "hasLift",
    "hasTactileFlooring",
    "hasBrailleSignage",
    "hasHearingLoop",
    "status",
    "apCode",
    "qCode",
    "latitude",
    "longitude",
  )
}

data class CharacteristicValue(
  val propertyName: String,
  val value: Boolean
)

data class ApprovedPremisesSeedCsvRow(
  val id: UUID,
  val name: String,
  val addressLine1: String,
  val postcode: String,
  val totalBeds: Int,
  val notes: String,
  val probationRegion: String,
  val localAuthorityArea: String,
  val characteristics: List<String>,
  val isIAP: String,
  val isPIPE: String,
  val isESAP: String,
  val isSemiSpecialistMentalHealth: String,
  val isRecoveryFocussed: String,
  val isSuitableForVulnerable: String,
  val acceptsSexOffenders: String,
  val acceptsChildSexOffenders: String,
  val acceptsNonSexualChildOffenders: String,
  val acceptsHateCrimeOffenders: String,
  val isCatered: String,
  val hasWideStepFreeAccess: String,
  val hasWideAccessToCommunalAreas: String,
  val hasStepFreeAccessToCommunalAreas: String,
  val hasWheelChairAccessibleBathrooms: String,
  val hasLift: String,
  val hasTactileFlooring: String,
  val hasBrailleSignage: String,
  val hasHearingLoop: String,
  val status: PropertyStatus,
  val apCode: String,
  val qCode: String,
  val latitude: Double?,
  val longitude: Double?
)
