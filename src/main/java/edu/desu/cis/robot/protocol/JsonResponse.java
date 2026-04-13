package edu.desu.cis.robot.protocol;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Represents a standardized JSON response received from the robot.
 * This class is used to deserialize JSON strings into a structured object,
 * providing easy access to the response's status, message, error code, and data payload.
 *
 * @author Marwan Rasamny
 * @version 0.5.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JsonResponse {

    private Payload payload;
    private String id;
    private String type;

    // --- Inner Payload Class ---
    /**
     * Represents the payload section of a JSON response.
     * Contains the status, message, error code, and actual data of the response.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Payload {
        private String status;
        private String message;
        private String errorCode;
        private JsonNode data;

        /**
         * Default constructor for Jackson deserialization.
         */
        public Payload() {} // Default constructor for Jackson

        /**
         * Gets the status of the payload (e.g., "OK", "ERROR").
         * @return The status string.
         */
        public String getStatus() { return status; }
        /**
         * Sets the status of the payload.
         * @param status The status string to set.
         */
        public void setStatus(String status) { this.status = status; }

        /**
         * Gets the message associated with the payload.
         * @return The message string.
         */
        public String getMessage() { return message; }
        /**
         * Sets the message associated with the payload.
         * @param message The message string to set.
         */
        public void setMessage(String message) { this.message = message; }

        /**
         * Gets the error code, if the status is "ERROR".
         * @return The error code string.
         */
        public String getErrorCode() { return errorCode; }
        /**
         * Sets the error code.
         * @param errorCode The error code string to set.
         */
        public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

        /**
         * Gets the data payload as a {@link JsonNode}.
         * @return The data as a JsonNode.
         */
        public JsonNode getData() { return data; }
        /**
         * Sets the data payload.
         * @param data The JsonNode data to set.
         */
        public void setData(JsonNode data) { this.data = data; }
    }

    // --- JsonResponse Setters (Required for Jackson) ---

    /**
     * Sets the payload of the JSON response.
     * @param payload The {@link Payload} object to set.
     */
    public void setPayload(Payload payload) {
        this.payload = payload;
    }

    /**
     * Sets the ID of the JSON response.
     * @param id The ID string to set.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Sets the type of the JSON response.
     * @param type The type string to set.
     */
    public void setType(String type) {
        this.type = type;
    }

    // --- Convenience Helper Methods ---

    /**
     * Checks if the response status is "OK".
     * @return true if the status is "OK", false otherwise.
     */
    public boolean isOk() {
        return payload != null && "OK".equalsIgnoreCase(payload.getStatus());
    }

    /**
     * Checks if the response status is "ERROR".
     * @return true if the status is "ERROR", false otherwise.
     */
    public boolean isError() {
        return payload != null && "ERROR".equalsIgnoreCase(payload.getStatus());
    }

    /**
     * Retrieves the data payload from the response.
     * @return The {@link JsonNode} containing the response data, or null if no payload exists.
     */
    public JsonNode getData() {
        return (payload != null) ? payload.getData() : null;
    }

    /**
     * Retrieves the message from the response payload.
     * @return The message string, or null if no payload exists.
     */
    public String getMessage() {
        return (payload != null) ? payload.getMessage() : null;
    }

    /**
     * Retrieves the error code from the response payload.
     * @return The error code string, or null if no payload exists.
     */
    public String getErrorCode() {
        return (payload != null) ? payload.getErrorCode() : null;
    }
}