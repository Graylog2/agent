:: Graylog Collector service management for Windows.
::
:: ATTENTION
::
:: Please be REALLY careful when changing this script to make sure it runs
:: on different Windows versions. Especially quoting seems to be a problem.
:: See the history of this file for previous fixes.

@ECHO OFF

SETLOCAL ENABLEEXTENSIONS ENABLEDELAYEDEXPANSION
TITLE Graylog Collector Service ${project.version}

:: Check if JAVA_HOME environment variable is set.
IF DEFINED JAVA_HOME goto :dispatchAction

:: Exit if there is no JAVA_HOME set.
ECHO JAVA_HOME not set! 1>&2
EXIT /B 1

:dispatchAction
:: Get the directory of this script. NEEDS to run before argument parsing!
SET COLLECTOR_BIN_DIR=%~dp0

:: Get the ACTION and SERVICE_NAME from the script arguments.
IF "x%1x" == "xx" GOTO usage
SET ACTION=%1
SHIFT
IF "x%1x" == "xx" GOTO usage
SET SERVICE_NAME=%1

:: Get root directory of the Collector.
FOR %%D in ("%COLLECTOR_BIN_DIR%..") DO SET COLLECTOR_ROOT=%%~dpfD

:: Detect if we are running on 32bit or 64bit Windows.
"%JAVA_HOME%\bin\java.exe" -d32 -version 2>&1 | "%windir%\System32\find" "does not support" >nul:
IF errorlevel 1 (SET ARCH=x86) ELSE (SET ARCH=x64)

:: Use the correct executable based on the architecture.
SET PROCRUN=%COLLECTOR_BIN_DIR%\windows\graylog-collector-service-%ARCH%.exe

:: Dispatch the supported commands, show usage otherwise.
IF /i %ACTION% == install GOTO actionInstallCheck
IF /i %ACTION% == uninstall GOTO actionUninstall
IF /i %ACTION% == manage GOTO actionManage
IF /i %ACTION% == start GOTO actionStart
IF /i %ACTION% == stop GOTO actionStop

:usage
ECHO.
ECHO Usage: %~nx0 install^|uninstall^|manage^|start^|stop SERVICE_NAME
ECHO.
ECHO Example:
ECHO.
ECHO * Install Collector as service with service name "GC"
ECHO     graylog-collector-service.bat install GC
GOTO:EOF

:actionInstallCheck
ECHO Installing service for Graylog Collector %COLLECTOR_VERSION%
ECHO.
ECHO Service name: "%SERVICE_NAME%"
ECHO JAVA_HOME:    "%JAVA_HOME%"
ECHO ARCH:         "%ARCH%"
ECHO.

:: Select the correct JVM DLL.
SET JVM_DLL=%JAVA_HOME%\jre\bin\default\jvm.dll
IF EXIST "%JVM_DLL%" GOTO actionInstall
SET JVM_DLL=%JAVA_HOME%\jre\bin\server\jvm.dll
IF EXIST "%JVM_DLL%" GOTO actionInstall
SET JVM_DLL=%JAVA_HOME%\bin\server\jvm.dll
IF EXIST "%JVM_DLL%" GOTO actionInstall
SET JVM_DLL=%JAVA_HOME%\bin\client\jvm.dll

IF EXIST "%JVM_DLL%" (
ECHO WARNING: JAVA_HOME points to a JRE and not JDK installation; a client (not a server^) JVM will be used...
) ELSE (
ECHO ERROR: Invalid Java installation (no jvm.dll found in "%JAVA_HOME%"^). Abort...
GOTO:EOF
)

