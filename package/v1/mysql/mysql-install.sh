#!/bin/bash

rootpass=123
install_dir=/usr/local
mydatadir=/data/mysql_data

yum remove -y mariadb* > /dev/null 2>&1
echo 'remove mariadb'

rpm -q libaio > /dev/null || yum install -y libaio > /dev/null 2>&1
echo 'install libaio'

if id -g mysql > /dev/null 2>&1
then
    echo 'mysql group exists'
else
    groupadd mysql > /dev/null 2>&1
    echo 'create mysql group'
fi

if id -u mysql > /dev/null 2>&1
then
    echo 'mysql user exists'
else
    useradd -r -g mysql -s /sbin/nologin mysql > /dev/null 2>&1
    echo 'create mysql user'
fi

[ -d $mydatadir ] || mkdir -p $mydatadir

mysql_package=$(/usr/bin/ls mysql*.tar.gz)
tar zxf ${mysql_package} -C ${install_dir}
cd ${install_dir}
ln -s $(/usr/bin/ls -d mysql*) mysql
echo $PATH|grep "/usr/local/mysql/bin" || echo "export PATH=$PATH:/usr/local/mysql/bin" >> /etc/profile
source /etc/profile

cat << EOF > /etc/my.cnf
[client]
user = root
#password

[mysql]
prompt=(\\\u@\\\h) [\\\d]>\\\_

[mysqld]
port=3306
user = mysql
socket = /tmp/mysql.sock
datadir = /data/mysql_data
log_error = error.log
server-id = 001
log_bin = binlog
sync_binlog = 1
innodb_flush_log_at_trx_commit = 1
EOF

cd mysql

mkdir mysql-files
chmod 750 mysql-files
chown mysql:mysql .
bin/mysqld --initialize-insecure --user=mysql > /tmp/install.log
[ $? -eq 0 ] && echo "Installing mysql-server success"
chown -R root .
cp support-files/mysql.server /etc/init.d/mysql.server

systemctl enable mysql.server > /dev/null 2>&1
systemctl start mysql.server
echo 'start mysql server'

cat << EOF > /tmp/set-pass.sql
set password='${rootpass}';
EOF

mysql < /tmp/set-pass.sql
rm -rf /tmp/set-pass.sql

sed -i "s/#password/password = ${rootpass}/" /etc/my.cnf

mysql < /pkg/mysql/lhj.sql
