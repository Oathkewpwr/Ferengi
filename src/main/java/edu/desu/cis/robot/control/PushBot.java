package edu.desu.cis.robot.control;
import edu.desu.cis.robot.control.RobotController;
import edu.desu.cis.robot.service.SensorSnapshot;

public class PushBot extends RobotController {

    public PushBot(String robotName) {
        super(robotName);
    }

    @Override
    public void run() {
        System.out.println("Starting push test...");
        mbot.pushObject();
        System.out.println("Push complete!");



    }

    public static void main(String[] args) {
        try (PushBot robot = new PushBot("Liang")) {
            robot.run();
        }
    }
}