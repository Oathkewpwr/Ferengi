package edu.desu.cis.robot.commands;

/**
 * An exception thrown when an error occurs during the execution of a robot command.
 * This class extends RuntimeException, indicating that it is an unchecked exception.
 * @author Marwan Rasamny
 * @version 0.5.0
 */
public class RobotCommandException extends RuntimeException {
    /**
     * Constructs a new RobotCommandException with the specified detail message and cause.
     * @param message The detail message (which is saved for later retrieval by the Throwable.getMessage() method).
     * @param cause The cause (which is saved for later retrieval by the Throwable.getCause() method).
     *              (A null value is permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public RobotCommandException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new RobotCommandException with the specified detail message.
     * @param message The detail message (which is saved for later retrieval by the Throwable.getMessage() method).
     */
    public RobotCommandException(String message) {
        super(message);
    }
}