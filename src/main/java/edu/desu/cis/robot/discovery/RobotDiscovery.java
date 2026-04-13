package edu.desu.cis.robot.discovery;

/**
 * An interface for discovering robots on the network.
 * Implementations of this interface provide methods to find and connect to robots.
 * @author Marwan Rasamny
 * @version 0.5.0
 */
public interface RobotDiscovery {
    /**
     * Discovers a robot with the specified robot ID.
     * @param robotId The unique identifier of the robot to discover.
     * @return A DiscoveredRobot object containing information about the discovered robot.
     */
    DiscoveredRobot discover(String robotId);
}
