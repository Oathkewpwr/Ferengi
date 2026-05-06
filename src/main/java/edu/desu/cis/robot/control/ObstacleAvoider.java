package edu.desu.cis.robot.control;
import edu.desu.cis.robot.control.RobotController;
import edu.desu.cis.robot.service.SensorSnapshot;
public class ObstacleAvoider extends RobotController {
    public ObstacleAvoider(String robotName) {
        super(robotName);
    }
    @Override
    public void run() {
        // Start the background behavior — returns immediately
        /*mbot.steerAround(25, 30, 20);
        // Let it roam for 20 seconds, then stop
        try {
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        mbot.stopBehavior("STEER_AROUND");*/

            mbot.followLine();

            while (true) {
                SensorSnapshot s = awaitNewData();

                if (s.distance() < 20) {
                    System.out.println("Obstacle detected");

                    mbot.stopBehavior("FOLLOW_LINE");
                    mbot.stop();

                    mbot.steerAround(25, 25, 20,true);
                    while (true) {
                        s = awaitNewData();
                        if (s.distance() > 30) {
                            break;
                        }
                    }

                    mbot.stopBehavior("STEER_AROUND");
                    mbot.stop();
                    mbot.followLine();
                }
            }
        }
    /*public void run() {
        while (true) {
            System.out.println("before await");
            SensorSnapshot s = awaitNewData();
            System.out.println("distance=" + s.distance());

            pause(300);
        }
    }*/
    private void pause(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static void main(String[] args) {
        try (ObstacleAvoider robot = new ObstacleAvoider("Liang")) {
            robot.run();
        }
    }
}