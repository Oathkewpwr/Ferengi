package edu.desu.cis.robot.control;
import edu.desu.cis.robot.control.RobotController;
public class CollectSample extends RobotController {
    /**
     * Constructs a new RobotController. This constructor handles robot discovery,
     * establishes a connection, and sets up telemetry. A shutdown hook is also
     * registered to ensure resources are released on JVM termination.
     *
     * @param robotName
     */
    public CollectSample(String robotName) {
        super(robotName);
    }

    @Override
    public void run() {
        System.out.println("COLLECT_SAMPLE");
        mbot.stopAllBehaviors();
        mbot.stop();
        /*mbot.turnRight(360);
        try { Thread.sleep(3000); } catch (Exception e) {}
        mbot.flashLed(5, 255, 0, 0, 0.3);
        try { Thread.sleep(1000); } catch (Exception e) {}
        mbot.flashLed(3, 0, 255, 0, 0.3);
        try { Thread.sleep(1000); } catch (Exception e) {}
        mbot.straight(5);
        try { Thread.sleep(1000); } catch (Exception e) {}
        mbot.straight(-5);
        try { Thread.sleep(1000); } catch (Exception e) {}*/
        /*mbot.turn(180);
        mbot.turn(180);
        mbot.flashLed(5, 255, 0, 0, 0.3);
        mbot.flashLed(3, 0, 255, 0, 0.3);
        mbot.straight(5);
        mbot.straight(-5);*/
        String color = mbot.getColorObjectFromCamera();
        System.out.println("Raw color returned: [" + color + "]");
        System.out.println("Equals RED: " + color.equals("RED"));
        System.out.println("Equals BLUE: " + color.equals("BLUE"));
        System.out.println("Equals GREEN: " + color.equals("GREEN"));
        System.out.println("Equals None: " + color.equals("None"));
    }

    public static void main(String[] args) {
        try (CollectSample robot = new CollectSample("Liang")) {
            robot.run();
        }
    }
}
