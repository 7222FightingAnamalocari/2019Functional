package frc.robot;

import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.drive.DifferentialDrive;
import edu.wpi.first.cameraserver.CameraServer;
import edu.wpi.first.wpilibj.Compressor;
import edu.wpi.first.wpilibj.DoubleSolenoid;
import edu.wpi.cscore.CvSink;
import edu.wpi.cscore.CvSource;
import edu.wpi.cscore.UsbCamera;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.Spark;
import edu.wpi.first.wpilibj.SpeedControllerGroup;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

public class Robot extends TimedRobot {
  private static final String kDefaultAuto = "Default";
  private static final String kCustomAuto = "My Auto";
  private String m_autoSelected;
  private final SendableChooser<String> m_chooser = new SendableChooser<>();

  Joystick stick0 = new Joystick(0);
  Joystick stick1 = new Joystick(1);

  Spark m_leftFront = new Spark(0);
  Spark m_leftBack = new Spark(1);
  Spark m_rightFront = new Spark(2);
  Spark m_rightBack = new Spark(3);

  Spark elevator0 = new Spark(4);
  Spark winch = new Spark(5);

  Spark intakeRight = new Spark(6);
  Spark intakeLeft = new Spark(7);

  Spark driveBottom = new Spark(8);

  SpeedControllerGroup m_left = new SpeedControllerGroup(m_leftFront, m_leftBack);
  SpeedControllerGroup m_right = new SpeedControllerGroup(m_rightFront, m_rightBack);

  DifferentialDrive r_drive = new DifferentialDrive(m_left, m_right);

  DoubleSolenoid sol0 = new DoubleSolenoid(0, 1);
  DoubleSolenoid sol1 = new DoubleSolenoid(2, 3);
  //DoubleSolenoid sol2 = new DoubleSolenoid(4,5);
  DoubleSolenoid sol3 = new DoubleSolenoid(4,5);
  
  Compressor compressor = new Compressor(0);

  boolean toggleOn;
  boolean frontToggle;
  boolean backToggle;
  boolean hatchToggle;

  boolean togglePressed = false;
  boolean frontPressed = false;
  boolean backPressed = false;
  boolean hatchPressed = false;

  boolean enabled = compressor.enabled();

