package edu.desu.cis.robot.service;

/**
 * An interface defining the contract for communicating with a robot.
 * This service layer abstracts the underlying communication mechanism,
 * allowing for sending commands and receiving responses from the robot.
 * @author Marwan Rasamny
 * @version 0.5.0
 */
public interface RobotService extends AutoCloseable {

    /**
     * Establishes a connection to the robot.
     * @throws RobotServiceException if the connection fails.
     */
    void connect() throws RobotServiceException;

    /**
     * Disconnects from the robot.
     */
    void disconnect();

    /**
     * Sends a message (command) to the robot.
     * @param message The message string to send.
     * @throws RobotServiceException if there is an error sending the message.
     */
    void send(String message) throws RobotServiceException;

    /**
     * Receives a response message from the robot.
     * @return The response message as a string.
     * @throws RobotServiceException if there is an error receiving the message.
     */
    String receive() throws RobotServiceException;

    /**
     * Checks if the service is currently connected to a robot.
     * @return true if connected, false otherwise.
     */
    boolean isConnected();
}
