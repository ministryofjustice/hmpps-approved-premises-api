package uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator

import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.CreateDataFrameDsl
import org.jetbrains.kotlinx.dataframe.api.cast
import org.jetbrains.kotlinx.dataframe.api.toDataFrame

abstract class ReportGenerator<Input, Output, Properties> {
  protected abstract fun filter(properties: Properties): (Input) -> Boolean
  protected abstract val convert: CreateDataFrameDsl<Input>.() -> Unit

  fun createReport(data: List<Input>, properties: Properties): DataFrame<Output> {
    val filter = filter(properties)
    val convert = convertUnchecked()

    // Small amount of jiggery-pokery here to coerce the DataFrame into the desired type.
    // The extension method `Iterable<T>.toDataFrame()` requires its type parameters to be
    // reifiable, which they aren't here.
    // This seems overly cautious as DataFrame allows reshaping of the columns pretty much at will,
    // but casting the input items as Any will allow it to work, as long as the converter DSL
    // closure is also cast to accept an Any.
    // From there we can call `DataFrame.cast()` to get it to be the desired `DataFrame<Output>`
    // type.
    return data.filter(filter)
      .map { it as Any }
      .toDataFrame(convert)
      .cast()
  }

  // This cast of the `convert` closure to use Any as the type parameter is safe as in practice
  // it will only ever have items of the type specified by the type parameter `Input`.
  @Suppress("UNCHECKED_CAST")
  private fun convertUnchecked(): CreateDataFrameDsl<Any>.() -> Unit = convert as CreateDataFrameDsl<Any>.() -> Unit
}
