#!/bin/bash

# Build and prepare Maven bundle for Central Publisher Portal

# Parse command-line arguments
VERSION_ARG=""
PUBLISH_FLAG=false

# Process all command-line arguments
for arg in "$@"; do
  if [[ "$arg" == "--publish" ]]; then
    PUBLISH_FLAG=true
  elif [[ -z "$VERSION_ARG" ]]; then
    # First non-flag argument is treated as the version
    VERSION_ARG="$arg"
  fi
done

# Get version - either from command line or prompt
if [ -n "$VERSION_ARG" ]; then
  # Use the command-line argument as version
  VERSION="$VERSION_ARG"
  echo "Using version from command line: $VERSION"
else
  # Get the version from build.gradle as default
  GET_VERSION=$(grep -e 'versionName' linkrunner/build.gradle | head -1 | sed 's/.*versionName "\(.*\)".*/\1/')

  # Prompt for version confirmation or entry
  echo -n "Enter version number for this release [$GET_VERSION]: "
  read VERSION_INPUT

  # Use input version or default from build.gradle
  VERSION=${VERSION_INPUT:-$GET_VERSION}
fi

# Validate version format (basic semver check)
if ! [[ $VERSION =~ ^[0-9]+\.[0-9]+\.[0-9]+(-[0-9A-Za-z-]+(\.[0-9A-Za-z-]+)*)?$ ]]; then
  echo "ERROR: Invalid version format. Please use semantic versioning (e.g. 1.2.3)"
  exit 1
fi

echo ""
echo "Building version: $VERSION"
echo ""

OUTPUT_DIR="maven-bundle"
ARTIFACT_ID="android-sdk"
GROUP_ID_PATH="io/linkrunner"
ARTIFACT_PATH="$GROUP_ID_PATH/$ARTIFACT_ID/$VERSION"
TARGET_DIR="$OUTPUT_DIR/$ARTIFACT_PATH"

# Clean previous builds
echo "Cleaning previous builds..."
rm -rf "$OUTPUT_DIR"
rm -f linkrunner-maven-bundle.zip

# Run gradle build to generate all required artifacts
echo "Building SDK version $VERSION..."
./gradlew clean :linkrunner:assembleRelease :linkrunner:sourcesJar :linkrunner:javadocJar :linkrunner:generatePomFileForReleasePublication

# Create directory structure for Maven bundle
echo "Creating Maven bundle directory structure..."
mkdir -p "$TARGET_DIR"

# Copy AAR file
echo "Copying AAR file..."
cp "linkrunner/build/outputs/aar/linkrunner-release.aar" "$TARGET_DIR/$ARTIFACT_ID-$VERSION.aar"

# Copy POM file 
echo "Copying POM file..."
cp "linkrunner/build/publications/release/pom-default.xml" "$TARGET_DIR/$ARTIFACT_ID-$VERSION.pom" 2>/dev/null
if [ ! -f "$TARGET_DIR/$ARTIFACT_ID-$VERSION.pom" ]; then
  echo "ERROR: POM file not found!"
  exit 1
fi

# Copy JAR files
echo "Copying Javadoc and Sources JARs..."
cp "linkrunner/build/libs/linkrunner-javadoc.jar" "$TARGET_DIR/$ARTIFACT_ID-$VERSION-javadoc.jar" 2>/dev/null
cp "linkrunner/build/libs/linkrunner-sources.jar" "$TARGET_DIR/$ARTIFACT_ID-$VERSION-sources.jar" 2>/dev/null

# Create checksums for all files
echo "Generating checksums..."
cd "$TARGET_DIR"
for file in *.*; do
  if [[ "$file" != *".asc" && "$file" != *".md5" && "$file" != *".sha1" ]]; then
    echo "Creating checksums for $file"
    md5sum "$file" | cut -d' ' -f1 > "$file.md5"
    shasum -a 1 "$file" | cut -d' ' -f1 > "$file.sha1"
  fi
done
cd - > /dev/null

# Try to create signatures if gpg is available
if command -v gpg &> /dev/null; then
  echo "Generating signatures..."
  cd "$TARGET_DIR"
  
  # Check if GPG_PASSPHRASE environment variable is set
  if [ -n "$GPG_PASSPHRASE" ]; then
    echo "Using GPG passphrase from environment variable"
    for file in *.*; do
      if [[ "$file" != *".asc" && "$file" != *".md5" && "$file" != *".sha1" ]]; then
        echo "Signing $file"
        echo "$GPG_PASSPHRASE" | gpg --batch --passphrase-fd 0 -ab "$file" || echo "Warning: Could not sign $file"
      fi
    done
  else
    echo "No GPG_PASSPHRASE environment variable found, using interactive mode"
    for file in *.*; do
      if [[ "$file" != *".asc" && "$file" != *".md5" && "$file" != *".sha1" ]]; then
        echo "Signing $file"
        gpg -ab "$file" || echo "Warning: Could not sign $file"
      fi
    done
  fi
  
  cd - > /dev/null
else
  echo "Warning: GPG not found, skipping signature generation"
  echo "You will need to manually create .asc signature files before uploading"