  @Override
  public void robotInit() {
    m_chooser.addDefault("Default Auto", kDefaultAuto);
    m_chooser.addObject("My Auto", kCustomAuto);
    SmartDashboard.putData("Auto choices", m_chooser);
    new Thread (() -> {
      UsbCamera camera = CameraServer.getInstance().startAutomaticCapture(0);
      camera.setResolution(320, 240);

      CvSink cvSink = CameraServer.getInstance().getVideo();
      CvSource outputStream = CameraServer.getInstance().putVideo("cam0", 320, 240);

      Mat source = new Mat();
      Mat output = new Mat();
      while(!Thread.interrupted()) {
        cvSink.grabFrame(source);
        Imgproc.cvtColor(source, output, Imgproc.COLOR_BGR2GRAY);
        outputStream.putFrame(output);
      }
    }).start();
    m_left.setInverted(true);
    intakeLeft.setInverted(true);
  }
  @Override
  public void robotPeriodic() {
    SmartDashboard.putBoolean("Hatch Grabber", hatchToggle);
    SmartDashboard.putNumber("Compressor Power", compressor.getCompressorCurrent());
    SmartDashboard.putBoolean("Pressure Low?", compressor.getPressureSwitchValue());
    //This function is called every robot packet, no matter the mode.
  }
  @Override
  public void autonomousInit() {
    m_autoSelected = m_chooser.getSelected();
    // autoSelected = SmartDashboard.getString("Auto Selector",
    // defaultAuto);
    System.out.println("Auto selected: " + m_autoSelected);
  }
 /*
  * This autonomous (along with the chooser code above) shows how to select
  * between different autonomous modes using the dashboard. The sendable
  * chooser code works with the Java SmartDashboard. If you prefer the
  * LabVIEW Dashboard, remove all of the chooser code and uncomment the
  * getString line to get the auto name from the text box below the Gyro
  *
  * <p>You can add additional auto modes by adding additional comparisons to
  * the switch structure below with additional strings. If using the
  * SendableChooser make sure to add them to the chooser code above as well.
  */
  @Override
  public void autonomousPeriodic() {
    switch (m_autoSelected) {
      case kCustomAuto:
        // Put custom auto code here
        break;
      case kDefaultAuto:
      default:
        // Put default auto code here

  r_drive.arcadeDrive(-stick0.getX(), stick0.getY());

  new Thread (() -> {
    if(frontToggle) { //this is the front lift
      sol0.set(DoubleSolenoid.Value.kForward);
    } else {
      sol0.set(DoubleSolenoid.Value.kReverse);
  }
    if(stick0.getRawButtonPressed(7)) { //more front lift
      if(!frontPressed) {
        frontToggle = !frontToggle;
        frontPressed = true;
      } else {
        frontPressed = false;
      }
    }
  }).start();
  new Thread(() -> {
    if(backToggle) { //back lift
      sol1.set(DoubleSolenoid.Value.kForward);
    } else {
      sol1.set(DoubleSolenoid.Value.kReverse);
    }
    if(stick0.getRawButtonPressed(8)) { //more back lift
      if(!backPressed) {
        backToggle = !backToggle;
        backPressed = true;
      }
    } else {
      backPressed = false;
    }
  }).start();
  new Thread(() -> {
    if(hatchToggle) { //this is the hatch grabber code
      sol3.set(DoubleSolenoid.Value.kForward);
    } else {
      sol3.set(DoubleSolenoid.Value.kReverse);
    }    
    if(stick1.getRawButtonPressed(1)) {
      if(!hatchPressed) {
        hatchToggle = !hatchToggle;
        hatchPressed = true;
      } else {
        hatchPressed = false;
      }
    }
  }).start();

  new Thread(() -> { //elevator code
    if((stick1.getRawAxis(1) >= .2) || (stick1.getRawAxis(1) <= -.2)) { //first level elevator
      elevator0.set(-stick1.getRawAxis(1));
    } else {
      elevator0.set(0);
    }
  }).start();

  new Thread(() -> {
    if(stick1.getRawButton(7)) { //dover intake
      intakeLeft.set(.5);
      intakeRight.set(.5);
    } else {
      if(stick1.getRawButton(8)) {
        intakeLeft.set(-1);
        intakeRight.set(-1);
      } else {
        intakeLeft.set(0);
        intakeRight.set(0);
      }
    }
  }).start();

  new Thread(() -> { //winch code
    if(stick1.getRawAxis(3) <= -.3) {
      winch.set(.7);
    } else {
      if(stick1.getRawAxis(3) >= .3) {
        winch.set(-.5);
      } else {
        winch.set(0);
      }
    }
  }).start();

  new Thread(() -> { //Drive  bottom drive code
    if(stick0.getRawButton(9) == true) {
      driveBottom.set(.8);
    } else {
      if(stick0.getRawButton(10) == true) {
        driveBottom.set(-.8);
      } else {
        driveBottom.set(0);
      }
    }
  }).start();
        break;
    }
  }
  @Override
  public void teleopPeriodic() {

  compressor.setClosedLoopControl(true);
  compressor.getPressureSwitchValue();

  r_drive.arcadeDrive(-stick0.getX(), stick0.getY());

  new Thread (() -> {
    if(frontToggle) { //this is the front lift
      sol0.set(DoubleSolenoid.Value.kForward);
    } else {
      sol0.set(DoubleSolenoid.Value.kReverse);
  }
    if(stick0.getRawButtonPressed(7)) { //more front lift
      if(!frontPressed) {
        frontToggle = !frontToggle;
        frontPressed = true;
      } else {
        frontPressed = false;
      }
    }
  }).start();
  new Thread(() -> {
    if(backToggle) { //back lift
      sol1.set(DoubleSolenoid.Value.kForward);
    } else {
      sol1.set(DoubleSolenoid.Value.kReverse);
    }
    if(stick0.getRawButtonPressed(8)) { //more back lift
      if(!backPressed) {
        backToggle = !backToggle;
        backPressed = true;
      }
    } else {
      backPressed = false;
    }
  }).start();
  new Thread(() -> {
    if(hatchToggle) { //this is the hatch grabber code
      sol3.set(DoubleSolenoid.Value.kForward);
    } else {
      sol3.set(DoubleSolenoid.Value.kReverse);
    }    
    if(stick1.getRawButtonPressed(1)) {
      if(!hatchPressed) {
        hatchToggle = !hatchToggle;
        hatchPressed = true;
      } else {
        hatchPressed = false;
      }
    }
  }).start();

  new Thread(() -> { //elevator code
    if((stick1.getRawAxis(1) >= .2) || (stick1.getRawAxis(1) <= -.2)) { //first level elevator
      elevator0.set(-stick1.getRawAxis(1));
    } else {
      elevator0.set(0);
    }
  }).start();

  new Thread(() -> {
    if(stick1.getRawButton(7)) { //dover intake
      intakeLeft.set(.5);
      intakeRight.set(.5);
    } else {
      if(stick1.getRawButton(8)) {
        intakeLeft.set(-1);
        intakeRight.set(-1);
      } else {
        intakeLeft.set(0);
        intakeRight.set(0);
      }
    }
  }).start();

  new Thread(() -> { //winch code
    if(stick1.getRawAxis(3) <= -.3) {
      winch.set(.7);
    } else {
      if(stick1.getRawAxis(3) >= .3) {
        winch.set(-.5);
      } else {
        winch.set(0);
      }
    }
  }).start();

  new Thread(() -> { //Drive  bottom drive code
    if(stick0.getRawButton(9) == true) {
      driveBottom.set(.8);
    } else {
      if(stick0.getRawButton(10) == true) {
        driveBottom.set(-.6);
      } else {
        driveBottom.set(0);
      }
    }
  }).start();
}
  @Override
  public void testPeriodic() {
  }
}