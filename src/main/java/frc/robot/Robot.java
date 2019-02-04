package frc.robot;

import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import edu.wpi.first.wpilibj.IterativeRobot;
import edu.wpi.first.wpilibj.CameraServer;
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

public class Robot extends IterativeRobot {
  private static final String kDefaultAuto = "Default";
  private static final String kCustomAuto = "My Auto";
  private String m_autoSelected;
  private final SendableChooser<String> m_chooser = new SendableChooser<>();

  Joystick stick0 = new Joystick(0);

  Spark m_leftFront = new Spark(0);
  Spark m_leftBack = new Spark(1);
  Spark m_rightFront = new Spark(2);
  Spark m_rightBack = new Spark(3);

  SpeedControllerGroup m_left = new SpeedControllerGroup(m_leftFront, m_leftBack);
  SpeedControllerGroup m_right = new SpeedControllerGroup(m_rightFront, m_rightBack);

  DoubleSolenoid cannonFeed = new DoubleSolenoid(1, 2);
  
  Compressor compressor = new Compressor(0);

  boolean toggleOn = false;
  boolean togglePressed = false;

  boolean enabled = compressor.enabled();
  boolean pressureSwitch = compressor.getPressureSwitchValue();
  double current = compressor.getCompressorCurrent();

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
    new Thread(() -> {
      UsbCamera camera = CameraServer.getInstance().startAutomaticCapture(1);
      camera.setResolution(1280, 720);

      CvSink cvsink = CameraServer.getInstance().getVideo();
      CvSource outputStream = CameraServer.getInstance().putVideo("cam1", 1280, 720);

      Mat source = new Mat();
      Mat output = new Mat();

      while(!Thread.interrupted()) {
        cvsink.grabFrame(source);
        Imgproc.cvtColor(source, output, Imgproc.COLOR_BGR2GRAY);
        outputStream.putFrame(output);
      }
    }).start();
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
    new Thread(() -> {
      compressor.setClosedLoopControl(true);
      if(toggleOn) {
        cannonFeed.set(DoubleSolenoid.Value.kForward);
      } else {
        cannonFeed.set(DoubleSolenoid.Value.kForward);
      }
      {
        if(stick0.getRawButton(1)) {
          if(!togglePressed) {
            toggleOn = !toggleOn;
            togglePressed = true;
          }
        } else {
          togglePressed = false;
        }
      }
    }).start();
  }
  @Override
  public void testPeriodic() {
  }
}