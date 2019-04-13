/*----------------------------------------------------------------------------*/
/* Copyright (c) 2017-2018 FIRST. All Rights Reserved.                        */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

package frc.robot;

import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.XboxController;
import edu.wpi.first.wpilibj.GenericHID.RumbleType;
// import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.cscore.UsbCamera;
import edu.wpi.first.cameraserver.CameraServer;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.GenericHID;

public class Robot extends TimedRobot {

    DriverJoystick driver;
    XboxController secondary;
    
    Hardware hw;
    
    Climber climber;
    Drive drive;
    Hatch hatch;

    VisionCalculator vision;

    Toggle hatchDisable;
    Toggle compressorDisable;
    // Toggle aggressiveRamp;

    DriverStation ds;

    @Override
    public void robotInit() {
        driver = new DriverJoystick(0);
        secondary = new XboxController(1);
        
        hw = new Hardware();
        
        climber = new Climber(hw);
        drive = new Drive(hw);
        hatch = new Hatch(hw);

        vision = new VisionCalculator(hw, new VisionGetter());

        // hw.gyro.calibrateGX();

        hatchDisable = new Toggle();
        compressorDisable = new Toggle();
        // aggressiveRamp = new Toggle();

        UsbCamera cam = CameraServer.getInstance().startAutomaticCapture();
        cam.setResolution(40, 40);
        cam.setFPS(30);

        ds = DriverStation.getInstance();
      }

    @Override
    public void robotPeriodic() {
    }

    @Override
    public void autonomousInit() {
        teleopInit();
    }

    @Override
    public void autonomousPeriodic() {
        teleopPeriodic();
    }

    @Override
    public void teleopInit(){
        drive.enable();
        hatch.enable();
        hatchDisable.set(false);
        compressorDisable.set(true);
    }

    @Override
    public void teleopPeriodic() {
        //Collect all joystick inputs
        boolean dLB = driver.joystick.getBumper(GenericHID.Hand.kLeft);
        double dRT = driver.joystick.getTriggerAxis(GenericHID.Hand.kRight);
        double dLT = driver.joystick.getTriggerAxis(GenericHID.Hand.kLeft);
        boolean dY = driver.joystick.getYButton();
        boolean dX = driver.joystick.getXButton();

        //Full speed climb
        boolean sA = secondary.getAButton();
        boolean sB = secondary.getBButton();
        //Half speed climb
        boolean sY = secondary.getYButton();
        boolean sX = secondary.getXButton();

        double sLT = secondary.getTriggerAxis(GenericHID.Hand.kLeft);

        boolean sRB = secondary.getBumper(GenericHID.Hand.kRight);

        double rumbleIntensity = ds.getMatchTime() >= 28 && ds.getMatchTime() <= 30? 0.75 : 0;

        driver.joystick.setRumble(RumbleType.kRightRumble, rumbleIntensity);
        driver.joystick.setRumble(RumbleType.kLeftRumble, rumbleIntensity);
        secondary.setRumble(RumbleType.kRightRumble, rumbleIntensity);
        secondary.setRumble(RumbleType.kLeftRumble, rumbleIntensity);

        // double hip = secondary.getRawAxis(1);
        // climber.manualDrive(hip);

        hatchDisable.update(sRB);

        compressorDisable.update(sA);

        // aggressiveRamp.update(dY);


        if(!hatchDisable.get()) {
            hatch.setOpen(dLB);
        } else {
            hatch.setOpen(true);
        }

        if(compressorDisable.get()) {
            hw.compressor.start();
        } else {
            hw.compressor.stop();
        }

        // SmartDashboard.putNumber("Gyro", hw.gyro.getGyroX());

        double climberSpeed = 0.0;

        if(dRT > 0.2 || dLT > 0.2) {
            drive.setTank(true);
            drive.oneSideTurn(dLT, dRT);
            vision.disable();
        } else if(dX) {
            vision.enable();
        } else if(sX && sY) {
            vision.disable();
            climberSpeed = 0.60;
            drive.updateSpeeds(-Math.abs(DriverJoystick.getForward()), -Math.abs(DriverJoystick.getTurn()), true);
        } else {
            vision.disable();
            drive.setTank(false);
            drive.updateSpeeds(-DriverJoystick.getForward(), -DriverJoystick.getTurn(), true);
        }

        if(sLT > 0.3) {
            climberSpeed = -sLT/2.0;
        }

        climber.manualDrive(climberSpeed);

    }

    @Override
    public void testPeriodic() {
    }

    @Override
    public void disabledPeriodic(){
        drive.disable();
        hatch.disable();
    }
}