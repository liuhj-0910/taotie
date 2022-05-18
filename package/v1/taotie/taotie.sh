#!/bin/bash

SERVICE_NAME=taotie
PATH_TO_JAR=/pkg/taotie/taotie.jar
JAVA_OPTS="-Xms7g -Xmx7g -XX:MetaspaceSize=64m -XX:MaxMetaspaceSize=64m -XX:+PrintGCDetails -Xloggc:/var/log/taotie-gc.log"
CMD_OPTS=""
PID_PATH_NAME=`dirname ${PATH_TO_JAR}`/${SERVICE_NAME}.pid
LOGFILE=`dirname ${PATH_TO_JAR}`/${SERVICE_NAME}.log

mkdir -p `dirname ${PATH_TO_JAR}`/log

case $1 in
    start)
        echo "Starting $SERVICE_NAME ..."
        if [ ! -f $PID_PATH_NAME ]; then
            cd `dirname ${PATH_TO_JAR}`
            nohup java ${JAVA_OPTS} -jar $PATH_TO_JAR $CMD_OPTS >> $LOGFILE 2>&1 &
            echo "$!" > $PID_PATH_NAME
            echo "$SERVICE_NAME started ..."
        else
            echo "$SERVICE_NAME is already running ..."
        fi
		
		sh -c ./crontab-log.sh
    ;;
    stop)
        if [ -f $PID_PATH_NAME ]; then
            PID=$(cat $PID_PATH_NAME);
            echo "$SERVICE_NAME stoping ..."
            kill $PID > /dev/null 2>&1
            echo "$SERVICE_NAME stopped ..."
        else
            echo "$SERVICE_NAME is not running ..."
        fi
        ps aux | grep `basename $PATH_TO_JAR` | grep -v grep | awk '{print $2;}' | xargs kill -9 > /dev/null 2>&1
        rm -f $PID_PATH_NAME
		
		crontab -r
		
    ;;
    restart)
        $0 stop
        $0 start
    ;;
esac
