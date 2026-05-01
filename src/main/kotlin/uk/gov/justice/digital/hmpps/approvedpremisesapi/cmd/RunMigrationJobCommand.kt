package uk.gov.justice.digital.hmpps.approvedpremisesapi.cmd

import kotlin.system.exitProcess

object RunMigrationJobCommand {
  @JvmStatic
  fun main(args: Array<String>) {
    if (args.isEmpty()) {
      System.err.println("Usage: RunMigrationJobCommand <migration_job_id>")
      exitProcess(1)
    }

    InvokeAdminJobEndpoint.invokeEndpoint(
      "migration-job",
      mapOf(
        "jobType" to args[0],
      ),
    )
  }
}
