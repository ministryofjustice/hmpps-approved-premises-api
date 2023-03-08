package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed

import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PropertyStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LocalAuthorityAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LocalAuthorityAreaRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.CharacteristicService
import java.util.UUID

class ApprovedPremisesSeedJob(
  fileName: String,
  private val premisesRepository: PremisesRepository,
  private val probationRegionRepository: ProbationRegionRepository,
  private val localAuthorityAreaRepository: LocalAuthorityAreaRepository,
  private val characteristicService: CharacteristicService
) : SeedJob<ApprovedPremisesSeedCsvRow>(
  id = UUID.randomUUID(),
  fileName = fileName,
  requiredColumns = 12
) {
  private val log = LoggerFactory.getLogger(this::class.java)

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
    isIAP = parseBooleanOrThrow(columns["isIAP"]!!, "isIAP"),
    isPIPE = parseBooleanOrThrow(columns["isPIPE"]!!, "isPIPE"),
    isESAP = parseBooleanOrThrow(columns["isESAP"]!!, "isESAP"),
    isSemiSpecialistMentalHealth = parseBooleanOrThrow(columns["isSemiSpecialistMentalHealth"]!!, "isSemiSpecialistMentalHealth"),
    isRecoveryFocussed = parseBooleanOrThrow(columns["isRecoveryFocussed"]!!, "isRecoveryFocussed"),
    isSuitableForVulnerable = parseBooleanOrThrow(columns["isSuitableForVulnerable"]!!, "isSuitableForVulnerable"),
    acceptsSexOffenders = parseBooleanOrThrow(columns["acceptsSexOffenders"]!!, "acceptsSexOffenders"),
    acceptsChildSexOffenders = parseBooleanOrThrow(columns["acceptsChildSexOffenders"]!!, "acceptsChildSexOffenders"),
    acceptsNonSexualChildOffenders = parseBooleanOrThrow(columns["acceptsNonSexualChildOffenders"]!!, "acceptsNonSexualChildOffenders"),
    acceptsHateCrimeOffenders = parseBooleanOrThrow(columns["acceptsHateCrimeOffenders"]!!, "acceptsHateCrimeOffenders"),
    isCatered = parseBooleanOrThrow(columns["isCatered"]!!, "isCatered"),
    hasWideStepFreeAccess = parseBooleanOrThrow(columns["hasWideStepFreeAccess"]!!, "hasWideStepFreeAccess"),
    hasWideAccessToCommunalAreas = parseBooleanOrThrow(columns["hasWideAccessToCommunalAreas"]!!, "hasWideAccessToCommunalAreas"),
    hasStepFreeAccessToCommunalAreas = parseBooleanOrThrow(columns["hasStepFreeAccessToCommunalAreas"]!!, "hasStepFreeAccessToCommunalAreas"),
    hasWheelChairAccessibleBathrooms = parseBooleanOrThrow(columns["hasWheelChairAccessibleBathrooms"]!!, "hasWheelChairAccessibleBathrooms"),
    hasLift = parseBooleanOrThrow(columns["hasLift"]!!, "hasLift"),
    hasTactileFlooring = parseBooleanOrThrow(columns["hasTactileFlooring"]!!, "hasTactileFlooring"),
    hasBrailleSignage = parseBooleanOrThrow(columns["hasBrailleSignage"]!!, "hasBrailleSignage"),
    hasHearingLoop = parseBooleanOrThrow(columns["hasHearingLoop"]!!, "hasHearingLoop"),
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

    val characteristics = row.characteristics.map {
      characteristicService.getCharacteristic(it)
        ?: throw RuntimeException("Characteristic $it does not exist")
    }

    if (existingPremises != null) {
      updateExistingApprovedPremises(row, existingPremises as ApprovedPremisesEntity, probationRegion, localAuthorityArea, characteristics)
    } else {
      createNewApprovedPremises(row, probationRegion, localAuthorityArea, characteristics)
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

    characteristics.forEach {
      if (! characteristicService.serviceScopeMatches(it, approvedPremises)) {
        throw RuntimeException("Service scope does not match for Characteristic ${it.id}")
      }

      if (! characteristicService.modelScopeMatches(it, approvedPremises)) {
        throw RuntimeException("Model scope does not match for Characteristic ${it.id}")
      }

      approvedPremises.characteristics.add(it)
    }

    premisesRepository.save(approvedPremises)
  }

  private fun parseBooleanOrThrow(value: String, fieldName: String): String {
    val booleanString = listOf("YES", "NO").find { it == value.trim().uppercase() }
      ?: throw RuntimeException("'$value' is not a recognised boolean for '$fieldName' (use yes | no)")

    return if (booleanString == "YES") "YES" else "NO"
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
      if (! characteristicService.serviceScopeMatches(it, existingApprovedPremises)) {
        throw RuntimeException("Service scope does not match for Characteristic $it")
      }

      if (! characteristicService.modelScopeMatches(it, existingApprovedPremises)) {
        throw RuntimeException("Model scope does not match for Characteristic $it")
      }

      if (existingApprovedPremises.characteristics.none { existingCharacteristic -> existingCharacteristic.id == it.id }) {
        existingApprovedPremises.characteristics.add(it)
      }
    }

    premisesRepository.save(existingApprovedPremises)
  }
}

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
