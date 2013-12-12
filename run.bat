@echo off
SETLOCAL enabledelayedexpansion
set java.system.class.loader=com.mucommander.commons.file.AbstractFileClassLoader

set cp=tmp/main

FOR /R %%F IN (*.jar) DO (
  SET cp=!cp!;%%F%
)

echo CLASSPATH is: %cp%

java -cp "%cp%" com.mucommander.Launcher

ENDLOCAL