# 3. Use Open API Generator to scaffold Controllers from Open API file

Date: 2022-08-03

## Status

Approved

## Context

To provide API documentation in an industry standard way we have already decided to use an OpenAPI file.  Doing so 
adds the burden of ensuring that the actual API implementation aligns with what is documented in the file.

## Decision

We will use [OpenAPI Generator](https://github.com/OpenAPITools/openapi-generator) to automate the process of 
creating Spring Controllers.  By making the `compileKotlin` gradle Task depend on the `openApiGenerate` task this 
will happen automatically each time the project is built.

## Consequences

This reduces repetitive work and ensures that the OpenAPI file and the actual endpoints always align.  It also opens 
the possibility of generating other types of code in the future to reduce repetitive work for clients of the API:
 - [TypeScript Client](https://openapi-generator.tech/docs/generators/typescript-fetch)
 - [Kotlin Client](https://openapi-generator.tech/docs/generators/kotlin)


Unfortunately, OpenAPI Generator does not support the OpenAPI 3 `anyOf`, `oneOf` options on request/response bodies
which are useful if you want to return completely different responses from an endpoint from within the delegate.  
Whilst this is on their [roadmap](https://openapi-generator.tech/docs/roadmap/#short-term) it has been there for
some time.  However, since we have already decided to use the
[Problem Spring Web Library](https://github.com/zalando/problem-spring-web) which uses exceptions & exception handlers
to return the Problem responses - this won't be an issue.
