package com.konfuse.util;

import com.alibaba.fastjson.JSON;
import com.konfuse.bean.Bound;
import com.konfuse.bean.Point;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

/**
 * @Author: Konfuse
 * @Date: 19-3-17 下午6:42
 */
public class KafkaUtil {
    public static final String broker_list = "localhost:9092";

    public static void writeByFake() {
        Properties props = new Properties();
        props.put("bootstrap.servers", broker_list);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        KafkaProducer producer = new KafkaProducer<String, String>(props);

        String topic = "bound";

        for (int i = 0; i < 5; i++) {
            Bound bound = new Bound(-122.42 - i * 0.01, 37.7 - i * 0.01, -122.41 + i * 0.01, 37.71 + i * 0.01);
            ProducerRecord record = new ProducerRecord<String, String>(topic, null, null, JSON.toJSONString(bound));
            producer.send(record);
            System.out.println("发送数据: " + JSON.toJSONString(bound));
            producer.flush();
        }
    }

    public static void write() throws InterruptedException {
        String topic = "point";
        String path = "D:\\SchoolWork\\HUST\\DataBaseGroup\\Roma\\preprocess_5_Roma_by_hour";
        File[] fileList = new File(path).listFiles();
        BufferedReader reader = null;
        double x, y;
        String line, timestamp;
        String fileId = null;
        String[] item = null;

        Properties props = new Properties();
        props.put("bootstrap.servers", broker_list);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        KafkaProducer producer = new KafkaProducer<String, String>(props);

        try {
            for (int i = 0; i < 10; i++) {
                fileId = fileList[i].toString().split("\\.")[0];

                reader = new BufferedReader(new FileReader(fileList[i]));
                while ((line = reader.readLine()) != null) {
                    item = line.split(";");
                    if (item.length != 3 || item[0] == null || item[1] == null)
                        continue;
                    x = Double.parseDouble(item[0]);
                    y = Double.parseDouble(item[1]);
                    timestamp = item[2];
                    Point point = new Point(x, y, timestamp);
                    System.out.println(point.toString());
                    ProducerRecord record = new ProducerRecord<String, String>(topic, null, null, JSON.toJSONString(point));
                    producer.send(record);
                }
                reader.close();
                Thread.sleep(300);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        write();
    }
}
