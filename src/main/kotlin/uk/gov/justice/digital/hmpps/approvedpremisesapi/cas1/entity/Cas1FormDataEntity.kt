package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.entity

import io.hypersistence.utils.hibernate.type.json.JsonType
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Type
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.asHibernateProxy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.getHibernateClass

@Repository
interface Cas1FormDataRepository : JpaRepository<Cas1FormDataEntity, String>

@Entity
@Table(name = "cas1_form_data")
data class Cas1FormDataEntity(
  @Id
  val id: String,
  @Type(JsonType::class)
  val value: String,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null) return false
    if (this.getHibernateClass() != other.getHibernateClass()) return false
    other as Cas1FormDataEntity

    return id == other.id
  }

  override fun hashCode(): Int = this.asHibernateProxy()?.hibernateLazyInitializer?.persistentClass?.hashCode() ?: javaClass.hashCode()

  override fun toString(): String = "Cas1FormDataEntity(id=$id)"
}
