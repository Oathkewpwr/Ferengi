package edu.desu.cis.robot.control;
import edu.desu.cis.robot.control.RobotController;
public class ObstacleAvoider extends RobotController {
    public ObstacleAvoider(String robotName) {
        super(robotName);
    }
    @Override
    public void run() {
        // Start the background behavior — returns immediately
        mbot.steerAround(25, 40, 20);
        // Let it roam for 20 seconds, then stop
        try {
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        mbot.stopBehavior("STEER_AROUND");
    }
    public static void main(String[] args) {
        try (ObstacleAvoider robot = new ObstacleAvoider("Liang")) {
            robot.run();
        }
    }
}