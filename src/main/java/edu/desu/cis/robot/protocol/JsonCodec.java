package edu.desu.cis.robot.protocol;

import java.util.Map;

/**
 * An interface for encoding commands into JSON and decoding JSON responses.
 * Implementations of this interface handle the serialization and deserialization
 * of messages exchanged with the robot.
 * @author Marwan Rasamny
 * @version 0.5.0
 */
public interface JsonCodec {

    /**
     * Encodes a command and its parameters into a JSON string.
     * @param command The name of the command.
     * @param params A map of parameters for the command.
     * @return A JSON string representing the encoded command.
     */
    String encodeCommand(String command, Map<String, Object> params);

    /**
     * Decodes a JSON response string into a {@link JsonResponse} object.
     * @param json The JSON string to decode.
     * @return A JsonResponse object parsed from the JSON string.
     */
    JsonResponse decodeResponse(String json);
}
