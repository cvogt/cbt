@ECHO OFF

REM Launcher batch script that bootstraps CBT from source.
REM Some of the code for reporting missing dependencies and waiting for nailgun to come up is a bit weird.
REM This is inentionally kept as small as posible.
REM Welcomed improvements:
REM - reduce code size through better ideas
REM - reduce code size by moving more of this into type-checked Java/Scala code (if possible without performance loss).
REM - reduction of dependencies
REM - performance improvements

REM optional dependencies:
REM - ncat        https://nmap.org/
REM - nailgun     http://martiansoftware.com/nailgun/
REM - inotifywait https://github.com/thekid/inotify-win

REM 0 is true, 1 is false

REM TODO
REM   time_taken function
REM   separate log files for commands different from nailgun server


SETLOCAL EnableExtensions EnableDelayedExpansion

REM < INIT >
SET time_taken=1.1

SET SCALA_VER_MAJOR=2
SET SCALA_VER_MINOR=11
SET SCALA_VER_BUILD=8

SET TARGET_SCALA=target\scala-%SCALA_VER_MAJOR%.%SCALA_VER_MINOR%
SET TARGET=%TARGET_SCALA%\classes\


SET NAILGUN_PORT=4444
SET NG=ng --nailgun-port %NAILGUN_PORT%
SET NAILGUN=%CBT_HOME%\nailgun_launcher
SET NAILGUN_TARGET=%NAILGUN%\%TARGET%
SET NAILGUN_INDICATOR=%NAILGUN%\%TARGET_SCALA%\classes.last-success
SET nailgun_out=%NAILGUN_TARGET%nailgun.stdout.log
SET nailgun_err=%NAILGUN_TARGET%nailgun.stderr.log

IF NOT EXIST %NAILGUN_TARGET% ( MD %NAILGUN_TARGET% )
IF NOT EXIST %NAILGUN_INDICATOR% ( type nul > %NAILGUN_INDICATOR% )


REM - ng-server.jar absolute path
FOR /F "tokens=*" %%x in ('where ng-server.jar') DO SET NG_SERVER_JAR=%%x

REM - CWD current working directory
SET "CWD=%cd%"

SET CBT_LOOP_FILE=%CWD%\target\.cbt-loop.tmp
SET CBT_KILL_FILE=%CWD%\target\.cbt-kill.tmp

SET USER_PRESSED_CTRL_C=130
REM </ INIT >


REM < JAVA >
WHERE javac > NUL 2>&1
IF %ERRORLEVEL% NEQ 0 (
  ECHO You must install javac! CBT needs it to bootstrap from Java sources into Scala.
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
  ECHO You must install javac version 1.7 or greater!
  ECHO Current javac version is %javac_ver%
  GOTO :EOF
)
REM </ JAVA >


REM < NAILGUN >
SET nailgun_installed=0
WHERE ng > NUL 2>&1
IF %ERRORLEVEL% NEQ 0 (
  SET nailgun_installed=1
  CALL :log "You can install Nailgun to make CBT slightly faster." %*
)

IF "%NG_SERVER_JAR%" EQU "" (
  SET nailgun_installed=1
  CALL :log "You can install Nailgun Server to make CBT slightly faster." %*
)
REM </ NAILGUN >

REM < GPG >
SET gpg_installed=0
WHERE gpg > NUL 2>&1
IF %ERRORLEVEL% NEQ 0 (
  SET gpg_installed=1
  CALL :log "You need to install gpg for login." %*
)
REM </ GPG >


REM < NCAT >
SET nc_installed=0
WHERE ncat > NUL 2>&1
IF %ERRORLEVEL% NEQ 0 (
  SET nc_installed=1
  CALL :log "(Note: ncat not found. It will make slightly startup faster.)" %*
)

SET server_up=1
IF %nc_installed% EQU 0 (
  ncat -z -n -w 1 127.0.0.1 %NAILGUN_PORT% > NUL 2>&1
  SET server_up=!ERRORLEVEL!
)
REM </ NCAT >


REM < MAIN >
CALL :set_debug %*
IF NOT "%debug%"=="" SHIFT

SET JAVA_OPTS_CBT=%debug% -Xmx1536m -Xss10M "-XX:MaxJavaStackTraceDepth=-1" -XX:+TieredCompilation "-XX:TieredStopAtLevel=1" -Xverify:none


IF /I "%~1"=="kill" (
  ECHO Stopping background process (nailgun^)
  REM TODO >> "%NAILGUN_OUT%" 2>> "%NAILGUN_ERR%"
  %NG% ng-stop
  GOTO :EOF
)

SET use_nailgun=0
SET loop=1
SET clear_screen=1

REM https://superuser.com/questions/743197/how-to-shift-all-parameters-in-a-batch
SET "jvmArgs="

