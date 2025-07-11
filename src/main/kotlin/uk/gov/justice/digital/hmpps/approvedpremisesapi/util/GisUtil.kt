package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel

object GisUtil {
  const val LAT_LON_SRID = 4326

  val geometryFactory = GeometryFactory(PrecisionModel(PrecisionModel.FLOATING), LAT_LON_SRID)

  fun createPoint(lat: Double, lon: Double) = geometryFactory.createPoint(Coordinate(lat, lon))
}
