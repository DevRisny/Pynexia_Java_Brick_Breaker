@echo off
rem Compile and start Brick Breaker (needs JDK 17+)
cd /d "%~dp0"
if exist out rmdir /s /q out
dir /s /b src\*.java > sources.txt
javac --release 17 -d out @sources.txt
del sources.txt
java -cp out com.risny.brickbreaker.BrickBreaker
