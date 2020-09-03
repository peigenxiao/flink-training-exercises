/*
 * Copyright 2015 data Artisans GmbH, 2019 Ververica GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ververica.flinktraining.examples.table_java.stream.popularPlaces;

import com.ververica.flinktraining.exercises.datastream_java.utils.GeoUtils;
import com.ververica.flinktraining.examples.table_java.sources.TaxiRideTableSource;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.Slide;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.java.StreamTableEnvironment;
import org.apache.flink.types.Row;

public class PopularPlacesTableApi {

	public static void main(String[] args) throws Exception {

		// read parameters
		ParameterTool params = ParameterTool.fromArgs(args);
		String input = params.getRequired("input");

		final int maxEventDelay = 60;       // events are out of order by max 60 seconds
		final int servingSpeedFactor = 600; // events of 10 minutes are served in 1 second

		// set up streaming execution environment
		StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
		env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);

		// create a TableEnvironment
		StreamTableEnvironment tEnv = StreamTableEnvironment.create(env);

		// register TaxiRideTableSource as table "TaxiRides"
		tEnv.registerTableSource(
				"TaxiRides",
				new TaxiRideTableSource(
						input,
						maxEventDelay,
						servingSpeedFactor));

		// register user-defined functions
		tEnv.registerFunction("isInNYC", new GeoUtils.IsInNYC());
		tEnv.registerFunction("toCellId", new GeoUtils.ToCellId());
		tEnv.registerFunction("toCoords", new GeoUtils.ToCoords());

		Table popPlaces = tEnv
				// scan TaxiRides table
				.scan("TaxiRides")
				// filter for valid rides
				.filter("isInNYC(startLon, startLat) && isInNYC(endLon, endLat)")
				// select fields and compute grid cell of departure or arrival coordinates
				.select("eventTime, " +
						"isStart, " +
						"(isStart = true).?(toCellId(startLon, startLat), toCellId(endLon, endLat)) AS cell")
				// specify sliding window over 15 minutes with slide of 5 minutes
				.window(Slide.over("15.minutes").every("5.minutes").on("eventTime").as("w"))
				// group by cell, isStart, and window
				.groupBy("cell, isStart, w")
				// count departures and arrivals per cell (location) and window (time)
				.select("cell, isStart, w.start AS start, w.end AS end, count(isStart) AS popCnt")
				// filter for popular places
				.filter("popCnt > 20")
				// convert cell back to coordinates
				.select("toCoords(cell) AS location, start, end, isStart, popCnt");

		// convert Table into an append stream and print it
		tEnv.toAppendStream(popPlaces, Row.class).print();

		// execute query
		env.execute();
	}

}
