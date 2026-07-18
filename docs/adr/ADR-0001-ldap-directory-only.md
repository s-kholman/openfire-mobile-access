# ADR-0001: LDAP is directory-only

- Status: Accepted
- Date: 2026-07-18

## Context

Openfire must use Active Directory for usernames, groups, display names, and email while avoiding the use of AD passwords on personal mobile devices.

## Decision

Use LDAP user, group, and vCard providers only. Do not use `LDAPAuthProvider` in the target architecture.

## Consequences

LDAP remains authoritative for identity and eligibility. Windows authentication uses Kerberos. Mobile authentication requires a separate local credential. The plugin must not modify directory data.
