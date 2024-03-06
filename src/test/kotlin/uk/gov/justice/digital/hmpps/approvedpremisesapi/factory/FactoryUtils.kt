package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.typeOf

@Suppress("UNCHECKED_CAST")
inline fun <reified F : Factory<T>, reified T : Any> F.from(value: T): F = apply {
  val yieldedProperties = F::class
    .memberProperties
    .filter { it.returnType.jvmErasure == typeOf<Yielded<*>>().jvmErasure }
    .mapNotNull { it as? KMutableProperty1<F, Any?> }

  val valueProperties = T::class.memberProperties.associateBy { it.name }

  yieldedProperties.forEach { prop ->
    val propertyName = prop.name
    val propertyValue = valueProperties[propertyName]?.get(value)

    propertyValue?.let { prop.set(this, { it }) }
  }
}
