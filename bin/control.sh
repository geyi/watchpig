#!/bin/sh
 
# 此处是打包的jar包名称，不带.jar后缀
API_NAME=WatchPig-1.0-SNAPSHOT
JAR_NAME=$API_NAME\.jar
# PID
PID=watchpig.pid

PRG_PATH=$(dirname $0)
PRG_HOME=$(readlink -f $PRG_PATH/..)
CONFIG_PATH=$PRG_HOME/config
LIB_PATH=$PRG_HOME/lib
CLASS_PATH=$PRG_HOME:$LIB_PATH:$LIB_PATH/*:$CONFIG_PATH
echo $CLASS_PATH
BOOT_CLASS=com.airing.Startup

JAVA_OPTS="-Xmx128m -Xms128m -Dfile.encoding=UTF-8 -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCDateStamps
-XX:+PrintGCCause -XX:+PrintTenuringDistribution -Xloggc:$PRG_HOME/logs/gc-%t.log -XX:+UseGCLogFileRotation
-XX:NumberOfGCLogFiles=5 -XX:GCLogFileSize=20M"

# 使用说明，用来提示输入参数
usage() {
    echo "Usage: sh 执行脚本.sh [start|stop|restart|status]"
    exit 1
}
 
# 检查程序是否在运行
is_exist() {
  cd $PRG_HOME
  if [ -e "/data/filename" ];then
    return 0
  else
    return 1
  fi
  pid=`cat watchpig.pid`
  # 如果不存在返回1，存在返回0
  if [ -z "${pid}" ]; then
    return 1
  else
    return 0
  fi
}
 
# 启动方法
start() {
  is_exist
  if [ $? -eq "0" ]; then
    echo ">>> ${JAR_NAME} is already running PID=${pid} <<<"
  else
    cd $PRG_HOME
    nohup java -cp $CLASS_PATH $JAVA_OPTS $BOOT_CLASS >nohup.log 2>&1 &
    # echo $! > $PID
    echo ">>> start $JAR_NAME successed PID=$! <<<"
   fi
}
 
# 停止方法
stop() {
  # is_exist
  cd $PRG_HOME
  pidf=$(cat $PID)
  # echo "$pidf"
  echo ">>> api PID = $pidf begin kill $pidf <<<"
  kill $pidf
  rm -rf $PID
  sleep 2
  is_exist
  if [ $? -eq "0" ]; then
    echo ">>> api 2 PID = $pid begin kill -9 $pid  <<<"
    kill -9 $pid
    sleep 2
    echo ">>> $JAR_NAME process stopped <<<" 
  else
    echo ">>> ${JAR_NAME} is not running <<<"
  fi 
}
 
# 输出运行状态
status() {
  is_exist
  if [ $? -eq "0" ]; then
    echo ">>> ${JAR_NAME} is running PID is ${pid} <<<"
  else
    echo ">>> ${JAR_NAME} is not running <<<"
  fi
}
 
# 重启
restart() {
  stop
  start
}
 
# 根据输入参数，选择执行对应方法，不输入则执行使用说明
case "$1" in
  "start")
    start
    ;;
  "stop")
    stop
    ;;
  "status")
    status
    ;;
  "restart")
    restart
    ;;
  *)
    usage
    ;;
esac
exit 0
