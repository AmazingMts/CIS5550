#!/bin/bash

PWD=`pwd`
kvsWorkers=1 # number of kvs workers to launch
flameWorkers=1 # number of flame workers to launch

rm -r worker1
rm *.jar

# Compile and create Indexer.jar
javac -cp lib/kvs.jar:lib/webserver.jar:lib/flame.jar -d bin --source-path src src/cis5550/jobs/Indexer.java
sleep 1
jar cf indexer.jar bin/cis5550/jobs/Indexer.class
sleep 1

# Compile and create PageRank.jar
javac -cp lib/kvs.jar:lib/webserver.jar:lib/flame.jar -d bin --source-path src src/cis5550/jobs/PageRank.java
sleep 1
jar cf pagerank.jar bin/cis5550/jobs/PageRank.class
sleep 1

# Compile all Java files
javac -cp lib/webserver.jar:lib/kvs.jar:lib/flame.jar --source-path src -d bin $(find src -name '*.java')

# Launch KVS Coordinator
echo "cd '$(PWD)'; java -cp bin:lib/webserver.jar:lib/kvs.jar cis5550.kvs.Coordinator 8000" > kvscoordinator.sh
chmod +x kvscoordinator.sh
open -a Terminal kvscoordinator.sh

sleep 2

# Launch KVS Workers
for i in `seq 1 $kvsWorkers`
do
    dir=worker$i
    if [ ! -d $dir ]
    then
        mkdir $dir
    fi
    echo "cd '$(PWD)'; java -cp bin:lib/webserver.jar:lib/kvs.jar cis5550.kvs.Worker $((8000+$i)) $dir localhost:8000" > kvsworker$i.sh
    chmod +x kvsworker$i.sh
    open -a Terminal kvsworker$i.sh
done

# Launch Flame Coordinator
echo "cd '$(PWD)'; java -cp bin:lib/webserver.jar:lib/kvs.jar:lib/flame.jar cis5550.flame.Coordinator 9000 localhost:8000" > flamecoordinator.sh
chmod +x flamecoordinator.sh
open -a Terminal flamecoordinator.sh

sleep 2

# Launch Flame Workers
for i in `seq 1 $flameWorkers`
do
    echo "cd '$(PWD)'; java -cp bin:lib/webserver.jar:lib/kvs.jar:lib/flame.jar cis5550.flame.Worker $((9000+$i)) localhost:9000" > flameworker$i.sh
    chmod +x flameworker$i.sh
    open -a Terminal flameworker$i.sh
done
