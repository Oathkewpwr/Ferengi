package edu.desu.cis.robot.discovery;

/**
 * Represents a robot discovered on the network.
 * This record holds essential information about a discovered robot,
 * including its unique identifier, IP address, and the port it's listening on.
 *
 * @param robotId The unique identifier of the robot.
 * @param ip The IP address of the robot.
 * @param port The port number the robot's service is listening on.
 * @author Marwan Rasamny
 * @version 0.5.0
 */
public record DiscoveredRobot(String robotId, String ip, int port) {
}
