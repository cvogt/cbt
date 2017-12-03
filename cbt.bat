@ECHO OFF
REM Launcher batch script that bootstraps CBT from source.
REM Some of the code for reporting missing dependencies and waiting for nailgun to come up is a bit weird.
REM This is inentionally kept as small as posible.
REM Welcomed improvements to this file:
REM - reduce code size through better ideas
REM - reduce code size by moving more of this into type-checked Java/Scala code (if possible without performance loss).
REM - reduction of dependencies
REM - performance improvements

REM optional dependencies: 
REM - ncat      https://nmap.org/
REM - nailgun   http://martiansoftware.com/nailgun/

REM 0 is true, 1 is false

REM TODO 
REM   time_taken function
REM   fswatch
REM   kill


SETLOCAL EnableExtensions EnableDelayedExpansion

REM < INIT >
SET time_taken=1.1

SET SCALA_VER_MAJOR=2
SET SCALA_VER_MINOR=11
SET SCALA_VER_BUILD=8

SET TARGET=target\scala-%SCALA_VER_MAJOR%.%SCALA_VER_MINOR%\classes\

SET NG_SERVER_JAR=ng-server.jar
SET NAILGUN_PORT=4444
SET NG=ng --nailgun-port %NAILGUN_PORT%
SET NAILGUN=%CBT_HOME%\nailgun_launcher
SET NAILGUN_TARGET=%NAILGUN%\%TARGET%
SET NAILGUN_INDICATOR=%NAILGUN_TARGET%..\classes.last-success
SET NAILGUN_OUT=%NAILGUN_TARGET%nailgun.stdout.log
SET NAILGUN_ERR=%NAILGUN_TARGET%nailgun.strerr.log
MD %NAILGUN_TARGET%

REM - CWD current working directory
SET "CWD=%cd%"

SET CBT_LOOP_FILE=%CWD%\target\.cbt-loop.tmp
SET CBT_KILL_FILE=%CWD%\target\.cbt-kill.tmp

SET USER_PRESSED_CTRL_C=130
REM </ INIT >


REM < JAVA >
WHERE javac > NUL 2>&1
IF %ERRORLEVEL% NEQ 0 (
  ECHO You need to install javac! CBT needs it to bootstrap from Java sources into Scala.
  GOTO :EOF
)

REM - ensure that javac >= 1.7.x
FOR /F "tokens=2" %%i IN ('javac -version 2^>^&1') DO ( SET javac_ver=%%i )
FOR /F "delims=. tokens=1-3" %%v IN ("%javac_ver%") DO (
  SET javac_ver_major=%%v
  SET javac_ver_minor=%%w
  SET javac_ver_build=%%x
)
IF javac_ver_minor LSS 8 (
  ECHO You need to install javac version 1.7 or greater!
  ECHO Current javac version is %javac_ver%
  GOTO :EOF
)
REM </ JAVA >


REM < NAILGUN >
SET nailgun_installed=0
WHERE ng > NUL 2>&1
IF %ERRORLEVEL% NEQ 0 (
  SET nailgun_installed=1
  ECHO You need to install Nailgun to make CBT slightly faster. > NUL 2>&1
)
WHERE %NG_SERVER_JAR% > NUL 2>&1
IF %ERRORLEVEL% NEQ 0 (
  SET nailgun_installed=1
  ECHO You need to install Nailgun Server to make CBT slightly faster.  
)
REM </ NAILGUN >


REM < GPG >
SET gpg_installed=0
WHERE gpg > NUL 2>&1
IF %ERRORLEVEL% NEQ 0 (
  SET gpg_installed=1
  ECHO You need to install gpg for login.
)
REM </ GPG >


REM < NCAT >
SET nc_installed=0
WHERE ncat > NUL 2>&1
IF %ERRORLEVEL% NEQ 0 (
  SET nc_installed=1
  ECHO (Note: ncat not found. It will make slightly startup faster.)
)

SET server_up=1
IF nc_installed EQU 0 (
  ncat -z -n -w 1 127.0.0.1 %NAILGUN_PORT% > NUL 2>&1
  SET server_up=%ERRORLEVEL%
)
REM </ NCAT >


REM < MAIN >
CALL :set_debug %*
if not "%debug%"=="" shift

SET JAVA_OPTS_CBT=%debug% -Xmx1536m -Xss10M "-XX:MaxJavaStackTraceDepth=-1" -XX:+TieredCompilation "-XX:TieredStopAtLevel=1" -Xverify:none


IF /I "%~1"=="kill" (
  ECHO Stopping background process (nailgun^)
  %NG% ng-stop >> "%NAILGUN_OUT%" 2>> "%NAILGUN_ERR%"
  REM EXIT 1
  GOTO :EOF
)

SET use_nailgun=0
IF /I "%~1"=="direct" (
	SET use_nailgun=1
	SHIFT
)

