package uk.gov.justice.digital.hmpps.approvedpremisesapi.cmd

import kotlin.system.exitProcess

object RunSeedJobCommand {
  @JvmStatic
  fun main(args: Array<String>) {
    if (args.size < 2) {
      System.err.println("Usage: RunSeedJobCommand <seed_type> <file_name>")
      exitProcess(1)
    }

    InvokeAdminJobEndpoint.invokeEndpoint(
      "seed",
      mapOf(
        "seedType" to args[0],
        "fileName" to args[1],
      ),
    )
  }
}
