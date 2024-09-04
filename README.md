# DNS Forwarder

A simple DNS Forwarder implemented in Java. This application listens for DNS queries on a specified port, forwards the queries to an upstream DNS resolver, and returns the resolved IP address back to the client. The server handles DNS queries of type `A` and supports multiple questions per DNS packet by splitting them into individual requests.

## Features

- **Forwarding DNS Queries**: Forwards incoming DNS queries to a specified DNS resolver and returns the response to the client.
- **Handles Multiple Queries**: Splits DNS queries containing multiple questions into individual requests and combines the results into a single response.
- **DNS Response Parsing**: Correctly handles and parses DNS response packets, including DNS name compression.

# Note: 
This Project was made during an Online Challenge ' build your x'
