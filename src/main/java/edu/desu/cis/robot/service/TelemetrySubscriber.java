package edu.desu.cis.robot.service;

/**
 * An interface for objects that can subscribe to and receive telemetry data.
 * @author Marwan Rasamny
 * @version 0.5.0
 */
interface TelemetrySubscriber {
    /**
     * Called when a new telemetry message is received.
     * @param json The telemetry data in JSON format.
     */
    void onTelemetry(String json);
}

