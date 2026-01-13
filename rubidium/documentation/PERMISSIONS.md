# Permission System

> **Document Purpose**: Complete reference for Rubidium's role-based permission system.

## Overview

Rubidium's permission system provides:
- **Hierarchical Roles**: Inheritance-based role system
- **Fine-Grained Permissions**: Individual permission nodes
- **Contexts**: World, time, and condition-based permissions
- **Groups**: Organize players into groups
- **Temporary Permissions**: Time-limited grants
- **Caching**: High-performance permission checks

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    PermissionManager                             │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │ Core Components                                           │   │
│  │ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐     │   │
│  │ │ Role     │ │ Permission│ │ Context  │ │ Cache    │     │   │
│  │ │ Manager  │ │ Registry │ │ Manager  │ │ Manager  │     │   │
│  │ └──────────┘ └──────────┘ └──────────┘ └──────────┘     │   │
│  └──────────────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │ Storage                                                   │   │
│  │ ┌──────────┐ ┌──────────┐ ┌──────────┐                  │   │
│  │ │ Player   │ │ Role     │ │ Group    │                  │   │
│  │ │ Data     │ │ Data     │ │ Data     │                  │   │
│  │ └──────────┘ └──────────┘ └──────────┘                  │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

## Core Classes

### Permission

```java
package com.yellowtale.rubidium.permissions;

public class Permission {
    private String node;
    private String description;
    private PermissionDefault defaultValue;
    private Set<String> children;
    
    public enum PermissionDefault {
        TRUE,           // Granted by default
        FALSE,          // Denied by default
        OP,             // Granted to operators
        NOT_OP          // Granted to non-operators
    }
    
    // Permission node format: "category.subcategory.action"
    // Examples:
    // - "teleport.home.set"
    // - "chat.color.use"
    // - "economy.shop.create"
    // - "rubidium.admin.*" (wildcard)
}
```

### Role

```java
public class Role {
    private String id;
    private String name;
    private String prefix;
    private String suffix;
    private int priority;
    private int color;
    
    private Set<Role> parents;
    private Map<String, PermissionValue> permissions;
    private Map<String, String> metadata;
    
    private boolean defaultRole;
    private RoleType type;
    
    public enum RoleType {
        DEFAULT,        // Base role for all players
        DONATOR,        // Donation tier roles
        STAFF,          // Staff roles
        ADMIN,          // Administrative roles
        CUSTOM          // Custom roles
    }
    
    public enum PermissionValue {
        TRUE,           // Explicitly granted
        FALSE,          // Explicitly denied
        UNDEFINED       // Not set (inherit from parent)
    }
    
    // Built-in roles
    public static final Role DEFAULT = new Role("default", "Player", "", "", 0, 0xAAAAAA, RoleType.DEFAULT);
    public static final Role VIP = new Role("vip", "VIP", "[VIP] ", "", 100, 0x55FF55, RoleType.DONATOR);
    public static final Role MODERATOR = new Role("mod", "Moderator", "[MOD] ", "", 500, 0x55FFFF, RoleType.STAFF);
    public static final Role ADMIN = new Role("admin", "Admin", "[ADMIN] ", "", 1000, 0xFF5555, RoleType.ADMIN);
}
```

### PermissionContext

```java
public record PermissionContext(
    String world,
    String server,
    Map<String, String> conditions
) {
    public static PermissionContext global() {
        return new PermissionContext(null, null, Map.of());
    }
    
    public static PermissionContext world(String world) {
        return new PermissionContext(world, null, Map.of());
    }
    
    public static PermissionContext with(String key, String value) {
        return new PermissionContext(null, null, Map.of(key, value));
    }
    
    public boolean matches(PermissionContext required) {
        if (required.world() != null && !required.world().equals(this.world())) {
            return false;
        }
        if (required.server() != null && !required.server().equals(this.server())) {
            return false;
        }
        for (var entry : required.conditions().entrySet()) {
            if (!entry.getValue().equals(this.conditions().get(entry.getKey()))) {
                return false;
            }
        }
        return true;
    }
}
```

### PlayerPermissions

```java
public class PlayerPermissions {
    private UUID playerId;
    private Set<Role> roles;
    private Map<String, PermissionGrant> directPermissions;
    private Map<String, Long> temporaryPermissions;
    private Role primaryRole;
    private String customPrefix;
    private String customSuffix;
    
    public record PermissionGrant(
        String permission,
        PermissionValue value,
        PermissionContext context,
        long grantedAt,
        Long expiresAt,
        UUID grantedBy,
        String reason
    ) {}
}
```

