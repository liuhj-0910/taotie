# taotie
高并发，高吞吐，数据上报。

taotie第一个版本，不做任何调参优化，基本上是默认配置，看看能跑到多少tps。

先准备3台机器，配置4C8G。有小伙伴说了我没有这么多高配置的机器怎么办？上阿里云啊。3台4C8G的机器也要好多钱，我没有啊？不要998，不要98，只要4毛，哥告诉你怎么拥有。

现在在阿里云申请个账号，充值150元（不是说4毛吗？怎么150了?按时间租用后付费，阿里云要求余额不能少于100。大家放心，阿里云不是ofo，可以退的。我猜的，我没真正退过。）
申请ECS -> 抢占式实例，3台4C8G规格计算型（原独享） sn1机器，只要 0.383 /时。做个实验1小时够了。

机器准备好了，开始安装程序了。
| 机器           | 备注   |
| :----------- | :--- |
| mysql        |      |
| springboot应用 |      |
| ab测试         |      |

建议设置下hostname：

```shell
hostnamectl set-hostname mysql

hostnamectl set-hostname application

hostnamectl set-hostname apachebench
```

安装前说明：

1、mysql安装包太大，github没有上传，需要去官网自行下载，我下载的版本是mysql-5.7.37-linux-glibc2.12-x86_64.tar.gz，放入\taotie\package\v1\mysql

2、mvn clean package，将打好包的taotie.jar放入\taotie\package\v1\taotie

3、将目录mysql、taotie、ab压缩为mysql.zip、taotie.zip、ab.zip



mysql安装：

```shell
##安装必要软件
yum install -y lrzsz dos2unix unzip

mkdir -p /pkg && cd /pkg

##上传mysql.zip
rz -b

unzip mysql.zip && cd mysql && dos2unix *.sql *.sh && chmod +x *.sh 
./mysql-install.sh 

##环境变量生效
source /etc/profile

#安装完成后，验证
##默认用户和密码已写入my.config
$mysql  
SQL> use lhj;
SQL> show tables;
##会看到已创建好64张表

##如果需要卸载mysql
./mysql-remove.sh

```

springboot应用安装：

```shell
##安装必要软件
yum install -y lrzsz dos2unix unzip java-1.8.0-openjdk*

mkdir -p /pkg && cd /pkg

##上传taotie.zip
rz -b

unzip taotie.zip && cd taotie && dos2unix *.sh && chmod +x *.sh 

#设置本地host mysql的连接地址
vi /etc/hosts
172.25.147.147  mysql.frag.com

#启动 推荐使用restart，会kill掉之前启动的应用
./taotie.sh restart

```



ab测试脚本安装：

```shell
##安装必要软件
yum install -y lrzsz dos2unix unzip

##安装ab
yum install -y httpd-tools

mkdir -p /pkg && cd /pkg

##上传ab.zip
rz -b

unzip ab.zip
cd ab
dos2unix *.sh && chmod +x *.sh 

vi ab-test.sh
##设置为你的应用程序的ip
APPIP=172.25.147.148  

#启动
./taotie.sh restart

##查看日志,有业务日志，数据上报记录，gc日志，操作系统各种指标日志，用与支持后续系统参数调优的依据。
cd ./log  

#如需停止测试脚本
./kill-ab-test.sh

```





taotie第一个版本虽然没做定制优化，但并不是从基础开始慢慢演进的，起点是有一点高度的，应该可以支持上万并发的。

简单说一下业务，模拟打点数据上报。可以是用户、车辆行动踪迹上报，也可以是机器设备状态上报。不做具体业务细化。上报主体抽象为userID，上报数据抽象为一个字符串。表设计如下：

```sql
CREATE TABLE report_0 
(
    id bigint(20) NOT NULL AUTO_INCREMENT,
    user_id bigint(20) NOT NULL COMMENT 'userID',
    msg varchar(255) COMMENT '上报打点数据',
    create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY IDX_REPORT_0_USER (user_id)
);
```

表在user_id加了一个普通索引。

MySQL按user_id做hash分表，先分64张表。

mysql没有挂多个磁盘，没有做内核参数调整。

但为了保证接近真实生产标准，mysql还是开启了双1配置和binlog。

MySQL配置：

```xml
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
```



说一下taotie项目的设计方案：



采用MySQL批量插入的方式。

先做个检测测试说明一下批量插入的必要性。

#### 每条sql一个事务
```sql
INSERT INTO report_0 (user_id, msg) VALUES (8, 'safghfhsjhskkjklkslklkaslkklsaklsa');
INSERT INTO report_0 (user_id, msg) VALUES (16, 'safghfhsjhskkjklkslklkaslkklsaklsa');
INSERT INTO report_0 (user_id, msg) VALUES (96, 'safghfhsjhskkjklkslklkaslkklsaklsa');
```

#### 多条sql一个事务