fi

# Create the final zip bundle
echo "Creating final zip bundle..."
cd "$OUTPUT_DIR"
zip -r "../linkrunner-maven-bundle.zip" "."
cd - > /dev/null

echo ""
echo "Maven bundle created at linkrunner-maven-bundle.zip"
echo "The bundle contains:"
find "$TARGET_DIR" -type f | sort

# Check for .env file and create it if it doesn't exist
ENV_FILE=".env"
if [ ! -f "$ENV_FILE" ]; then
  echo "Creating $ENV_FILE file. Please edit it with your Maven Central credentials."
  cat > "$ENV_FILE" << EOF
# Maven Central API credentials
MAVEN_CENTRAL_USERNAME=your_username
MAVEN_CENTRAL_PASSWORD=your_password
EOF
  chmod 600 "$ENV_FILE"  # Restrict permissions for security
  echo "$ENV_FILE file created. Please edit it with your credentials before publishing."
  echo "Then run this script again with the --publish flag."
  exit 0
fi

# Function to publish to Maven Central using the Central Publisher API
publish_to_maven_central() {
  echo "Publishing to Maven Central..."
  
  # Load credentials from .env file
  if [ -f "$ENV_FILE" ]; then
    source "$ENV_FILE"
  else
    echo "ERROR: $ENV_FILE not found!"
    exit 1
  fi
  
  # Check if credentials are set
  if [ -z "$MAVEN_CENTRAL_USERNAME" ] || [ -z "$MAVEN_CENTRAL_PASSWORD" ]; then
    echo "ERROR: Maven Central credentials not set in $ENV_FILE!"
    exit 1
  fi
  
  # Central Publisher API endpoint with query parameters
  # name: human-readable name for the deployment
  # publishingType: AUTOMATIC for validation and auto-publishing if passed
  DEPLOYMENT_NAME="io.linkrunner:android-sdk:$VERSION"
  
  # Function to URL encode a string
  urlencode() {
    local string="$1"
    local strlen=${#string}
    local encoded=""
    local pos c o
    
    for (( pos=0; pos<strlen; pos++ )); do
      c=${string:$pos:1}
      case "$c" in
        [-_.~a-zA-Z0-9] ) o="$c" ;;
        * )               printf -v o '%%%02x' "'$c" ;;
      esac
      encoded="$encoded$o"
    done
    echo "$encoded"
  }
  
  # URL encode the deployment name
  if command -v jq &> /dev/null; then
    # Use jq if available (faster and more reliable)
    ENCODED_NAME=$(echo -n "$DEPLOYMENT_NAME" | jq -sRr @uri)
  else
    # Fallback to custom function if jq is not available
    ENCODED_NAME=$(urlencode "$DEPLOYMENT_NAME")
  fi
  
  API_ENDPOINT="https://central.sonatype.com/api/v1/publisher/upload?name=$ENCODED_NAME&publishingType=AUTOMATIC"
  
  echo "Uploading linkrunner-maven-bundle.zip to Maven Central using Publisher API..."
  echo "Deployment Name: $DEPLOYMENT_NAME"
  echo "Publishing Type: AUTOMATIC"
  
  # Upload the bundle using curl
  # Note: The API expects multipart/form-data with a part named 'bundle'
  RESPONSE=$(curl --request POST \
    --verbose \
    --header "Authorization: Basic $(echo -n "$MAVEN_CENTRAL_USERNAME:$MAVEN_CENTRAL_PASSWORD" | base64)" \
    --form bundle=@linkrunner-maven-bundle.zip \
    "$API_ENDPOINT" 2>&1)
  
  CURL_EXIT_CODE=$?
  if [ $CURL_EXIT_CODE -eq 0 ]; then
    # Check if the response contains a successful status code (2xx)
    HTTP_STATUS=$(echo "$RESPONSE" | grep -oE "HTTP/[0-9.]+ [0-9]+" | tail -1 | awk '{print $2}')
    if [[ "$HTTP_STATUS" =~ ^2[0-9][0-9]$ ]]; then
      echo "Upload successful!"
      echo "Bundle has been published to Maven Central."
      echo "Please allow some time for it to be processed and released."
      
      # Try to extract and display the upload ID if available
      UPLOAD_ID=$(echo "$RESPONSE" | grep -o '"uploadId":[^,}]*' | cut -d":" -f2 | tr -d '"\r')
      if [ -n "$UPLOAD_ID" ]; then
        echo "Upload ID: $UPLOAD_ID"
      fi
    else
      echo "ERROR: Upload failed with HTTP status $HTTP_STATUS"
      echo "Response:"
      echo "$RESPONSE"
      exit 1
    fi
  else
    echo "ERROR: Upload failed with curl exit code $CURL_EXIT_CODE"
    echo "Response:"
    echo "$RESPONSE"
    exit 1
  fi
}

# Check if we should publish
if [ "$PUBLISH_FLAG" = true ]; then
  publish_to_maven_central
else
  echo ""
  echo "Maven bundle has been created at linkrunner-maven-bundle.zip"
  echo "To publish to Maven Central, run: $0 --publish"
fi
