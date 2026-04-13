package edu.desu.cis.robot.protocol;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Map;
import java.util.UUID;

/**
 * An implementation of {@link JsonCodec} specifically for the MBot2 robot's JSON protocol.
 * This class handles the encoding of commands to JSON strings and decoding of JSON responses
 * from the MBot2 robot.
 * @author Marwan Rasamny
 * @version 0.5.0
 */
public class MBot2JsonCodec implements JsonCodec {

    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Encodes a command and its parameters into a JSON string suitable for the MBot2 robot.
     * The command will be structured with a "COMMAND" type, a unique ID, and the given
     * command and parameters within the "payload".
     *
     * @param command The name of the command to be sent (e.g., "MOVE_TIME", "LED").
     * @param parameters A map of string keys to object values representing the command's parameters.
     *                   Can be null if the command has no parameters.
     * @return A JSON string representing the encoded command.
     * @throws RuntimeException if there is an error during JSON processing.
     */
    public String encodeCommand(String command,
                                Map<String, Object> parameters) {

        ObjectNode root = mapper.createObjectNode();

        root.put("type", "COMMAND");
        root.put("id", UUID.randomUUID().toString());

        ObjectNode payload = root.putObject("payload");
        payload.put("command", command);

        ObjectNode paramsNode = payload.putObject("parameters");
        if (parameters != null) {
            parameters.forEach(paramsNode::putPOJO);
        }
        //System.out.println(toJson(root));
        return toJson(root);
    }

    /**
     * Decodes a JSON response string from the MBot2 robot into a {@link JsonResponse} object.
     *
     * @param json The JSON string received from the robot.
     * @return A {@link JsonResponse} object containing the parsed response.
     * @throws RuntimeException if the JSON string is invalid or cannot be processed.
     */
    public JsonResponse decodeResponse(String json) {
        try {
            return mapper.readValue(json, JsonResponse.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(
                    "Invalid JSON response from robot", e
            );
        }
    }

    private String toJson(ObjectNode node) {
        try {
            return mapper.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(
                    "Failed to encode JSON command", e
            );
        }
    }
}
