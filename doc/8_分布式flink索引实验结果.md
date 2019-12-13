# 基于Flink的分布式R-Tree空间索引查询结果对比

在系统接下来的实验中将空间索引R-Tree加入了分布式系统Flink的支持，索引由本地的索引变为全局索引和局部索引结合的方式，然后同样将索引在已有的数据集上进行测试，将实验结果与之前的实验结果进行正确性对比。

由于分布式的架构形式，以及本次实验是运行在单机上所以目前只对正确性做检测。

## 1. 测试环境

| **操作系统** | **Windows 10**                |
| ------------ | ----------------------------- |
| **CPU**      | Inter Core i7-4710MQ @ 2.5GHZ |
| **内存**     | 12 GB                         |

## 2. 实验内容

本系统提供了两种类型的R-Tree，包括空间点的R-Tree与路段的R-Tree，使用STR的方法进行构建。实验的对比是通过在相同数据集上，使用本系统的构建方法共同构建出R-Tree，并使用相同的查询用例分别进行查询，然后将结果进行对比，得到相似性的结果。

### 2.1 对比的开源方法

对比的开源方法选择在github上排名最高的r-tree方法的实现。

使用了开源项目`spatial flink`中的构建local rtree的方法和对应的查询方法的结果作为正确性的结果作为对比。

### 2.2 路段的R-Tree对比

#### 2.2.1 数据集

数据集选取的是知乎用户*uncle GIS*提供的达累斯萨拉姆市的数据，下载于OpenStreetMap。

数据包括了该市所有的路段信息，实验中用到的信息包括路网数据库表中的：`gid, x1, y1, x2, y2`，其中`gid`是路段的唯一标识，`x1, y1`是路段的起始点坐标，`x2, y2`是路段的终止点坐标。

数据集的一些特性如下：

- x-min：-6.8394859
- y-min：39.2084899
- x-max：-6.7762898
- y-max：39.3037728

#### 2.2.2 测试用例

测试用例使用100组随机生成的测试数据，包含在查询区域内随机分布的100个随机矩形区域。

#### 2.2.3 实验对比

使用该市所有的路段建立R-Tree，并将`gid`作为路段的唯一标识。分别使用本系统的方法和Dave Moten的方法构建r-tree，并使用随机查询区域执行rangeQuery查询，将查询结果的路段id保存至本地文件，使用自动化脚本做对比。

对比的策略选取MinHash算法来计算两个结果集合的相似度：`相似度 = |A∩B| / |A∪B|`。

#### 2.3.4 实验结果

实验结果集的相似度对比如图所示：

![](https://github.com/Konfuse/TR-Flink/blob/master/doc/pic/lines_areas_query_flink.png)

测试结果与实验一致。

### 2.3 点集的R-Tree对比

#### 2.3.1 数据集

数据集是使用模拟随机生成的数据集点组成。

数据包括一些基本的信息：`id, x, y`，其中`id`是点的唯一标识，`x, y`分别是点的坐标值。

数据集的一些特性如下：

- x-min：100
- y-min：100
- x-max：200
- y-max：200
- size：1000000

#### 2.3.2 测试用例

测试用例使用100组随机生成的测试数据，包含在查询区域内随机分布的100个随机矩形区域。

#### 2.3.3 实验对比

使用所有的点集建立R-Tree，并将`id`作为点的唯一标识。分别使用本系统的方法和Dave Moten的方法构建r-tree，并使用随机查询区域执行rangeQuery以及knnQuery查询，将查询结果的路段id保存至本地文件，使用自动化脚本做对比。

同时，使用github上的`spatial flink`中的构建方法做对比。

对比的策略选取MinHash算法来计算两个结果集合的相似度：`相似度 = |A∩B| / |A∪B|`。

#### 2.3.4 实验结果

##### 1. range query：

本系统的方法和正确实验结果集的相似度对比如图所示：

![](https://github.com/Konfuse/TR-Flink/blob/master/doc/pic/points_areas_query_flink.png)

可以看出结果一致。

##### 2. knn query：

- 使用分区分别knnQuery然后全局汇总的查询方法和正确结果存在差异，经过很久的调查研究也没有发现问题所在，本系统的方法和正确性实验结果集的相似度对比如图所示：

![](https://github.com/Konfuse/TR-Flink/blob/master/doc/pic/points_knn_query_flink_error.png)

可以看出，两者查询结果有差异。



- 使用分区数据全局查询的方法，结果和正确结果一致，本系统的方法和正确的实验结果集的相似度对比如图所示：

![](https://github.com/Konfuse/TR-Flink/blob/master/doc/pic/points_knn_query_flink.png)

可以看出结果一致。

##### 3. circle range query：

使用circle range query查询的结果和正确结果一致，本系统的方法和正确的实验结果集的相似度对比如图所示：

![](https://github.com/Konfuse/TR-Flink/blob/master/doc/pic/points_knn_query_flink.png)

可以看出结果一致。