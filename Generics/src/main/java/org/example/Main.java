package org.example;

import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.function.Function;

// Core metadata interface
interface GetMetadata {
    String getName();
    int getId();
}

interface Copyable<T> {
    T copy();
}

//        pub trait Calibrate {
//    fn set_calibration(&mut self, speed_sensor: i32);
//}
interface Calibrate {
    void setCalibration(Calibration calibration);
}



// Generic build interface — any vehicle can be built with any engine type E
interface Buildable<E>{
    void build(E engine, int id);
}

// Engine enums (unchanged)
enum FuelCategory { PETROL, DIESEL }
enum PowerCategory { ELECTRIC, HYBRID }
enum CylinderCategory { INLINE, V_SHAPED }
enum IgnitionCategory { SPARK, COMPRESSION }

sealed interface EngineType extends GetMetadata, Copyable<EngineType>
        permits EngineType.Fuel, EngineType.Power, EngineType.Cylinder, EngineType.Ignition {

    @Override
    default String getName() {
        return switch (this) {
            case Fuel f     -> "Gasoline engine";
            case Power p    -> "Electric motor";
            case Cylinder c -> "CYLINDER_LAYOUT";
            case Ignition i -> "IGNITION";
        };
    }

    @Override
    default int getId() {
        return switch (this) {
            case Fuel f     -> 1;
            case Power p    -> 2;
            case Cylinder c -> 3;
            case Ignition i -> 4;
        };
    }

    default EngineType copy() {
        return switch (this) {
            case Fuel f     -> new Fuel(f.category());
            case Power p    -> new Power(p.category());
            case Cylinder c -> new Cylinder(c.category());
            case Ignition i -> new Ignition(i.category());
        };
    }

    record Fuel(FuelCategory category) implements EngineType {}
    record Power(PowerCategory category) implements EngineType {}
    record Cylinder(CylinderCategory category) implements EngineType {}
    record Ignition(IgnitionCategory category) implements EngineType {}
}
class Calibration {
    public int speedSensor;
    public Calibration(int speedSensor) {
        this.speedSensor = speedSensor;
    }
}

// making this mutable instead of immutable(using records)
class Car<T extends GetMetadata> implements GetMetadata, Buildable<T>, Calibrate, Copyable<Car<T>>, Cloneable {
    private int id;
    private Optional<T> engine;
    private final String name;
    public Optional<Calibration> calibration;

    // Constructor: Initialize with the name, leave others for the build step
    public Car(String name) {
        this.name = name;
        this.engine = Optional.empty();
        this.id = 0;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public int getId() {
        return this.id;
    }

    public T getEngine() {
        return this.engine.orElseThrow(() ->
                new IllegalStateException("Engine not yet built for car: " + name));
    }

    @Override
    public void build(T engine, int id) {
        // Now this is legal because fields are not final
        this.engine = Optional.of(engine);
        this.id = id;
    }

    @Override
    public void setCalibration(Calibration calibration) {
        this.calibration = Optional.ofNullable(calibration);
    }

    @Override
    public Car copy() {
        // create a new object and return it
        Car<T> clone = new Car<>(this.name);
        return clone;
    }
}


public class Main {

    public static <T extends GetMetadata> void showMetadata(T t) {
        System.out.println("Name - " + t.getName() + ", id - " + t.getId());
    }

    public static <E extends GetMetadata & Copyable<E>, V extends GetMetadata & Buildable<E> &  Copyable<V>>
    Stream<V> createCars(E engine, V prototype) {
        return Stream.iterate(0, i -> i < 10, i -> i + 1)
                .map(i -> {
                    // The method should clone the prototype
                    // Method should clone the Engine
                    // There should be Build method in the new prototype which takes in
                    // new engine
                    // stream should return the new Car or truck ....
                    V new_clone = prototype.copy();
                    E new_engine = engine.copy();
                    // Build the new protyope
                    new_clone.build(new_engine, i);
                    return new_clone;
                });
    }

    public static <T extends GetMetadata & Calibrate & Cloneable> void applyCalibration(
            T vehicle,
            Function<T, Integer> algorithm
    ) {
        // 1. The Worker executes the algorithm provided by the Master
        // This is Dynamic Dispatch: algorithm is an object, .apply() is a virtual call
        Integer calibrationSpecific = algorithm.apply(vehicle);
        // create the calibration object here and pass it to setcalibration
        Calibration new_calibration = new Calibration(calibrationSpecific);
        // 2. Apply the result to the vehicle
        vehicle.setCalibration(new_calibration);
    }

    // add closure

    public static void main(String[] args) {
        EngineType electricEngine = new EngineType.Power(PowerCategory.ELECTRIC);

        Car<EngineType> carPrototype = new Car<>("Ford");

        // Explicit type arguments to help inference
        Stream<Car<EngineType>> fleet = createCars(electricEngine, carPrototype);

        System.out.println("\nFactory output (10 cars):");
        fleet.forEach(Main::showMetadata);

        // create anewCar for calibrartion
        // create a car with no calibration
        // create a calibration object and pass it to set calibration
        Car<EngineType> newCar = new Car<>("Tesla");
        newCar.build(electricEngine,10);

        // 1. Defining the Logic (Master Node side)
        System.out.println("Calibration not yet set");

        double qualityControlMultiplier = 2.0;
        double factoryAltitudeMeters = 4.0;

        Function<Car<EngineType>, Integer> algorithm = (v) -> {
            int engineId = Optional.ofNullable(v.getEngine())
                    .map(GetMetadata::getId)
                    .orElse(0);
            System.out.println("[Logic] Processing for Vehicle ID: " + v.getId());

            double calc = (v.getId() * qualityControlMultiplier) + (factoryAltitudeMeters / 10.0);

            double finalSensorValue;
            if (engineId == 2) {
                System.out.println("[Logic] Electric Motor detected. Applying high-torque sync.");
                finalSensorValue = calc * 2.0;
            } else {
                finalSensorValue = calc;
            }

            return (int) finalSensorValue;
        };

        // 2. Execution (Worker Node side)
        // The worker receives 'newCar' and the 'algorithm' object
        applyCalibration(newCar, algorithm);

        // 3. Result Checking
        if (newCar.calibration.isPresent()) {
            System.out.println("Car calibrated with Speed Sensor: " +
                    newCar.calibration.get().speedSensor);
        } else {
            System.out.println("Calibration unknown");
        }
    }
}