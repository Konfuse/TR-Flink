# Spatial Flink构建R-Tree的代码说明
最近参考了Spatial Flink的代码，我对Flink的使用和整体流程也有了更深的理解。这个代码也对项目具有很多的借鉴意义。参考该项目，我重新提取出里面的STR Rtree的构建方法，其可拓展性也较高，以后有需要的时候使用。这份代码的分区思想还是很值得学习的。

原项目地址：[https://github.com/thaohtp/spatial-flink](https://github.com/thaohtp/spatial-flink)

## 项目的整体流程
1. 提取包含所有点的MBR（在分布式中，作为全局变量。将得到的值广播到每一个子任务中（分区）并进行相应的运算）。
2. 对所有的点进行采样（能够近似所有点的分布），得到采样点。对采样点进行STR分区，根据得到的分区MBR，构建一个sample R-tree。
3. 利用sample R-tree的分区，将所有点分配到不同的分区中（每个分区对应一个子任务）。
4. 对每个分区（子任务）中的点构建一个Local R-tree。收集所有子任务得到的Local R-tree。最后根据每个Local Rtree的根节点的MBR构建一个真正的Global R-tree。

## 结合Flink代码细节说明
### 数据类型
> 1. Point 基本数据类型
> 2. MBR 基本数据类型
> 3. RtreeNode R-tree的节点
> 4. LeafNode<T> 叶子节点 extends RTreeNode
> 5. MBRLeafNode 通过MBR构建的叶子节点 extends LeafNode<PartitionedMBR>
> 6. PointLeafNode 通过Point构建的叶子节点 extends LeafNode<Point>
> 7. NonLeafNode 非叶子节点 extends RTreeNode
> 8. PartitionedMBR 带了partitionNumber的MBR，指代分区（子任务号）。

## 索引构建细节
### Step 1：根据采样点构建分区MBR
```
//参数说明：
//data: 所有数据点
//nbNodePerEntr: 每个节点的最大容量
//sampleRate: 采样率
//parallelism: 并行数
STRPartitioner partitioner = createSTRPartitioner(data, nbDimension, nbNodePerEntry, sampleRate, parallelism);
```
在函数中首先遍历所有点得到所有点的MBR(globalBoundDS)。将其作为一个全局变量传递给子任务。子任务就是对所有采样点进行reduce操作，得到所有STR分区边界。由于得到的分区数可能大于设置的并行个数，这里使用 i % parallelism 将每个分区i对应到一个子任务中（合并成一个分区）。

Broadcast(广播数据) ：通过withBroadcastSet(DataSet, String) 进行广播数据，并给这份数据起名字。

Broadcast java实现示例：
```
// 1. 准备等待广播的DataSet数据
DataSet<Integer> toBroadcast = env.fromElements(1, 2, 3);
 
DataSet<String> data = env.fromElements("a", "b");
 
data.map(new RichMapFunction<String, String>() {
    @Override
    public void open(Configuration parameters) throws Exception {
      // 3. 获取广播的DataSet数据 作为一个Collection
      Collection<Integer> broadcastSet = getRuntimeContext().getBroadcastVariable("broadcastSetName");
    }
 
 
    @Override
    public String map(String value) throws Exception {
        ...
    }
}).withBroadcastSet(toBroadcast, "broadcastSetName"); // 2. 广播DataSet
```
### Step 2：将所有的数据点分配到不同的分区中（对应不同的子任务）
```
STRPartitioner partitioner = createSTRPartitioner(data, nbDimension, nbNodePerEntry, sampleRate, parallelism);

DataSet<Point> partitionedData = data.partitionCustom(partitioner, new KeySelector<Point, Point>() {
            @Override
            public Point getKey(Point point) throws Exception {
                return point;
            }
        });
```
STRPartitioner 实现了Flink中的 Partitioner<Point>接口， 并在类中重写了partiton方法,使得所有点落到不同的分区中。partiton方法的返回值（int类型），指定子任务号（在这里也代表分区号）。

partitionCustom java实现示例：
```
dataStream.partitionCustom(new Partitioner<String>() {
            
            @Override
            public int partition(String key, int numPartitions) {
                return key.hashCode() % numPartitions;
            }
        },1);
```
```
DataStream<Tuple1<Long>> stream = map.partitionCustom(new MyPartition(), 0);

public static class MyPartition implements Partitioner<Long> {
    @Override
    public int partition(Long key, int i) {
        System.out.println("分区总数："+ i);
        if(key % 2 == 0){
            return 0;
        }else{
            return 1;
        }
    }
}
```

### Step 3: 构建本地索引
对不同的分区中的点，构建Local R-Tree，最后从子任务中收集所有R-tree，得到DataSet\<RTree>。
```
DataSet<RTree> localRTree = partitionedData.mapPartition(new RichMapPartitionFunction<Point, RTree>() {
            @Override
            public void mapPartition(Iterable<Point> iterable, Collector<RTree> collector) throws Exception {
                List<Point> points = new ArrayList<Point>();
                Iterator<Point> pointIter = iterable.iterator();
                while (pointIter.hasNext()) {
                    points.add(pointIter.next());
                }

                if(!points.isEmpty()){
                    RTree rTree = createLocalRTree(points, nbDimension, nbNodePerEntry);
                    collector.collect(rTree);
                }
            }
        });
```
### Step 4: 构建全局索引
这里构建R-tree方式与前面类型，只不过这里是对每个MBR进行构建。得到子任务的编号（分区编号）与所有Local R-Tree根节点所指代的MBR相对应，并将信息保存到Global R-Tree的叶子节点中，最终构造整个R-Tree，便实现了分布式R-Tree索引的构建。

## 序列化方法
这里使用了kryo。kryo是一个高性能的序列化/反序列化工具，由于其变长存储特性并使用了字节码生成机制，拥有较高的运行速度和较小的体积。

kryo提供两种读写方式。记录类型信息的writeClassAndObject/readClassAndObject方法，以及传统的writeObject/readObject方法。

这里与以前写的保存成二进制文件很相似，不再赘述。
```
public class PointSerializer extends Serializer<Point>{

    @Override
    public void write(Kryo kryo, Output output, Point object) {
        output.writeInt(object.getNbDimension());
        for(int i =0; i<object.getNbDimension(); i++){
            kryo.writeObject(output, object.getDimension(i));
        }
    }

    @Override
    public Point read(Kryo kryo, Input input, Class<Point> type) {
        int numDimension = input.readInt();
        float[] vals = new float[numDimension];
        for(int i =0; i< numDimension; i++){
            vals[i] = input.readFloat();
        }
        Point p = new Point(vals);
        p.setNumbBytes( 4 * (numDimension +1));
        return p;
    }
}
```

## 利用分布式R-tree的搜索流程
这里拿范围搜索（针对Point数据）举例，其他搜索类似。

```
//1. 输入参数是所有点，找出所有的在MBR范围内的点集。
public DataSet<Point> boxRangeQuery(final MBR box, DataSet<Point> data) 
//2. 输入的是构建好的global R-tree，local R-tree。找出所有的MBR范围内的点集。
public DataSet<Point> boxRangeQuery(final MBR box, DataSet<RTree> globalTree, DataSet<RTree> localTrees)
```
### boxRangeQuery(final MBR box, DataSet<Point> data)
1. 该方法首先对data数据集进行分区，构建全局R树，得到经过分区的批数据、Global R-tree。

2. 通过Global R-tree，找到与其相交的所有叶子结点，并得到叶子结点中相应的分区号。最后将这些分区号收集起来得到DataSet<Integer> partitionFlags，指代所有相交的分区号。

3. 将partitionFlags作为分布式系统中的全局变量，通过Broadcast函数进行广播，传递到执行搜索操作的不同数据分区的每一个子任务中。通过getRuntimeContext().getIndexOfThisSubtask()得到当前正在搜索的分区号（与子任务号相对应）。在partitionFlags序号集合中的分区中执行搜索，对满足box.contains(point)的Point进行收集。最终得到所有满足的点。

### public DataSet<Point> boxRangeQuery(final MBR box, DataSet<RTree> globalTree, DataSet<RTree> localTrees)
1. 该方法首先通过Global R-tree，找到与其相交的所有叶子结点，并得到叶子结点中相应的分区号。最后将这些分区号收集起来得到DataSet<Integer> partitionFlags，指代所有相交的分区号。

2. 与前面一致，不过这里是在对应local R-tree中。由于local R-tree的叶子节点存储了Point数据，于是直接从每一个满足条件local R-tree的中收集所有满足条件的点，最后将所有结果返回。

