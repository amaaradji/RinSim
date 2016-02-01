/*
 * Copyright (C) 2011-2016 Rinde van Lon, iMinds-DistriNet, KU Leuven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.rinde.rinsim.scenario.generator;

import static com.google.common.base.Preconditions.checkArgument;

import java.math.RoundingMode;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;

import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.pdptw.common.AddParcelEvent;
import com.github.rinde.rinsim.scenario.generator.Locations.LocationGenerator;
import com.github.rinde.rinsim.scenario.generator.ScenarioGenerator.TravelTimes;
import com.github.rinde.rinsim.scenario.generator.TimeSeries.TimeSeriesGenerator;
import com.github.rinde.rinsim.scenario.generator.TimeWindows.TimeWindowGenerator;
import com.github.rinde.rinsim.util.StochasticSupplier;
import com.github.rinde.rinsim.util.StochasticSuppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.math.DoubleMath;

/**
 * Utility class for creating {@link ParcelGenerator}s.
 * @author Rinde van Lon
 */
public final class Parcels {
  private Parcels() {}

  /**
   * @return A new {@link Builder} for creating {@link ParcelGenerator}s.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * A generator of {@link AddParcelEvent}s.
   * @author Rinde van Lon
   */
  public interface ParcelGenerator {

    /**
     * Should generate a list of {@link AddParcelEvent}s.
     * @param seed The random seed.
     * @param travelTimes The {@link TravelTimes} provides information about the
     *          expected vehicle travel time.
     * @param endTime The end time of the scenario.
     * @return A list of events.
     */
    ImmutableList<AddParcelEvent> generate(long seed,
        TravelTimes travelTimes, long endTime);

    /**
     * @return The expected center of all generated locations.
     */
    Point getCenter();

    /**
     * @return A position representing the lowest possible coordinates.
     */
    Point getMin();

    /**
     * @return A position representing the highest possible coordinates.
     */
    Point getMax();
  }

  static class DefaultParcelGenerator implements ParcelGenerator {
    private final RandomGenerator rng;
    private final TimeSeriesGenerator announceTimeGenerator;
    private final LocationGenerator locationGenerator;
    private final TimeWindowGenerator timeWindowGenerator;
    private final StochasticSupplier<Long> pickupDurationGenerator;
    private final StochasticSupplier<Long> deliveryDurationGenerator;
    private final StochasticSupplier<Integer> neededCapacityGenerator;

    DefaultParcelGenerator(Builder b) {
      rng = new MersenneTwister();
      announceTimeGenerator = b.announceTimeGenerator;
      locationGenerator = b.locationGenerator;
      timeWindowGenerator = b.timeWindowGenerator;
      pickupDurationGenerator = b.pickupDurationGenerator;
      deliveryDurationGenerator = b.deliveryDurationGenerator;
      neededCapacityGenerator = b.neededCapacityGenerator;
    }

    @Override
    public ImmutableList<AddParcelEvent> generate(long seed,
        TravelTimes travelModel, long endTime) {
      rng.setSeed(seed);
      final ImmutableList.Builder<AddParcelEvent> eventList = ImmutableList
        .builder();
      final List<Double> times = announceTimeGenerator.generate(rng.nextLong());
      final Iterator<Point> locs = locationGenerator.generate(rng.nextLong(),
        times.size() * 2).iterator();

      for (final double time : times) {
        final long arrivalTime = DoubleMath.roundToLong(time,
          RoundingMode.FLOOR);
        final Point origin = locs.next();
        final Point destination = locs.next();

        checkArgument(
          time < endTime,
          "All times generated by the announceTimeGenerator must be below the"
            + " endTime (%s), found %s.",
          endTime, time);

        final Parcel.Builder parcelBuilder = Parcel
          .builder(origin, destination)
          .orderAnnounceTime(arrivalTime)
          .pickupDuration(pickupDurationGenerator.get(rng.nextLong()))
          .deliveryDuration(deliveryDurationGenerator.get(rng.nextLong()))
          .neededCapacity(neededCapacityGenerator.get(rng.nextLong()));

        timeWindowGenerator.generate(rng.nextLong(), parcelBuilder,
          travelModel, endTime);

        eventList.add(AddParcelEvent.create(parcelBuilder.buildDTO()));
      }
      return eventList.build();
    }

    @Override
    public Point getCenter() {
      return locationGenerator.getCenter();
    }

