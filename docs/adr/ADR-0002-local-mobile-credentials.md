# ADR-0002: Separate local mobile credentials

- Status: Accepted
- Date: 2026-07-18

## Context

Personal mobile devices must authenticate to Openfire without receiving the user's Active Directory or VPN password.

## Decision

Store a separate Openfire-local mobile credential for the same username. Password creation, rotation, and revocation are controlled through the plugin and Openfire APIs.

## Consequences

There is one logical username, not a duplicate mobile user. Compromise of the mobile credential does not directly expose AD credentials. Local credential lifecycle and audit become explicit plugin responsibilities.
