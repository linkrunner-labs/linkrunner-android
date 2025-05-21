# Linkrunner Android SDK

A native Android SDK for integrating Linkrunner functionality into your Android applications.

## Requirements
- Android 5.0 (API level 21) and above
- Android Studio Flamingo (2022.2.1) or newer
- Gradle 8.0+

## Documentation
For setup instructions, usage examples, and API reference, please visit:
[Linkrunner Android SDK Documentation](https://docs.linkrunner.io/sdk/android/installation)

## Publishing to Maven Central

### Updating Version

Before publishing a new release, update the version in the following locations:

1. In `linkrunner/build.gradle` file:
   ```gradle
   defaultConfig {
       // ...
       versionName "x.y.z"  // Update this version
   }
   ```

2. In `linkrunner/src/main/java/com/linkrunner/sdk/LinkRunner.kt`:
   ```kotlin
   private val packageVersion = "x.y.z"  // Update this version
   ```

### Setting Up Credentials

Create a `.env` file in the project root with the following variables:

```
MAVEN_CENTRAL_USERNAME=your_username
MAVEN_CENTRAL_PASSWORD=your_password
GPG_PASSPHRASE=your_gpg_passphrase  # If using GPG signing
```

Make sure to set proper permissions on this file:
```bash
chmod 600 .env
```

### Building and Publishing

To build the Maven bundle:

```bash
./prepare-maven-bundle.sh
```

You can also specify the version directly from the command line:

```bash
./prepare-maven-bundle.sh 1.2.3
```

To build and publish in one step:

```bash
./prepare-maven-bundle.sh --publish
```

Or specify both version and publish flag:

```bash
./prepare-maven-bundle.sh 1.2.3 --publish
```

The script will handle:  
- Building the SDK  
- Creating the bundle with all required artifacts  
- Generating checksums and signatures  
- Publishing to Maven Central with automatic release (if --publish is used)

## License
This project is licensed under the MIT License - see the [LICENSE](./LICENSE) file for details.

Copyright (c) 2025 Linkrunner Private Limited
