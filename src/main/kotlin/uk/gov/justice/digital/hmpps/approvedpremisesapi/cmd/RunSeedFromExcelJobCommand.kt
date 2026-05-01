package uk.gov.justice.digital.hmpps.approvedpremisesapi.cmd

import kotlin.system.exitProcess

object RunSeedFromExcelJobCommand {
  @JvmStatic
  fun main(args: Array<String>) {
    if (args.size < 2) {
      System.err.println("Usage: RunSeedFromExcelJobCommand <seed_type> <file_name>")
      exitProcess(1)
    }

    InvokeAdminJobEndpoint.invokeEndpoint(
      "seedFromExcel/file",
      mapOf(
        "seedType" to args[0],
        "fileName" to args[1],
      ),
    )
  }
}
