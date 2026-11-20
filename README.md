# ReachBell Android SDK

Android client SDK for [ReachBell](https://reachbell.com). It fetches the device's Firebase Cloud Messaging (FCM) token, registers it with ReachBell, and lets you identify and tag the subscriber so your dashboard or the [Node server SDK](https://docs.reachbell.com/server/node) can target the device.

It does not send pushes or render notification UI. FCM delivers them and Android renders them.

Kotlin, minSdk 21, coroutines. `firebase-messaging` is an `api` dependency, so you do not add it yourself.

## Install

```kotlin
dependencies {
    implementation("com.reachbell:reachbell-android:1.0.0")
}
```

Via [JitPack](https://jitpack.io/#reachbell/reachbell-android):

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
| `onNewToken(token)` | `POST /subscribers` in the background, only after a successful `register()` |
| `suspend identify(externalId)` | `POST /subscribers/identify` |
| `suspend tag(tags)` | `POST /subscribers/tag` |
| `suspend untag(tags)` | `POST /subscribers/untag` |
| `getSubscriberToken(): String?` | none |
| `reset()` | none, forgets the stored token and external id |

`register()` without an `externalId` keeps the one stored by an earlier `register()` or `identify()`.

## Errors

Network methods throw `ReachBellException` with a `code`, a `statusCode` for HTTP errors, and a `reason` for rejected requests.

| `code` | Meaning |
|---|---|
| `NOT_INITIALIZED` | `init()` was never called. |
| `NO_SUBSCRIPTION` | `identify()`, `tag()` or `untag()` called before a successful `register()`. |
| `FCM_TOKEN_UNAVAILABLE` | Firebase could not return an FCM token. |
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
