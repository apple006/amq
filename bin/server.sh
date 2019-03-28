#!/bin/sh
clear

mvn clean compile -DskipTests=true -f pom.xml -P prod
#mvn exec:exec -Dexec.executable="java" -Dexec.args="-DsystemProperty1=value1 -DsystemProperty2=value2 -XX:MaxPermSize=256m -classpath %classpath com.artlongs.amq.core.AioMqServer"
#mvn exec:exec
#cd target/classes/com/artlongs/amq/core
mvn exec:java -Dexec.mainClass="com.artlongs.amq.core.AioMqServer" -Dexec.classpathScope=runtime

ps -ef | grep java | grep -v grep
# kill 进程
ps -ef | grep java | grep -v grep | awk '{print $2}' | xargs kill -9

#echo `ps -ef | grep java | grep -v AioMqServer | awk '{print $2}' `

echo shutdown | nc localhost 8888
echo done