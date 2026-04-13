package edu.desu.cis.robot.service;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;

/**
 * A listener that runs in a separate thread to receive telemetry data from the robot over UDP.
 * It parses the data and updates a {@link SensorCache}.
 * @author Marwan Rasamny
 * @version 0.5.0
 */
public class TelemetryListener implements Runnable, AutoCloseable {

    private final int port;
    private final SensorCache cache;
    private volatile boolean running = true;
    private DatagramSocket socket;

    /**
     * Constructs a new TelemetryListener.
     * @param port The UDP port to listen on for telemetry data.
     * @param cache The SensorCache to update with new data.
     */
    public TelemetryListener(int port, SensorCache cache) {
        this.port = port;
        this.cache = cache;
    }

    /**
     * The main loop for the listener thread. It continuously listens for UDP packets,
     * parses them as telemetry messages, and updates the sensor cache.
     */
    @Override
    public void run() {
        try {
            socket = new DatagramSocket(port);

            byte[] buffer = new byte[4096];
            System.out.println("Telemetry listening on UDP " + port);

            while (running) {
                DatagramPacket packet =
                        new DatagramPacket(buffer, buffer.length);

                socket.receive(packet);

                String msg = new String(
                        packet.getData(),
                        0,
                        packet.getLength(),
                        StandardCharsets.UTF_8
                );
                //System.out.println("Recevied Message:"+msg);
                handleTelemetry(msg);
            }

        } catch (Exception e) {
            if (running)
                e.printStackTrace();
        }
    }

    /**
     * Gets the port this listener is running on.
     * @return The UDP port number.
     */
    public int getPort(){
        return port;
    }

    /**
     * Handles an incoming telemetry message in JSON format.
     * @param json The JSON string containing telemetry data.
     */
    private void handleTelemetry(String json) {
        cache.updateFromTelemetry(json);
    }

    /**
     * Stops the telemetry listener and closes the underlying socket.
     */
    public void stop() {
        running = false;
        if (socket != null && !socket.isClosed())
            socket.close();
    }

    /**
     * Implements the AutoCloseable interface to ensure resources are released.
     * This method simply calls {@link #stop()}.
     */
    @Override
    public void close() {
        stop();
    }
}

