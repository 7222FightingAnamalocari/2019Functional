package frc.robot;

import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

//import edu.wpi.first.wpilibj.IterativeRobot;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.drive.DifferentialDrive;
//import edu.wpi.first.wpilibj.CameraServer;
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
  Spark elevator1 = new Spark(5);

  Spark intakeRight = new Spark(6);
  Spark intakeLeft = new Spark(7);

  Spark driveBottom = new Spark(8);

  SpeedControllerGroup m_left = new SpeedControllerGroup(m_leftFront, m_leftBack);
  SpeedControllerGroup m_right = new SpeedControllerGroup(m_rightFront, m_rightBack);

  DifferentialDrive r_drive = new DifferentialDrive(m_left, m_right);

  DoubleSolenoid sol0 = new DoubleSolenoid(0, 1);
  DoubleSolenoid sol1 = new DoubleSolenoid(2, 3);
  DoubleSolenoid sol2 = new DoubleSolenoid(4,5);
  DoubleSolenoid sol3 = new DoubleSolenoid(6,7);
  
  Compressor compressor = new Compressor(0);

  boolean toggleOn = false;
  boolean frontToggle = false;
  boolean backToggle = false;

  boolean togglePressed = false;
  boolean frontPressed = false;
  boolean backPressed = false;

  boolean enabled = compressor.enabled();

  @Override
  public void robotInit() {
    m_chooser.addDefault("Default Auto", kDefaultAuto);
    m_chooser.addObject("My Auto", kCustomAuto);
    SmartDashboard.putData("Auto choices", m_chooser);
    new Thread (() -> {
      UsbCamera camera = CameraServer.getInstance().startAutomaticCapture(0);
      camera.setResolution(640, 480);

      CvSink cvSink = CameraServer.getInstance().getVideo();
      CvSource outputStream = CameraServer.getInstance().putVideo("cam0", 640, 480);

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
        break;
    }
  }
  @Override
  public void teleopPeriodic() {
    /*
    new Thread(() -> {
      double stick0Y = stick0.getY();
      double stick0X = stick0.getX();
      if((stick0Y >= .2) || (stick0Y <= -.2)) {
        m_left.set(stick0Y/1.2);
        m_right.set(stick0Y/1.2);
      } else {
        if(((stick0X == 0) && (stick0Y ==0)) ) {
          m_left.set(0);
          m_right.set(0);
        }
      }
    }).start();

    new Thread (() -> {
      double stick0X = stick0.getX();
      if((stick0X >= .3) || (stick0X <= -.3)) {
        m_left.set(stick0X/1.2);
        m_right.set(-stick0X/1.2);
      }
    }).start();
*/
    compressor.setClosedLoopControl(true);
    new Thread(() -> {
      r_drive.arcadeDrive(stick0.getY(), stick0.getX());
    }).start();
    new Thread (() -> { //bot lift codeEA
      {
      if(frontToggle) { //this is the front lift
        sol0.set(DoubleSolenoid.Value.kForward);
      } else {
        sol0.set(DoubleSolenoid.Value.kReverse);
      }
    
      if(stick0.getRawButton(11) == true) { //more front lift
        if(frontPressed) {
          frontToggle = !frontToggle;
          frontPressed = true;
        } else {
        frontPressed = false;
        }
      }
    }
  }).start();
  new Thread(() -> {
    {
      if(backToggle) { //back lift
        sol1.set(DoubleSolenoid.Value.kForward);
      } else {
        sol1.set(DoubleSolenoid.Value.kReverse);
      }

      if(stick0.getRawButton(12) == true) { //more back lift
        if(backPressed) {
          backToggle = !backToggle;
          backPressed = true;
        }
      } else {
        backPressed = false;
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
    if((stick1.getRawAxis(3) >= .2) || (stick1.getRawAxis(3) <= -.2)) { //second elevator
      elevator1.set(stick1.getRawAxis(3));
    } else {
      elevator1.set(0);
    }
  }).start();

  new Thread(() -> { //dover intake code
      if(stick1.getPOV() == 1) {
        intakeLeft.set(1);
        intakeRight.set(1);
      } else {
        if(stick1.getPOV() == 3) {
          intakeLeft.set(-1);
          intakeRight.set(-1);
        } else {
          intakeLeft.set(0);
          intakeRight.set(0);
        }
      }
    }).start();

    new Thread(() -> {
      if(stick0.getPOV() == 1) {
        driveBottom.set(.3);
      } else {
        if(stick0.getPOV() == 3) {
          driveBottom.set(-.3);
        } else {
          driveBottom.set(0);
        }
      }
    }).start();
/*
    new Thread(() -> {
      {
        if(toggleOn) {
          sol2.set(DoubleSolenoid.Value.kForward);
        } else {
          sol2.set(DoubleSolenoid.Value.kReverse);
        }
        if(stick0.getRawButton(7) == true) { //more back lift
          if(!this.togglePressed) {
            this.toggleOn = !toggleOn;
            this.togglePressed = true;
          }
        } else {
          this.togglePressed = false;
        }
      }
    }).start();

    new Thread(() -> {
      {
        if(this.toggleOn) {
          sol2.set(DoubleSolenoid.Value.kForward);
        } else {
          sol2.set(DoubleSolenoid.Value.kReverse);
        }
        if(stick0.getRawButton(8) == true) { //more back lift
          if(!this.togglePressed) {
            this.toggleOn = !toggleOn;
            this.togglePressed = true;
          }
        } else {
          this.togglePressed = false;
        }
      }
    }).start();
    */
  }
  @Override
  public void testPeriodic() {
  }
}
