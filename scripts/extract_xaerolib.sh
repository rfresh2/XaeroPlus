#!/bin/bash

set -e

if [ "$#" -ne 3 ]; then
    echo "Usage: $0 <input_jar_path> <mc_version> <mod_loader>"
    echo "  mod_loader: fabric, forge, or neoforge"
    exit 1
fi

INPUT_JAR="$1"
MC_VERSION="$2"
MOD_LOADER="$3"

if [[ "$MOD_LOADER" != "fabric" && "$MOD_LOADER" != "forge" && "$MOD_LOADER" != "neoforge" ]]; then
    echo "Error: mod_loader must be 'fabric', 'forge', or 'neoforge'."
    exit 1
fi

if [ ! -f "$INPUT_JAR" ]; then
    echo "Error: Input file not found at '$INPUT_JAR'"
    exit 1
fi

TEMP_DIR=$(mktemp -d)
trap 'echo "Cleaning up temp directory..." &>/dev/null; rm -rf "$TEMP_DIR"' EXIT

echo "Extracting META-INF from $INPUT_JAR to search for nested jar..."
unzip -q "$INPUT_JAR" "META-INF/**" -d "$TEMP_DIR"

CANDIDATE_JARS=($(find "$TEMP_DIR/META-INF" -name "xaerolib-${MOD_LOADER}-*.jar"))

if [ ${#CANDIDATE_JARS[@]} -eq 0 ]; then
    echo "Error: No matching xaerolib jar found for mod loader '$MOD_LOADER'."
    exit 1
fi

if [ ${#CANDIDATE_JARS[@]} -gt 1 ]; then
    echo "Error: Multiple matching xaerolib jars found. Aborting."
    printf " - %s\n" "${CANDIDATE_JARS[@]}"
    exit 1
fi

NESTED_JAR_PATH="${CANDIDATE_JARS[0]}"
NESTED_JAR_FILENAME=$(basename "$NESTED_JAR_PATH")

echo "Found nested jar: $NESTED_JAR_FILENAME"

# Filename format: xaerolib-<mod_loader>-<lib_mc_version>-<lib_version>.jar
# We need to extract <lib_version>
# Remove prefix 'xaerolib-<mod_loader>-'
temp_version_string="${NESTED_JAR_FILENAME#xaerolib-${MOD_LOADER}-}"
# Remove suffix '.jar'
temp_version_string="${temp_version_string%.jar}"
# The lib version is the part after the last hyphen
LIB_VERSION="${temp_version_string##*-}"

if [ -z "$LIB_VERSION" ]; then
    echo "Error: Could not extract library version from filename: $NESTED_JAR_FILENAME"
    exit 1
fi

LIB_MC_VERSION="${temp_version_string%-$LIB_VERSION}"

echo "Extracted lib version: $LIB_VERSION"
echo "Detected lib MC version: $LIB_MC_VERSION (using provided $MC_VERSION for upload)"

VERSION="${LIB_VERSION}+${MC_VERSION}"
ARTIFACT_ID="xaerolib-${MOD_LOADER}"

echo "Uploading to Maven with the following details:"
echo "  GroupId:    com.github.rfresh2"
echo "  ArtifactId: $ARTIFACT_ID"
echo "  Version:    $VERSION"
echo "  Repository: maven.2b2t.vc"
echo "  File:       $NESTED_JAR_FILENAME"

read -p "Proceed with upload? (y/N) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Aborting upload."
    exit 1
fi

echo "Proceeding with upload..."
mvn deploy:deploy-file \
    -DgroupId=com.github.rfresh2 \
    -DrepositoryId=maven.2b2t.vc \
    -Durl=https://maven.2b2t.vc/releases \
    -Dversion="$VERSION" \
    -DartifactId="$ARTIFACT_ID" \
    -Dfile="$NESTED_JAR_PATH"

echo "Upload complete."
