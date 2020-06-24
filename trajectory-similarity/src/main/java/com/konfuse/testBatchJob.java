package com.konfuse;

import org.apache.flink.api.common.functions.MapPartitionFunction;
import org.apache.flink.api.common.functions.Partitioner;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.util.Collector;


/**
 * @author todd
 * @date 2020/5/22 13:52
 * @description: TODO
 */
public class testBatchJob {

    public static void main(String[] args) throws Exception {
        final ExecutionEnvironment env = ExecutionEnvironment
                .getExecutionEnvironment();

        MyPartitioner partitioner= new MyPartitioner();

        DataSet<Tuple2<Integer, Integer>> data = env.fromElements(
                new Tuple2<Integer, Integer>(1, 1), new Tuple2<Integer, Integer>(3, 3),
                new Tuple2<Integer, Integer>(1, 2), new Tuple2<Integer, Integer>(3, 4),
                new Tuple2<Integer, Integer>(1, 5), new Tuple2<Integer, Integer>(3, 6),
                new Tuple2<Integer, Integer>(1, 7), new Tuple2<Integer, Integer>(3, 8),
                new Tuple2<Integer, Integer>(1, 9), new Tuple2<Integer, Integer>(3, 10),
                new Tuple2<Integer, Integer>(1, 11), new Tuple2<Integer, Integer>(3, 12),
                new Tuple2<Integer, Integer>(1, 13), new Tuple2<Integer, Integer>(3, 14),
                new Tuple2<Integer, Integer>(1, 15), new Tuple2<Integer, Integer>(3, 16)
        );


//        data.groupBy(new KeySelector<Tuple2<Integer, Integer>, Integer>() {
//            @Override
//            public Integer getKey(Tuple2<Integer, Integer> integerIntegerTuple2) throws Exception {
//                return integerIntegerTuple2.f0 % 2;
//            }
//        }).reduce(new ReduceFunction<Tuple2<Integer, Integer>>() {
//            @Override
//            public Tuple2<Integer, Integer> reduce(Tuple2<Integer, Integer> integerIntegerTuple2, Tuple2<Integer, Integer> t1) throws Exception {
//                return new Tuple2<>(integerIntegerTuple2.f0 + t1.f0, integerIntegerTuple2.f1 + t1.f1);
//            }
//        }).mapPartition(new MapPartitionFunction<Tuple2<Integer, Integer>, Tuple2<Long, Long>>() {
//            @Override
//            public void mapPartition(Iterable<Tuple2<Integer, Integer>> iterable, Collector<Tuple2<Long, Long>> collector) throws Exception {
//                long even = 0;
//                long odd = 0;
//                for (Tuple2<Integer, Integer> value : iterable) {
//                    if(value.f0 % 2 == 0) {
//                        even++;
//                    } else {
//                        odd++;
//                    }
//                }
//                collector.collect(new Tuple2<>(odd, even));
//            }
//        }).setParallelism(1).print();

       data.partitionCustom(partitioner, new KeySelector<Tuple2<Integer, Integer>, Integer>() {
           @Override
           public Integer getKey(Tuple2<Integer, Integer> integerIntegerTuple2) throws Exception {
               return integerIntegerTuple2.f0;
           }
       }).mapPartition(new MapPartitionFunction<Tuple2<Integer, Integer>, Integer>() {
           @Override
           public void mapPartition(Iterable<Tuple2<Integer, Integer>> iterable, Collector<Integer> collector) throws Exception {
               int sum = 0;
               for (Tuple2<Integer, Integer> integerIntegerTuple2 : iterable) {
                   sum += integerIntegerTuple2.f1;
               }
               collector.collect(sum);
           }
       }).setParallelism(5).print();

//               .mapPartition(new MapPartitionFunction<Tuple2<Integer, Integer>, Tuple2<Long, Long>>() {
//                    @Override
//                    public void mapPartition(Iterable<Tuple2<Integer, Integer>> iterable, Collector<Tuple2<Long, Long>> collector) throws Exception {
//                        long even = 0;
//                        long odd = 0;
//                        for (Tuple2<Integer, Integer> value : iterable) {
//                            if(value.f0 % 2 == 0) {
//                                even++;
//                            } else {
//                                odd++;
//                            }
//                        }
//                        collector.collect(new Tuple2<>(odd, even));
//                    }
//                })
        env.execute("Java Example");
    }

    public static class MyPartitioner implements Partitioner<Integer> {

        @Override
        public int partition(Integer a, int i) {
            return a;
        }
    }
}
