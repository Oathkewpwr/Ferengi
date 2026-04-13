package edu.desu.cis.robot.control;

import edu.desu.cis.robot.discovery.DiscoveredRobot;
import edu.desu.cis.robot.discovery.RobotDiscovery;
import edu.desu.cis.robot.discovery.UdpDiscoveryService;
import edu.desu.cis.robot.service.*;
import edu.desu.cis.robot.commands.MBot2;

/**
 * An abstract controller for a robot, providing core functionalities for
 * discovery, connection, and teardown. This class is intended to be subclassed
 * by specific robot controller implementations that define the robot's
 * behavior.
 * @author Marwan Rasamny
 * @version 0.5.0
 */
public abstract class RobotController implements AutoCloseable {
    private volatile boolean isClosed = true;

    /** The MBot2 instance for sending commands to the robot. */
    protected final MBot2 mbot;
    /** The service for managing the robot connection. */
    private final RobotService service;
    /** The cache for storing sensor data from the robot. */
    private final SensorCache sensors;
    private final TelemetryListener telemetry;

    /**
     * Constructs a new RobotController. This constructor handles robot discovery,
     * establishes a connection, and sets up telemetry. A shutdown hook is also
     * registered to ensure resources are released on JVM termination.
     */
    public RobotController(String robotName) {
        RobotDiscovery discovery = new UdpDiscoveryService();
        DiscoveredRobot discoveredRobot = discovery.discover(robotName);
        service = new TcpRobotService(discoveredRobot);
        //ServiceLocator.register(RobotService.class, service);
        service.connect();
        isClosed = false;
        mbot = new MBot2(service);
        sensors = new SensorCache();
        telemetry = mbot.registerTelemetry(9991, sensors);
        Runtime.getRuntime().addShutdownHook(new Thread(this::close));
    }

    /**
     * Closes the connection to the robot and releases all associated resources.
     * This method is synchronized and idempotent.
     */
    @Override
    public synchronized void close() {
        if (isClosed){
            return;
        }
        isClosed = true;
        if (service != null && service.isConnected()) {
            mbot.stopAllBehaviors();
            if (telemetry != null) {
                mbot.deregisterTelemetry(telemetry);
            }
            service.disconnect();
        }
    }

    /**
     * A blocking method that waits until new sensor data is available. This is
     * useful for synchronizing robot actions with sensor updates.
     */
    protected SensorSnapshot awaitNewData() {
        while (!sensors.hasNewData()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException ie) {}
        }
        return sensors.takeSnapshot();    // clears flag and returns snapshot atomically
    }

    /**
     * The main logic loop for the robot's behavior. Subclasses must implement
     * this method to define the robot's specific tasks and actions.
     */
    public abstract void run();

}