### PermissionManager

```java
public class PermissionManager {
    
    // Role Management
    public void registerRole(Role role);
    public void unregisterRole(String roleId);
    public Optional<Role> getRole(String roleId);
    public List<Role> getAllRoles();
    public Role getDefaultRole();
    
    // Player Roles
    public void addRole(UUID playerId, Role role);
    public void removeRole(UUID playerId, Role role);
    public void setPrimaryRole(UUID playerId, Role role);
    public Set<Role> getPlayerRoles(UUID playerId);
    public Role getPrimaryRole(UUID playerId);
    
    // Permission Checks
    public boolean hasPermission(UUID playerId, String permission);
    public boolean hasPermission(UUID playerId, String permission, PermissionContext context);
    public PermissionValue getPermissionValue(UUID playerId, String permission);
    
    // Direct Permissions
    public void setPermission(UUID playerId, String permission, PermissionValue value);
    public void setPermission(UUID playerId, String permission, PermissionValue value, Duration duration);
    public void unsetPermission(UUID playerId, String permission);
    public Map<String, PermissionGrant> getDirectPermissions(UUID playerId);
    
    // Role Permissions
    public void setRolePermission(String roleId, String permission, PermissionValue value);
    public void unsetRolePermission(String roleId, String permission);
    
    // Prefix/Suffix
    public String getPrefix(UUID playerId);
    public String getSuffix(UUID playerId);
    public void setCustomPrefix(UUID playerId, String prefix);
    public void setCustomSuffix(UUID playerId, String suffix);
    
    // Caching
    public void invalidateCache(UUID playerId);
    public void invalidateAllCaches();
}
```

## Permission Inheritance

### Inheritance Order

```
Player Direct Permissions (highest priority)
    ↓
Player's Primary Role
    ↓
Player's Secondary Roles (by priority)
    ↓
Parent Roles (recursively)
    ↓
Default Role (lowest priority)
```

### Resolution Algorithm

```java
public PermissionValue resolve(UUID playerId, String permission, PermissionContext context) {
    // 1. Check direct permissions
    PermissionGrant direct = getDirectPermission(playerId, permission);
    if (direct != null && direct.context().matches(context)) {
        if (direct.expiresAt() == null || direct.expiresAt() > System.currentTimeMillis()) {
            return direct.value();
        }
    }
    
    // 2. Check roles by priority (highest first)
    List<Role> roles = getPlayerRoles(playerId).stream()
        .sorted(Comparator.comparingInt(Role::getPriority).reversed())
        .toList();
    
    for (Role role : roles) {
        PermissionValue value = resolveRolePermission(role, permission, context, new HashSet<>());
        if (value != PermissionValue.UNDEFINED) {
            return value;
        }
    }
    
    // 3. Check wildcard permissions
    PermissionValue wildcard = resolveWildcard(playerId, permission, context);
    if (wildcard != PermissionValue.UNDEFINED) {
        return wildcard;
    }
    
    // 4. Check default value
    Permission perm = permissionRegistry.get(permission);
    if (perm != null) {
        return perm.getDefaultValue() == PermissionDefault.TRUE 
            ? PermissionValue.TRUE 
            : PermissionValue.FALSE;
    }
    
    return PermissionValue.FALSE;
}

private PermissionValue resolveRolePermission(Role role, String permission, PermissionContext context, Set<String> visited) {
    if (visited.contains(role.getId())) {
        return PermissionValue.UNDEFINED; // Prevent cycles
    }
    visited.add(role.getId());
    
    // Check role's direct permission
    PermissionValue value = role.getPermissions().get(permission);
    if (value != null && value != PermissionValue.UNDEFINED) {
        return value;
    }
    
    // Check parent roles
    for (Role parent : role.getParents()) {
        PermissionValue parentValue = resolveRolePermission(parent, permission, context, visited);
        if (parentValue != PermissionValue.UNDEFINED) {
            return parentValue;
        }
    }
    
    return PermissionValue.UNDEFINED;
}
```

### Wildcard Support

