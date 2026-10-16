package top.likoslupus.cellulosesz.application.config

import kotlinx.serialization.Serializable

/**
 * Server-configured capabilities for one addon. Only privileged/system operations are gated here;
 * ordinary player actions still go through [top.likoslupus.cellulosesz.core.permission.PermissionService].
 */
@Serializable
public data class AddonIntegrationSettings(
    public val allowedCapabilities: List<String> = emptyList(),
)

/**
 * The `[api.integrations]` table. It is the single opt-in point for addons that need to perform
 * system-level operations (system mail, kit rewards, moderation actions). By default no addon is
 * granted any system capability.
 */
@Serializable
public data class ApiIntegrationSettings(
    public val enabled: Boolean = false,
    public val addons: Map<String, AddonIntegrationSettings> = emptyMap(),
)
