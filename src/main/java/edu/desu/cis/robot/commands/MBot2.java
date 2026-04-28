package edu.desu.cis.robot.commands;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.desu.cis.robot.protocol.MBot2JsonCodec;
import edu.desu.cis.robot.protocol.JsonResponse;
import edu.desu.cis.robot.service.RobotService;
import edu.desu.cis.robot.service.SensorCache;
import edu.desu.cis.robot.service.TelemetryListener;

import java.util.List;
import java.util.Map;

/**
 * Represents an MBot2 robot and provides an interface for controlling it.
 * This class encapsulates the communication with the robot's service layer,
 * allowing for high-level commands to be sent to the robot.
 * @author Marwan Rasamny
 * @version 0.5.0
 */
public class MBot2 {

    private final RobotService service;
    private final MBot2JsonCodec codec = new MBot2JsonCodec();

    /**
     * Constructs a new MBot2 instance.
     * @param service The RobotService to use for communication with the robot.
     */
    public MBot2(RobotService service) {
        this.service = service;
    }

    // Motion methods

    /**
     * Moves the robot straight for a specified distance.
     * @param distanceInCm The distance to move in centimeters.
     */
    public void straight(double distanceInCm) {
        execute("MOVE_STRAIGHT",
                Map.of("distance", distanceInCm)
        );
    }

    /**
     * Moves the robot forward for a specified amount of time.
     * @param time The duration to move forward in seconds.
     */
    public void forward(double speed, double time) {
        execute("MOVE_TIME",
                Map.of(
                        "time", time,
                        "speed", speed
                )
        );
    }

    /**
     * Moves the robot forward indefinitely.
     */
    public void forward(double speed) {
        execute("MOVE_TIME",
                Map.of(
                        "time", -1,
                        "speed", speed
                )
        );
    }

    /**
     * Turns the robot left by a specified number of degrees.
     * @param degrees The angle to turn in degrees.
     */
    public void turnLeft(double degrees) {
        execute(
                "TURN",
                Map.of("degrees", degrees)
        );
    }

    /**
     * Turns the robot right by a specified number of degrees.
     * @param degrees The angle to turn in degrees.
     */
    public void turnRight(double degrees) {
        execute(
                "TURN",
                Map.of("degrees", -degrees)
        );
    }

    /**
     * Turns the robot to the left like a four-wheeled robot.
     *
     * @param speed The speed of the robot.
     * @param duration Amount of time in seconds to move the robot.
     *                 0 or negative will move it indefinitely.
     * @param diff The differential value, which controls the arclength of
     *             the turn.
     *
     */
    public void moveAndTurnLeft(double speed, double duration, double diff) {
        execute(
                "MOVE_AND_TURN",
                Map.of(
                        "speed", speed,
                        "duration", duration,
                        "diff", diff,
                        "is_left", true
                )
        );
    }

    /**
     * Turns the robot to the left like a four-wheeled robot.
     *
     * @param speed The speed of the robot.
     * @param duration Amount of time in seconds to move the robot.
     *                 0 or negative will move it indefinitely.
     * @param diff The differential value, which controls the arclength of
     *             the turn.
     *
     */
    public void moveAndTurnRight(double speed, double duration, double diff) {
        execute(
                "MOVE_AND_TURN",
                Map.of(
                        "speed", speed,
                        "duration", duration,
                        "diff", diff,
                        "is_left", false
                )
        );
    }

    /**
     * Set the left and right motor power.
     *
     * @param leftPower The power to set on the left motor.  Value must be between -100 and 100.
     * @param rightPower The power to set on the right motor.  Value must be between -100 and 100.
     */
    public void setMotorPower(double leftPower, double rightPower){
        execute(
                "SET_MOTOR_POWER",
                Map.of(
                        "left", leftPower,
                        "right", rightPower
                )
        );
    }

    /**
     * Set the left motor power.
     *
     * @param power The power to set on the left motor.  Value must be between -100 and 100.
     */
    public void setLeftMotorPower(double power){
        execute(
                "SET_MOTOR_POWER",
                Map.of(
                        "left", power,
                        "right", "AS_IS"
                )
        );
    }

    /**
     * Set the right motor power.
     *
     * @param power The power to set on the right motor.  Value must be between -100 and 100.
     */
    public void setRightMotorPower(double power){
        execute(
                "SET_MOTOR_POWER",
                Map.of(
                        "left", "AS_IS",
                        "right", power
                )
        );
    }

    /**
     * Stops the robot's movement.
     */
    public void stop(){
        execute("STOP", null);
    }

    // LED methods

