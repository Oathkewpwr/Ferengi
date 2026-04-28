package edu.desu.cis.robot.control;

import edu.desu.cis.robot.service.SensorSnapshot;

public class PushBot extends RobotController {

    public PushBot(String robotName) {
        super(robotName);
    }

    @Override
    public void run() {
        mbot.avoidCrashing(15);
        while (true) {
            SensorSnapshot sensor = awaitNewData();
            if (sensor.distance() <= 15) {
                mbot.stopBehavior("AVOID_CRASHING");
                mbot.pushObject();
                mbot.avoidCrashing(15);
            }
        }
    }

    public static void main(String[] args) {
        try (PushBot robot = new PushBot("Liang")) {
            robot.run();
        }
    }
}