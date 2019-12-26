# 使用k8s搭建Flink运行环境

Kubernetes 是一个目前非常流行的容器编排系统，在其之上可以运行 Web 服务、大数据处理等各类应用。这些应用被打包在一个个非常轻量的容器中，我们通过声明的方式来告知 Kubernetes 要如何部署和扩容这些程序，并对外提供服务。将Flink运行于Kubernetes 之上，结合容器的轻量易扩展的特性，可以使得应用更健壮和高可扩，并且能够更安全地和其它服务共享一个 Kubernetes 集群。

在Kubernetes上部署 Flink 有两种方式：**会话集群（Session Cluster）**和**脚本集群（Job Cluster）**。

> **会话集群**是一个长期运行的集群的部署，和独立部署一个 Flink 集群类似，但是将底层资源换成了 K8s 容器，而非直接运行在操作系统上。在集群部署完成之后，可以向集群同时提交多个任务，每项任务必须要提交方可运行。适合运行短时脚本和即时查询的任务。
>
> **脚本集群**则是为单个脚本部署一整套服务，包括 JobManager 和 TaskManager，运行结束后这些资源也随即释放。我们需要为每个脚本构建专门的容器镜像，分配独立的资源，因而这种方式可以更好地和其他脚本隔离开，同时便于扩容或缩容。

本教程目前主要采用Windows作为系统环境，分别介绍会话集群和脚本集群的搭建方法，整体按照Docker for Windows的安装，会话集群搭建的方法以及脚本集群的搭建方法的顺序来介绍。

## 1. Docker for Windows的安装

Docker是用于创建Docker应用程序的完整开发平台，Docker for Windows是在Windows系统上开始使用Docker的最佳方式。

### 1.1 系统要求

因为Docker for Windows需要使用Windows系统的虚拟化功能，所以对系统版本有严格的要求：

- Windows 10 64-bit: Pro, Enterprise, or Education (Build 15063 or later).
- Hyper-V 和 Containers Windows features 功能必须开启.

以及下列硬件要求：

- 64位处理器
- 4GB系统内存
- 在BIOS设置中开启BIOS-级别的硬件虚拟化支持

