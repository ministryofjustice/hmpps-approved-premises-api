Out of the box, the open api generator will create controllers with a response type of ResponseEntity<Resource> for binary file responses, which does not support writing directly to the servlet output stream.

The custom template in this folder adds a rule that ensures ResponseEntity<StreamingResponseBody> is used as the return type for endpoints with a binary file response, allowing files to be written directly to the servlet output stream.

The original template is defined here - https://github.com/OpenAPITools/openapi-generator/blob/v5.4.0/modules/openapi-generator/src/main/resources/kotlin-spring/returnTypes.mustache. To determine properties available to use in mustache logic, we can look at https://github.com/OpenAPITools/openapi-generator/blob/v5.4.0/modules/openapi-generator/src/main/java/org/openapitools/codegen/CodegenOperation.java