:actionInstall
SET COLLECTOR_VERSION=${project.version}
SET COLLECTOR_JAR=%COLLECTOR_ROOT%\${project.artifactId}.jar
SET COLLECTOR_CLASS=org.graylog.collector.cli.Main
SET COLLECTOR_JVM_MS=12m
SET COLLECTOR_JVM_MX=64m
SET COLLECTOR_JVM_OPTIONS=-Dfile.encoding=UTF-8 ${collector.jvm-opts}
SET COLLECTOR_CLASSPATH=%COLLECTOR_JAR%;%COLLECTOR_ROOT%\lib\sigar\*
SET COLLECTOR_STOP_TIMEOUT=0
SET COLLECTOR_STARTUP=auto
SET COLLECTOR_MODE=jvm
SET COLLECTOR_START_METHOD=main
SET COLLECTOR_START_PARAMS="run;-f;%COLLECTOR_ROOT%\config\collector.conf"
SET COLLECTOR_STOP_METHOD=stop
SET COLLECTOR_PID_FILE="%SERVICE_NAME%.pid"
SET COLLECTOR_LOG_DIR=%COLLECTOR_ROOT%\logs
SET COLLECTOR_LOG_OPTIONS=--LogPath "%COLLECTOR_LOG_DIR%" --LogPrefix "graylog-collector" --StdError auto --StdOutput auto

SET COLLECTOR_JVM_OPTIONS=%COLLECTOR_JVM_OPTIONS: =;%

"%PROCRUN%" //IS//%SERVICE_NAME% --Classpath "%COLLECTOR_CLASSPATH%" --Jvm "%JVM_DLL%" --JvmMs %COLLECTOR_JVM_MS% --JvmMx %COLLECTOR_JVM_MX% --JvmOptions %COLLECTOR_JVM_OPTIONS% --StartPath "%COLLECTOR_ROOT%" --Startup %COLLECTOR_STARTUP% --StartMode %COLLECTOR_MODE% --StartClass %COLLECTOR_CLASS% --StartMethod %COLLECTOR_START_METHOD% --StartParams %COLLECTOR_START_PARAMS% --StopMode %COLLECTOR_MODE% --StopClass %COLLECTOR_CLASS% --StopMethod %COLLECTOR_STOP_METHOD% --StopTimeout %COLLECTOR_STOP_TIMEOUT% --PidFile "%COLLECTOR_PID_FILE%" --DisplayName "Graylog Collector (%SERVICE_NAME%)" --Description "Graylog Collector %COLLECTOR_VERSION% service. See http://www.graylog.org/ for details." %COLLECTOR_LOG_OPTIONS%

IF NOT errorlevel 1 GOTO actionInstallSuccess
ECHO ERROR: Failed to install service: %SERVICE_NAME%
GOTO:EOF

:actionInstallSuccess
ECHO Service '%SERVICE_NAME%' has been installed
GOTO:EOF

:actionUninstall
:: Remove the service
"%PROCRUN%" //DS//%SERVICE_NAME% %COLLECTOR_LOG_OPTIONS%
IF NOT errorlevel 1 GOTO actionUninstallSuccess
ECHO ERROR: Failed to uninstall service: %SERVICE_NAME%
GOTO:EOF

:actionUninstallSuccess
ECHO Service '%SERVICE_NAME%' has been removed
GOTO:EOF

:actionManage
SET PRUNMGR=%COLLECTOR_BIN_DIR%\windows\graylog-collector-service-manager.exe
"%PRUNMGR%" //ES//%SERVICE_NAME%
IF NOT errorlevel 1 GOTO actionManageSuccess
ECHO ERROR: Failed to start service manager for service: %SERVICE_NAME%
GOTO:EOF

:actionManageSuccess
ECHO Successfully ran service manager for service: %SERVICE_NAME%
GOTO:EOF

:actionStart
"%PROCRUN%" //ES//%SERVICE_NAME% %COLLECTOR_LOG_OPTIONS%
IF NOT errorlevel 1 GOTO actionStartSuccess
ECHO ERROR: Failed to start service: %SERVICE_NAME%
GOTO:EOF

:actionStartSuccess
ECHO Service '%SERVICE_NAME%' has been started
GOTO:EOF

:actionStop
"%PROCRUN%" //SS//%SERVICE_NAME% %COLLECTOR_LOG_OPTIONS%
IF NOT errorlevel 1 GOTO actionStopSuccess
ECHO ERROR: Failed to stop service: %SERVICE_NAME%
GOTO:EOF

:actionStopSuccess
ECHO Service '%SERVICE_NAME%' has been stopped
GOTO:EOF

ENDLOCAL
