@ECHO OFF
REM Launcher bash script that bootstraps CBT from source.
REM (Some of the code for reporting missing dependencies and waiting for nailgun to come up is a bit weird.)
REM This is inentionally kept as small as posible.
REM Welcome improvements to this file:
REM - reduce code size through better ideas
REM - reduce code size by moving more of this into type-checked Java/Scala code (if possible without performance loss).
REM - reduction of dependencies
REM - performance improvements

REM utility function to log message to stderr with stating the time
setlocal EnableExtensions EnableDelayedExpansion

where javac > nul 2>&1

if ERRORLEVEL 1 (
    ECHO You need to install javac! CBT needs it to bootstrap from Java sources into Scala.
    GOTO :EOF
)

javac -version > %CBT_HOME%temp.txt 2>&1
SET /p javac_version_output=<%CBT_HOME%temp.txt

FOR /f "tokens=2" %%G IN ("%javac_version_output%") DO SET javac_version=%%G 

FOR /f "tokens=2 delims=." %%G IN ("%javac_version%") DO SET javac_version_minor=%%G 

IF javac_version_minor LSS 8 (
	echo You need to install javac version 1.7 or greater!
	echo Current javac version is %javac_version
	GOTO :EOF
)

SET nailgun_installed=0

where ng > nul 2>&1

if ERRORLEVEL 1 (
	ECHO You need to install Nailgun to make CBT slightly faster. > nul 2>&1
) ELSE ( SET nailgun_installed=1 )

where ng-server > nul 2>&1

if ERRORLEVEL 1 (
    ECHO You need to install Nailgun to make CBT slightly faster.
) ELSE ( SET nailgun_installed=1 )

IF %nailgun_installed%==0 ECHO Note: nailgun not found. It makes CBT faster! > nul 2>&1

where gpg > nul 2>&1
SET gpg_installed=0
if ERRORLEVEL 1 (
    ECHO You need to install gpg for login.
) ELSE ( SET gpg_installed=0 )



SET NAILGUN_PORT=4444
SET NG=ng --nailgun-port %NAILGUN_PORT%

CHDIR > %CBT_HOME%temp.txt 2>&1
SET /p CWD=<%CBT_HOME%temp.txt

SET CBT_HOME=%~dp0

SET SCALA_VERSION=2.11.8

SET NAILGUN=%CBT_HOME%nailgun_launcher\
SET STAGE1=%CBT_HOME%stage1\
SET TARGET=target\scala-2.11\classes\

mkdir %NAILGUN%%TARGET% > nul 2>&1
mkdir %STAGE1%%TARGET%  > nul 2>&1

SET nailgun_out=%NAILGUN%\target\nailgun.stdout.log
SET nailgun_err=%NAILGUN%\target\nailgun.strerr.log

where nc > nul 2>&1

SET nc_installed=1
SET server_up=0

IF ERRORLEVEL 1 (
    ECHO Note: nc not found. It will make slightly startup faster.
) ELSE ( 
	nc -z -n -w 1 127.0.0.1 %NAILGUN_PORT% > nul 2>&1
	SET server_up=1
)

IF "%1%"=="kill" (
	echo Stopping nailgun 1>&2
	%NG% ng-stop >> %nailgun_out 2>> %nailgun_err%
	GOTO :EOF
)

SET use_nailgun=1

SET res=0
IF %nailgun_installed%==0 SET res=1
IF "%1%"=="publishSigned" SET res=1
IF "%2%"=="publishSigned" SET res=1
IF "%1%"=="direct"        SET res=1
IF "%2%"=="direct"		  SET res=1

IF %res%==1 SET use_nailgun=0

IF %use_nailgun%==1 IF NOT %server_up%==1 (
	REM try to start nailgun-server, just in case it's not up
	ng-server 127.0.0.1:%NAILGUN_PORT >> %nailgun_out 2>> %nailgun_err
)

:RUN
	CALL :stage1 %*
	IF NOT "%1%"=="loop" (
		GOTO :EOF
	) ELSE (
		GOTO :RUN
		echo "======= Restarting CBT =======" 1>&2
	)

:stage1
SETLOCAL
SET NAILGUN_INDICATOR=%NAILGUN%%TARGET%cbt\NailgunLauncher.class
SET changed=0

FOR %%i IN (%NAILGUN%*.java) DO (
	FOR /F %%z IN ('DIR /B /O:D %%i%% %NAILGUN_INDICATOR% 2^> nul') DO SET NEWEST=%%z
 	if "%NEWEST:~-4%"=="java" ( SET changed=1 )

)

IF %changed%==1 (
	REM defensive delete of potentially broken class files
	REM DEL /s /q /f %NAILGUN%%TARGET%cbt\*.class > nul 2>&1 

	echo Compiling cbt/nailgun_launcher
	
	CD %NAILGUN%
	
	dir /B /A:-D *.java > %CBT_HOME%temp.txt
	
	CD %CWD%
	
	FOR /F "tokens=*" %%j in (%CBT_HOME%temp.txt) do SET "files=!files! %NAILGUN%%%j"

	ECHO javac -Xlint:deprecation -d %NAILGUN%%TARGET% !files!
	GOTO :EOF
	
	IF ERRORLEVEL 1 (
		REM triggers recompilation next time.
		DEL %NAILGUN%TARGET/cbt/*.class 2> NUL REM triggers recompilation next time.
		GOTO :eof
	)
	
	IF %use_nailgun%==0 (
		REM echo "Stopping nailgun" 1>&2
		%NG% ng-stop >> %nailgun_out% 2>> %nailgun_err%
		REM echo "Restarting nailgun" 1>&2
		ng-server 127.0.0.1:%NAILGUN_PORT% >> %nailgun_out% 2>> %nailgun_err%
	)
)


IF %use_nailgun%==0 java -cp %NAILGUN%%TARGET% cbt.NailgunLauncher %CWD% %*
IF %use_nailgun%==0 GOTO :ENDIF

REM FOR %%i in ("0 1 2 3 4 5 6 7 8 9") do (
%NG% ng-cp %NAILGUN%%TARGET% >> %nailgun_out% 2>> %nailgun_err%
%NG% cbt.NailgunLauncher check-alive >> %nailgun_out% 2>> %nailgun_err%

SET isAlive=0
IF %errorlevel%==131 SET isAlive=1
IF %errorlevel%==33  SET isAlive=1


IF %isAlive%==1 (
	GOTO :EOF
) ELSE (
	IF %%i GTR 1 echo Waiting for nailgun to start... (For problems try -Dlog=nailgun or check logs in cbt/nailgun_launcher/target/*.log) 1>&2
)

SLEEP 0.3

REM )
%NG% cbt.NailgunLauncher "%CWD%" %*
ENDLOCAL
:ENDIF
:ENDPROGRAM
ENDLOCAL
DEL %CBT_HOME%temp.txt > NUL 1>&2
