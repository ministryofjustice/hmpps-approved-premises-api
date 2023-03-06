package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PostCodeDistrictEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDouble
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomOf
import java.math.BigDecimal
import java.util.UUID

class PostCodeDistrictEntityFactory : Factory<PostCodeDistrictEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var outcode: Yielded<String> = { randomOf(listOf("SW18", "SY11", "YO51", "AB24")) }
  private var latitude: Yielded<BigDecimal> = { randomDouble(49.0, 59.0).toBigDecimal() }
  private var longitude: Yielded<BigDecimal> = { randomDouble(2.0, 8.0).toBigDecimal() }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withLatitude(latitude: BigDecimal) = apply {
    this.latitude = { latitude }
  }

  fun withLongitude(longitude: BigDecimal) = apply {
    this.longitude = { longitude }
  }

  override fun produce() = PostCodeDistrictEntity(
    id = this.id(),
    outcode = this.outcode(),
    latitude = this.latitude(),
    longitude = this.longitude()
  )
}
