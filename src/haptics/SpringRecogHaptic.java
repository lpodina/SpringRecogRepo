package haptics;

import fisica.FBody;
import fisica.FWorld;
import fisica.Fisica;
import haptics.tech.Board;
import haptics.tech.Device;
import haptics.tech.DeviceType;
import haptics.tech.HVirtualCoupling;
import processing.core.PApplet;
import processing.core.PShape;
import processing.core.PVector;
import processing.data.StringList;
import processing.data.Table;
import processing.serial.Serial;
import recog.OneDollar;
import recog.SpringRecog;
import com.dhchoi.CountdownTimer;
import com.dhchoi.CountdownTimerService;

import java.util.ArrayList;

import static haptics.tech.DeviceType.HaplyTwoDOF;

public class SpringRecogHaptic extends SpringRecog {


    public static void main(String[] args) {
        PApplet.main(new String[]{"haptics.SpringRecogHaptic"});
    }


    /* Device block definitions ********************************************************************************************/
    Device haply_2DoF;
    byte deviceID = 5;
    Board haply_board;
    DeviceType degreesOfFreedom;
    boolean rendering_force = false;
    HVirtualCoupling s;

    /* Simulation Speed Parameters ****************************************************************************************/
    final long        SIMULATION_PERIOD          = 1; //ms
    final long        HOUR_IN_MILLIS             = 36000000;
    CountdownTimer    haptic_timer;
    float             dt                        = SIMULATION_PERIOD/1000.0f;


    /* generic data for a 2DOF device */

    /* joint space */
    PVector angles                    = new PVector(0, 0);
    PVector           torques                   = new PVector(0, 0);

    /* task space */
    PVector           pos_ee                    = new PVector(0, 0);
    PVector           pos_ee_last               = new PVector(0, 0);
    PVector           f_ee                      = new PVector(0, 0);
    float             offsetX                   =0;
    float             offsetY                   =0;

    /* where the edges are */

    float edgeTopLeftX = 0.0f;
    float edgeTopLeftY = 0.0f;
    float edgeBottomRightX =width;
    float edgeBottomRightY = height;


    @Override
    public void setup(){
        super.setup();
        s = new HVirtualCoupling((1));
        //***************************
        /* Initialization of the Board, Device, and Device Components */

        /* BOARD */

        haply_board = new Board(this, "COM3", 0); //Put your COM# port here

        /* DEVICE */
        haply_2DoF = new Device(HaplyTwoDOF, deviceID, haply_board);

        /* Initialize graphical simulation components */

        /* set device in middle of frame on the x-axis and in the fifth on the y-axis */
        //device_origin.add((width/2), (height/5) );

        /* create pantograph graphics */
        //createpantograph();

        /* haptics event timer, create and start a timer that has been configured to trigger onTickEvents */
        /* every TICK (1ms or 1kHz) and run for HOUR_IN_MILLIS (1hr), then resetting */
        haptic_timer = CountdownTimerService.getNewCountdownTimer(this).configure(SIMULATION_PERIOD, HOUR_IN_MILLIS).start();

    }

    @Override
    public void draw() {
        background(255);
        if (!rendering_force) {
            world.step();
            world.draw(this);
        }
        if (ava != null) {
            println("force in x", ava.getX(), "Force in Y direction", ava.getY());
        }



    }

    public void detected(String gesture, float percent, int startX, int startY, int centroidX, int centroidY, int endX, int endY){
        //println("Gesture: "+gesture+", "+startX+"/"+startY+", "+centroidX+"/"+centroidY+", "+endX+"/"+endY);
    }



    /**********************************************************************************************************************
     * Haptics simulation event, engages state of physical mechanism, calculates and updates physics simulation conditions
     **********************************************************************************************************************/

    public void onTickEvent(CountdownTimer t, long timeLeftUntilFinish){


        rendering_force = true;
        if (ava!=null){
            println("ava not null");
//  //  /* GET END-EFFECTOR STATE (TASK SPACE) */
            if (haply_board.data_available()) {

                println("haply data available");

                angles.set(haply_2DoF.get_device_angles());
                pos_ee.set( haply_2DoF.get_device_position(angles.array()));
                pos_ee.mult(500);

            }
            f_ee.set(-(ava.getX()+(pos_ee.x*20)+offsetX)*1000, +(ava.getY()-(pos_ee.y*20)+offsetY)*1000);
            //f_ee.set(0,0);
            f_ee.div(200); //
            haply_2DoF.set_device_torques(f_ee.array());
            torques.set(haply_2DoF.mechanisms.get_torque());
            haply_2DoF.device_write_torques();
            ava.setPosition(-(pos_ee.x*20)-offsetX, (pos_ee.y*20)-offsetY);

        }

        println("ava null"); //TODO check avatar setting to a body, that might have a bug in it

        //world.step(1.0f/25.0f);
        rendering_force = false;

        //world.step(1.0f/1000.0f);

        s.updateCouplingForce();

        f_ee.set(-s.getVCforceX(), s.getVCforceY());

        f_ee.div(100000);
        haply_2DoF.set_device_torques(f_ee.array());
        torques.set(haply_2DoF.mechanisms.get_torque());
        haply_2DoF.device_write_torques();
        angles.set(haply_2DoF.get_device_angles());
        pos_ee.set( haply_2DoF.get_device_position(angles.array()));
        pos_ee.mult(100);

    }


    /* Timer control event functions **************************************************************************************/

    /**
     * haptic timer reset
     */
    public void onFinishEvent(CountdownTimer t){
        println("Resetting timer...");
        haptic_timer.reset(CountdownTimer.StopBehavior.STOP_IMMEDIATELY);
        haptic_timer = CountdownTimerService.getNewCountdownTimer(this).configure(SIMULATION_PERIOD, HOUR_IN_MILLIS).start();
    }



}
