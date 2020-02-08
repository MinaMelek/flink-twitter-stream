package org.apache.flink.lab;

import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.assigners.SlidingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.functions.AssignerWithPeriodicWatermarks;
import org.apache.flink.streaming.api.watermark.Watermark;
import org.apache.flink.streaming.connectors.twitter.TwitterSource;
import org.apache.flink.util.Collector;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.JsonNode;


public class TwitterLangCount2 {

    public static void main(String[] args) throws Exception {

        final ParameterTool params = ParameterTool.fromArgs(args);

        StreamExecutionEnvironment env =
                StreamExecutionEnvironment.getExecutionEnvironment();

        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
        env.getConfig().setGlobalJobParameters(params);
        env.getConfig().setAutoWatermarkInterval(200L);

        if (!params.has(TwitterSource.CONSUMER_KEY) ||
                !params.has(TwitterSource.CONSUMER_SECRET) ||
                !params.has(TwitterSource.TOKEN) ||
                !params.has(TwitterSource.TOKEN_SECRET)) {
            System.out.println("Use --twitter-source.consumerKey <key> "
                    + "--twitter-source.consumerSecret <secret> "
                    + "--twitter-source.token <token> "
                    + "--twitter-source.tokenSecret <tokenSecret> "
                    + "to specify authentication info.");
            System.exit(1);
            return;
        }

        DataStream<String> streamSource = env.addSource(
                new TwitterSource(params.getProperties()));//each tweet is in a json format

        DataStream<Tuple3<Long, String, Integer>> tweets = streamSource
                .flatMap(new LanguageCountEvent())
                .assignTimestampsAndWatermarks(new AddWatermark())
                .keyBy(1)
                .timeWindow(Time.seconds(10))
                .sum(2);

        tweets.print();

        env.execute("Windowed Twitter Language Count");
    }

    public static class LanguageCountEvent
            implements FlatMapFunction<String, Tuple3<Long, String, Integer>> {

        private transient ObjectMapper jsonParser; //instance of objectMapper that we will use to parse json

        public void flatMap(String value, Collector<Tuple3<Long, String, Integer>> out)
                throws Exception {

            if(jsonParser == null) { // instantiate the json in the first time only. No need to instantiate each time
                jsonParser = new ObjectMapper();
            }

            JsonNode jsonNode = jsonParser.readValue(value, JsonNode.class);
            // if the tweet has user field and the user field has a language then get it. otherwise unknown

            String language = jsonNode.has("lang")
                    ? jsonNode.get("lang").getValueAsText()
                    : "unknown";

            long timestamp = jsonNode.get("timestamp_ms").asLong();
            // System.out.println("Current timestamp_ms: " + (timestamp));
            // System.out.println("Current created_at: " + (jsonNode.get("created_at").asLong()));
            out.collect(new Tuple3<Long, String, Integer>(timestamp, language, 1));
        }
    }
    
    public static class AddWatermark 
            implements AssignerWithPeriodicWatermarks<Tuple3<Long, String, Integer>> {
        
        private final long        maxLateness = 3000L;
        private long              currentMaxTimestamp;

        @Override
        public Watermark getCurrentWatermark() {
            // System.out.println("Get Current Watermark: " + (currentMaxTimestamp - maxLateness));
            return new Watermark(currentMaxTimestamp - maxLateness);
        }

        @Override
        public long extractTimestamp(Tuple3<Long, String, Integer> element,
                                    long previousElementTimestamp) {
            long timestamp = element.f0;
            currentMaxTimestamp = Math.max(timestamp,
                                        currentMaxTimestamp);
            return timestamp;
        }
    }


}












