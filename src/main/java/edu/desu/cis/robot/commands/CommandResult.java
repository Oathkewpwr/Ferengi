package edu.desu.cis.robot.commands;

/**
 * Represents the result of a robot command execution.
 * This record encapsulates the outcome of a command, including whether it was successful,
 * any data returned by the command, and a message providing more details.
 *
 * @param isSuccessful true if the command was successful, false otherwise.
 * @param data The data returned by the command.
 * @param message A message associated with the command's execution.
 * @param <T> The type of data returned by the command.
 * @author Marwan Rasamny
 * @version 0.5.0
 */
public record CommandResult<T>(
        boolean isSuccessful,
        T data,
        String message
) {}