REM https://stackoverflow.com/a/3981086/4496364
:parse_args
IF NOT [%1]==[] (
  IF /I "%~1"=="direct" ( SET use_nailgun=1 ) ELSE (
    IF /I "%~1"=="loop" ( SET loop=0 ) ELSE (
      IF /I "%~1"=="clear" ( SET clear_screen=0 ) ELSE (
        SET ^"jvmArgs=%jvmArgs% %1"
      )
    )
  )
  SHIFT
  GOTO :parse_args
)

IF %nailgun_installed% EQU 1 ( SET use_nailgun=1 )
IF /I "%~1"=="publishSigned" ( SET use_nailgun=1 )

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
    START "" java %options% %JAVA_OPTS_CBT% -jar "%NG_SERVER_JAR%" 127.0.0.1:%NAILGUN_PORT% >> %nailgun_out% 2>> %nailgun_err%

    IF NOT "%debug%"=="" (
      ECHO "Started nailgun server in debug mode"
      GOTO :EOF
    )
  )
)


REM - need inotifywait for loop command
IF %loop% EQU 0 (
	WHERE inotifywait > NUL 2>&1
  IF %ERRORLEVEL% NEQ 0 (
		ECHO "Please install inotifywait to use cbt loop"
		GOTO :EOF
	)
)

:main_loop_while

	IF %clear_screen% EQU 0 ( CLS )
	IF EXIST %CBT_LOOP_FILE% ( DEL %CBT_LOOP_FILE% )
	IF EXIST %CBT_KILL_FILE% ( DEL %CBT_KILL_FILE% )

	CALL :stage1 %*

	IF NOT %loop% EQU 0 (
    CALL :log "not looping, exiting" %*
    GOTO :END
  )

  REM - exit_code is set in stage1
  IF %exit_code% EQU %USER_PRESSED_CTRL_C% (
		CALL :log "not looping, exiting" %*
    GOTO :END
  )

  REM nailgun_sources is set in stage1
  FOR %%f IN (%nailgun_sources%) DO (
    ECHO %%f >> "%CBT_LOOP_FILE%"
  )

  SET pids=
  IF EXIST "%CBT_KILL_FILE%" (
    REM FIXME: should we uniq here?
    SET pids=(type "%CBT_KILL_FILE%")
    REM DEL %CBT_LOOP_FILE%
    FOR %%p IN (%pids%) DO (
      IF %%p=="" ( ECHO warning: empty pid found in pid kill list ) ELSE (
        CALL :log "killing process %%p"
        TASKKILL /PID %%p /T
      )
    )
  )

  ECHO "Watching for file changes... (Ctrl+C short press for loop, long press for abort)"
  SET files=
  IF EXIST "%CBT_LOOP_FILE%" (
    REM https://stackoverflow.com/questions/3068929/how-to-read-file-contents-into-a-variable-in-a-batch-file
    FOR /f "delims=" %%f IN (%CBT_LOOP_FILE%) DO (
      IF %%f=="" ( ECHO warning: empty file found in loop file list )
      SET ^"files=!files! %%f"
    )
  )
  inotifywait -q !files!

GOTO :main_loop_while

GOTO :END
REM </ MAIN >


REM < FUNCTIONS >

