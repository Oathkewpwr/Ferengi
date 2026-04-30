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
    private boolean cruisingStarted = false;
    private boolean avoidingStarted = false;


    public void run() {
        state = RobotState.CRUISE;
        SensorSnapshot s;
        while (state != RobotState.STOP) {
            s = awaitNewData();// CORRECT — fresh snapshot every iteration
            switch (state) {
                case CRUISE:
                    System.out.println("CRUISE");
                    if (!cruisingStarted) {
                        mbot.followLine();
                        mbot.avoidCrashing(8);
                        cruisingStarted = true;
                    }
                    if (s.distance() < 10) {
                        mbot.stopAllBehaviors();
                        mbot.stop();
                        cruisingStarted = false;
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
                    /*mbot.steerAround(10, 30, 28, false);
                    try { Thread.sleep(1200); }
                    catch (InterruptedException e) {}

                    mbot.stopAllBehaviors();
                    mbot.moveAndTurn(20, 20, true);
                    try { Thread.sleep(1200); }
                    catch (InterruptedException e) {}*/
                    mbot.moveAndTurn(30,25, false);
                    try { Thread.sleep(1000); }
                    catch (InterruptedException e) {}
                    mbot.straight(8);

                    mbot.stopAllBehaviors();
                    mbot.moveAndTurn(30, 20, true);
                    try { Thread.sleep(1800); }
                    catch (InterruptedException e) {}
                    mbot.stopAllBehaviors();
                    s = awaitNewData();
                    while (s.lineStatus() == 0) {
                        mbot.moveAndTurn(30, 20, true);
                        try { Thread.sleep(200); }
                        catch (InterruptedException e) {}
                        mbot.stopAllBehaviors();
                        s = awaitNewData();
                    }
                    cruisingStarted = false;
                    state = RobotState.CRUISE;
                    break;
                case MOVE_OBJECT:
                    System.out.println("MOVE_OBJECT");
                    mbot.stopAllBehaviors();
                    mbot.stop();
                    mbot.pushObject();
                    mbot.stopAllBehaviors();
                    cruisingStarted = false;
                    state = RobotState.CRUISE; // Is All Clear --> CRUISE
                    break;
                case COLLECT_SAMPLE:
                    System.out.println("COLLECT_SAMPLE");
                    mbot.stopAllBehaviors();
                    mbot.stop();
                    mbot.turnRight(360);
                    mbot.flashLed(5, 255, 0, 0, 0.3);
                    mbot.flashLed(3, 0, 255, 0, 0.3);
                    mbot.straight(5);
                    mbot.straight(-5);
                    hasSample = true;
                    cruisingStarted = false;
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
