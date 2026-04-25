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
        STOP
    }

    private RobotState state;
    private boolean hasSample = false;


    public void run() {
        state = RobotState.CRUISE;
        SensorSnapshot s;
        while (state != RobotState.STOP) {
            s = awaitNewData();// CORRECT — fresh snapshot every iteration
            switch (state) {
                case CRUISE:
                    System.out.println("CRUISE");
                    //TODO: start navigating（contains avoid_crushing)
                    // Is Object Detected --> IDENTIFY_OBJECT
                    mbot.followLine();
                    mbot.avoidCrashing(15);
                    if (s.distance() < 20){
                        mbot.stopAllBehaviors();
                        mbot.stop();
                        state = RobotState.IDENTIFY_OBJECT;
                    }
                    break;
                case IDENTIFY_OBJECT:
                    System.out.println("IDENTIFY_OBJECT");
                    String color = mbot.getColorObjectFromCamera();
                    if (color.equals("blue")) {
                        state = RobotState.AVOID_OBJECT;
                    } else if (color.equals("green")) {
                        state = RobotState.MOVE_OBJECT;
                    }else if (color.equals("red")) {
                        state = RobotState.COLLECT_SAMPLE;
                    }else if (color.equals("yellow") && hasSample) {
                            mbot.stop();
                            mbot.stopAllBehaviors();
                            System.out.println("VICTORY");
                            state = RobotState.STOP;
                    }else {
                        state = RobotState.AVOID_OBJECT;
                    }
                    break;
                // ... other cases use s.distance(), s.lineStatus(), s.lineOffset()
                case AVOID_OBJECT:
                    System.out.println("AVOID_OBJECT");
                    // TODO: AVIOD, mbot.steer_around()?
                    // Is All Clear --> CRUISE
                    if (s.distance() > 30) {
                        mbot.stopAllBehaviors();
                        state = RobotState.CRUISE;
                    }
                    break;
                case MOVE_OBJECT:
                    System.out.println("MOVE_OBJECT");
                    //TODO: PUSH
                    mbot.stopAllBehaviors();
                    state = RobotState.CRUISE; // Is All Clear --> CRUISE
                    break;
                case COLLECT_SAMPLE:
                    System.out.println("COLLECT_SAMPLE");
                    mbot.stopAllBehaviors();
                    mbot.stop();
                    // TODO: RETRIEVE
                    hasSample = true;
                    state = RobotState.CRUISE;
                    break;

            }
        }
    }

    /**
     * The main entry point for the MazeRobot application.
     * @param args Command line arguments (not used).
     */
    public static void main(String[] args) {
        try (MazeRobot amazin = new MazeRobot("Liang")) {
            amazin.run();
        }
    }
}
