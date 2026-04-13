package edu.desu.cis.robot.control;
import edu.desu.cis.robot.control.RobotController;
public class SquareRobot extends RobotController {
    public SquareRobot(String robotName) {
        super(robotName);
    }
    @Override
    public void run() {
        for(int count = 0; count < 4;count++){
            mbot.straight(30);
            mbot.turnRight(90);
        }
    }
    public static void main(String[] args) {
        try (SquareRobot robot = new SquareRobot("Liang")) {
            robot.run();
        }
    }
}

