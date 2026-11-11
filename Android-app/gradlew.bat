@if "%DEBUG%"=="" @echo off
@rem ##########################################################################
@rem
@rem  Gradle startup script for Windows
@rem
@rem ##########################################################################

if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
set DBG_CMD=
set CMD_LINE_ARGS=
:winCommandLoop
if %1.==. goto execute

set CMD_LINE_ARGS=%CMD_LINE_ARGS% %1
shift
goto winCommandLoop

:execute
@rem Setup the command line

set CLASSPATH=%DIRNAME%\gradle\wrapper\gradle-wrapper.jar

@rem Determine JAVA_EXE
set JAVACMD=
if exist "%USERPROFILE%\.gradle\jdks\eclipse_adoptium-21-amd64-windows.2\bin\java.exe" (
    set "JAVACMD=%USERPROFILE%\.gradle\jdks\eclipse_adoptium-21-amd64-windows.2\bin\java.exe"
)
if "%JAVACMD%"=="" if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\java.exe" set "JAVACMD=%JAVA_HOME%\bin\java.exe"
)
if "%JAVACMD%"=="" (
    set JAVACMD=java.exe
)

"%JAVACMD%" -Xmx64m -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %CMD_LINE_ARGS%
if %ERRORLEVEL% NEQ 0 goto mainEnd

:mainEnd
if "%OS%"=="Windows_NT" endlocal