```java
private PermissionValue resolveWildcard(UUID playerId, String permission, PermissionContext context) {
    // Build permission tree
    // Example: "teleport.home.set" checks:
    // 1. "teleport.home.set"
    // 2. "teleport.home.*"
    // 3. "teleport.*"
    // 4. "*"
    
    String[] parts = permission.split("\\.");
    StringBuilder builder = new StringBuilder();
    
    for (int i = 0; i < parts.length - 1; i++) {
        if (i > 0) builder.append(".");
        builder.append(parts[i]);
        
        String wildcard = builder + ".*";
        PermissionValue value = getDirectPermissionValue(playerId, wildcard, context);
        if (value != PermissionValue.UNDEFINED) {
            return value;
        }
    }
    
    // Check global wildcard
    return getDirectPermissionValue(playerId, "*", context);
}
```

## Commands

### Permission Commands

```
/perm check <player> <permission> - Check if player has permission
/perm info <player>               - Show player's permissions info
/perm list <player>               - List player's permissions
/perm set <player> <perm> <true|false> - Set player permission
/perm unset <player> <perm>       - Remove player permission
/perm temp <player> <perm> <true|false> <duration> - Temporary permission
```

### Role Commands

```
/role list                        - List all roles
/role info <role>                 - Show role information
/role create <name> [priority]    - Create new role
/role delete <name>               - Delete a role
/role setperm <role> <perm> <true|false> - Set role permission
/role unsetperm <role> <perm>     - Remove role permission
/role setparent <role> <parent>   - Add parent role
/role removeparent <role> <parent> - Remove parent role
/role setprefix <role> <prefix>   - Set role prefix
/role setsuffix <role> <suffix>   - Set role suffix
/role setcolor <role> <color>     - Set role color
/role setpriority <role> <priority> - Set role priority
```

### Player Role Commands

```
/role add <player> <role>         - Add role to player
/role remove <player> <role>      - Remove role from player
/role setprimary <player> <role>  - Set player's primary role
/role player <player>             - List player's roles
```

### Prefix/Suffix Commands

```
/prefix set <player> <prefix>     - Set custom prefix
/prefix clear <player>            - Clear custom prefix
/suffix set <player> <suffix>     - Set custom suffix
/suffix clear <player>            - Clear custom suffix
```

## Built-in Permissions

### Core Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `rubidium.*` | All Rubidium permissions | Admin |
| `rubidium.admin` | Administrative commands | Admin |
| `rubidium.reload` | Reload configuration | Admin |
| `rubidium.debug` | Debug commands | Admin |

### QoL Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `qol.use` | Use QoL features | All |
| `qol.manage` | Manage QoL features | Staff |
| `qol.staff.*` | All staff tools | Staff |
| `qol.staff.vanish` | Use /vanish | Staff |
| `qol.staff.godmode` | Use /godmode | Staff |
| `qol.staff.freeze` | Use /freeze | Staff |

### Teleport Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `teleport.*` | All teleport permissions | Admin |
| `teleport.tpa` | Use /tpa | All |
| `teleport.home` | Use /home | All |
| `teleport.home.multiple` | Multiple homes | All |
| `teleport.warp` | Use /warp | All |
| `teleport.warp.create` | Create warps | Staff |
| `teleport.bypass.*` | Bypass all restrictions | VIP |

### Economy Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `economy.*` | All economy permissions | Admin |
| `economy.balance` | Check balance | All |
| `economy.pay` | Send money | All |
| `economy.shop.create` | Create shops | All |
| `economy.admin` | Economy admin commands | Admin |

### Chat Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `chat.*` | All chat permissions | Admin |
| `chat.color` | Use color codes | VIP |
| `chat.format` | Use formatting | VIP |
| `chat.links` | Post links | VIP |
| `chat.bypass.filter` | Bypass chat filter | Staff |
| `chat.bypass.cooldown` | Bypass chat cooldown | VIP |

## Storage Format

### Player Permissions File

```yaml
# /data/players/{uuid}/permissions.yml
version: 1
primary_role: "vip"
roles:
  - "vip"
  - "builder"
  
direct_permissions:
  "home.limit.10":
    value: true
    granted_at: 1704067200000
    granted_by: "admin-uuid"
    reason: "Special home limit"
    
  "economy.shop.tax.exempt":
    value: true
    granted_at: 1704067200000
    expires_at: 1706659200000
    reason: "30-day trial"

prefix: null
suffix: "[Builder]"

metadata:
  last_updated: 1704153600000
```

### Roles Configuration

