package edu.desu.cis.robot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

/**
 * A thread-safe cache for storing and providing access to the robot's sensor data.
 * This class is updated by the {@link TelemetryListener} and read by the robot controller.
 * It uses volatile fields for high-speed, thread-safe access to primitive sensor values.
 * @author Marwan Rasamny
 * @version 0.5.0
 */
public class SensorCache {
    private final ObjectMapper mapper = new ObjectMapper();
    private final ObjectReader reader = mapper.reader();

    // Use volatile primitives for high-speed, thread-safe access
    // In SensorCache — telemetry thread writes one reference atomically
    private volatile SensorSnapshot latest;
    private volatile boolean hasNewData;
    // For complex objects, we still use a thread-safe container or volatile reference
    private volatile BallData ball;

    /**
     * A simple, immutable container for data about a detected ball.
     */
    public static class BallData {
        public final String color;
        public final int x, y, area;

        public BallData() { this("unknown", -1, -1, 0); }

        /**
         * Constructs a new BallData instance.
         * @param color The color of the ball.
         * @param x The x-coordinate of the ball.
         * @param y The y-coordinate of the ball.
         * @param area The area of the ball.
         */
        public BallData(String color, int x, int y, int area) {
            this.color = color; this.x = x; this.y = y; this.area = area;
        }
    }

    /**
     * Constructs a new, empty SensorCache.
     */
    public SensorCache(){
        latest = new SensorSnapshot(Double.NaN, 0.0, 0);
        ball = new BallData();
        hasNewData = false;
    }

    /**
     * Updates the cache with new sensor data from a telemetry JSON string.
     * This method is designed to be called from a high-frequency telemetry thread.
     * @param json The JSON string containing telemetry data.
     */
    public void updateFromTelemetry(String json) {
        try {
            // One-pass parsing directly into a tree
            JsonNode root = reader.readTree(json);

            // Update primitives directly - very fast

            if (root.has("distance") &&
                    root.has("line_offset") &&
                    root.has("line_status")) {
                latest = new SensorSnapshot(
                        root.get("distance").asDouble(),
                        root.get("line_offset").asDouble(),
                        root.get("line_status").asInt()
                ); // atomic reference write
            }

            // Handle nested objects by replacing the reference (Atomic update)
            if (root.has("ball")) {
                JsonNode b = root.get("ball");
                this.ball = new BallData(
                        b.path("color").asText("unknown"),
                        b.path("x").asInt(-1),
                        b.path("y").asInt(-1),
                        b.path("area").asInt(0)
                );
            }
            hasNewData = true;
        } catch (Exception e) {
            // No println here! Use a lightweight log or skip.
        }
    }

    /**
     * Clears the flag indicating that new data is available.
     */
    public void clearHasDataFlag(){
        hasNewData = false;
    }

    /**
     * Checks if new sensor data is available.
     * @return true if new data has been received since the last check, false otherwise.
     */
    public boolean hasNewData() {
        return hasNewData;
    }

    /**
     * Provides a snapshot of the data.
     * @return SensorSnapshot object with the current data.
     */
    public SensorSnapshot takeSnapshot() {
        hasNewData = false;                // reset atomically with the read
        return latest;
    }

    /**
     * Gets the last known distance from the ultrasonic sensor.
     * @return The distance in centimeters.
     */
    public double getDistance() { return latest.distance(); }
    /**
     * Gets the last known offset from the line sensor.
     * @return The line offset.
     */
    public double getLineOffset() { return latest.lineOffset(); }
    /**
     * Gets the last known status from the line sensor.
     * @return The line status.
     */
    public int getLineStatus() { return latest.lineStatus(); }
    /**
     * Gets the color of the last detected ball.
     * @return The ball's color as a string.
     */
    public String getBallColor() { return ball.color; }
    /**
     * Gets the x-coordinate of the last detected ball.
     * @return The ball's x-coordinate.
     */
    public int getBallX() { return ball.x; }
    /**
     * Gets the y-coordinate of the last detected ball.
     * @return The ball's y-coordinate.
     */
    public int getBallY() { return ball.y; }
    /**
     * Gets the area of the last detected ball.
     * @return The ball's area.
     */
    public int getBallArea() { return ball.area; }
    /**
     * Returns a string representation of the sensor cache's current state.
     * @return A string containing key sensor values.
     */
    public String toString(){
        String result = "";
        result += "{\n";
        result += "lineStatus: " + latest.lineStatus() + "\n";
        result += "lineOffset: " + latest.lineOffset() + "\n";
        result += "color: " + ball.color + "\n";
        return result + "}";
    }
}