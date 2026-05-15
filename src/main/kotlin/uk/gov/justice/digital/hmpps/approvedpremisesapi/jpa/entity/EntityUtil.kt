package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.hibernate.proxy.HibernateProxy

/**
 * Due to https://youtrack.jetbrains.com/issue/KTLC-365 we receive warnings
 * when trying to determine if an instance is a `HibernateProxy` because
 * this is not possible according to the compile-time type hierarchy.
 *
 * But - it is possible at runtime because of how Hibernate manipulates
 * the byte code. Therefore, this function is provided as a way to do
 * the same check without upsetting Kotlin
 */
fun Any.asHibernateProxy() = this as? HibernateProxy

fun Any.getHibernateClass(): Class<*> = if (this is HibernateProxy) this.hibernateLazyInitializer.persistentClass else this.javaClass
