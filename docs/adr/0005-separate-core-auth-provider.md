# ADR-0005: Deploy the authentication provider outside the plugin classloader

- Status: Accepted
- Date: 2026-07-18

## Context

Openfire plugins use an isolated classloader and can be loaded or unloaded while the server is running. The configured `AuthProvider`, however, is instantiated by Openfire core and must remain available for the complete server lifecycle.

The official Openfire custom authentication provider guide explicitly recommends placing a custom provider JAR in the Openfire `lib` directory instead of packaging it only as a plugin.

## Decision

The project produces two deployment artifacts:

1. `mobileaccess.jar` — Openfire plugin for administration, directory eligibility, credential management and audit.
2. `mobileaccess-auth-provider.jar` — core-classpath library installed in the Openfire `lib` directory and referenced by `HybridAuthProvider`.

The provider is read-only from the authentication API perspective:

- plaintext passwords cannot be retrieved;
- `setPassword` is not used;
- credential creation, rotation and revocation are performed by the administration plugin;
- the provider only verifies supplied credentials.

## Consequences

- Installing or updating the auth-provider JAR requires an Openfire restart.
- Unloading the administration plugin does not make the configured authentication provider class disappear.
- Plugin and provider must share a stable database contract rather than direct Java object references.
- Releases must contain and document both artifacts.
