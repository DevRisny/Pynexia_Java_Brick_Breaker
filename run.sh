#!/usr/bin/env sh
# Compile and start Brick Breaker (needs JDK 17+)
set -e
cd "$(dirname "$0")"
rm -rf out && javac --release 17 -d out $(find src -name "*.java")
java -cp out com.risny.brickbreaker.BrickBreaker
