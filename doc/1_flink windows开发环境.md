# 如何在Windows下使用Flink开发

本文主要用来介绍在Windows下搭建一个flink的开发环境，集群环境的开发推荐使用Linux操作系统，具体的搭建方法可以参考[环境搭建指南](https://github.com/Konfuse/BigData/blob/master/%E7%8E%AF%E5%A2%83%E9%85%8D%E7%BD%AE%E7%AF%87.md)。

Flink支持自定义的数据源，并基于此设计了很多和详细中间件的Connector，Flink最常用的详细中间件是Kafka，因为在众多消息中间件中Kafka具有吞吐量较高的优势。

在系统中可以采用Kafka作为流数据模拟工具，其中Kafka的运行又依赖于Zookeeper，所以在安装Kafka之前需要在机器上安装Zookeeper作为分布式通信工具。

下面介绍一步步介绍开发的环境搭建。

## 1. Java是所有环境搭建的基础

推荐使用Java1.8以上的版本，注意要把Java的路径写入本机环境变量中，如果测试`java -version`命令执行成功，说明Java环境已经配置完成。

## 2. Kafka的运行依赖于Zookeeper

由于Kafka的运行依赖于Zookeeper，所以在运行Kafka之前需要安装并运行Zookeeper。本文选取的Zookeeper版本为3.5.6。

安装过程分为5步：

1. 首先，从[Zookeeper官网]( http://zookeeper.apache.org/releases.html#download )下载Zookeeper的安装包，注意

   ![](https://github.com/Konfuse/TR-Flink/blob/master/doc/pic/Zookeeper.png?raw=true)

   选择上边红圈内的作为安装包

2. 解压压缩包到指定目录下。

3. 进入解压目录下，进入`conf`目录中，复制zoo_sample.cfg重命名成zoo.cfg，并配置`dataDir`与`datalogDir`的目录为指定的文件，并配置端口号，注意在单机环境下无需配置客户端的IP与端口号，例如：

   ```shell
   dataDir = E:\\zookeeper\\data
   datalogDir = E:\\zookeeper\\log
   clientPort = 2181
   ```

4. 为Zookeeper添加环境变量，在系统变量里添加：

   ```shell
   ZOOKEEPER_HOME          E:\zookeeper\
   ```

   并在Path中添加：

   ```shell
   %ZOOKEEPER_HOME%\bin
   ```

5. 配置完成，即可在Zookeeper的bin目录下，使用`zkServer.cmd`命令打开Zookeeper运行环境。

## 3. 安装Kafka

Kafka的安装十分简单，在单机环境下只需要配置`server.properties`文件即可。

安装分为4步：

1. 首先，从[官网下载](http://kafka.apache.org/downloads)Kafka安装包，本文中选择scala版本为2.11，Kafka2.3.1的版本。

2. 下载完成后，解压缩到指定目录下。

3. 进入解压目录，打开文件`config\\server.properties`，修改配置中的`log.dirs`为指定目录，例如：

   ```shell
   log.dirs=E:\\kafka_2.11-2.3.1\\kafka-logs
   ```

4. 配置完成，进入Kafka的根目录，执行命令：

   ```shell
   .\bin\windows\kafka-server-start.bat .\config\server.properties
   ```

   即可启动Kafka通讯的服务器broker，但是请注意，Kafka的服务是依赖于Zookeeper的，在启动Kafka之前需要先启动Zookeeper。

### 测试Kafka是否安装成功

Kafka启动成功之后进入`\\bin\\windows`目录，为Kafka创建Topic，可以观察到生产者和消费者的通信。

1. 首先启动生产者（Producer），输入以下命令：

   ```shell
   kafka-console-producer.bat --broker-list localhost:9092 --topic testDemo
   ```

   在9092端口上创建testDemo的topic。

2. 然后新建一个CMD窗口，启动消费者（Consumer），输入以下命令：

   ```shell
   kafka-console-consumer.bat --bootstrap-server localhost:9092 --topic testDemo
   ```

3. 完成之后，在生产者中输入信息，消费者中就可以获取到信息了。

## 4. 搭建Flink环境

Flink在Windows下的安装，需要在[Flink官网](https://flink.apache.org/downloads.html)下载并解压官方推出的开发包。

注意Flink开发环境中，如果需要依赖于HDFS则需要选择`with hadoop`的版本。因为本地开发环境搭建过程没有依赖Hadoop，所以不需要选择`with hadoop`的版本。对于Flink版本，这里选择较为稳定的 [Apache Flink 1.7.2 for Scala 2.11](https://www.apache.org/dyn/closer.lua/flink/flink-1.7.2/flink-1.7.2-bin-scala_2.11.tgz) 。

下载完成后把压缩文件解压到指定目录下，安装即完成。然后进入flink的根目录，在`bin\\`目录下找到`start-cluster.bat`，然后再CMD中运行，即在本机启动flink环境。

环境启动后可以在本地`localhost:8081`查看flink的运行状况。

### Flink的提交命令

在Flink系统中提交命令需要首先把程序打包为jar包，然后在Flink的根目录下运行执行命令，例如：

```
./bin/flink run examples/streaming/SocketWindowWordCount.jar --port 9000
```

其中`examples/streaming/SocketWindowWordCount.jar`是打包后的jar包路径，后边`--port 9000`是程序所需参数。