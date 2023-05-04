package uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator

import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.cast
import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties

abstract class ReportGenerator<Input : Any, Output : Any, Properties>(private val outputType: KClass<Output>) {
  protected abstract fun filter(properties: Properties): (Input) -> Boolean
  protected abstract val convert: Input.(properties: Properties) -> List<Output>

  fun createReport(data: List<Input>, properties: Properties): DataFrame<Output> {
    val filter = filter(properties)

    // Small amount of jiggery-pokery here to coerce the DataFrame into the desired type.
    // The extension method `Iterable<T>.toDataFrame()` requires its type parameters to be
    // reifiable, which they aren't here.
    // This seems overly cautious as DataFrame allows reshaping of the columns pretty much at will,
    // but casting the input items as Any will allow it to work, as long as the converter DSL
    // closure is also cast to accept an Any.
    // From there we can call `DataFrame.cast()` to get it to be the desired `DataFrame<Output>`
    // type.
    val converted = data.filter(filter)
      .flatMap { convert(it, properties) }

    return converted
      .map { it as Any }
      .toDataFrame(*outputType.memberProperties.toTypedArray())
      .cast()
  }
}
