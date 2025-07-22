# 28 CAS endpoint pattern

## Status

Accepted

## Context

We need to make sure our pattern of setting up endpoints is consistent across the API.

# Helpful links
- Status codes: https://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html
- Methods: https://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html#sec9

# Changes

| Method | Request Body              | Response Body                      | Status codes  | When to use                                                                      |
|--------|---------------------------|------------------------------------|---------------|----------------------------------------------------------------------------------|
| DELETE | NEVER                     | ALWAYS - returns deleted resource  | 200           | Deleting a resource                                                              |
| GET    | NEVER                     | ALWAYS                             | 200           | Getting resource/s                                                               |
| HEAD	  | NEVER                     | NEVER	                             | 204	          | Checking if a resource exists                                                    |
| PATCH  | ALWAYS - partial resource | 	ALWAYS - returns updated resource | 	200	         | Updating some fields in a resource                                               |
| POST	  | SOMETIMES	                | SOMETIMES                          | 	200/201/204	 | Creating a resource or doing something that requires changing multiple resources |
| PUT    | ALWAYS - full resource	   | ALWAYS - returns updated resource  | 	200	         | Replacing an entire resource                                                     |
