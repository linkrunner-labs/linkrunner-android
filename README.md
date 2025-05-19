# Linkrunner Android SDK

A native Android SDK for integrating Linkrunner functionality into your Android applications.

## Features

- Easy initialization and setup
- User management and tracking
- Payment tracking and management
- Event tracking
- Deeplink handling

## Requirements

- Android 5.0 (API level 21) and above
- Android Studio Flamingo (2022.2.1) or newer
- Gradle 8.0+

## Installation

### Add the dependency

Add the following to your app's `build.gradle`:

```gradle
dependencies {
    implementation 'com.linkrunner:sdk:1.0.0'
}
```

### Initialize the SDK

In your `Application` class or main `Activity`:

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        LinkRunner.initialize(applicationContext)
    }
}
```

## Usage

```kotlin
// Get the instance
val linkRunner = LinkRunner.getInstance()

// Use SDK methods
val result = linkRunner.processMessage("Your message here")
```

## Sample App

The project includes a sample app that demonstrates how to use the SDK. To run it:

1. Open the project in Android Studio
2. Select the 'sample-app' run configuration
3. Run on an emulator or physical device

## Building the AAR

To build the AAR file:

```bash
./gradlew :linkrunner:assembleRelease
```

The AAR file will be available at:
`linkrunner/build/outputs/aar/linkrunner-release.aar`

## Documentation

For setup instructions, usage examples, and API reference, please visit:

[Linkrunner Android SDK Documentation](https://docs.linkrunner.io/sdk/android/installation)

## License

This project is licensed under the MIT License - see the [LICENSE](./LICENSE) file for details.

Copyright (c) 2025 Linkrunner Private Limited
