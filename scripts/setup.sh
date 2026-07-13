#!/usr/bin/env bash
# Living World Engine — Development Setup
# In jeder neuen Shell ausführen: source scripts/setup.sh

JDK_HOME="$HOME/.local/share/jdk21"

if [ -d "$JDK_HOME" ]; then
  export JAVA_HOME="$JDK_HOME"
  export PATH="$JAVA_HOME/bin:$PATH"
  echo "✅ JAVA_HOME=$JAVA_HOME"
  java -version 2>&1 | head -1
else
  echo "⚠️  JDK nicht gefunden unter $JDK_HOME"
fi
