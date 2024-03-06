#!/bin/bash

# Remove line 32 from Dockerfile
sed -i '32d' Dockerfile

# Create the directory if it doesn't exist
mkdir -p libs/gradle

# Download Gradle distribution and save it to the specified directory
wget -P libs/gradle https://services.gradle.org/distributions/gradle-8.5-bin.zip
