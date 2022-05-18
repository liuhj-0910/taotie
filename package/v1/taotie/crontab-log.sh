#!/bin/bash

TAOTIE_LOG=/pkg/taotie/log

rm -rf ${TAOTIE_LOG}/*.log

crontab -r

cat << EOF > ${TAOTIE_LOG}/crontab
* * * * * date && netstat -anl|grep 3306| awk '{print $6}'|sort |uniq -c >> ${TAOTIE_LOG}/taotie-netstat.log

* * * * * date && cat /proc/interrupts >> ${TAOTIE_LOG}/taotie-interrupts.log

* * * * * date && cat /proc/softirqs >> ${TAOTIE_LOG}/taotie-softirqs.log

* * * * * date && cat /proc/$(cat /pkg/taotie/taotie.pid)/status >> ${TAOTIE_LOG}/taotie-pidstatus.log

* * * * * date && ifstat 5 10 >> ${TAOTIE_LOG}/taotie-ifstat.log

* * * * * date && pidstat -u -d -r -w -p $(cat /pkg/taotie/taotie.pid) 5 10 >> ${TAOTIE_LOG}/taotie-pidstat.log

* * * * * date && vmstat 5 10 >> ${TAOTIE_LOG}/taotie-vmstat.log

* * * * * date && mpstat -P ALL 5 10 >> ${TAOTIE_LOG}/taotie-mpstat.log

* * * * * date && sar -u -r  -B -W -d -p -b -n DEV 5 10 >> ${TAOTIE_LOG}/taotie-sar.log

* * * * * uptime >> ${TAOTIE_LOG}/taotie-uptime.log

* * * * * free -h >> ${TAOTIE_LOG}/taotie-free.log
EOF

crontab ${TAOTIE_LOG}/crontab


