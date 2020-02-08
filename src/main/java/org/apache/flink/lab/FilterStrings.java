package org.apache.flink.lab;

import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.IterativeStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.util.Collector;
import java.util.Arrays;
import java.util.List;


public class FilterStrings {
    public static String[] swords = new String[]{"some", "for", "do", "its", "yours", "such", "into", "of", "most", "itself", "other", "off", "is", "s", "am", "or", "who", "as", "from", "him", "each"};
    public static List<String> SListwords = Arrays.asList(swords);
    public static void main(String[] args) throws Exception {

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        
        DataStream<Tuple2<String, Integer>> dataStream = env
                .socketTextStream("localhost", 5567)
                .flatMap(new Splitter())
                .filter((x)-> !SListwords.contains(x.f0) )
                .keyBy(0)
                .sum(1);

        // IterativeStream<Tuple2<String, Integer>> iteration = dataStream.iterate();
        // DataStream<Tuple2<String, Integer>> iterationBody = iteration.map((x) -> x);
        // iteration.closeWith(iterationBody.filter((x)-> !SListwords.contains(x)));
        // DataStream<Tuple2<String, Integer>> output = iterationBody.keyBy(0).sum(1);
        dataStream.print();

        env.execute("FilterStrings");
    }

    public static class Splitter implements FlatMapFunction<String, Tuple2<String, Integer>> {
        @Override
        public void flatMap(String sentence, Collector<Tuple2<String, Integer>> out) throws Exception {
            for (String word: sentence.split(" ")) {
                out.collect(new Tuple2<String, Integer>(word, 1));
            }
        }
    }
}