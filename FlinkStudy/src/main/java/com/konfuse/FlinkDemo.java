package com.konfuse;

import com.alibaba.fastjson.JSON;
import com.konfuse.bean.Point;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer011;

import java.util.Properties;

/**
 * @Author: Konfuse
 * @Date: 2019/11/20 11:38
 */
public class FlinkDemo {
    public static void main(String[] args) throws Exception {
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("zookeeper.connect", "localhost:2181");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("auto.offset.reset", "latest");

        DataStream<String> dataStream = env.addSource(new FlinkKafkaConsumer011<>("point", new SimpleStringSchema(), props))
                .setParallelism(1);
        SingleOutputStreamOperator<Point> singleOutputStreamOperator = dataStream.map(string -> JSON.parseObject(string, Point.class));
        singleOutputStreamOperator.print();

        env.execute("Execute Query...");
    }
}
