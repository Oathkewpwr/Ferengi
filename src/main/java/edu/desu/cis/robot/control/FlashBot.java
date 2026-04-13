package edu.desu.cis.robot.control;
import edu.desu.cis.robot.control.RobotController;
public class FlashBot extends RobotController {
    public FlashBot(String robotName) {
        super(robotName);
    }
    @Override
    public void run() {
        // Flash blue 5 times, then flash red 3 times
        mbot.flashLed(5, 0,   0,   255, 0.3);
        mbot.flashLed(3, 255, 0,   0,   0.2);
        // Drive forward 20 cm AFTER flashing is complete
        mbot.straight(20);
    }
    public static void main(String[] args) {
        try (FlashBot robot = new FlashBot("Liang")) {
            robot.run();
        }
    }
}