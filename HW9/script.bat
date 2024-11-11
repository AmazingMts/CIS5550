@echo off
:: Script for WINDOWS users

set kvsWorkers=1
set flameWorkers=1

:: Clean up from previous runs
rmdir /s /q worker1
del *.jar

:: Compile and create Indexer.jar
javac -cp "lib\kvs.jar;lib\webserver.jar;lib\flame.jar" -d bin --source-path src src\cis5550\jobs\Indexer.java
jar cf indexer.jar -C bin cis5550\jobs\Indexer.class

:: Compile and create PageRank.jar
javac -cp "lib\kvs.jar;lib\webserver.jar;lib\flame.jar" -d bin --source-path src src\cis5550\jobs\PageRank.java
jar cf pagerank.jar -C bin cis5550\jobs\PageRank.class

:: Compile all Java files
javac -cp "lib\webserver.jar;lib\kvs.jar;lib\flame.jar" --source-path src -d bin src\cis5550\*.java src\cis5550\kvs\*.java src\cis5550\flame\*.java

:: Launch KVS Coordinator
(
    echo cd %cd%
    echo java -cp bin;lib\webserver.jar;lib\kvs.jar cis5550.kvs.Coordinator 8000
) > kvscoordinator.bat
start cmd.exe /k kvscoordinator.bat

:: Launch KVS Workers
setlocal enabledelayedexpansion
for /l %%i in (1,1,%kvsWorkers%) do (
    set "dir=worker%%i"
    if not exist !dir! mkdir !dir!
    (
        echo cd %cd%\!dir!
        echo java -cp ..\bin;..\lib\webserver.jar;..\lib\kvs.jar cis5550.kvs.Worker 800%%i !dir! localhost:8000
    ) > kvsworker%%i.bat
    start cmd.exe /k kvsworker%%i.bat
)

:: Launch Flame Coordinator
(
    echo cd %cd%
    echo java -cp bin;lib\webserver.jar;lib\kvs.jar;lib\flame.jar cis5550.flame.Coordinator 9000 localhost:8000
) > flamecoordinator.bat
start cmd.exe /k flamecoordinator.bat

:: Launch Flame Workers
for /l %%i in (1,1,%flameWorkers%) do (
    (
        echo cd %cd%
        echo java -cp bin;lib\webserver.jar;lib\kvs.jar;lib\flame.jar cis5550.flame.Worker 900%%i localhost:9000
    ) > flameworker%%i.bat
    start cmd.exe /k flameworker%%i.bat
)

endlocal
