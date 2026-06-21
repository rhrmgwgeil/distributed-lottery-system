#!/bin/sh
# Pin the JDK to version 21 for this project's compilation
export JAVA_HOME="/c/Program Files/Java/jdk-21.0.2"
export PATH="$JAVA_HOME/bin:$PATH"

# Execute the discovered system Maven executable directly
exec "/d/SI1396/apache-maven-3.9.9/bin/mvn" "$@"