SET loop=1
IF /I "%~1"=="loop" (
	SET loop=0
	SHIFT
)

SET clear_screen=1
IF /I "%~1"=="clear" (
	SET clear_screen=0
	SHIFT
)

REM there is no OR operator for batch scripts...
IF %nailgun_installed% EQU 1  SET use_nailgun=1
IF /I "%~1"=="publishSigned"  SET use_nailgun=1

REM there is no AND operator for batch scripts...
IF %use_nailgun% EQU 0 (
  IF %server_up% EQU 0 (
    IF NOT "%debug%"=="" (
      ECHO Cant use `-debug` (without `direct`^) when nailgun is already running. 
      ECHO   If you started it up with `-debug` you can still connect to it. Otherwise use `cbt kill` to kill it.
      GOTO :EOF
    )
  ) ELSE (
    CALL :log "Starting background process (nailgun)" %*
    REM try to start nailgun-server, just in case it's not up
    java %options% %JAVA_OPTS_CBT% -jar %NG_SERVER_JAR% 127.0.0.1:%NAILGUN_PORT% >> %nailgun_out% 2>> %nailgun_err%
     
    echo STARTAnje nailguna
    IF NOT "%debug%"=="" (
      ECHO "Started nailgun server in debug mode"
      GOTO :EOF
    )
  )
)

REM - need fswatch for loop command
if %loop% equ 0 (
	WHERE fswatch > NUL 2>&1
  IF %ERRORLEVEL% NEQ 0 (
		echo "Please install fswatch to use cbt loop"
		GOTO :EOF
	)
)

:main_loop_while
	if %clear_screen% EQU 0 ( CLS )
  
	if exist %CBT_LOOP_FILE% ( DEL %CBT_LOOP_FILE% )
  
	if exist %CBT_KILL_FILE% ( DEL %CBT_KILL_FILE% )
  
	CALL :stage1 %*
  
	if not %loop% equ 0 (
    CALL :log "not looping, exiting" %*
    GOTO :END
  )
  REM - exit_code is set in stage1
  if %exit_code% EQU %USER_PRESSED_CTRL_C% (
		CALL :log "not looping, exiting" %*
    GOTO :END
  )
  
  REM nailgun_sources is set in stage1
  for file in "%nailgun_sources%" DO (
    echo "%file%" >> "%CBT_LOOP_FILE%"
  )
  SET files=
  if exist "%CBT_LOOP_FILE%" (
    SET files=(sort "%CBT_LOOP_FILE%")
  )
  
  SET pids=
  if exist "%CBT_KILL_FILE%" (
    REM FIXME: should we uniq here?
    SET pids=(type "%CBT_KILL_FILE%") 
    REM DEL %CBT_LOOP_FILE%
  )
  echo ""
  echo "Watching for file changes... (Ctrl+C short press for loop, long press for abort)"
  for %%f in "%files%" DO (
    if %%f=="" ( echo warning: empty file found in loop file list )
  )
  fswatch --one-event "%files%"
  for %%p in "%pids%" DO (
    if %%p=="" ( echo warning: empty pid found in pid kill list )
    else (
      CALL :log "killing process %%p"
      kill -KILL %%p
    )
  )
GOTO :main_loop_while

GOTO :END
REM </ MAIN >


REM < FUNCTIONS >