    /**
     * Turns on a specific LED with a given color.
     * @param id The ID of the LED to turn on.
     * @param red The red component of the color (0-255).
     * @param green The green component of the color (0-255).
     * @param blue The blue component of the color (0-255).
     */
    public void turnLedOn(int id, int red, int green, int blue){
        execute("LED",
                Map.of(
                        "status", "ON",
                        "id", id,
                        "red", red,
                        "green", green,
                        "blue", blue
                )
        );
    }

    /**
     * Displays a color pattern on the LEDs.
     * @param colorPattern A string of color(s) of the five LEDs, set in the color1 color2 color3 color4 color5 mode,
     *                     with one space between any two colors. If you set more than five colors, only the first
     *                     five colors are used. You can set this parameter to the full name or abbreviation of the
     *                     colors. The options include the following:
     *
     *                      red, r
     *                      green, g
     *                      blue, b
     *                      yellow, y
     *                      cyan, c
     *                      purple, p
     *                      white, w
     *                      orange, o
     *                      black, k
     *
     */
    public void showLed(String colorPattern){
        execute("LED",
                Map.of(
                        "status", "SHOW",
                        "color", colorPattern
                )
        );
    }

    /**
     * Moves the LED display by a certain step.
     * @param step The step to move the LED display.
     */
    public void moveLed(int step){
        execute("LED",
                Map.of(
                        "status", "MOVE",
                        "step", step
                )
        );
    }

    /**
     * Turns off a specific LED.
     * @param id The ID of the LED to turn off.  if id < 1 or id > 0
     *           then all 5 LEDs are turned off.
     */
    public void turnLedOff(int id){
        execute("LED",
                Map.of(
                        "status", "OFF",
                        "id", id
                )
        );
    }

    /**
     * Turns on the camera light.
     *
     */
    public void turnCameraLightOn(){
        execute("CAMERA_LED",
                Map.of(
                        "status", "ON"
                )
        );
    }

    /**
     * Turns off the camera light.
     *
     */
    public void turnCameraLightOff(){
        execute("CAMERA_LED",
                Map.of(
                        "status", "OFF"
                )
        );
    }

    // Sensor methods

    /**
     * Reads the distance from the ultrasonic sensor.
     * @return The distance in centimeters or -1 if it fails.
     */
    public double readUltrasonic() {
        CommandResult<JsonNode> result = execute(
                "GET_SENSOR",
                Map.of("sensor", "ULTRASONIC")
        );
        if (!result.isSuccessful()){
            return -1;
        }
        return result.data().get("distance_cm").asDouble();
    }

    /**
     * Reads the status of the line-following sensor.
     * @return The line status or -1 if it fails.
     */
    public int readLineStatus() {
        CommandResult<JsonNode> result = execute(
                "GET_SENSOR",
                Map.of("sensor", "LINE_STATUS")
        );
        if (!result.isSuccessful()){
            return -1;
        }
        return result.data().get("line_status").asInt();
    }

    /**
     * Reads the offset from the line for tracking purposes.
     * @return The line offset track value.
     */
    public int readLineOffsetTrack() {
        CommandResult<JsonNode> result = execute(
                "GET_SENSOR",
                Map.of("sensor", "LINE_OFFSET")
        );
        if (!result.isSuccessful()){
            return 0;
        }
        return result.data().get("line_offset").asInt();
    }

    /**
     * Gets the color of an object detected by the camera.
     * @return The detected color as a string.
     */
    public String getColorObjectFromCamera() {
        return getColorObjectFromCamera(true);
    }

    /**
     * Gets the color of an object detected by the camera.
     *
     * @param needsLight if true, turns light on before reading label then
     *                   turns the light back off; otherwise, does not activate
     *                   the light.
     * @return The detected color as a string.
     */
    public String getColorObjectFromCamera(boolean needsLight) {
        CommandResult<JsonNode> result =
                execute("GET_SENSOR",
                    Map.of(
                            "sensor", "CAMERA_COLOR",
                            "light", needsLight ? "YES" : "NO"
                    )
                );

        return result.data().get("color").asText();
    }

    /**
     * Gets the labels of objects detected by the camera.

     * @return A list of integer labels for the detected objects or null
     */
    public List<Integer> getLabelFromCamera() {
        return getLabelFromCamera(true);
    }

