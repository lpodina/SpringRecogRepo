package haptics;

import com.dhchoi.CountdownTimer;
import com.dhchoi.CountdownTimerService;
import processing.core.PApplet;
import processing.serial.Serial;

public class SerialTest extends PApplet{

    public static void main(String[] args) {
        PApplet.main(new String[]{"haptics.SerialTest"});
    }

    final long        SIMULATION_PERIOD          = 1; //ms
    final long        HOUR_IN_MILLIS             = 36000000;
    Serial port;
    CountdownTimer    timer;

    // TODO make a serial port with some baud rate. implement ontick and ontickfinish and get them to send things through the port.
    // TODO this is to make sure that the serial stuff even works.

    @Override
    public void setup() {
        size(200,200); //make our canvas 200 x 200 pixels big
        String portName = Serial.list()[0]; //change the 0 to a 1 or 2 etc. to match your port
        port = new Serial(this, portName, 9600);
    }

    @Override
    public void draw() {
        background(255);
        if (mousePressed)
        {                           //if we clicked in the window
            port.write('1');         //send a 1
            println("1");
        } else
        {                           //otherwise
            port.write('0');          //send a 0
        }
    }
/*

    public void onTickEvent(CountdownTimer t, long timeLeftUntilFinish){
        if (mousePressed)
        {                           //if we clicked in the window
            port.write('1');         //send a 1
            println("1");
        } else
        {                           //otherwise
            port.write('0');          //send a 0
        }
    }


    */
/* Timer control event functions **************************************************************************************//*


    */
/**
     * haptic timer reset
     *//*

    public void onFinishEvent(CountdownTimer t){
        println("Resetting timer...");
        timer.reset(CountdownTimer.StopBehavior.STOP_IMMEDIATELY);
        timer = CountdownTimerService.getNewCountdownTimer(this).configure(SIMULATION_PERIOD, HOUR_IN_MILLIS).start();
    }
*/


}
