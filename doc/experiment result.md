# R-Tree空间索引查询结果对比

由于在本系统中独立地开发了一个空间索引R-Tree，并提供了R-Tree的构建方法和相应的查询方法，所以需要结合一些较为出名的开源的R-Tree方法，在一个正确性和效率的对比验证。

## 1. 测试环境

| **操作系统** | **Windows 10**                |
| ------------ | ----------------------------- |
| **CPU**      | Inter Core i7-4710MQ @ 2.5GHZ |
| **内存**     | 12 GB                         |

## 2. 实验内容

本系统提供了两种类型的R-Tree，包括空间点的R-Tree与路段的R-Tree，使用STR的方法进行构建。实验的对比是通过在相同数据集上，使用本系统的构建方法和对比方法共同构建出R-Tree，并使用相同的查询用例分别进行查询，然后将结果进行对比，得到相似性的结果。

### 2.1 对比的开源方法

对比的开源方法选择在github上排名最高的r-tree方法的实现。

该r-tree方法的作者是Dave Moten，在[github](https://github.com/davidmoten/rtree)上可以找到对应的实现，它的星数截至目前为止是773颗。Dave Moten的方法中具有线程安全，快速以及合理空间内存分配的特性。

另外使用了开源项目`spatial flink`中的构建local rtree的方法和对应的查询方法。

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

![](https://github.com/Konfuse/TR-Flink/blob/master/doc/pic/lines_areas_query.png)

由于精度的原因，存在个别情况下，两者查询结果有差异。

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

本系统的方法和Dave Moten实验结果集的相似度对比如图所示：

![](https://github.com/Konfuse/TR-Flink/blob/master/doc/pic/points_areas_query.png)

可以看出，由于精度的原因，存在个别情况下，两者查询结果有差异。



而本系统的方法和`spatial flink`的实验结果集的相似度对比如图所示：

![](https://github.com/Konfuse/TR-Flink/blob/master/doc/pic/points_areas_query_spflink.png)

可以看出结果一致。



另外对于**时间效率**：

本系统的方法和`spatial flink`的实验的时间效率对比如图所示：

![](https://github.com/Konfuse/TR-Flink/blob/master/doc/pic/points_areas_query_spflink_time.png)



##### 2. knn query：

本系统的方法和Dave Moten实验结果集的相似度对比如图所示：

![](https://github.com/Konfuse/TR-Flink/blob/master/doc/pic/points_knn_query_david.png)

可以看出，由于精度的原因，存在个别情况下，两者查询结果有差异。



而本系统的方法和`spatial flink`的实验结果集的相似度对比如图所示：

![](https://github.com/Konfuse/TR-Flink/blob/master/doc/pic/points_knn_query_spflink.png)

可以看出结果一致。



另外，本系统方法和David Moten实验的时间效率对比如图所示：

![](https://github.com/Konfuse/TR-Flink/blob/master/doc/pic/points_knn_query_david_time.png)



和`spatial flink`的实验的时间效率对比如图所示：

![](https://github.com/Konfuse/TR-Flink/blob/master/doc/pic/points_knn_query_spflink_time.png)

## 3. 实验结果分析：

从图中可以看出，在一些情况下，查询的结果具有差异，差异的原因可能存在有两个：

1. Dave Moten的R-Tree使用的是float作为精度，很多查询条件下，输入的地理数据的精度是double，所以会产生精度损失。
2. Dave Moten构建R-Tree时也使用了float作为精度，也会产生精度损失。

## 4. knnQuery实现方法
总体思路：首先利用R-tree的矩形范围搜索方法（boxRangeQuery）粗略估计一个包含不小于k个结果的圆形范围（半径r），利用圆形范围搜索（circleRangeQuery）查找半径（半径r）内的所有点，再对所有得到的结果进行排序。

1. 先构建一个基于每个非叶子节点的MBR的优先队列，按照MBR与查询点q的距离进行排序。
2. 利用R-tree的矩形范围搜索方法（boxRangeQuery）进行搜索，每次弹出与查询点q最近的非叶子节点，如果子节点是叶子节点，遍历所有叶子节点，计算其与查询点q的距离，并将其存储到一个double类型的列表中。当列表的长度不小于k时，跳出循环，得到列表中最大的距离，作为圆形范围搜索的查找半径。 
3. 利用圆形范围搜索（circleRangeQuery）查找半径（半径r）内的所有点，再对所有得到的结果进行排序。

<img src="https://github.com/Konfuse/TR-Flink/blob/master/doc/pic/knnQuery.jpg" style="zoom:50%;" />

