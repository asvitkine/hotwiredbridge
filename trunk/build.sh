#!/bin/sh

cd HotWiredBridge/bin
FILES=`find hotwiredbridge org -name '*.class' -print`
TARGET=../../hotwired.jar
rm "$TARGET"
jar cvfm "$TARGET" ../../hotwired.manifest $FILES

