# ReachBell Android SDK

Android client SDK for [ReachBell](https://reachbell.com). It fetches the device's Firebase Cloud Messaging (FCM) token, registers it with ReachBell, and lets you identify and tag the subscriber so your dashboard or the [Node server SDK](https://docs.reachbell.com/server/node) can target the device.

It does not send pushes or render notification UI. FCM delivers them and Android renders them.

Kotlin, minSdk 21, coroutines. `firebase-messaging` is an `api` dependency, so you do not add it yourself.

## Install

The SDK is published through [JitPack](https://jitpack.io/#reachbell/reachbell-android), built from the Git tag. It is not on Maven Central. Add the JitPack repository in `settings.gradle.kts` and depend on the JitPack coordinate:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

dependencies {
    implementation("com.github.reachbell:reachbell-android:1.0.0")
}
```

ReachBell sends Android push through your own Firebase project. Add your app to a Firebase project, put `google-services.json` in your app module, apply the `com.google.gms.google-services` plugin, and add the FCM service-account JSON in the ReachBell dashboard.

The `INTERNET` and `POST_NOTIFICATIONS` permissions are declared in the library manifest. On Android 13+ you still have to request `POST_NOTIFICATIONS` at runtime.

## Usage

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ReachBell.init(this, "rb_live_yourkey")
    }
}
```

```kotlin
lifecycleScope.launch {
    try {
        ReachBell.register(externalId = "user_42")
        ReachBell.tag(listOf("pro", "weekly_digest"))
    } catch (e: ReachBellException) {
        Log.w("ReachBell", "push setup failed: ${e.code}", e)
    }
}
```

```kotlin
class MyMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        ReachBell.onNewToken(token)
    }
}
```

| Method | Endpoint |
|---|---|
| `init(context, apiKey, apiUrl = "https://api.reachbell.com")` | none |
| `suspend register(externalId: String? = null): String` | `POST /subscribers` |
| `onNewToken(token)` | `POST /subscribers` with `previousToken` in the background, only after a successful `register()` |
| `suspend identify(externalId)` | `POST /subscribers/identify` |
| `suspend tag(tags)` | `POST /subscribers/tag` |
| `suspend untag(tags)` | `POST /subscribers/untag` |
| `getSubscriberToken(): String?` | none |
| `reset()` | `POST /subscribers/unidentify` in the background (best effort), then forgets the stored token and external id |

`register()` without an `externalId` keeps the one stored by an earlier `register()` or `identify()`.

When FCM rotates the token, `onNewToken()` (and a `register()` that finds a different token) sends the stored subscriber token as `previousToken`. ReachBell moves the old subscriber's tags and external id to the new token and deactivates the old one, so you do not re-tag or re-identify after a rotation.

Call `reset()` when the user logs out. It returns immediately; SDK calls made after it wait for it to finish, and `getSubscriberToken()` returns `null` straight away. A `register()` that is still fetching its FCM token when `reset()` is called fails with `NO_SUBSCRIPTION` instead of saving the old subscription again. Call `register()` again afterwards to keep the device subscribed anonymously or for the next user.

Requests carry a `User-Agent` such as `ReachBell/1.0.0 (Linux; Android 14; Pixel 8) reachbell-android/1.0.0` plus `os`, `osVersion` and `device` fields, so the dashboard shows the device as Android mobile or tablet.

## Errors

Network methods throw `ReachBellException` with a `code`, a `statusCode` for HTTP errors, and a `reason` for rejected requests.

| `code` | Meaning |
|---|---|
| `NOT_INITIALIZED` | `init()` was never called. |
| `NO_SUBSCRIPTION` | `identify()`, `tag()` or `untag()` called before a successful `register()`, or `reset()` called while `register()` was fetching its token. |
| `FCM_TOKEN_UNAVAILABLE` | Firebase could not return an FCM token, or did not return one within 30 seconds. |
| `NETWORK_ERROR` | Transport failure before a response. |
| `SERVER_ERROR` | Non-2xx response, see `statusCode`. `403` means the plan's subscriber limit is reached. |
| `REJECTED` | 2xx response with `success: false`, see `reason` (for example `subscriber_not_found`). |

Each code also has a lowercase `value` such as `not_initialized`.

## Build

```bash
./gradlew :reachbell:assembleRelease :reachbell:testReleaseUnitTest
./gradlew :reachbell:publishToMavenLocal
```

Requires JDK 17 and the Android SDK (compileSdk 35).

Full documentation: https://docs.reachbell.com/mobile/android

## License

MIT
