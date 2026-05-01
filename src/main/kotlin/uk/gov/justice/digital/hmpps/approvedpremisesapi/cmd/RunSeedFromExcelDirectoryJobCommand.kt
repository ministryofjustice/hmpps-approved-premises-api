package uk.gov.justice.digital.hmpps.approvedpremisesapi.cmd

import kotlin.system.exitProcess

object RunSeedFromExcelDirectoryJobCommand {
  @JvmStatic
  fun main(args: Array<String>) {
    if (args.size < 2) {
      System.err.println("Usage: RunSeedFromExcelDirectoryJobCommand <seed_type> <directory_name>")
      exitProcess(1)
    }

    InvokeAdminJobEndpoint.invokeEndpoint(
      "seedFromExcel/directory",
      mapOf(
        "seedType" to args[0],
        "directoryName" to args[1],
      ),
    )
  }
}
