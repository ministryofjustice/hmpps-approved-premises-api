# 14. Paginating large data sets

Date: 2023-08-01

## Status

Accepted

## Context

As the service evolves, and more data is being added to the application, a lot of our "list-type"
pages are becoming hard to navigate, as well as causing performance issues in the API and the 
frontend.

As such we need to start looking at using pagination to break up these datasets into smaller, 
more manageable chunks, as well as returning metadata to communicate to the client how many 
pages there are, what the current page is, and the number of the next page etc.

Before we start this work, we should ensure that we agree a common approach to be rolled out
across all endpoints and get agreement from everyone.

## Decision

We will implement "page-based pagination", starting first with the `/placement-requests/dashboard`
endpoint. The page query will look as follows:

```
/placement-requests/dashboard?page=$pageNumber
```

Where `$pageNumber` is the page number the client wants to return (starting at 1). If the 
`$pageNumber` is not present, the API returns all results. This can be tweaked as the 
service evolve, and we have pagination on all endpoints.

This will then be passed into the service to determine which page of results to return.

We could possibly use a [`PagingAndSortingRepository`][1] for this.

As well as returning the data in the body, we will also need to return metadata about the 
pagination. To ensure compatibility and also make frontend integration easier, we should
return this information in the header with the following information:

- The current page
- The total number of pages
- The total number of results
- The page size (this will be a default value, but useful for compatibility)

This could be implemented in the OpenAPI spec like so:

```yml
  /placement-requests/dashboard:
    get:
      tags:
        - Placement requests
      summary: Gets all placement requests
      parameters:
        - name: isParole
          in: query
          description: States whether or not to return parole cases
          schema:
            type: boolean
      responses:
        200:
          description: successfully retrieved placement requests
          headers:
            X-Pagination-CurrentPage:
              schema:
                type: integer
              description: The current page number
            X-Pagination-TotalPages:
              schema:
                type: integer
              description: The total number of pages
            X-Pagination-TotalResults:
              schema:
                type: integer
              description: The total number of results
            X-Pagination-PageSize:
              schema:
                type: integer
              description: The total number of results
          content:
            'application/json':
              schema:
                type: array
                items:
                  $ref: '#/components/schemas/PlacementRequest'
        401:
          $ref: '#/components/responses/401Response'
        403:
          $ref: '#/components/responses/403Response'
        500:
          $ref: '#/components/responses/500Response'
```

Again, using the `PagingAndSortingRepository`, we can get this metadata from the response and return
it alongside the actual data itself, perhaps using a `Pair`, e.g:

```kotlin
// PlacementRequestEntity.kt
@Repository
interface PlacementRequestRepository : PagingAndSortingRepository<PlacementRequestEntity, UUID> {
  fun findAllByReallocatedAtNullAndBooking_IdNullAndIsWithdrawnFalse(pageable: Pageable): Page<PlacementRequestEntity>
}

// PlacementRequestService.kt
data class PaginationMetadata(val currentPage: Int, val totalPages: Int, val totalResults: Int, val pageSize: Int)

fun getAllReallocatable(page: Int): Pair<List<PlacementRequestEntity>, PaginationMetadata> {
    val pageable = PageRequest.of(page, 10)
    val response = placementRequestRepository.findAllByReallocatedAtNullAndBooking_IdNullAndIsWithdrawnFalse(pageable)
    return Pair(
        response.content,
        PaginationMetadata(page, response.totalPages, response.totalResults, 10)
    )
}
```

Returning the pages in the header will then allow our clients to separate the response from the
metadata, ensuring we don't pollute our actual responses  with metadata and making rolling out
pagination easier. This could be handled in the frontend like so:

```typescript
type PaginatedResponse<T> = { 
    body: T; 
    pageNumber: number; 
    totalPages: number, 
    totalResults: number, 
    pageSize: number 
}

async all(pageNumber: number): Promise<PaginatedResponse<Array<PlacementRequest>>> {
    const response = (await this.restClient.get({
      path: paths.placementRequests.index.pattern,
      query: { pageNumber: String(pageNumber) },
      raw: true,
    })) as superagent.Response
    
    return {
      body: response.body,
      pageNumber,
      totalPages: response.headers['X-Pagination-TotalPages'],
      totalResults: response.headers['X-Pagination-TotalResults'],
      pageSize: response.headers['X-Pagination-PageSize'],
    }
}
```

## Consequences

When combined with the requisite frontend changes, this will make responses quicker and easier
for users to see. Using a `PagingAndSortingRepository` will also make it easier to do things
like filtering and sorting too, without having to alter the underlying queries.

However, when implementing changes to endpoints used by more than one service, we will need
to ensure that it is either easy to enable/disable pagination based on the service request
header, or all teams work together to ensure the changes to all applications are worked on
and deployed in tandem. This could be mitigated by returning all results unless a page number
is provided.

[1]: https://www.baeldung.com/spring-data-jpa-pagination-sorting