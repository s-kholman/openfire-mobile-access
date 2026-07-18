# ADR-0003: Kerberos for managed Windows clients

- Status: Accepted
- Date: 2026-07-18

## Context

Domain-joined Windows workstations can authenticate transparently without prompting users for passwords.

## Decision

Use SASL GSSAPI/Kerberos as the primary authentication mechanism for managed Windows Spark clients.

## Consequences

Windows SSO depends on correct DNS, SPN, time synchronization, keytab protection, and client configuration. Kerberos is not treated as the mobile authentication mechanism.
