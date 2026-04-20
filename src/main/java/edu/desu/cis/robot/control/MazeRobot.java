package edu.desu.cis.robot.control;

import edu.desu.cis.robot.service.SensorSnapshot;

/**
 * A specific implementation of a robot controller that navigates a maze,
 * identifies objects, and performs actions based on the object's color.
 *
 */
public class MazeRobot extends RobotController {

    /**
     * Constructs a new MazeRobot.
     */
    public MazeRobot(String robotName) {
        super(robotName);
    }

    public enum RobotState
    {
        CRUISE,
        IDENTIFY_OBJECT,
        AVOID_OBJECT,
        MOVE_OBJECT,
        COLLECT_SAMPLE,
        RETURN,
        STOP
    }

    private RobotState state;


    public void run() {
        state = RobotState.CRUISE;
        SensorSnapshot s;
        while (state != RobotState.STOP) {
            s = awaitNewData();// CORRECT — fresh snapshot every iteration
            switch (state) {
                case CRUISE:
                    System.out.println("CRUISE");
                    if (s.distance() < 20){
                        mbot.stop();
                        stete = RobotState.IDENTIFY_OBJECT;
                    }
                    break;
                case IDENTIFY_OBJECT:
                    System.out.println("IDENTIFY_OBJECT");
                    String color = mbot.getColorObjectFromCamera();
                    if (color.equals("blue")) {
                        state == RobotState.AVOID_OBJECT;
                    } else if (color.equals("green")) {
                        state == RobotState.MOVE_OBJECT;
                    }else if (color.equals("red")) {
                        state = RobotState.COLLECT_SAMPLE;
                    }
                    break;
                // ... other cases use s.distance(), s.lineStatus(), s.lineOffset()
                case AVOID_OBJECT:
                    //
                    System.out.println("AVOID_OBJECT");
                    break;
                case MOVE_OBJECT:
                    //
                    System.out.println("MOVE_OBJECT");
                    break;
                case COLLECT_SAMPLE:
                    //
                    System.out.println("COLLECT_SAMPLE");
                    break;
                case RETURN:
                    //
                    System.out.println("RETURN");
                    if (atInsertionPoint()){
                        mbot.stop();
                        System.out.println("VICTORY");
                        state = RobotState.STOP;
                    }
                    break;

            }
        }
    }

    /**
     * The main entry point for the MazeRobot application.
     * @param args Command line arguments (not used).
     */
    public static void main(String[] args) {
        try (MazeRobot amazin = new MazeRobot("StingBot")) {
            amazin.run();
        }
    }
}
