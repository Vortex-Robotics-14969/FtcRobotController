

package org.firstinspires.ftc.teamcode.OpModes;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;

import java.util.List;
import org.firstinspires.ftc.robotcore.external.ClassFactory;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer.CameraDirection;
import org.firstinspires.ftc.robotcore.external.tfod.TFObjectDetector;
import org.firstinspires.ftc.robotcore.external.tfod.Recognition;
import org.firstinspires.ftc.teamcode.Helper.Robot;

import com.qualcomm.robotcore.util.ElapsedTime;

@Autonomous(name = "Auto Meet 1", group = "Concept")
public class AutoLeft extends LinearOpMode {

    private static final String TFOD_MODEL_ASSET = "PowerPlay.tflite";
    // private static final String TFOD_MODEL_FILE  = "/sdcard/FIRST/tflitemodels/CustomTeamModel.tflite";

    private static final String[] LABELS = {
            "1 Bolt",
            "2 Bulb",
            "3 Panel"
    };
    private static final String VUFORIA_KEY =
            "AWtcstb/////AAABmfYaB2Q4dURcmKS8qV2asrhnGIuQxM/ioq6TnYqZseP/c52ZaYTjs4/2xhW/91XEaX7c3aw74P3kGZybIaXued3nGShb7oNQyRkVePnFYbabnU/G8em37JQrH309U1zOYtM3bEhRej91Sq6cf6yLjiSXJ+DxxLtSgWvO5f+wM3Wny8MbGUpVSiogYnI7UxEz8OY88d+hgal9u3GhhISdnNucsL+fRAE8mKwT1jGDgUVE1uAJoZFvo95AJWS2Yhdq/N/HpxEH3sBXEm99ci+mdQsl0m96PMCDfV5RgWBjhLbBEIJyQ/xKAbw5Yfr/AKCeB86WDPhR3+Mr8BUvsrycZA6FDJnN5sZZwTg0ZE22+gFL";
    private VuforiaLocalizer vuforia;
    private TFObjectDetector tfod;


    private ElapsedTime runtime = new ElapsedTime();
    double timeout_ms = 0;
    public int parkingTarget = 2;
    Robot robot = new Robot();

    public enum AutoSteps {
        detectSignal, deliverPreLoad, CycleThreeCones, park, endAuto
    }

    public AutoSteps Step = AutoSteps.detectSignal;

    @Override
    public void runOpMode() throws InterruptedException {
        // The TFObjectDetector uses the camera frames from the VuforiaLocalizer, so we create that
        // first.
        initVuforia();
        initTfod();
        robot.init(hardwareMap);

        if (tfod != null) {
            tfod.activate();

            tfod.setZoom(1.25, 16.0 / 9.0);
        }

//        robot.vSlider.setTargetPosition(-165);
//        robot.vSlider.setMode(DcMotor.RunMode.RUN_TO_POSITION);
//        robot.vSlider.setPower(0.6);

        robot.claw.setPosition(0.6);


        /** Wait for the game to begin */
        telemetry.addData(">", "Press Play to start op mode");
        telemetry.addData("FL Motor Encoder", robot.FLMotor.getCurrentPosition());
        telemetry.addData("BL Motor Encoder", robot.BLMotor.getCurrentPosition());
        telemetry.addData("BR Motor Encoder", robot.BRMotor.getCurrentPosition());
        telemetry.addData("FR Motor Encoder", robot.FRMotor.getCurrentPosition());
        telemetry.update();
        waitForStart();

        if (opModeIsActive()) {
            while (opModeIsActive()) {
                if (tfod != null) {
                    // getUpdatedRecognitions() will return null if no new information is available since
                    // the last time that call was made.
                    List<Recognition> updatedRecognitions = tfod.getUpdatedRecognitions();
                    if (updatedRecognitions != null) {
                        telemetry.addData("# Objects Detected", updatedRecognitions.size());

                        // step through the list of recognitions and display image position/size information for each one
                        // Note: "Image number" refers to the randomized image orientation/number
                        for (Recognition recognition : updatedRecognitions) {
                            double col = (recognition.getLeft() + recognition.getRight()) / 2;
                            double row = (recognition.getTop() + recognition.getBottom()) / 2;
                            double width = Math.abs(recognition.getRight() - recognition.getLeft());
                            double height = Math.abs(recognition.getTop() - recognition.getBottom());
                            String objectLabel = recognition.getLabel();

                            if (objectLabel == "1 Bolt") {
                                parkingTarget = 1;
                            } else if (objectLabel == "2 Bulb") {
                                parkingTarget = 2;
                            } else if (objectLabel == "3 Panel") {
                                parkingTarget = 3;
                            }
                            telemetry.addData("", " ");
                            telemetry.addData("Image", "%s (%.0f %% Conf.)", recognition.getLabel(), recognition.getConfidence() * 100);
                            telemetry.addData("- Position (Row/Col)", "%.0f / %.0f", row, col);
                            telemetry.addData("- Size (Width/Height)", "%.0f / %.0f", width, height);
                            telemetry.addData("Robot Location", robot.Location);
                        }
                        telemetry.update();
                    }
                }

                switch (Step) {
                    case detectSignal:
                        telemetry.addData("Parking Target ", parkingTarget);
                        telemetry.update();
                        Step = AutoSteps.deliverPreLoad;
                        break;

                    case deliverPreLoad:
                        DeliverPreLoad();
                        Step = AutoSteps.park;
                        break;

                    case CycleThreeCones:
                        for(int i = 0; i < 4; i++) {
                            CycleCone();
                        }
                        Step = AutoSteps.park;
                        break;

                    case park:
                        Park(parkingTarget);
                        Step = AutoSteps.endAuto;
                        break;

                    case endAuto:
                        telemetry.addData("➡️", "Auto Finished");
                        telemetry.update();
                        break;
                }
            }
        }
    }