```yaml
# /config/permissions.yml
roles:
  default:
    name: "Player"
    prefix: ""
    suffix: ""
    priority: 0
    color: "#AAAAAA"
    default: true
    permissions:
      teleport.home: true
      teleport.spawn: true
      economy.balance: true
      economy.pay: true
      
  vip:
    name: "VIP"
    prefix: "&a[VIP] "
    suffix: ""
    priority: 100
    color: "#55FF55"
    parents:
      - default
    permissions:
      teleport.bypass.cooldown: true
      home.limit.5: true
      chat.color: true
      
  moderator:
    name: "Moderator"
    prefix: "&b[MOD] "
    suffix: ""
    priority: 500
    color: "#55FFFF"
    parents:
      - vip
    permissions:
      qol.staff.*: true
      teleport.tp: true
      chat.bypass.filter: true
      
  admin:
    name: "Admin"
    prefix: "&c[ADMIN] "
    suffix: ""
    priority: 1000
    color: "#FF5555"
    parents:
      - moderator
    permissions:
      "*": true
```

## Integration Examples

### Chat Integration

```java
chatManager.onMessageFormat((player, message) -> {
    String prefix = permissionManager.getPrefix(player.getId());
    String suffix = permissionManager.getSuffix(player.getId());
    Role primaryRole = permissionManager.getPrimaryRole(player.getId());
    
    return String.format(
        "%s%s%s: %s",
        prefix,
        player.getName(),
        suffix,
        formatMessage(player, message)
    );
});

private String formatMessage(Player player, String message) {
    if (!permissionManager.hasPermission(player.getId(), "chat.color")) {
        message = stripColorCodes(message);
    }
    if (!permissionManager.hasPermission(player.getId(), "chat.format")) {
        message = stripFormatCodes(message);
    }
    return message;
}
```

### Command Registration

```java
commandManager.register("setwarp", (sender, args) -> {
    if (!permissionManager.hasPermission(sender.getId(), "teleport.warp.create")) {
        sender.sendMessage("You don't have permission to create warps");
        return;
    }
    // Create warp...
}, "teleport.warp.create");
```

### Home Limit

```java
public int getMaxHomes(UUID playerId) {
    // Check specific limit permissions
    for (int limit = 100; limit >= 1; limit--) {
        if (permissionManager.hasPermission(playerId, "home.limit." + limit)) {
            return limit;
        }
    }
    return config.getDefaultMaxHomes();
}
```

## Caching

### Cache Structure

```java
public class PermissionCache {
    private Map<UUID, CachedPlayerPermissions> playerCache;
    private Map<String, CachedRolePermissions> roleCache;
    private Duration cacheExpiry = Duration.ofMinutes(5);
    
    public record CachedPlayerPermissions(
        Set<Role> roles,
        Map<String, PermissionValue> resolvedPermissions,
        String prefix,
        String suffix,
        long cachedAt
    ) {}
    
    public PermissionValue checkCached(UUID playerId, String permission) {
        CachedPlayerPermissions cached = playerCache.get(playerId);
        if (cached == null || isExpired(cached)) {
            return null; // Cache miss
        }
        return cached.resolvedPermissions().get(permission);
    }
}
```

### Cache Invalidation

```java
// Invalidate on role change
permissionManager.onRoleAdded((playerId, role) -> {
    cache.invalidate(playerId);
});

permissionManager.onRoleRemoved((playerId, role) -> {
    cache.invalidate(playerId);
});

// Invalidate on permission change
permissionManager.onPermissionChanged((playerId, permission) -> {
    cache.invalidate(playerId);
});

// Invalidate on role definition change
permissionManager.onRoleUpdated((role) -> {
    // Invalidate all players with this role
    for (UUID playerId : getPlayersWithRole(role)) {
        cache.invalidate(playerId);
    }
});
```

## Performance

### Optimization Tips

1. **Cache permission checks**: Use the built-in cache for repeated checks
2. **Batch permission checks**: Use `hasPermissions(uuid, Set<String>)` for multiple checks
3. **Precompute at login**: Resolve all permissions when player joins
4. **Minimize role hierarchy depth**: Deep hierarchies slow resolution
5. **Use wildcards sparingly**: Wildcard resolution has O(n) complexity

### Benchmarks

| Operation | Time (avg) |
|-----------|------------|
| Cached permission check | ~50ns |
| Uncached permission check | ~500ns |
| Role resolution | ~200ns |
| Wildcard resolution | ~1μs |
| Full cache rebuild | ~10ms |
