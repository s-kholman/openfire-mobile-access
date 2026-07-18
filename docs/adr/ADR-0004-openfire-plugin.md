# ADR-0004: Extend Openfire through a plugin

- Status: Accepted
- Date: 2026-07-18

## Context

The solution needs administrative UI, LDAP eligibility checks, credential lifecycle management, and auditing without maintaining a private Openfire fork.

## Decision

Implement the solution as a dedicated Openfire plugin and use supported Openfire services and extension points wherever possible.

## Consequences

Openfire core remains upgradeable. Compatibility must be tested against supported Openfire releases. Direct database access is isolated and used only when no supported API exists, with explicit review and tests.
