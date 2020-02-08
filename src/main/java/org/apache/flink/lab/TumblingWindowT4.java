package org.apache.flink.lab;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
public class TumblingWindowT4 {

    public static void main(String[] args) throws Exception {
        final ParameterTool params = ParameterTool.fromArgs(args);

        final StreamExecutionEnvironment env =
                StreamExecutionEnvironment.getExecutionEnvironment();

        env.getConfig().setGlobalJobParameters(params);

        DataStream<String> dataStream = StreamUtil.getDataStream(env, params);

        if (dataStream == null) {
            System.exit(1);
            return;
        }

        DataStream<Tuple2<String, Double>> SumViewStream = dataStream
                .map(new RowSplitter())
                .keyBy(0)
                .window(TumblingProcessingTimeWindows.of(Time.seconds(30)))
                .reduce(new Sum())
                .map(new Converter());

        SumViewStream.print();

        env.execute("Tumbling Window average");
    }


    public static class RowSplitter implements
            MapFunction<String, Tuple3<String, Double, Double>> {

        public Tuple3<String, Double, Double> map(String row)
                throws Exception {
            String[] fields = row.split(" ");
            if (fields.length == 2) {
                return new Tuple3<String, Double, Double>(
                        fields[0] /* webpage id */,
                        Double.parseDouble(fields[1]) /* view time in minutes */,
                        1.0);
            }


            return null;
        }
    }

    public static class Converter implements
            MapFunction<Tuple3<String, Double, Double>, Tuple2<String, Double>> {

        public Tuple2<String, Double> map(Tuple3<String, Double, Double> val)
                throws Exception {
            return new Tuple2<String, Double>(
                    val.f0/* webpage id */,
                    val.f1 / val.f2 /* avg time */);
        }
    }

    public static class Sum implements
            ReduceFunction<Tuple3<String, Double, Double>> {

        public Tuple3<String, Double, Double> reduce(
                Tuple3<String, Double, Double> cumulative,
                Tuple3<String, Double, Double> input) {

            return new Tuple3<String, Double, Double>(
                    input.f0,
                    cumulative.f1 + input.f1,
                    cumulative.f2 + input.f2
                    );
        }
    }



}
