package com.konfuse.template;

import org.apache.flink.api.common.functions.*;
import org.apache.flink.api.common.operators.Order;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.util.Collector;

import java.util.HashSet;
import java.util.Set;

/**
 * @Auther todd
 * @Date 2019/12/10
 */
public class OperatorExample {
    public static void main(String[] args) throws Exception{
        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

        DataSet<String> text = env.fromElements(
                "But a man is not made for defeat",
                "A man can be destroyed but not defeated",
                "But then nothing is easy",
                "Now is no time to think of what you do not have",
                "Think of what you can do with what there is",
                "If you said a good thing it might not happen",
                "If I were him I would put in everything now and go until something broke",
                "The water was a dark blue now",
                "so dark that it was almost purple",
                "It is silly not to hope he thought");

        DataSet<Integer> text2 = env.fromElements(1, 100, 1000, 10000, 1000000, 999, 254, 456, 789, 99999, 666, 888, 22, 15678);

        DataSet<Tuple2<String, Integer>> text3 = env.fromElements(
                new Tuple2<String, Integer>("hello", 0),
                new Tuple2<String, Integer>("hello", 0),
                new Tuple2<String, Integer>("hello", 0),
                new Tuple2<String, Integer>("thanks", 0),
                new Tuple2<String, Integer>("thanks", 0),
                new Tuple2<String, Integer>("thanks", 0),
                new Tuple2<String, Integer>("thanks", 0),
                new Tuple2<String, Integer>("thanks", 0),
                new Tuple2<String, Integer>("brown", 1),
                new Tuple2<String, Integer>("hello", 1),
                new Tuple2<String, Integer>("hello", 1),
                new Tuple2<String, Integer>("thanks", 2),
                new Tuple2<String, Integer>("thanks", 2),
                new Tuple2<String, Integer>("apple", 2));

//        DataSet<String> rest = testOperatorMap(text);
//        DataSet<Tuple2<String, Integer>> rest = testOperatorFlatMap(text);
//        DataSet<Tuple2<String, Integer>> rest2 =testOperatorReduceGroup(text3);
//        DataSet<Long> rest = testOperatorMapPartition(text);
//        DataSet<Integer> rest = testOperatorFilter(text2);
//        DataSet<Integer> rest = testOperatorReduce(text2);
//        DataSet<Tuple2<String, Integer>> out1 = text3.first(5);
//        DataSet<Tuple2<String, Integer>> out1 = text3
//                .groupBy(1)
//                .first(2);
//        DataSet<Tuple2<String, Integer>> out1 = text3.groupBy(1)
//                .sortGroup(0, Order.ASCENDING)
//                .first(3);
//        out1.print();
    }

    //Takes one element and produces one element.
    public static DataSet<String> testOperatorMap(DataSet<String> text){
        DataSet<String> rest = text
                .map(new RichMapFunction<String, String>() {
                    @Override
                    public String map(String value) {
                        return value;
                    }
                });

        return rest;
    }

    //Takes one element and produces zero, one, or more elements.
    public static DataSet<Tuple2<String, Integer>> testOperatorFlatMap(DataSet<String> text){
        DataSet<Tuple2<String, Integer>> wordCounts = text
                .flatMap(new FlatMapFunction<String, Tuple2<String, Integer>>() {
                    @Override
                    public void flatMap(String line, Collector<Tuple2<String, Integer>> out) {
                        for (String word : line.split(" ")) {
                            out.collect(new Tuple2<String, Integer>(word.toLowerCase(), 1));
                        }
                    }
                });
//                .groupBy(0)
//                .sum(1);

        return wordCounts;
    }

    //Transforms a parallel partition in a single function call.
    // The function gets the partition as an Iterable stream and can produce an arbitrary number of result values.
    // The number of elements in each partition depends on the degree-of-parallelism and previous operations.
    public static DataSet<Long> testOperatorMapPartition(DataSet<String> text){
        DataSet<Long> rest = text
                .mapPartition(new MapPartitionFunction<String, Long>() {
                    @Override
                    public void mapPartition(Iterable<String> values, Collector<Long> out) {
                        long c = 0;
                        for (String s : values) {
                            c++;
                            System.out.println(Long.toString(c)+ " " + s);
                        }
                        out.collect(c);
                    }
                });

        return rest;
    }



    //Evaluates a boolean function for each element and retains those for which the function returns true.
    public static DataSet<Integer> testOperatorFilter(DataSet<Integer> text){
        DataSet<Integer> rest= text
                .filter(new FilterFunction<Integer>() {
                    @Override
                    public boolean filter(Integer value) { return value > 1000; }
                });

        return rest;
    }

    //Combines a group of elements into a single element by repeatedly combining two elements into one.
    // Reduce may be applied on a full data set or on a grouped data set.
    public static DataSet<Integer> testOperatorReduce(DataSet<Integer> text){
        DataSet<Integer> rest= text
                .reduce(new ReduceFunction<Integer>() {
                    @Override
                    public Integer reduce(Integer t0, Integer t1) throws Exception {
                        return t0 + t1;
                    }
                });

        return rest;
    }

    //Combines a group of elements into one or more elements. ReduceGroup may be applied on a full data set or on a grouped data set.
    public static DataSet<Tuple2<String, Integer>> testOperatorReduceGroup(DataSet<Tuple2<String, Integer>> text){
        DataSet<Tuple2<String, Integer>> rest= text
                .groupBy(1)
                .reduceGroup(new GroupReduceFunction<Tuple2<String, Integer>, Tuple2<String, Integer>>() {
                    @Override
                    public void reduce(Iterable<Tuple2<String, Integer>> iterable, Collector<Tuple2<String, Integer>> collector) throws Exception {
                        Set<String> uniqStrings = new HashSet<String>();
                        Integer key = null;

                        // add all strings of the group to the set
                        for (Tuple2<String, Integer> t : iterable) {
                            key = t.f1;
                            uniqStrings.add(t.f0);
                        }

                        // emit all unique strings.
                        for (String s : uniqStrings) {
                            collector.collect(new Tuple2<String, Integer>(s, key));
                        }
                    }
                });
        return rest;
    }
}