    private void initVuforia() {
        /*
         * Configure Vuforia by creating a Parameter object, and passing it to the Vuforia engine.
         */
        VuforiaLocalizer.Parameters parameters = new VuforiaLocalizer.Parameters();

        parameters.vuforiaLicenseKey = VUFORIA_KEY;
        parameters.cameraDirection = CameraDirection.BACK;

        //  Instantiate the Vuforia engine
        vuforia = ClassFactory.getInstance().createVuforia(parameters);
    }


    private void initTfod() {
        int tfodMonitorViewId = hardwareMap.appContext.getResources().getIdentifier(
                "tfodMonitorViewId", "id", hardwareMap.appContext.getPackageName());
        TFObjectDetector.Parameters tfodParameters = new TFObjectDetector.Parameters(tfodMonitorViewId);
        tfodParameters.minResultConfidence = 0.75f;
        tfodParameters.isModelTensorFlow2 = true;
        tfodParameters.inputSize = 300;
        tfod = ClassFactory.getInstance().createTFObjectDetector(tfodParameters, vuforia);

        // Use loadModelFromAsset() if the TF Model is built in as an asset by Android Studio
        // Use loadModelFromFile() if you have downloaded a custom team model to the Robot Controller's FLASH.
        tfod.loadModelFromAsset(TFOD_MODEL_ASSET, LABELS);
        // tfod.loadModelFromFile(TFOD_MODEL_FILE, LABELS);
    }

    private void Park(int location) {
        if (location == 1) {
            robot.DriveToPosition(0.3, 75, 70);
        }

        if (location == 2) {
            robot.DriveToPosition(0.3, 0, 70);
        }

        if (location == 3) {
            robot.DriveToPosition(0.3, -75, 70);

        }
    }

    public void DeliverPreLoad() {
        //First swing the arm up and go to the pole.
        robot.claw.setPosition(1);
        robot.SwingArmToPosition(1, 65);
        robot.swingArm.setPower(robot.swingArmHoldingPower);
        robot.DriveToPosition(0.8, 15, 45);
        //Next, move the slider to the right height, swing the arm down, drop the cone, swing the arm back up, and lower the slider.
        robot.MoveSliderToPosition(0.6, 500);
        robot.claw.setPosition(0);
        robot.SwingArmToPosition(1, 20);
        robot.claw.setPosition(1);
        sleep(500);
        robot.claw.setPosition(0);
        robot.SwingArmToPosition(1, 65);
        robot.swingArm.setPower(robot.swingArmHoldingPower);
        robot.MoveSliderToPosition(0.6, 0);
    }

    public void CycleCone(){
        /** First go to the stack of cones and grab a cone **/
        //Drive to the stack
        robot.DriveToPosition(0.8, -15, 0);
        robot.DriveToPosition(0.8, 0, 60);
        robot.turnRobotToAngle(90);
        //Open the claw and swing the arm down
        robot.claw.setPosition(1);
        robot.SwingArmToPosition(1,20);
        //Drive forward slightly
        robot.DriveToPosition(0.6, 0, 25);
        //close the claw and grab onto the cone
        robot.claw.setPosition(0);
        /** Now drive to the medium pole **/
        //Drive to the pole and face it
        robot.DriveToPosition(0.7,0,-60);
        robot.turnRobotToAngle(210);
        robot.stopDriveMotors();
        /** Now deliver the cone **/
        //Move the slider to the right height and swing down
        robot.MoveSliderToPosition(0.6, 500);
        robot.SwingArmToPosition(1, 20);
        //Open and close claw
        robot.claw.setPosition(1);
        sleep(500);
        robot.claw.setPosition(0);
        //swing arm back up
        robot.SwingArmToPosition(1, 65);
        robot.swingArm.setPower(robot.swingArmHoldingPower);
        //lower slider
        robot.MoveSliderToPosition(0.6, 0);

    }
}