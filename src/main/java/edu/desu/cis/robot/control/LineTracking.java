package edu.desu.cis.robot.control;
import edu.desu.cis.robot.service.SensorSnapshot;
public class LineTracking extends RobotController {
    public LineTracking(String robotName) {
        super(robotName);
    }
    private void pause(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    @Override
    public void run() {
       mbot.followLine();

        while (true) {
            pause(200);
        }
    }
        /*while (true) {
            SensorSnapshot s = awaitNewData();
            System.out.println("lineStatus=" + s.lineStatus() + ", lineOffset=" + s.lineOffset());
            pause(300);
            }
        }*/
    public static void main(String[] args) {
        try (LineTracking cold = new LineTracking("Liang")) {
            //cold.run(500);
            cold.run();
        }
    }
    public void



    run(long time){
        mbot.setMotorPower(10, 10);
        pause(time);

        mbot.stop();
        System.out.println("Test finished.");
    }
}