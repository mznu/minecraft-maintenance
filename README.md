# minecraft-maintenance

A lightweight maintenance-mode plugin for Paper servers.

## About this project

This is an independent, Paper-only implementation inspired by
[Maintenance](https://github.com/kennytv/Maintenance) by kennytv. It is not a fork or drop-in replacement; existing
Maintenance configuration and language files are incompatible, and no migration is provided.

## Requirements

- Paper 26.2
- Java 25

## Features

- Controls maintenance mode at runtime and blocks or kicks non-whitelisted players.
- Schedules maintenance using absolute, current, relative, and time-only values or an end duration.
- Sends configurable notifications when maintenance is scheduled and approaches.
- Customizes the server-list MOTD, player-count text, hover text, and server icon.
- Provides English and Korean player-facing content with runtime configuration reloads.

## Installation

1. Stop the server. If the original Maintenance plugin is installed, remove its JAR and `plugins/Maintenance` data
   directory after backing up anything needed; the plugins and their data cannot coexist.
2. Build the plugin as described in [Building](#building), or download a published JAR from
   [GitHub Releases](https://github.com/mznu/minecraft-maintenance/releases).
3. Copy `maintenance-<version>.jar` into the server's `plugins` directory and start the server.
4. Edit the generated files under `plugins/Maintenance`, then restart or run `/maintenance reload`.

## Commands

| Command | Description |
| --- | --- |
| `/maintenance` | Shows the plugin version and help hint. |
| `/maintenance help` | Lists available commands. |
| `/maintenance status` | Shows the current schedule, or whether maintenance mode is enabled. |
| `/maintenance on` | Cancels an existing schedule and enables maintenance mode. |
| `/maintenance off` | Cancels an existing schedule and disables maintenance mode. |
| `/maintenance reload` | Reloads the configuration, language, schedule, and whitelist. |
| `/maintenance schedule <start> <end\|duration>` | Creates or replaces a maintenance schedule. |
| `/maintenance schedule status` | Shows the current schedule. |
| `/maintenance schedule cancel` | Cancels the schedule and disables maintenance mode. |
| `/maintenance whitelist add <player>` | Adds a player to the maintenance whitelist. |
| `/maintenance whitelist remove <player>` | Removes a player from the maintenance whitelist. |
| `/maintenance whitelist list` | Lists whitelisted players. |

The `/mt` alias is available for every command. All commands require the `maintenance.command` permission, which is
granted to server operators by default.

### Schedule input formats

| Input | Start | End | Meaning |
| --- | --- | --- | --- |
| `2026-09-01T02:00`, `2026-09-01T02:00+09:00` | Yes | Yes | Local or offset ISO-8601 date-time. |
| `9`, `09`, `09:30` | Yes | Yes | Time-only value; an hour without minutes means `HH:00`. |
| `now`, `now+30m` | Yes | No | The command instant, optionally plus a duration. |
| `1h`, `30m`, `1h30m` | No | Yes | The start instant plus a duration. |

Local and time-only values use `schedule.time-zone`. A time-only start uses the current local date without rolling
forward. A time-only end uses the start's local date and moves to the next date when earlier; equality remains invalid.

Command durations use ordered, positive whole-number `d`, `h`, and `m` components without spaces. Seconds are not
accepted. The `schedule.starts-at` and `schedule.ends-at` configuration values remain ISO-8601-only; command inputs are
resolved and persisted as absolute ISO-8601 date-times. Plugin startup fails if only one value is set, either value is
not a valid ISO-8601 string, the end is not after the start, or the end is not in the future. Reload rejects the same
values and retains the current runtime state.

Examples:

```text
/maintenance schedule now 1h
/maintenance schedule now+30m 2h
/maintenance schedule 22 1
```

## Configuration files

- `config.yml`: locale, maintenance state, schedule, reminders, upcoming MOTD timing, and server icon. Icon paths are
  relative to `plugins/Maintenance`; icons must be 64x64 PNG files.
- `lang/en.yml`: default English player-facing messages and server-list content.
- `lang/ko.yml`: bundled Korean player-facing messages and server-list content.
- `whitelist.yml`: players allowed to connect while maintenance mode is enabled.

## Localization

Set `locale` in `config.yml` to a BCP 47 tag. English (`en`) and Korean (`ko`) are bundled; region tags fall back through
their language to English, such as `ko-KR` → `ko` → `en`. Bundled files are copied to `plugins/Maintenance/lang` when
needed. To add a language, copy and translate `lang/en.yml`, select its tag, and reload; missing keys follow the same
fallback chain.

Language files control player-facing messages and server-list content and support
[MiniMessage](https://docs.papermc.io/adventure/minimessage/format/). `schedule.display-format` uses a Java
`DateTimeFormatter` pattern for player-facing schedule times. Console responses and logs use the same ISO-8601 offset
date-time format stored in `config.yml`. Commands, console output, warnings, and internal validation remain English-only.

## Building

Build with the Gradle wrapper and JDK 25. The build runs the tests and writes the deployable Shadow JAR to
`build/libs/maintenance-dev.jar`:

```shell
./gradlew clean build
```

Use [mise](https://mise.jdx.dev/) when needed, or inject a version for a versioned local build:

```shell
mise exec java@temurin-25 -- ./gradlew clean build
./gradlew clean build -PbuildVersion=1.0.0
```

Run only the tests with `./gradlew test`.

## Releasing

After squash-merging the release changes into `main`, create and push a stable SemVer tag:

```shell
git tag v1.0.0
git push origin v1.0.0
```

The workflow accepts only `vMAJOR.MINOR.PATCH` tags contained in `main`, verifies and publishes the JAR, and generates
release notes with [git-cliff](https://git-cliff.org/) according to [`cliff.toml`](cliff.toml). Pull request titles and
squash subjects must use `type(scope): description`; the scope is optional and `!` marks a breaking change:

```text
feat(schedule): add maintenance reminders
feat(config)!: replace schedule keys
```

Configure GitHub to use PR titles as squash subjects and require the `Validate title` check.

## License

Licensed under the [GNU General Public License v3.0 or later](LICENSE).
