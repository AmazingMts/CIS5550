#!/bin/bash

# This script can be run by typing "bash script.sh" in the terminal 

kvsWorkers=1  # number of kvs workers to launch
flameWorkers=2  # number of flame workers to launch

javac -cp lib/webserver.jar:lib/kvs.jar --source-path src -d bin $(find src -name '*.java')

echo "cd $(pwd); java -cp bin:lib/webserver.jar:lib/kvs.jar cis5550.kvs.Coordinator 8000" > kvscoordinator.sh
chmod +x kvscoordinator.sh
open -a Terminal kvscoordinator.sh

for i in `seq 1 $kvsWorkers`
do
    dir=worker$i
    if [ ! -d $dir ]
    then
        mkdir $dir
    fi
    echo "cd $(pwd); java -cp bin:lib/webserver.jar:lib/kvs.jar cis5550.kvs.Worker $((8000+$i)) $dir localhost:8000" > kvsworker$i.sh
    chmod +x kvsworker$i.sh
    open -a Terminal kvsworker$i.sh
done

echo "cd $(pwd); java -cp bin:lib/webserver.jar:lib/kvs.jar cis5550.flame.Coordinator 9000 localhost:8000" > flamecoordinator.sh
chmod +x flamecoordinator.sh
open -a Terminal flamecoordinator.sh

for i in `seq 1 $flameWorkers`
do
    echo "cd $(pwd); java -cp bin:lib/webserver.jar:lib/kvs.jar cis5550.flame.Worker $((9000+$i)) localhost:9000" > flameworker$i.sh
    chmod +x flameworker$i.sh
    open -a Terminal flameworker$i.sh
done
