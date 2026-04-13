package edu.desu.cis.robot.service;

/**
 * An exception thrown when an error occurs within the robot service layer.
 * This class extends RuntimeException, indicating that it is an unchecked exception.
 * @author Marwan Rasamny
 * @version 0.5.0
 */
public class RobotServiceException extends RuntimeException {
    /**
     * Constructs a new RobotServiceException with the specified detail message and cause.
     * @param message The detail message (which is saved for later retrieval by the Throwable.getMessage() method).
     * @param cause The cause (which is saved for later retrieval by the Throwable.getCause() method).
     *              (A null value is permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public RobotServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new RobotServiceException with the specified detail message.
     * @param message The detail message (which is saved for later retrieval by the Throwable.getMessage() method).
     */
    public RobotServiceException(String message) {
        super(message);
    }
}
