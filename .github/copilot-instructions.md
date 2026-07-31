# Controller (API) Style Guide

* @Operation annotations should not define an operationId. These are provided on-the-fly in OpenApiConfiguration
* @PathVariable and @RequestParam annotations should not include redundant values (e.g. where the name of the variable being annotation matches the value defined in the annotation)
* @RequestParam and @Parameter annotations should not include redundant 'required' entries in annotations because this can be inferred from the kotlin definition (e.g. if the type is suffixed with '?')
* @Schema annotations should not include empty descriptions. If a description is empty it is redundant and should be removed
* In the controller function argument list put each argument on a new line

# Post-change tidy up

Once you've finished generating code, you should tidy it up and validate as follows:

Run

`./gradlew ktlintCheck`

And

`./gradlew detekt`

and fix any issues raised by detekt
