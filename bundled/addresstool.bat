@echo off
rem -- Detect current dir and App home --
set OLD_DIR=%CD%
set TOOL_HOME=%~dp0..%

start %JAVA_HOME%/javaw.exe -jar AddressToolApp.jar 