REM first stage of CBT
:stage1
	CALL :log "Checking for changes in cbt\nailgun_launcher" %*

	SET changed=1
	SET nailgun_sources=%NAILGUN%\src\cbt\*.java %CBT_HOME%\libraries\common-0\src\cbt\reflect\*.java

  FOR %%f IN (%NAILGUN_INDICATOR%) DO (
    CALL :date_to_yyyy_mm_dd "%%~tf"
    SET ng_ind_last_mod=!date_formatted!
  )

  FOR %%f IN (%nailgun_sources%) DO (
    CALL :date_to_yyyy_mm_dd "%%~tf"
    SET ng_source_last_mod=!date_formatted!

    IF "!ng_source_last_mod!" GTR "!ng_ind_last_mod!" ( SET changed=0 )
  )

	SET exit_code=0
	IF %changed% EQU 0 (
		ECHO Stopping background process (nailgun^) if running
    REM TODO >> %nailgun_out% 2>> %nailgun_err%
		%NG% ng-stop

		REM DEL %NAILGUN_TARGET%cbt\*.class 2>NUL
    REM defensive delete of potentially broken class files
		ECHO Compiling cbt\nailgun_launcher
		javac -Xlint:deprecation -Xlint:unchecked -d %NAILGUN_TARGET% %nailgun_sources%
		SET exit_code=%ERRORLEVEL%
		IF %exit_code% EQU 0 (
			REM touch, set modified
      IF NOT EXIST %NAILGUN_INDICATOR% ( type nul > %NAILGUN_INDICATOR% ) ELSE (
        COPY /B %NAILGUN_INDICATOR% +,, %NAILGUN_INDICATOR%
      )
			IF %use_nailgun% EQU 0 (
				ECHO Starting background process (nailgun^)
				START "" java %options% %JAVA_OPTS_CBT% -jar %NG_SERVER_JAR% 127.0.0.1:%NAILGUN_PORT% >> %nailgun_out% 2>> %nailgun_err%
				CALL :sleep 1000
			)
		)
	)

	CALL :log "run CBT and loop if desired. This allows recompiling CBT itself as part of compile looping." %*

	IF NOT %exit_code% EQU 0 ( GOTO :EOF )

  IF NOT %use_nailgun% EQU 0 (
    CALL :log "Running JVM directly" %*
    SET options=%JAVA_OPTS%

    REM JVM options to improve startup time. See https://github.com/cvogt/cbt/pull/262
    java %options% %JAVA_OPTS_CBT% -cp %NAILGUN_TARGET% cbt.NailgunLauncher %time_taken% %CWD% %loop% %jvmArgs%
    SET exit_code=%ERRORLEVEL%
  ) ELSE (
    CALL :log "Running via background process (nailgun^)" %*
    FOR /L %%i IN (0,1,9) DO (
      CALL :log "Adding classpath." %*
      REM TODO >> %nailgun_out% 2>> %nailgun_err%
      %NG% ng-cp %NAILGUN_TARGET%

      CALL :log "Checking if nailgun is up yet." %*
      REM TODO >> %nailgun_out% 2>> %nailgun_err%
      %NG% cbt.NailgunLauncher check-alive
      SET alive=!ERRORLEVEL!

      IF "!alive!"=="33" (
        REM 33  is returned by NailgunLauncher on success
        REM 133 is returned by NailgunLauncher if it can't launch the class
        GOTO :break_for
      ) ELSE (
        CALL :log "Nope. Sleeping for 0.2 seconds" %*
        REM In case of problems try -Dlog=nailgun or check logs in cbt\nailgun_launcher\target\*.log
      )
      CALL :sleep 200
    )
    :break_for
    IF %%i EQU 9 (
      ECHO Nailgun call failed. Try 'cbt kill' and check the error log cbt\nailgun_launcher\target\nailgun.stderr.log
      EXIT /B %alive%
      GOTO :EOF
    )
    CALL :log "Running CBT via Nailgun." %*
    %NG% cbt.NailgunLauncher %time_taken% %CWD% %loop% %jvmArgs%
    SET exit_code=%ERRORLEVEL%
  )
  CALL :log "Done running CBT." %*

GOTO :EOF

REM - sets debug parameters
:set_debug
  SET debug=
  :set_debug_while
    REM - [%1]==[] tests if %1==null
    REM - CON is equiv to bash's /dev/stderr (more like display/console...)
    REM   https://superuser.com/questions/241272/windows-how-to-redirect-file-parameter-to-stdout-windows-equivalent-of-dev-s
    IF NOT [%1]==[] (
      IF "%~1"=="-debug" (
        SET debug="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005"
      ) ELSE IF "%~1"=="-Dlog=nailgun" (
        SET nailgun_out=CON
        SET nailgun_err=CON
      ) ELSE IF "%~1"=="-Dlog=all" (
        SET nailgun_out=CON
        SET nailgun_err=CON
      )
      SHIFT
      GOTO :set_debug_while
    )
GOTO :EOF

REM utility function to log message to stderr with stating the time
:log
	SET msg=%1
  REM strip surrounding "
  SET msg=%msg:"=%

	SET enabled=1
  :log_while
    REM id first parameter is not null
    IF NOT "%~1"=="" (
      SET param=%~1
      IF "!param!"=="-Dlog"  (	SET enabled=0	)
      REM These don't work because of EQUALS sign!
      REM IF "%param%"=="-Dlog=time"  (	SET enabled=0	)
      REM IF "%param%"=="-Dlog=bash"  (	SET enabled=0	)
      REM IF "%param%"=="-Dlog=all"   (	SET enabled=0	)
      SHIFT
      GOTO :log_while
    )
	IF %enabled% EQU 0 (
		SET delta=1
    REM delta=$(time_taken)
		ECHO [%delta%] %msg%
	)
GOTO :EOF

REM 02.12.2017. 22:25  -> 2017.12.02. 22:25
:date_to_yyyy_mm_dd
  SET d=%1
  REM strip "
  SET d=%d:"=%
  SET date_formatted=%d:~6,5%%d:~3,3%%d:~0,3%%d:~11,6%
GOTO :EOF

REM https://stackoverflow.com/a/735294/4496364
:sleep
  SET sleep_millis=%1
  ping 192.0.2.2 -n 1 -w %sleep_millis% > NUL
GOTO :EOF

REM </ FUNCTIONS >


:END
CALL :log "Exiting CBT" %*
ENDLOCAL