    /**
     * Gets the labels of objects detected by the camera.
     *
     * @param needsLight if true, turns light on before reading label then
     *                   turns the light back off; otherwise, does not activate
     *                   the light.
     * @return A list of integer labels for the detected objects or null
     */
    public List<Integer> getLabelFromCamera(boolean needsLight) {
        CommandResult<JsonNode> result =
                execute("GET_SENSOR",
                        Map.of(
                                "sensor", "CAMERA_LABEL",
                                "light", needsLight ? "YES" : "NO"
                        )
                );

        JsonNode data = result.data().get("labels");
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.treeToValue(data, List.class);
        } catch (IllegalArgumentException | JsonProcessingException e){
            System.out.println(e);
            return null;
        }
    }

    /**
     * Registers a telemetry listener for the robot.
     * @param port The port to listen for telemetry data on.
     * @param cache The SensorCache to store the telemetry data in.
     * @return A TelemetryListener instance if registration is successful, null otherwise.
     */
    public TelemetryListener registerTelemetry(int port, SensorCache cache){
        // start telemetry listener before registration
        TelemetryListener telemetry = new TelemetryListener(port, cache);
        new Thread(telemetry).start();
        CommandResult<JsonNode> result = execute(
                "TELEMETRY",
                Map.of(
                        "action","REGISTER",
                        "port", port
                )
        );

        if (!result.isSuccessful()){
            // failed to register - stop listener
            telemetry.stop();
            telemetry = null;
        }
        return telemetry;
    }

    /**
     * Deregisters a telemetry listener from the robot.
     * @param telemetry The TelemetryListener to deregister.
     */
    public void deregisterTelemetry(TelemetryListener telemetry) {
        if (telemetry == null) return;
        int port = telemetry.getPort(); // capture before stop
        telemetry.stop();
        execute("TELEMETRY",
                Map.of(
                        "action", "DEREGISTER",
                        "port", port)
        );
    }

    //==============================================
    // Behavior-based commands
    //==============================================

    /**
     * Commands the robot to enable its anti-crashing behavior.
     * @param thresholdInCm The distance threshold in centimeters to avoid a crash.
     */
    public void avoidCrashing(double thresholdInCm) {
        execute("AVOID_CRASHING",
                Map.of(
                        "threshold",thresholdInCm
                )
        );
    }

    /**
     * Commands the robot to stop when a line is detected.
     */
    public void stopAtLine() {
        execute("STOP_AT_LINE", null);
    }

    /**
     * Stops a specific behavior on the robot.
     * @param behaviorName The name of the behavior to stop.
     */
    public void stopBehavior(String behaviorName) {
        execute(
                "STOP_BEHAVIOR",
                Map.of("behavior_name", behaviorName)
        );
    }


    /**
     * Stops all active behaviors on the robot.
     */
    public void stopAllBehaviors() {
        execute("STOP_ALL_BEHAVIORS", null);
    }





    private CommandResult<JsonNode> execute(String command, Map<String,Object> params) {

        String json = codec.encodeCommand(command, params);
        service.send(json);
        JsonResponse response =
                codec.decodeResponse(service.receive());

        return new CommandResult<JsonNode>(
                response.isOk(),
                response.getData(),
                !response.isOk()
                        ? "ERROR("+response.getErrorCode() + "): "
                        : "" + response.getMessage()
        );

    }

    /**
     * Commands the robot to drive forward continuously, arcing left whenever
     * an obstacle is detected within the threshold distance.
     * Returns immediately; the behavior runs in the background until stopped.
     *
     * @param thresholdCm  Distance in centimetres at which to begin steering (e.g. 25).
     * @param speed        Forward speed for both motors (0–100).
     * @param diff         Amount to reduce the inner motor to create the arc (0 to speed).
     */
    public void steerAround(double thresholdCm, double speed, double diff) {
        execute("STEER_AROUND",
                Map.of(
                        "threshold", thresholdCm,
                        "speed",     speed,
                        "diff",      diff
                )
        );
    }

    /**
     * Flashes all LEDs a given number of times in the specified color.
     * Blocks until all flashes are complete.
     *
     * @param times  Number of flashes (1–20).
     * @param red    Red component (0–255).
     * @param green  Green component (0–255).
     * @param blue   Blue component (0–255).
     * @param delay  On/off duration in seconds (e.g. 0.3).
     */
    public void flashLed(int times, int red, int green, int blue, double delay) {
        execute("FLASH_LED",
                Map.of(
                        "times", times,
                        "red",   red,
                        "green", green,
                        "blue",  blue,
                        "delay", delay
                )
        );
    }

    /**
     * Commands the robot to walk left of the line.
     */
    public void followLine() {
        execute("FOLLOW_LINE", null);
    }

    /**
     * Starts the AVOID_OBJECT behavior on the mbot2.
     * The robot reverses, turns away from the obstacle,
     * moves past it, then turns back to its original direction.
     */
    public void avoidObject(String turnDirection, double reverseDist, double forwardDist, int turnDeg){
        execute("AVOID_OBJECT",
                Map.of(
                        "turn_direction", turnDirection,
                        "reverse_dist", reverseDist,
                        "forward_dist", forwardDist,
                        "turn_deg", turnDeg
                )
        );
    }

}
