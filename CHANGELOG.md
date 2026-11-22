# Changelog

All notable changes to this project are documented here. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added

- `reset()` calls `POST /subscribers/unidentify` with the stored subscriber token (best effort) before forgetting the local subscription, so the user is unlinked on the server at logout. Later SDK calls wait for it.
- On a token rotation, `POST /subscribers` sends `previousToken` (the stored subscriber token). The API moves tags and the external id to the new token and deactivates the old one.
- Requests send a `User-Agent` of the form `ReachBell/<ver> (Linux; Android <release>; <model>) reachbell-android/<ver>` and `os`, `osVersion`, `device` body fields, so subscribers are no longer recorded as `Unknown` / `desktop`.

### Fixed

- The FCM token is fetched outside the registration lock with a 30 second timeout, so a hung Firebase task no longer blocks later `register()` and `onNewToken()` calls. A token delivered to `onNewToken()` during the fetch is still the one registered.
- `reset()`, `identify()`, `tag()` and `untag()` are serialised with `register()`; an in-flight `register()` no longer re-saves a subscription that `reset()` cleared.
- `onNewToken()` no longer swallows coroutine cancellation.
- `getSubscriberToken()` reads an in-memory copy instead of SharedPreferences on the caller thread.
- README documents the JitPack coordinate only; the SDK is not published to Maven Central.

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
