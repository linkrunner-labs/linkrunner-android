# LinkRunner Android SDK

LinkRunner is a native Android SDK that provides essential functionality for your Android applications.

## Features

- Easy initialization and setup
- Thread-safe singleton pattern
- Sample application included

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

## License

```
Copyright 2023 Your Name

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