    @Override
    public Point getMin() {
      return locationGenerator.getMin();
    }

    @Override
    public Point getMax() {
      return locationGenerator.getMax();
    }
  }

  /**
   * A builder for creating {@link ParcelGenerator}s.
   * @author Rinde van Lon
   */
  public static class Builder {
    static final TimeSeriesGenerator DEFAULT_ANNOUNCE_TIMES = TimeSeries
      .homogenousPoisson(4 * 60 * 60 * 1000, 20);
    static final double DEFAULT_AREA_SIZE = 5d;
    static final LocationGenerator DEFAULT_LOCATIONS = Locations.builder()
      .square(DEFAULT_AREA_SIZE).buildUniform();
    static final TimeWindowGenerator DEFAULT_TIME_WINDOW_GENERATOR = TimeWindows
      .builder().build();
    static final StochasticSupplier<Long> DEFAULT_SERVICE_DURATION =
      StochasticSuppliers
        .constant(5 * 60 * 1000L);
    static final StochasticSupplier<Integer> DEFAULT_CAPACITY =
      StochasticSuppliers
        .constant(0);

    TimeSeriesGenerator announceTimeGenerator;
    TimeWindowGenerator timeWindowGenerator;
    LocationGenerator locationGenerator;
    StochasticSupplier<Long> pickupDurationGenerator;
    StochasticSupplier<Long> deliveryDurationGenerator;
    StochasticSupplier<Integer> neededCapacityGenerator;

    Builder() {
      announceTimeGenerator = DEFAULT_ANNOUNCE_TIMES;
      timeWindowGenerator = DEFAULT_TIME_WINDOW_GENERATOR;
      locationGenerator = DEFAULT_LOCATIONS;
      pickupDurationGenerator = DEFAULT_SERVICE_DURATION;
      deliveryDurationGenerator = DEFAULT_SERVICE_DURATION;
      neededCapacityGenerator = DEFAULT_CAPACITY;
    }

    /**
     * Sets a {@link TimeSeriesGenerator} which will be used for generating
     * parcel announce times. The {@link TimeSeriesGenerator#generate(long)}
     * method returns real-valued times, these are converted to longs by
     * rounding them down using the {@link RoundingMode#FLOOR} strategy.
     * @param atg The time series generator to use.
     * @return This, as per the builder pattern.
     */
    public Builder announceTimes(TimeSeriesGenerator atg) {
      announceTimeGenerator = atg;
      return this;
    }

    /**
     * Sets a {@link TimeWindowGenerator} to use for generating parcel pickup
     * and delivery time windows.
     * @param twg The time window generator to use.
     * @return This, as per the builder pattern.
     */
    public Builder timeWindows(TimeWindowGenerator twg) {
      timeWindowGenerator = twg;
      return this;
    }

    /**
     * Sets a {@link LocationGenerator} to use for generating parcel pickup and
     * delivery locations.
     * @param lg The location generator to use.
     * @return This, as per the builder pattern.
     */
    public Builder locations(LocationGenerator lg) {
      locationGenerator = lg;
      return this;
    }

    /**
     * Sets the durations of the parcel pickup operations.
     * @param durations The supplier to draw the durations from.
     * @return This, as per the builder pattern.
     */
    public Builder pickupDurations(StochasticSupplier<Long> durations) {
      pickupDurationGenerator = durations;
      return this;
    }

    /**
     * Sets the durations of the parcel delivery operations.
     * @param durations The supplier to draw the durations from.
     * @return This, as per the builder pattern.
     */
    public Builder deliveryDurations(StochasticSupplier<Long> durations) {
      deliveryDurationGenerator = durations;
      return this;
    }

    /**
     * Sets the durations of the parcel pickup and delivery operations.
     * @param durations The supplier to draw the durations from.
     * @return This, as per the builder pattern.
     */
    public Builder serviceDurations(StochasticSupplier<Long> durations) {
      return pickupDurations(durations).deliveryDurations(durations);
    }

    /**
     * Sets the capacities that are needed to carry the generated parcels.
     * @param capacities The supplier to draw the capacities from.
     * @return This, as per the builder pattern.
     */
    public Builder neededCapacities(StochasticSupplier<Integer> capacities) {
      neededCapacityGenerator = capacities;
      return this;
    }

    /**
     * @return A new {@link ParcelGenerator} instance.
     */
    public ParcelGenerator build() {
      return new DefaultParcelGenerator(this);
    }
  }
}
