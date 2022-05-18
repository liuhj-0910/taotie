#!/bin/bash

systemctl stop mysql 
unlink /usr/local/mysql > /dev/null 2>&1
rm -rf /usr/local/mysql*
rm -rf /data
rm -rf /etc/my.cnf
rm -rf /etc/init.d/mysql.server
rm -rf /tmp/install.log
rm -rf /tmp/mypass.txt