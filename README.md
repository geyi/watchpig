# watchpig

// TODO 版本

// TODO 介绍

## 下载 & 打包
```shell script
git clone https://github.com/geyi/watchpig.git
cd watchpig
mvn clean package
cd target
```
打包后在target目录下会生成一个watchpig.zip文件

## 安装

主机名和IP地址

| 主机名   | IP             |
| -----   | -------------- |
| node01  | 192.168.42.128 |
| node02  | 192.168.42.129 |


PostgreSQL版本和配置

| 项目 | 值 | 详情 |
| --- | --- | --- |
| PostgreSQL Version | 14.0 | - |
| port | 5432 | - |
| $PGDATA |	/var/lib/pgsql/14/data | - |
| Archive mode | on	| /var/lib/pgsql/archivedir |
| Replication Slots | Enable | - |
| Start automatically |	Enable | - |

### 基础设施准备

#### 关闭防火墙禁止开机启动
```
systemctl stop firewalld.service
systemctl disable firewalld.service
```

#### 关闭selinux
```
vi /etc/selinux/config
SELINUX=disabled
```

#### 设置固定IP
vi /etc/sysconfig/network-scripts/ifcfg-ens192
```
TYPE="Ethernet"
PROXY_METHOD="none"
BROWSER_ONLY="no"
BOOTPROTO="dhcp"
DEFROUTE="yes"
IPV4_FAILURE_FATAL="no"
IPV6INIT="yes"
IPV6_AUTOCONF="yes"
IPV6_DEFROUTE="yes"
IPV6_FAILURE_FATAL="no"
IPV6_ADDR_GEN_MODE="stable-privacy"
NAME="ens192"
UUID="ef1240cf-89c1-42c5-97c5-e33c0f20cd0d"
DEVICE="ens192"
ONBOOT="yes"
```

1. 将BOOTPROTO的值修改为static
2. 增加如下配置：
```
NM_CONTROLLED=yes
IPADDR=192.168.42.128
NETMASK=255.255.255.0
GATEWAY=192.168.42.2
DNS1=223.5.5.5
DNS2=114.114.114.114
```

修改后的配置如下：
```
TYPE="Ethernet"
PROXY_METHOD="none"
BROWSER_ONLY="no"
BOOTPROTO="static"
DEFROUTE="yes"
IPV4_FAILURE_FATAL="no"
IPV6INIT="yes"
IPV6_AUTOCONF="yes"
IPV6_DEFROUTE="yes"
IPV6_FAILURE_FATAL="no"
IPV6_ADDR_GEN_MODE="stable-privacy"
NAME="ens192"
UUID="ef1240cf-89c1-42c5-97c5-e33c0f20cd0d"
DEVICE="ens192"
ONBOOT="yes"
NM_CONTROLLED=yes
IPADDR=192.168.42.128
NETMASK=255.255.255.0
GATEWAY=192.168.42.2
DNS1=223.5.5.5
DNS2=114.114.114.114
```

#### 设置主机名
vi /etc/sysconfig/network
```
NETWORKING=yes
HOSTNAME=node01
```

#### 设置ip到主机名的映射关系
vi /etc/hosts
增加如下配置：
```
192.168.42.128 node01
192.168.42.129 node02
```
#### 安装JDK
略

---

### PostgreSQL安装
#### Install the repository RPM:
`sudo dnf install -y https://download.postgresql.org/pub/repos/yum/reporpms/EL-8-x86_64/pgdg-redhat-repo-latest.noarch.rpm`

#### Disable the built-in PostgreSQL module:
`sudo dnf -qy module disable postgresql`

#### Install PostgreSQL:
`sudo dnf install -y postgresql14-server`

#### Optionally initialize the database and enable automatic start:
`sudo /usr/pgsql-14/bin/postgresql-14-setup initdb`
> 执行以上命令后会在/var/lib/pgsql/14/data目录下初始化数据库

#### 修改监听的IP地址
```
vi postgresql.conf
listen_addresses = '*'
```

#### 修改远程访问限制
```
vi pg_hba.conf
host    all             all             0.0.0.0/0               scram-sha-256
```

#### 启动数据库
`systemctl start postgresql-14.service`

#### 修改postgres用户的密码
```
[root@localhost data]# su postgres
bash-4.4$ psql
psql (14.0)
输入 "help" 来获取帮助信息.
postgres=# alter user postgres with password 'postgres';
ALTER ROLE
postgres=# \q
```












































