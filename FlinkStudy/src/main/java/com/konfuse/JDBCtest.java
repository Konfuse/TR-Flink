package com.konfuse;
import com.konfuse.util.SourceFromPostgreSQL;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
/**
 * @Auther todd
 * @Date 2019/11/24
 */
public class JDBCtest {
    public static void main(String[] args) throws Exception {
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.addSource(new SourceFromPostgreSQL()).print();
        env.execute("Flink add data sourc");
    }
}
