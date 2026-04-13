package edu.desu.cis.robot.service;

import edu.desu.cis.robot.discovery.DiscoveredRobot;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

/**
 * An implementation of {@link RobotService} that uses TCP/IP for communication with a discovered robot.
 * This service establishes a socket connection, sends messages, and receives responses over TCP.
 * It also handles connection and disconnection logic.
 * @author Marwan Rasamny
 * @version 0.5.0
 */
public class TcpRobotService implements RobotService {

    private final DiscoveredRobot robot;

    private Socket socket;
    private BufferedReader in;
    private BufferedWriter out;

    /**
     * Constructs a new TcpRobotService.
     * @param robot The {@link DiscoveredRobot} instance containing the robot's connection details.
     */
    public TcpRobotService(DiscoveredRobot robot) {
        this.robot = robot;
    }

    /**
     * Establishes a TCP connection to the robot.
     * If already connected, this method does nothing.
     * @throws RobotServiceException if the connection cannot be established.
     */
    @Override
    public synchronized void connect() {
        if (isConnected()) {
            return;
        }

        try {
            socket = new Socket(robot.ip(), robot.port());
            //socket.setSoTimeout(5000); // read timeout (ms)

            in = new BufferedReader(
                    new InputStreamReader(
                            socket.getInputStream(),
                            StandardCharsets.UTF_8
                    )
            );

            out = new BufferedWriter(
                    new OutputStreamWriter(
                            socket.getOutputStream(),
                            StandardCharsets.UTF_8
                    )
            );

        } catch (IOException e) {
            throw new RobotServiceException(
                    "Failed to connect to robot " + robot.robotId(), e
            );
        }
    }

    /**
     * Disconnects from the robot and closes all associated resources.
     * This method can be called multiple times safely.
     */
    @Override
    public synchronized void disconnect() {
        closeQuietly(in);
        closeQuietly(out);
        closeQuietly(socket);

        in = null;
        out = null;
        socket = null;
        System.out.println("Closed socket!");
    }

    /**
     * Checks if the service is currently connected to the robot.
     * @return true if the socket is connected and not closed, false otherwise.
     */
    @Override
    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    // -------------------------------------------------
    // Transport operations
    // -------------------------------------------------

    /**
     * Sends a message to the connected robot.
     * A newline character is appended to the message as a protocol delimiter.
     * @param message The message string to send.
     * @throws RobotServiceException if the service is not connected or if an I/O error occurs.
     */
    @Override
    public synchronized void send(String message) {
        ensureConnected();

        try {
            out.write(message);
            out.write("\n");   // protocol delimiter
            out.flush();
        } catch (IOException e) {
            throw new RobotServiceException("Failed to send message", e);
        }
    }

    /**
     * Receives a message from the connected robot.
     * This method blocks until a line is received or a timeout occurs.
     * @return The received message string.
     * @throws RobotServiceException if the service is not connected, if the connection is closed,
     *                               or if an I/O error or timeout occurs.
     */
    @Override
    public synchronized String receive() {
        ensureConnected();

        try {
            String line = in.readLine();

            if (line == null) {
                throw new RobotServiceException(
                        "Connection closed by robot"
                );
            }

            return line;

        } catch (SocketTimeoutException e) {
            throw new RobotServiceException("Timed out waiting for response", e);
        } catch (IOException e) {
            throw new RobotServiceException("Failed to receive message", e);
        }
    }

    // -------------------------------------------------
    // AutoCloseable
    // -------------------------------------------------

    /**
     * Closes the service, disconnecting from the robot.
     * This method is part of the AutoCloseable interface.
     */
    @Override
    public void close() {
        disconnect();
    }

    // -------------------------------------------------
    // Helpers
    // -------------------------------------------------

    /**
     * Ensures that the service is connected.
     * @throws IllegalStateException if the service is not connected.
     */
    private void ensureConnected() {
        if (!isConnected()) {
            throw new IllegalStateException(
                    "RobotService is not connected"
            );
        }
    }

    /**
     * Closes a {@link Closeable} resource quietly, suppressing any {@link IOException}.
     * @param c The Closeable resource to close.
     */
    private void closeQuietly(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException ignored) {
            }
        }
    }
}
