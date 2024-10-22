:: This script sets up and launches KVS and Flame coordinators and workers for WINDOWS users

@echo off
set kvsWorkers=1
set flameWorkers=2

REM Compile Java code
for %%f in (src\*.java) do (
    javac -cp lib\webserver.jar;lib\kvs.jar --source-path src -d bin %%f
)

REM Launch kvs coordinator
(
    echo cd %cd%
    echo java -cp bin;lib\webserver.jar;lib\kvs.jar cis5550.kvs.Coordinator 8000
) > kvscoordinator.bat

REM Verify kvs coordinator batch file content
type kvscoordinator.bat

REM Launch kvs coordinator
start cmd.exe /k kvscoordinator.bat

REM Enable delayed expansion
setlocal enabledelayedexpansion

REM Launch kvs workers
for /l %%i in (1,1,%kvsWorkers%) do (
    set dir=worker%%i
    if not exist !dir! mkdir !dir!
    (
        echo cd %cd%
        set /a port=8000+%%i
        echo java -cp bin;lib\webserver.jar;lib\kvs.jar cis5550.kvs.Worker !port! !dir! localhost:8000
    ) > kvsworker%%i.bat
    start cmd.exe /k kvsworker%%i.bat
)

REM Disable delayed expansion
endlocal

REM Launch flame coordinator
(
    echo cd %cd%
    echo java -cp bin;lib\webserver.jar;lib\kvs.jar cis5550.flame.Coordinator 9000 localhost:8000
) > flamecoordinator.bat
start cmd.exe /k flamecoordinator.bat

REM Enable delayed expansion
setlocal enabledelayedexpansion

REM Launch flame workers
for /l %%i in (1,1,%flameWorkers%) do (
    (
        echo cd %cd%
        set /a port=9000+%%i
        echo java -cp bin;lib\webserver.jar;lib\kvs.jar cis5550.flame.Worker !port! localhost:9000
    ) > flameworker%%i.bat
    start cmd.exe /k flameworker%%i.bat
)

REM Disable delayed expansion
endlocal

