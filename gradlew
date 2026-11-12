#!/bin/sh
#
# Gradle start up script for POSIX generated for JARVIS-AI-ANDROID
#
APP_HOME=$( cd "${0%/*}" && pwd -P ) || exit
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

if [ -n "$JAVA_HOME" ] ; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi

if command -v gradle >/dev/null 2>&1 ; then
    exec gradle "$@"
else
    exec "$JAVACMD" -Xmx64m -Xms64m -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
fi
