#!/bin/bash

APPIP=172.25.147.148
echo > ab-test.log

for a in {1..1000}
do
  ab -n 1000 -c 2 -k "http://${APPIP}:8080/report1?userId=${a}&msg=sagasioiuiosljnlsuaJSJSAJKJ0sagasioiuiosljnlsuaJSJSAJKJ0sagasioiuiosljnlsuaJSJSAJKJ0sagasioiuiosljnlsuaJSJSAJKJ0${a}" >> ab-test.log & > /dev/null 2>&1
  
  if [ $((${a}%10)) -eq 0 ]; then
    echo ${a}
    #sleep 1
  fi
done
