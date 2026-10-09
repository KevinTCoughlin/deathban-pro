# Server-admin pilot

Recruit 3–5 volunteer administrators after the tested prerelease is available. Do not use edge request counts as install or human-visitor counts.

## Before installing

- Record the exact Spigot/Paper version, Java version, plugin version, player count and other ban/permissions plugins.
- Back up the server and plugin data. Use a staging server first and keep the previous JAR for rollback.
- Choose individual or shared mode. Document the death threshold, ban durations and disabled worlds.
- Operators bypass bans by default. Use an ordinary player when testing death enforcement.
- Keep `deathban.lives.add` and `deathban.team.manage` restricted to trusted players. Broad grants allow unrestricted refills and team switching.

## Acceptance exercises

1. Install, start, check help/theme commands, and verify no plugin startup errors.
2. As an ordinary player, reach the individual death threshold; verify delayed kick, denied login and expiry return.
3. Pardon or reset during the countdown; verify the player stays connected and the change survives restart.
4. In shared mode, consume lives with two players. Once empty, verify only the next dying player is banned.
5. Check that ordinary players can read lives but cannot refill, create or change teams. Test deliberate trusted grants separately.
6. Restart after a ban and pool change; verify both persist. Reload and switch modes without losing state.
7. Try disabled worlds and bypass permission. Check any interaction with existing permissions/ban plugins.
8. On a disposable test copy, interrupt the process after a confirmed state change and inspect recovery. Record timing; do not assume a hard power loss behaves like graceful shutdown.

## Feedback to collect

- Time from download to first successful session; exact installation/configuration failures.
- Whether admins use it again within seven days; why they retain or remove it.
- Whether penalties feel fair; which shared-life policy they actually want.
- Requested ban-history visibility or PlaceholderAPI integration, with the workflow that needs it.
- Sanitized logs and reproduction steps. Avoid publishing player UUIDs, IPs or private server configuration.

Consider wider distribution after the pilots can repeat the acceptance exercises without unexplained loss, bypass or startup errors. Themes, dashboards and cross-server sync remain later decisions based on observed admin needs.
