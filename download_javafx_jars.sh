#!/bin/bash

set -e

VERSION=25

# jmods/mac-m1-jars
rm -rf jmods/mac-m1-jars
mkdir -p jmods
curl -L https://download2.gluonhq.com/openjfx/${VERSION}/openjfx-${VERSION}_osx-aarch64_bin-sdk.zip -o sdk.zip
unzip -o sdk.zip -d jmods/mac-m1-jars
rm sdk.zip
mv jmods/mac-m1-jars/javafx-sdk-${VERSION}/lib/* jmods/mac-m1-jars/
rm -rf jmods/mac-m1-jars/javafx-sdk-${VERSION}