REM first stage of CBT
:stage1
	CALL :log "Checking for changes in cbt\nailgun_launcher" %*
	
	SET changed=1
	SET nailgun_sources=%NAILGUN%\src\cbt\*.java %CBT_HOME%\libraries\common-0\src\cbt\reflect\*.java
  
  for %%f in (%NAILGUN_INDICATOR%) do (
    CALL :date_to_yyyy_mm_dd "%%~tf"
    SET ng_ind_last_mod=!date_formatted!
    ECHO ng_indicator %%f
  )
  
  for %%f in (%nailgun_sources%) do (
    CALL :date_to_yyyy_mm_dd "%%~tf"
    SET ng_source_last_mod=!date_formatted!
    if "!ng_source_last_mod!" GTR "!ng_ind_last_mod!" ( SET changed=0 )
    ECHO nailgun_source %%f
  )
  
	SET exit_code=0
	if %changed% EQU 0 (
		echo Stopping background process (nailgun^) if running
		%NG% ng-stop >> %nailgun_out% 2>> %nailgun_err%
		REM DEL %NAILGUN_TARGET%cbt\*.class 2>NUL
    REM defensive delete of potentially broken class files
		echo Compiling cbt\nailgun_launcher
		javac -Xlint:deprecation -Xlint:unchecked -d %NAILGUN_TARGET% %nailgun_sources%
		SET exit_code=%ERRORLEVEL%
		if %exit_code% EQU 0 (
			REM touch, set modified
      copy /b %NAILGUN_INDICATOR% +,, %NAILGUN_INDICATOR%
			if %use_nailgun% EQU 0 (
				echo Starting background process (nailgun^)
				java %options% %JAVA_OPTS_CBT% -jar %NG_SERVER_JAR% 127.0.0.1:%NAILGUN_PORT% >> %nailgun_out% 2>> %nailgun_err%
				CALL :sleep 1000
			)
		)
	)

	CALL :log "run CBT and loop if desired. This allows recompiling CBT itself as part of compile looping." %*

	if not %exit_code% EQU 0 ( GOTO :EOF )
  
  if not %use_nailgun% EQU 0 (
    CALL :log "Running JVM directly" %*
    SET options=%JAVA_OPTS%
    
    REM JVM options to improve startup time. See https://github.com/cvogt/cbt/pull/262
    java %options% %JAVA_OPTS_CBT% -cp %NAILGUN_TARGET% cbt.NailgunLauncher %time_taken% %CWD% %loop% %*
    SET exit_code=%ERRORLEVEL%
  ) else (
    CALL :log "Running via background process (nailgun)" %*
    for /L %%i in (0,1,9) do (
      CALL :log "Adding classpath." %*
      %NG% ng-cp %NAILGUN_TARGET% >> %nailgun_out% 2>> %nailgun_err%
      CALL :log "Checking if nailgun is up yet." %*
      %NG% cbt.NailgunLauncher check-alive >> %nailgun_out% 2>> %nailgun_err%
      SET alive=%ERRORLEVEL%
      if "%alive%"=="33" (
        REM 33 is sent by NailgunLauncher on success
        REM if Nailgun can't launch the class, it's returning
        REM 133 which triggers the else branch
        GOTO :break_for
      ) else (
        CALL :log "Nope. Sleeping for 0.2 seconds" %*
        REM if %%i GTT 1  (
        REM	echo Waiting for nailgun to start... 
        REM   (In case of problems try -Dlog=nailgun or check logs in cbt\nailgun_launcher\target\*.log^)
        REM )
      )
      CALL :sleep 200
    )
    :break_for
    if %%i EQU 9 (
      echo Nailgun call failed. Try 'cbt kill' and check the error log cbt\nailgun_launcher\target\nailgun.stderr.log
      REM exit %alive%
      GOTO :EOF
    )
    CALL :log "Running CBT via Nailgun." %*
    %NG% cbt.NailgunLauncher %time_taken% %CWD% %loop% %*
    SET exit_code=%ERRORLEVEL%
  )
  CALL :log "Done running CBT." %*
	
GOTO :EOF

REM - sets debug parameters
:set_debug
  SET debug=
  :set_debug_while
    REM [%1]==[] tests if %1==null
    if not [%1]==[] (
      if "%~1"=="-debug" (
        SET debug="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005"
      ) else if "%~1"=="-Dlog=nailgun" (
        SET nailgun_out=2
        SET nailgun_err=2
      ) else if "%~1"=="-Dlog=all" (
        SET nailgun_out=2
        SET nailgun_err=2
      )
      shift
      goto :set_debug_while
    )
GOTO :EOF

REM utility function to log message to stderr with stating the time
:log
	SET msg=%1
  REM strip "
  SET msg=%msg:"=%
  
	SET enabled=1
  :log_while
    REM [%1]==[] tests if %1==null
    if not [%1]==[] (
      REM TODO maybe use string contains: https://stackoverflow.com/a/7006016/4496364
      SET param=%~1
      if "%param%"=="-Dlog=time"  (	SET enabled=0	)
      if "%param%"=="-Dlog=bash"  (	SET enabled=0	)
      if "%param%"=="-Dlog=all"   (	SET enabled=0	)
      REM if not "x!param:-Dlog=!"=="x!param!" (
      REM   if not "x!param:time=!"=="x!param!" (	SET enabled=0	)
      REM   if not "x!param:bash=!"=="x!param!" (	SET enabled=0	)
      REM   if not "x!param:all=!"=="x!param!"  ( SET enabled=0	)
      REM )
      shift
      goto :log_while
    )
	if %enabled% EQU 0 (
		SET delta=1
    REM delta=$(time_taken)
		echo [%delta%] %msg%
	)
GOTO :EOF

REM 02.12.2017. 22:25  -> 2017.12.02. 22:25
:date_to_yyyy_mm_dd
SET d=%1
REM strip "
set d=%d:"=%
SET date_formatted=%d:~6,5%%d:~3,3%%d:~0,3%%d:~11,6%
GOTO :EOF

REM https://stackoverflow.com/a/735294/4496364
:sleep
SET sleep_millis=%1
ping 192.0.2.2 -n 1 -w %sleep_millis% > nul
GOTO :EOF

REM </ FUNCTIONS >


:END
CALL :log "Exiting CBT" %*
ENDLOCAL
REM exit %exit_code%
