package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.PrecisionModel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PostCodeDistrictEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDouble
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomOf
import java.util.UUID

class PostCodeDistrictEntityFactory : Factory<PostCodeDistrictEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var outcode: Yielded<String> = { randomOf(listOf("SW10", "SW11", "SW12", "SW13", "SW14", "SW15", "SW16", "SW17", "SW18", "SY11", "YO51", "AB24")) }
  private var latitude: Yielded<Double> = { randomDouble(49.0, 59.0) }
  private var longitude: Yielded<Double> = { randomDouble(2.0, 8.0) }
  private var point: Yielded<Point>? = null

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withOutcode(outcode: String) = apply {
    this.outcode = { outcode }
  }

  fun withLatitude(latitude: Double) = apply {
    this.latitude = { latitude }
  }

  fun withLongitude(longitude: Double) = apply {
    this.longitude = { longitude }
  }

  fun withPoint(point: Point) = apply {
    this.point = { point }
  }

  override fun produce() = PostCodeDistrictEntity(
    id = this.id(),
    outcode = this.outcode(),
    latitude = this.latitude(),
    longitude = this.longitude(),
    point = this.point?.invoke() ?: GeometryFactory(PrecisionModel(PrecisionModel.FLOATING), 4326)
      .createPoint(Coordinate(this.latitude(), this.longitude())),
  )
}
