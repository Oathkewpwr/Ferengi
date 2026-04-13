package edu.desu.cis.robot.control;
import edu.desu.cis.robot.control.RobotController;
import edu.desu.cis.robot.service.SensorSnapshot;

public class RovingRobot extends RobotController {
    public RovingRobot(String robotName) {
        super(robotName);
    }
    @Override
    public void run() {
        mbot.avoidCrashing(15);
        mbot.forward(50);
        while (true) {
            SensorSnapshot sensors = awaitNewData();
            if (sensors.distance() <= 15) {
                mbot.turnRight(90);
                mbot.forward(50);
            }
        }
    }
    public static void main(String[] args) {
        try (RovingRobot robot = new RovingRobot("Liang")) {
            robot.run();
        }
    }
}