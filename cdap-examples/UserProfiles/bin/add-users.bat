@echo OFF

REM ############################################################################
REM ## Copyright (C) 2016 Cask Data, Inc.
REM ##
REM ## Licensed under the Apache License, Version 2.0 (the "License"); you may
REM ## not use this file except in compliance with the License. You may obtain a
REM ## copy of the License at
REM ##
REM ## http://www.apache.org/licenses/LICENSE-2.0
REM ##
REM ## Unless required by applicable law or agreed to in writing, software
REM ## distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
REM ## WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
REM ## License for the specific language governing permissions and limitations
REM ## under the License.
REM ##
REM ############################################################################

REM Set the base directory
for %%i in ("%~dp0..\") do (SET $APP_HOME=%%~dpi)

REM Set path for curl.exe
set $PATH=%PATH%;%APP_HOME%\..\..\libexec\bin

REM Process access token
set $ACCESS_TOKEN=
set $ACCESS_TOKEN_FILE=%HOMEPATH%\.cdap.accesstoken
if exist %$ACCESS_TOKEN_FILE% set /p $ACCESS_TOKEN=<%$ACCESS_TOKEN_FILE%

set $TABLE=profiles
set $ENDPOINT=v3/namespaces/default/apps/UserProfiles/services/UserProfileService/methods/%$TABLE%

SETLOCAL EnableDelayedExpansion

REM Set parameters
set $ACTION=%1
set $HOST=%2
set $ERROR=

if not DEFINED $ACTION set $ERROR=Action (either 'add' or 'delete') must be set
if DEFINED $ERROR goto :USAGE

goto :PROGRAM

:USAGE
SET PROGRAM_NAME=%0 
echo Tool for adding to or deleting users from the '%$TABLE%' table
echo Usage: !PROGRAM_NAME! add ^| delete ^[host^]
echo:
echo Options
echo     add       Add users to the '%$TABLE%' table
echo     delete    Delete users from the '%$TABLE%' table
echo     host      Specifies the host that CDAP is running on (default: localhost)
echo:
if DEFINED $ERROR echo Error: !$ERROR!
set $ERROR=
goto :FINALLY

:PROGRAM
if DEFINED $HOST set $GATEWAY=!$HOST!
if not DEFINED $GATEWAY set $GATEWAY=localhost
FOR /F "tokens=*" %%G IN (!$APP_HOME!resources\users.txt) DO (
    set $BODY=%%G
    set $BODY=!$BODY:"='!
    for /F "tokens=1 delims=," %%H IN ('echo !$BODY!') DO set $USERID=%%H
    set $USERID=!$USERID:~9,-1!
    if /I "!$ACTION!"=="add" call :SET_ADD
    if /I "!$ACTION!"=="delete" call :SET_DELETE
    set $AUTH=
    if DEFINED $ACCESS_TOKEN set $AUTH=-H "Authorization: Bearer !$ACCESS_TOKEN!"
    set $URL="http://!$GATEWAY!:10000/%$ENDPOINT%/!$USERID!"
    set $COMMAND=curl -qfsw "%%{http_code}" !$AUTH! !$CURLX! !$URL!
    for /F "tokens=* USEBACKQ" %%F IN (`!$COMMAND!`) DO (
        set $RESULTS=%%F
    )
    if /I not "!$RESULTS!"=="!$EXPECTED!" echo Failed to !$ACTION!: return code !$RESULTS!
)
goto :FINALLY

:SET_ADD
set $CURLX=-X PUT -d"!$BODY!"
set $EXPECTED=201
goto :eof

:SET_DELETE
set $CURLX=-X DELETE
set $EXPECTED=200
goto :eof

:FINALLY
