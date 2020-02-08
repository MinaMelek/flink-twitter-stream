package org.apache.flink.lab;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
public class SumViews {

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
                .reduce(new Sum());

        SumViewStream.print();

        env.execute("time spent in Views");
    }


    public static class RowSplitter implements
            MapFunction<String, Tuple2<String, Double>> {

        public Tuple2<String, Double> map(String row)
                throws Exception {
            String[] fields = row.split(" ");
            if (fields.length == 2) {
                return new Tuple2<String, Double>(
                        fields[0] /* webpage id */,
                        Double.parseDouble(fields[1]) /* view time in minutes */);
            }


            return null;
        }
    }

    public static class Sum implements
            ReduceFunction<Tuple2<String, Double>> {

        public Tuple2<String, Double> reduce(
                Tuple2<String, Double> cumulative,
                Tuple2<String, Double> input) {

            return new Tuple2<String, Double>(
                    input.f0,
                    cumulative.f1 + input.f1
                    );
        }
    }



}
