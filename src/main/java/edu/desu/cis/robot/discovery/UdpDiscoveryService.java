package edu.desu.cis.robot.discovery;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.UUID;

/**
 * An implementation of the {@link RobotDiscovery} interface that uses UDP broadcast
 * to discover robots on the local network.
 * This service sends a discovery message and listens for responses from available robots.
 * @author Marwan Rasamny
 * @version 0.5.0
 */
public class UdpDiscoveryService implements RobotDiscovery {

    private static final int DISCOVERY_PORT = 9998;
    private static final int RESPONSE_TIMEOUT_MS = 3000;

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Discovers a robot with the specified ID using UDP broadcast.
     * It sends a discovery message to all broadcast addresses and waits for a response.
     * @param robotId The ID of the robot to discover.
     * @return A {@link DiscoveredRobot} object containing the robot's details.
     * @throws RuntimeException if no robot is found within the timeout or if an error occurs during discovery.
     */
    @Override
    public DiscoveredRobot discover(String robotId) {
        String msg = """
        {
          "type": "DISCOVERY",
          "id": "%s",
          "payload": {
            "action": "DISCOVER",
            "robot": "%s"
          }
        }
        """.formatted(UUID.randomUUID().toString(),robotId);

        byte[] data = msg.getBytes(StandardCharsets.UTF_8);

        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setBroadcast(true);
            socket.setSoTimeout(RESPONSE_TIMEOUT_MS);

            // Send discovery on all broadcast addresses
            Enumeration<NetworkInterface> interfaces =
                    NetworkInterface.getNetworkInterfaces();

            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();

                if (!ni.isUp() || ni.isLoopback()) {
                    continue;
                }

                for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
                    InetAddress broadcast = ia.getBroadcast();
                    if (broadcast == null) {
                        continue;
                    }

                    DatagramPacket packet =
                            new DatagramPacket(data, data.length, broadcast, DISCOVERY_PORT);

                    socket.send(packet);
                }
            }

            // Wait for response
            byte[] buffer = new byte[2048];
            DatagramPacket response = new DatagramPacket(buffer, buffer.length);
            socket.receive(response);

            String responseJson = new String(
                    response.getData(),
                    0,
                    response.getLength()
            );

            JsonNode root = mapper.readTree(responseJson);

            JsonNode payload = root.path("payload");

            int commandPort = payload.path("commandPort").asInt();
            socket.close();

            String ip = response.getAddress().getHostAddress();
            return new DiscoveredRobot(robotId, ip, commandPort);

        } catch (SocketTimeoutException e) {
            throw new RuntimeException("No robot found (timeout)", e);
        } catch (Exception e) {
            throw new RuntimeException("Robot discovery failed", e);
        }
    }
}
