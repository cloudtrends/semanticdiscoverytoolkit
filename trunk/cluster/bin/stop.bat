@ECHO off

REM Stop the currently running cluster.

START /b run.bat org.sd.cluster.config.Admin -k