过旧的系统版本可能不支持虚拟化技术，可以参考[Windows10 使用docker toolbox安装docker](https://www.cnblogs.com/shaosks/p/6932319.html)，安装Docker Toolbox替代，Docker Toolbox使用Oracle Virtual Box提供虚拟化功能而不是win10的Hyper-V。

### 1.2 启用BISOS虚拟化

在电脑开机过程中，选择进入主板BIOS设置，在BIOS中找到Configuration选项，选择Virtualization并设置为开启状态，然后保存设置启动计算机。

### 1.3 启用Microsoft Hyper-V

在电脑上依次打开`控制面板`->`程序`-> `启动或关闭Windows功能`，然后点击`启用或关闭Windows功能`，在打开的选择框中勾选`Hyper-V`选项。

![](https://github.com/Konfuse/TR-Flink/blob/master/doc/pic/Hyper-V.png)

点击确定后，系统会自行更新，更新完成后提示是否重启电脑，选择立即重启电脑，然后可以在开始菜单中看到新增的`Hyper-V管理器`。

### 1.4 安装Docker

首先到Docker官网下载最新版本的Docker for Windows：[Docker下载](https://docs.docker.com/docker-for-windows/install/#download-docker-for-windows)。

双击Docker for Windows Installer.exe以运行安装程序，选择默认的安装选择，最后点击Finish启动Docker。Docker for Windows会默认把Docker的bin目录加入到环境变量中，并设置为开机启动。

稍后会经过Docker的引导程序，然后需要对Docker的一些设置进行配置，右键Docker图标，打开`settings`。

1. **建议**在`Advanced`的选项卡中选择内存容量为4096MB，根据具体机型的硬件做出选择。
2. 由于网络原因，需要为Docker设置国内镜像源，在`Daemon`的选项卡中的`Registry mirrors`中添加一条配置镜像源的地址`https://registry.docker-cn.com`。

### 1.5 常用的Docker命令

`docker version`：查看docker版本

`docker ps `：查看正在运行的docker

`docker info`：显示docker的基本信息

`docker images`：查看本地镜像

## 2. 会话集群搭建的方法

**会话集群**是一种可以长期运行的集群的部署方式，集群部署完成后，可以提交多项任务到集群中，每项任务必须要提交方可运行。在Kubernetes中一个最基础的Flink会话集群部署包括三个组件：

1. 运行`JobManager`的一个`Deployment或者Job`
2. 运行`TaskManagers`的一个`Deployment`
3. 一个服务用于暴露`JobManager`的REST和UI端口

### 2.1 配置kubectl

Kubernetes命令行工具kubectl可以对Kubernetes集群进行命令交互，可以在集群中部署应用，检查和管理集群资源，以及查看集群日志。

kubectl对版本有一定的要求：

> 使用的kubectl版本必须和运行中Kubernetes集群的版本在一个较小的差异之内。例如，一个v1.2的kubectl版本只能和v1.1, v1.2, 以及v1.3的集群版本配合工作。

可以通过右键Docker图标，选择`About Docker Desktop`查看Kubernetes的版本：

<img src="https://github.com/Konfuse/TR-Flink/blob/master/doc/pic/about_docker.png" style="zoom: 67%;" />

如果使用的是Docker for Windows，由于在2018年Docker和Kubernetes已经冰释前嫌，Docker for Windows中已经包含了对Kubernetes的支持，在安装的bin目录中添加了对应的版本的kubectl工具，并加入环境变量中。但是Kubernetes的功能默认是关闭的，对应的镜像也需要预加载才能完成Kubernetes集群的配置。

### 2.2 搭建Kubernetes集群

使用Docker for Windows搭建Kubernetes集群十分方便，只需要在`settings`的`Kubernetes`设置中选择开启即可，选择`Enable Kubernetes`与`Show system containers(advanced)`选项，Docker for Windows即会自动拉取Kubernetes对应版本的镜像并配置Kubernetes，**但是因为国内的原因，Docker Desktop在初始化Kubernetes时所用到的镜像image都是国外源，所以请求不到镜像，配置的方法有两种，设置代理或者使用阿里的镜像源。**

<img src="https://github.com/Konfuse/TR-Flink/blob/master/doc/pic/docker_Kubernetes.png" style="zoom:67%;" />

#### 2.2.1 配置方法一：设置代理

打开Docker for Windows的`settings`配置窗口，在`Proxies`的选项卡中选勾选`Manual Proxy configuration`，然后设置代理的ip为本机代理的ip地址。

<img src="https://github.com/Konfuse/TR-Flink/blob/master/doc/pic/docker_proxies.png" style="zoom:67%;" />

设置完代理之后，点击`Apply`按钮，等待Docker重启，然后在`Kubernetes`设置中选择开启即可，即选择`Enable Kubernetes`与`Show system containers(advanced)`选项。等待配置完成。

#### 2.2.2 配置方法二：使用阿里镜像源

[阿里云](https://github.com/AliyunContainerService/k8s-for-docker-desktop)为镜像服务分别提供了Mac与Windows下可执行的脚本，预先从阿里云Docker镜像服务下载 Kubernetes 所需要的镜像, 可以通过修改 `images.properties` 文件加载你自己需要的镜像。

首先从Github上拉取阿里云的配置文件

```shell
git clone https://github.com/AliyunContainerService/k8s-for-docker-desktop.git
```

然后**注意镜像服务的配置脚本和Kubernetes版本对应的关系**，阿里云提供了各个Kubernetes版本对应的脚本所在的分支，找到自己本地Docker for Windows中包含的Kubernetes版本对应的分支：

- 如Kubernetes版本为 v1.15.4, 请使用下面命令切换 [v1.15.4 分支](https://github.com/AliyunContainerService/k8s-for-docker-desktop/tree/v1.15.4) `git checkout v1.15.4`
- 如Kubernetes版本为 v1.14.8, 请使用下面命令切换 [v1.14.8 分支](https://github.com/AliyunContainerService/k8s-for-docker-desktop/tree/v1.14.8) `git checkout v1.14.8`
- 如Kubernetes版本为 v1.14.7, 请使用下面命令切换 [v1.14.7 分支](https://github.com/AliyunContainerService/k8s-for-docker-desktop/tree/v1.14.7) `git checkout v1.14.7`
- 如Kubernetes版本为 v1.14.6, 请使用下面命令切换 [v1.14.6 分支](https://github.com/AliyunContainerService/k8s-for-docker-desktop/tree/v1.14.6) `git checkout v1.14.6`
- 如Kubernetes版本为 v1.14.3, 请使用下面命令切换 [v1.14.3 分支](https://github.com/AliyunContainerService/k8s-for-docker-desktop/tree/v1.14.3) `git checkout v1.14.3`
- 如Kubernetes版本为 v1.14.1, 请使用下面命令切换 [v1.14.1 分支](https://github.com/AliyunContainerService/k8s-for-docker-desktop/tree/v1.14.1) `git checkout v1.14.1`
- 如Kubernetes版本为 v1.13.0, 请使用下面命令切换 [v1.13.0 分支](https://github.com/AliyunContainerService/k8s-for-docker-desktop/tree/v1.13.0) `git checkout v1.13.0`
- 如Kubernetes版本为 v1.10.11, 请使用下面命令切换 [v1.10.11 分支](https://github.com/AliyunContainerService/k8s-for-docker-desktop/tree/v1.10.11) `git checkout v1.10.11`

切换到对应的分支下（本次配置用到的版本是1.14.8，所以切换到分支`git checkout v1.14.8`）。

在Windows上，用“以管理员身份运行” 打开PowerShell，执行`Set-ExecutionPolicy RemoteSigned` 命令以关闭安全策略，然后在`k8s-for-docker-desktop`目录下执行脚本

```shell
 .\load_images.ps1
```

脚本运行完成之后，在Docker for Windows中开启 Kubernetes，并等待 Kubernetes 开始运行。

#### 2.2.3 配置帮助

如果在Kubernetes部署的过程中出现问题，可以在 C:\ProgramData\DockerDesktop下的service.txt 查看Docker日志

### 2.3 配置kubectl工具

需要切换Kubernetes运行上下文至 docker-desktop，如果在Docker for Windows开启Kubernetes服务，则Docker for Windows会帮助为kubectl工具配置执行上下文为docker-desktop，也可以使用命令配置：

```shell
kubectl config use-context docker-desktop
```

验证 Kubernetes 集群状态：

```shell
kubectl cluster-info
kubectl get nodes
```

Kubernetes为我们提供了Web管理界面 **Kubernetes dashboard**，但是默认处于关闭状态，需要手动配置以打开。执行命令

```shell
kubectl apply -f https://raw.githubusercontent.com/kubernetes/dashboard/v1.10.1/src/deploy/recommended/kubernetes-dashboard.yaml
```

或者

```shell
kubectl create -f kubernetes-dashboard.yaml
```

然后稍微等待镜像拉取以及配置完成，开启 API Server 访问代理，使Kubernetes集群内挂载API的端口映射到本机8001端口。通过如下URL访问：

http://localhost:8001/api/v1/namespaces/kube-system/services/https:kubernetes-dashboard:/proxy/#!/overview?namespace=default

打开后显示需要配置令牌以验证身份，对于Windows环境，使用如下命令进行配置：

```shell
$TOKEN=((kubectl -n kube-system describe secret default | Select-String "token:") -split " +")[1]
kubectl config set-credentials docker-for-desktop --token="${TOKEN}"
echo $TOKEN
```

登录可以选择令牌，输入以上控制台的输出内容，或者选择 **Kubeconfig** 文件，文件的路径在`%UserProfile%\.kube\config`下。

### 2.4 通过Kubernetes搭建Flink集群

#### 2.4.1 搭建

按照官网的 [Appendix](https://link.zhihu.com/?target=https%3A//ci.apache.org/projects/flink/flink-docs-stable/ops/deployment/kubernetes.html%23appendix)将集群的配置文件拷贝到本地。或者在****中找到。

**配置flink的版本号，注意在每个文件中，将flink的docker tag设置为对应版本的标签号，默认为最新的flink docker标签号：latest。**

通过会话集群的资源定义文件使用`kubectl`工具创建Flink集群：

```shell
kubectl create -f flink-configuration-configmap.yaml
kubectl create -f jobmanager-service.yaml
kubectl create -f jobmanager-deployment.yaml
kubectl create -f taskmanager-deployment.yaml
```

命令执行完成显示`created`，然后等待资源配置，可以通过命令`kubectl get pods`显示正在创建或者创建已完成的部署。注意可以在`flink-configuration-configmap.yaml`中定义自己的`flink-conf.yaml`自定义选项。

#### 2.4.2 端口映射

部署配置完成后，可以通过多种方法查看Flink UI：

- 通过命令`kubectl proxy`：
  1. 在“以管理员身份运行”打开的power shell中运行 `kubectl proxy` 命令
  2. 然后在浏览器中打开链接 http://localhost:8001/api/v1/namespaces/default/services/flink-jobmanager:ui/proxy

- 通过命令`kubectl port-forward`：
  1. 在“以管理员身份运行”打开的power shell中运行 `kubectl port-forward ${flink-jobmanager-pod} 8081:8081` ，把jobmanager的UI端口映射到本地的8081端口
  2. 在浏览器中打开链接 [http://localhost:8081](http://localhost:8081/)
  3. 部署配置完成可以通过命令来为集群提交任务：

```
./bin/flink run -m localhost:8081 ./examples/streaming/WordCount.jar
```

#### 2.4.3 删除会话集群

通过在“以管理员身份运行”打开的power shell中执行kubectl命令来删除集群：

```shell
kubectl delete -f jobmanager-deployment.yaml
kubectl delete -f taskmanager-deployment.yaml
kubectl delete -f jobmanager-service.yaml
kubectl delete -f flink-configuration-configmap.yaml
```

## 3. 脚本集群搭建方法

脚本集群为单个脚本部署一整套服务，包括 JobManager 和 TaskManager，运行结束后这些资源也随即释放。集群的搭建需要为每个脚本构建专门的容器镜像，分配独立的资源，所以这种方式可以更好地和其他脚本隔离开，同时便于扩容或缩容。搭建的具体步骤如下：

1. 编译并打包 Flink 脚本 Jar 文件；
2. 构建 Docker 容器镜像，添加 Flink 运行时库和上述 Jar 包；
3. 使用 Kubernetes Job 部署 Flink JobManager 组件；
4. 使用 Kubernetes Service 将 JobManager 服务端口开放到集群中；
5. 使用 Kubernetes Deployment 部署 Flink TaskManager；
6. 配置 Flink JobManager 高可用，需使用 ZooKeeper 和 HDFS；
7. 借助 Flink SavePoint 机制来停止和恢复脚本。

#### 3.1 编译并打包 Flink 脚本 Jar 文件

编写一个简单的实时处理脚本，该脚本会从某个端口中读取文本，分割为单词，并且每 5 秒钟打印一次每个单词出现的次数。以下代码是从 [Flink 官方示例文档](https://ci.apache.org/projects/flink/flink-docs-release-1.8/dev/datastream_api.html#example-program) 中获取并修改的，完整的示例项目可以到 [GitHub](https://github.com/jizhang/flink-on-kubernetes) 上查看。K8s 容器中的程序可以通过 IP `192.168.99.1` 来访问宿主机上的服务。因此在运行上述代码之前，需要先在宿主机上执行 `nc -lk 9999` 命令打开一个端口。

```java
DataStream<Tuple2<String, Integer>> dataStream = env
    .socketTextStream("192.168.99.1", 9999)
    .flatMap(new Splitter())
    .keyBy(0)
    .timeWindow(Time.seconds(5))
    .sum(1);

dataStream.print();
```

接下来执行 `mvn clean package` 命令，打包好的 Jar 文件路径为 `target/flink-on-kubernetes-0.0.1-SNAPSHOT-jar-with-dependencies.jar`。

#### 3.2 构建 Docker 容器镜像

Flink 提供了一个官方的容器镜像，可以从 [DockerHub](https://hub.docker.com/_/flink) 上下载。我们将以这个镜像为基础，构建独立的脚本镜像，将打包好的 Jar 文件放置进去。此外，新版 Flink 已将 Hadoop 依赖从官方发行版中剥离，因此我们在打镜像时也需要包含进去，可以从 [Hadoop Jar 包](https://repo.maven.apache.org/maven2/org/apache/flink/flink-shaded-hadoop-2-uber/2.8.3-7.0/flink-shaded-hadoop-2-uber-2.8.3-7.0.jar)处下载。

1. 新建一个文件夹，这个文件夹将是我们配置docker file的编译位置，在文件夹内新建一个文件`DockerFile`，加入：

   ```
   FROM flink:1.8.1-scala_2.12
   ARG hadoop_jar
   ARG job_jar
   COPY --chown=flink:flink $hadoop_jar $job_jar $FLINK_HOME/lib/
   USER flink
   ```

   

2. 准备添加 Flink 运行时库和上述 Jar 包hadoop的jar 包`hadoop.jar`以及flink脚本`job.jar`

3. 执行编译命令，将脚本镜像打包完毕，用于部署：

   ```shell
   docker build --build-arg hadoop_jar=hadoop.jar --build-arg job_jar=job.jar --tag flink-on-kubernetes:0.0.1 .
   ```

#### 3.3 部署 JobManager

首先，我们通过创建 Kubernetes Job 对象来部署 Flink JobManager。Job 和 Deployment 是 K8s 中两种不同的管理方式，他们都可以通过启动和维护多个 Pod 来执行任务。不同的是，Job 会在 Pod 执行完成后自动退出，而 Deployment 则会不断重启 Pod，直到手工删除。Pod 成功与否是通过命令行返回状态判断的，如果异常退出，Job 也会负责重启它。因此，Job 更适合用来部署 Flink 应用，当我们手工关闭一个 Flink 脚本时，K8s 就不会错误地重新启动它。

准备是 `jobmanager.yml`配置文件：

```yml
apiVersion: batch/v1
kind: Job
metadata:
  name: flink-on-kubernetes-jobmanager
spec:
  template:
    metadata:
      labels:
        app: flink
        instance: flink-on-kubernetes-jobmanager
    spec:
      restartPolicy: OnFailure
      containers:
      - name: jobmanager
        image: flink-on-kubernetes:0.0.1
        command: ["/opt/flink/bin/standalone-job.sh"]
        args: ["start-foreground",
               "-Djobmanager.rpc.address=flink-on-kubernetes-jobmanager",
               "-Dparallelism.default=1",
               "-Dblob.server.port=6124",
               "-Dqueryable-state.server.ports=6125"]
        ports:
        - containerPort: 6123
          name: rpc
        - containerPort: 6124
          name: blob
        - containerPort: 6125
          name: query
        - containerPort: 8081
          name: ui
```

- `${JOB}` 变量可以使用 `envsubst` 命令来替换，这样同一份配置文件就能够为多个脚本使用了；
- 容器的入口修改为了 `standalone-job.sh`，这是 Flink 的官方脚本，会以前台模式启动 JobManager，扫描类加载路径中的 `Main-Class` 作为脚本入口，我们也可以使用 `-j` 参数来指定完整的类名。之后，这个脚本会被自动提交到集群中。
- JobManager 的 RPC 地址修改为了 [Kubernetes Service](https://kubernetes.io/docs/concepts/services-networking/service/#virtual-ips-and-service-proxies) 的名称，我们将在下文创建。集群中的其他组件将通过这个名称来访问 JobManager。
- Flink Blob Server & Queryable State Server 的端口号默认是随机的，为了方便将其开放到集群中，我们修改为了固定端口。

使用命令`kubectl create -f jobmanager.yml `创建。

#### 3.4 使用 Kubernetes Service 将 JobManager 服务端口开放到集群中

创建一个 K8s Service 来将 JobManager 的端口开放出来，以便 TaskManager 前来注册

准备`service.yml`配置文件：

```yml
apiVersion: v1
kind: Service
metadata:
  name: flink-on-kubernetes-jobmanager
spec:
  selector:
    app: flink
    instance: flink-on-kubernetes-jobmanager
  type: NodePort
  ports:
  - name: rpc
    port: 6123
  - name: blob
    port: 6124
  - name: query
    port: 6125
  - name: ui
    port: 8081
```

其中可以通过`type: NodePort` 配置，在 K8s 集群之外访问 JobManager UI 和 RESTful API。

使用命令`kubectl create -f service.yml `创建。

#### 3.5 部署 TaskManager

准备`taskmanager.yml`配置文件：

```yml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: flink-on-kubernetes-taskmanager
spec:
  selector:
    matchLabels:
      app: flink
      instance: flink-on-kubernetes-taskmanager
  replicas: 1
  template:
    metadata:
      labels:
        app: flink
        instance: flink-on-kubernetes-taskmanager
    spec:
      containers:
      - name: taskmanager
        image: flink-on-kubernetes:0.0.1
        command: ["/opt/flink/bin/taskmanager.sh"]
        args: ["start-foreground", "-Djobmanager.rpc.address=flink-on-kubernetes-jobmanager"]
```

通过修改 `replicas` 配置，可以开启多个 TaskManager。镜像中的 `taskmanager.numberOfTaskSlots` 参数默认为 `1`，这也是我们推荐的配置，因为扩容缩容方面的工作应该交由 K8s 来完成，而非直接使用 TaskManager 的槽位机制。

使用命令`kubectl create -f taskmanager.yml `创建。

#### 3.6 执行

执行 `nc -lk 9999` 命令打开一个端口来向脚本中指定的`9999`端口发送消息。然后打开另一个终端，查看 TaskManager 的标准输出日志：

```shell
kubectl logs -f -l instance=flink-on-kubernetes-taskmanager
```

