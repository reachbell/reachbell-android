# Changelog

All notable changes to this project are documented here. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses [Semantic Versioning](https://semver.org/).

## [1.0.0] - 2026-11-21

### Added

- `ReachBell.init(context, apiKey, apiUrl)` with `apiUrl` defaulting to `https://api.reachbell.com`.
- `ReachBell.register(externalId)` fetches the FCM token, calls `POST /subscribers` and persists the subscriber token and external id.
- `ReachBell.onNewToken(token)` re-registers a rotated FCM token in the background, keeping the stored external id. It is a no-op before `init()` or before the first successful `register()`.
- `ReachBell.identify(externalId)`, `ReachBell.tag(tags)` and `ReachBell.untag(tags)`.
- `ReachBell.getSubscriberToken()` and `ReachBell.reset()`.
- `ReachBellException` with `code`, `statusCode` and `reason`. Codes: `NOT_INITIALIZED`, `NO_SUBSCRIPTION`, `FCM_TOKEN_UNAVAILABLE`, `NETWORK_ERROR`, `SERVER_ERROR`, `REJECTED`.
- Library manifest declares `INTERNET` and `POST_NOTIFICATIONS`.

[1.0.0]: https://github.com/reachbell/reachbell-android/releases/tag/1.0.0
