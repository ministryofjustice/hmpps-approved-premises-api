package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.client.restClient

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.json.JSONException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.dao.OptimisticLockingFailureException
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.restclient.retry
import java.rmi.UnexpectedException
import java.sql.SQLException
import java.util.concurrent.atomic.AtomicInteger

internal class RetryTest {

  @Test
  fun `when exception thrown retry until max retries`() {
    val counter = AtomicInteger(0)
    assertThrows<UnexpectedException> {
      retry(3) {
        counter.incrementAndGet()
        throw UnexpectedException("Unexpected Exception")
      }
    }
    assertThat(counter.get(), equalTo(3))
  }

  @Test
  fun `when successful no retries`() {
    val counter = AtomicInteger(0)
    val result = retry(3) {
      counter.incrementAndGet()
    }
    assertThat(result, equalTo(1))
  }

  @Test
  fun `when optimistic lock exception thrown retry until max retries`() {
    val counter = AtomicInteger(0)
    assertThrows<OptimisticLockingFailureException> {
      retry(3, listOf(OptimisticLockingFailureException::class, JSONException::class)) {
        counter.incrementAndGet()
        throw OptimisticLockingFailureException("OLE")
      }
    }
    assertThat(counter.get(), equalTo(3))
  }

  @Test
  fun `when SQL exception thrown no retries`() {
    val counter = AtomicInteger(0)
    assertThrows<SQLException> {
      retry(3, listOf(OptimisticLockingFailureException::class)) {
        counter.incrementAndGet()
        throw SQLException("SQLE")
      }
    }
    assertThat(counter.get(), equalTo(1))
  }
}
