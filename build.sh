#!/bin/sh

cd HotWiredBridge/bin
rm -rf tmp
mkdir tmp
cd tmp
unzip ../../lib/wired.jar
cd ..
mv tmp/wired .
FILES=`find hotwiredbridge org wired -name '*.class' -print`
TARGET=../../hotwired.jar
rm "$TARGET"
jar cvfm "$TARGET" ../../hotwired.manifest $FILES
rm -rf tmp wired