```sql
begin
INSERT INTO report_0 (user_id, msg) VALUES (8, 'safghfhsjhskkjklkslklkaslkklsaklsa');
INSERT INTO report_0 (user_id, msg) VALUES (16, 'safghfhsjhskkjklkslklkaslkklsaklsa');
INSERT INTO report_0 (user_id, msg) VALUES (96, 'safghfhsjhskkjklkslklkaslkklsaklsa');
commit;
```

#### 批量插入

```sql
INSERT INTO report_0 (user_id, msg) VALUES 
('8', 'safghfhsjhskkjklkslklkaslkklsaklsa'),
('16', 'safghfhsjhskkjklkslklkaslkklsaklsa'),
('96', 'safghfhsjhskkjklkslklkaslkklsaklsa');
```

3种方式各insert1万数据。

1，2方式，insert性能基本上一个数量级的，差别不大，插入1万数据需要122秒。对于业务侧的区别是一次网络传输，还是多次的区别：

```mysql
(root@localhost) [lhj]> select 'report_0', count(1), max(create_time) - min(create_time) from report_0;
+----------+----------+-------------------------------------+
| report_0 | count(1) | max(create_time) - min(create_time) |
+----------+----------+-------------------------------------+
| report_0 |    10000 |                                 122 |
+----------+----------+-------------------------------------+
1 row in set (0.01 sec)
```

方式3：插入1万数据只需0.10秒，相差1000倍。

```mysql
(root@localhost) [lhj]> source insert-3-1w.sql;
Query OK, 10000 rows affected (0.11 sec)
Records: 10000  Duplicates: 0  Warnings: 0

(root@localhost) [lhj]> source insert-3-1w.sql;
Query OK, 10000 rows affected (0.09 sec)
Records: 10000  Duplicates: 0  Warnings: 0

(root@localhost) [lhj]> source insert-3-1w.sql;
Query OK, 10000 rows affected (0.10 sec)
Records: 10000  Duplicates: 0  Warnings: 0

(root@localhost) [lhj]> source insert-3-1w.sql;
Query OK, 10000 rows affected (0.10 sec)
Records: 10000  Duplicates: 0  Warnings: 0

(root@localhost) [lhj]> source insert-3-1w.sql;
Query OK, 10000 rows affected (0.09 sec)
Records: 10000  Duplicates: 0  Warnings: 0
```



应用程序设计要点：

采用springboot + mybatis + druid开发。

```java
TaotieActuator<T> {
      /**
     * @param queuePower 该值为2的幂数，计算2的幂后的结果为内部保存数据队列的数量。该数量与业务分库分表的表数量一致。
     * @param benchCommitNum  批量提交数量
     * @param pollThreadPower 该值为2的幂数，计算2的幂后的结果为用于从数据队列中出队数据的线程数
     * @param commitThreadNum 批量提交线程数
     * @param offerTimeout    数据入队的超时时间，单位为毫秒
     * @param pollTimeout     数据出队的超时时间，单位为毫秒
     * @param benchCommit     批量提交数据的业务方法
     * @param getPartitionKey 从数据对象T中获取分区键的方法
     */
    public TaotieActuator(
      int queuePower, 
      int benchCommitNum, 
      int pollThreadPower, 
      int commitThreadNum, 
      float queueCapacityFactor, 
      int offerTimeout, 
      int pollTimeout, 
      Consumer<List<T>> benchCommit, 
      Function<T, Number> getPartitionKey)
}
```

上面是TaotieActuator<T>构造方法，举个具体例子说明一下：

```xml
taotie: 
  queuePower: 6
  pollThreadPower: 1
  benchCommitNum: 4000
  commitThreadNum: 10
  queueCapacityFactor: 1.0
  offerTimeout: 10
  pollTimeout: 100
```

数据库分64张表，对应queuePower=6，有64个队列，每个队列对应一张表。

有2个线程用来处理64个队列的出队操作，则pollThreadPower=1，每个线程处理32个队列。

每个队列出队4000条数据或100毫秒没有数据出队则批量提交一次。则benchCommitNum=4000，pollTimeout=100

队列在不停的入队和出队数据，如果入队和出队速度是匹配的，那么队列长度是可以很短的，但是实际上出队速度会比入队慢，随意为了保证入队不阻塞，需要给队列设置一个的容量，benchCommitNum * queueCapacityFactor就是每个队列的容量，理想情况在入队不阻塞的情况下，队列容量越小越好，节约堆内存。当入队阻塞超过10毫秒，会直接返回并打印warn日志，可以用来手动补偿，offerTimeout=10。



第一个版本已经到了业务与高并发数据上报框架解耦，Lambda表达式benchCommit是批量提交数据的业务方法，Lambda表达式getPartitionKey是从数据对象T中获取分区键的方法。



备注：这个版本代码还是有很多可以优化的地方，后面支持支持集群，k8s部署等等...



现在看看测试结果吧？

我测试的结果，单机可以到接近20000的并发数据入库。

具体分析后面写吧，第一次写博客台费劲了，想起了高中时写作文。















