package charges;
import haptics.tech.*;
import org.jbox2d.util.sph.Particle;
import processing.core.PApplet;
import processing.core.PVector;
import processing.serial.*;
import com.dhchoi.CountdownTimer;
import com.dhchoi.CountdownTimerService;
import grafica.*;

import java.util.ArrayList;
import java.util.Random;
import ddf.minim.*;
import ddf.minim.ugens.*;

public class ElectricChargesApplet extends PApplet {


    AudioPlayer player;
    Minim minim;//audio context


    /* Device block definitions ********************************************************************************************/
    Device haply_2DoF;
    byte              deviceID             = 5;
    Board haply_board;
    DeviceType device_type;


    /* Animation Speed Parameters *****************************************************************************************/
    long              baseFrameRate        = 200;
    long              count                = 0;
    int               pixelsPerMeter       = 10000;
    PVector           offset               =new PVector(0, 0);

    /* Simulation Speed Parameters ****************************************************************************************/
    final long        SIMULATION_PERIOD    = 1; //ms
    final long        HOUR_IN_MILLIS       = 36000000;
    CountdownTimer    haptic_timer;

    /* generic data for a 2DOF device */
    /* joint space */
    PVector           angles               = new PVector(0, 0);

    PVector           torques              = new PVector(0, 0);
    PVector           device_origin        = new PVector (0, 0) ;
    /* task space */
    PVector           pos_ee               = new PVector(0, 0); // intrinsic position of the haptic device
    PVector           lastpos_ee           = new PVector(0, 0);
    PVector           f_ee                 = new PVector(0, 0);
    PVector delta_pos = new PVector(0, 0);


    int qp=120, qn=-120;
    PVector actualForce = new PVector(0, 0); //actual force acting on the avatar charge
    float[] F={0,0};
    float K= (float) 8.99;
    float Dx,Dy,DD,F1x,F1y,FT;
    float x2,y2;
    Random r = new Random();

    int highlight = 0x6673b2;

    PVector pos_btn = new PVector(500, 700);
    PVector neg_btn = new PVector(700, 700);
    float   btn_width = 30;
    int draw_sign = 2;

    int     selected_obj;
//0: nothing selected
//1: pos_btn selected
//2: neg_btn selected
//3: a charge selected

    boolean mouse_ctrl = true;

    ElectricCharge current_charge;
    ElectricCharge hovered_charge;

    PVector force_vector;

    GPlot graph;
    GPointsArray points = new GPointsArray(1000);

    //set up electric fields
    ArrayList<Particle> particles = new ArrayList <Particle> ();
    int particle_size = 0;

    //set up electric charges
    ArrayList <ElectricCharge> charges = new ArrayList <ElectricCharge> ();


    public void setup() {
        minim = new Minim(this);

        lastpos_ee = new PVector(0, 0);

        player = minim.loadFile("fl1.mp3", 2048);
        player.loop();
        player.setGain(-60);
        //player.play();
        //size(1200, 800, P2D);
        size(1350, 950, P2D);
        smooth(16);
        background(255);
        strokeWeight((float) 0.75);
        frameRate(baseFrameRate);
        createGraph();
        PVector offset=new PVector(0,0);


        /* Initialization of the Board, Device, and Device Components */

        /* BOARD */
        haply_board =new Board(this, "COM3", 0); //Put your COM# port here

        /* DEVICE */
        haply_2DoF = new Device(device_type.HaplyTwoDOF, deviceID, haply_board);
//  device_origin.add((width/2), (height/5) );
        /* haptics event timer, create and start a timer that has been configured to trigger onTickEvents */
        /* every TICK (1ms or 1kHz) and run for HOUR_IN_MILLIS (1hr), then resetting */
        //haptic_timer = CountdownTimerService.getNewCountdownTimer(this).configure(SIMULATION_PERIOD, HOUR_IN_MILLIS).start();
        haptic_timer = CountdownTimerService.getNewCountdownTimer(this).configure(SIMULATION_PERIOD, HOUR_IN_MILLIS).start();

        graphSetup(); //sets up the torque graphs
    }

    public void draw() {

        if (current_charge != null) {
            println("curr position: ", current_charge.x_pos, ", ", current_charge.y_pos);
        }
        else { println("curr charge is null"); }
        if (hovered_charge != null) {
            println("hov position: ", hovered_charge.x_pos, ", ", hovered_charge.y_pos);
        }
        else {println("hov charge is null"); }

        println("offset: ", offset);
        //println("pos_ee: ", pos_ee.x*pixelsPerMeter, ", ", pos_ee.y*pixelsPerMeter);

        btnPanel();

        //Drawing the fluid dynamics
        flow();

        if (charges.size() == 0) {
            particle_size = 150;
        } else if (charges.size() == 1){
            particle_size = 1000;
        } else if (charges.size() == 2){
            particle_size = 1250;
        } else if (charges.size() == 3){
            particle_size = 1500;
        } else if (charges.size() == 4){
            particle_size = 1725;
        } else if (charges.size() == 5){
            particle_size = 2000;
        }

        while (particles.size () < particle_size) {
            Particle p = new Particle();
            particles.add(p);
        }

        if (frameCount % 1 == 0) {
            noStroke();
            //fill(#3c4677, 10);
            fill(255,8);
            rect(0, 0, width, height);
        }

        if (frameCount % 20 == 0) {

            ArrayList<Particle> temp = new ArrayList<Particle>();
            for (int i = particles.size()/2; i < particles.size(); i++){
                temp.add(particles.get(i));
            }

            particles = temp;
        }

        ArrayList<Particle> temp = new ArrayList<Particle>();

        //stroke(#9cadb5);
        stroke(0,128);
        for (Particle p : particles) {
            if(!stuck(p.loc)){
                p.run();
                temp.add(p);
            }
        }

        particles = temp;

        for( ElectricCharge e : charges) {

            for (int i = 0; i < e.c_radius; i+=5) {
                fill(e.colour, i/10); //change to color of its sign
                stroke(0x000000);
                noStroke();
                ellipse(e.x_pos, e.y_pos, e.c_radius - i, e.c_radius - i);

            }
        }

        if (current_charge != null) {
            fill(0, 102, 153);
            text(actualForce.mag() + " N", current_charge.x_pos, current_charge.y_pos-10);
        }


        if (force_vector != null){
            drawVector();
        }

        // graph.beginDraw();
        //graph.drawBox();

        // graph.drawPoints();
        //graph.endDraw();
        if (charges.size() == 1) {
            taus[0] = 0.0f;
            taus[1] = 0.0f;
        }
        graphDraw(); // draws the torque graphs

    }

    void btnPanel(){
        //set up control panel
        fill(0x3c4677);
        stroke(highlight);
        rect(450, 675, 300, 50, 7);

        fill(0xFF0000);
        if (selected_obj == 1){
            fill(highlight);
        }
        stroke(0xFF0000);
        ellipse(pos_btn.x, pos_btn.y, btn_width, btn_width);

        textSize(21);
        fill(0xFF0000);
        text("+", pos_btn.x -7, (float) (pos_btn.y +5.5));


        fill(0x0070FF);
        if (selected_obj == 2){
            fill(highlight);
        }
        stroke(0x0070FF);
        ellipse(neg_btn.x, neg_btn.y, btn_width, btn_width);

        textSize(23);
        fill(0x0070FF);
        text("-", neg_btn.x -6, (float) (neg_btn.y +5.5));

        onHover();
    }


    //1) change the current object under control
//2) highlight the selected object
    void onHover(){
        selected_obj = 0; // 0 means not hovering over anything important

        if( inCircle(pos_btn.x, pos_btn.y, mouseX, mouseY, btn_width) ) {
            selected_obj = 1; //hovering over a button
        }

        if( inCircle(neg_btn.x, neg_btn.y, mouseX, mouseY, btn_width) ) {
            selected_obj = 2; //hovering over a button
        }

        boolean found = false; // is false if there is no object currently hovered over. otherwise true

        for (ElectricCharge c : charges) {
            if( inCircle(c.x_pos, c.y_pos, mouseX, mouseY, c.c_radius) ) {
                found = true;
                selected_obj = 3; //hovering over a charge
                hovered_charge = c;
                break;
            }
        }
        if (!found) { hovered_charge = null; }

    }

    boolean inCircle(float x1, float y1, float x2, float y2, float diameter) {
        float dis_x = x1 - x2;
        float dis_y = y1 - y2;

        if( sqrt(sq(dis_x) + sq(dis_y)) < diameter/2) {
            return true;
        }

        return false;
    }

    boolean inRect(float x, float y, float w, float h) {
        //stub
        return false;
    }

    public void mousePressed() {
        mouse_ctrl = true;

        force_vector = null;
        if(selected_obj == 1){
            draw_sign = 1;
        }
        else if(selected_obj == 2){
            draw_sign = 0;
        }
        else if(selected_obj == 3){ // clicking selection of charge
            if (hovered_charge != null) { // if you are hovering over an actual charge
                current_charge = hovered_charge;
                offset.set(current_charge.x_pos -(pos_ee.x)*pixelsPerMeter, current_charge.y_pos -(pos_ee.y)*pixelsPerMeter); //whenever you change the current charge, you need to change the offset too
            }
            //assign this charge to Haply
        }
        else if (charges.size() < 7){

            if (draw_sign == 1 ){
                addCharge(1);

            }

            if (draw_sign == 0){
                addCharge(0);
            }
        }


    }

    public void addCharge(int sign){
        //if (haply_board.data_available()) {
        //     //  /* GET END-EFFECTOR STATE (TASK SPACE) */

        //     angles.set(haply_2DoF.get_device_angles());
        //     pos_ee.set( haply_2DoF.get_device_position(angles.array()));
        //}

        //println("Offset ",offset);

        ElectricCharge c = new ElectricCharge(10, 100, mouseX, mouseY, sign);
        current_charge = c;
        offset.set(current_charge.x_pos - (pos_ee.x)*pixelsPerMeter, current_charge.y_pos -(pos_ee.y)*pixelsPerMeter);
        charges.add(c);


        ArrayList<PVector> v_list = computeEachForce();
        computeTotalForce(v_list);
    }

    public void flow(){
        for (ElectricCharge e : charges) {
            for (Particle p : particles) {
                p.addCenter(e.x_pos, e.y_pos, e.sign);
            }

        }
    }

    public ArrayList<PVector> computeEachForce() {
        ArrayList<PVector> vectors = new ArrayList<PVector>();

        for(ElectricCharge c : charges){
            if (c != current_charge){
                float dx = c.x_pos - current_charge.x_pos;
                float dy = c.y_pos - current_charge.y_pos;
                float dd = sqrt( dx*dx + dy*dy);

                println("dd: ", dd);

                float ft = K* current_charge.q * c.q/(dd*dd); // current_charge.q * c.q = qp*qn
                PVector vector = new PVector(ft*dx/dd, ft*dy/dd);
                vectors.add(vector);
            }
        }

        return vectors;
    }

    public void computeTotalForce(ArrayList<PVector> vectors){ //sets f_ee to what it should be
        float fx_total = 0;
        float fy_total = 0;

        for(PVector v : vectors) {
            fx_total += v.x;
            fy_total += v.y;
            //println(fx_total,fy_total);

            //if((abs(f_ee.x))>5){
            //      f_ee.x=0;
            //      f_ee.y=0;
            //      println("too close");
            //      //|abs(f_ee.y)>5
            //}
            //if (fx_total==0&fy_total==0){
            // player.setGain(-60);
            //}
            //else{
            actualForce = new PVector(fx_total, fy_total);

            f_ee.x=-fx_total/25;
            f_ee.y=fy_total/25;
            //println(abs(-fx_total),abs(fy_total));
            if((30<=abs(fx_total)&abs(fx_total)<50)|30<=abs(fy_total)&abs(fy_total)<50) {
                f_ee.x=0;
                f_ee.y=0;
                player.setGain(1);
            }
            else if((50<=abs(fx_total))|50<=abs(fy_total)) {
                f_ee.x= (float) (.571*fx_total/(abs(fx_total)));
                f_ee.y= (float) (-.571*fy_total/(abs(fy_total)));
                //f_ee.x=0;
                //f_ee.y=0;
                player.setGain(1);
            }else{
                f_ee.x=-fx_total/25;
                f_ee.y=fy_total/25;
                player.setGain(-10/(fy_total*fy_total+fx_total*fx_total));
            }


        }

        force_vector = new PVector(5*fx_total, 5*fy_total);
    }

    public void drawVector() {
        x2=-(force_vector.x*10);
        y2=-(force_vector.y*10);

        if (current_charge != null) {
            stroke(0x000000);
            line(current_charge.x_pos, current_charge.y_pos, current_charge.x_pos+x2,current_charge.y_pos+y2);
            pushMatrix();
            translate(current_charge.x_pos+x2,current_charge.y_pos+y2);

            float a = atan2(-x2, y2);
            rotate(a);
            line(0, 0, -10, -10);
            line(0, 0, 10, -10);
            popMatrix();
        }
    }

    public void createGraph(){
        for (int i = 0; i < 1000; i++) {
            float x = 10 + random(200);
            float y = 10 * exp((float) (0.015 * x));
            float xErr = 2*((float) r.nextGaussian());
            float yErr = 2*((float) r.nextGaussian());
            points.add(x + xErr, y + yErr);
        }

        graph = new GPlot(this);
        graph.setPos(750, 450);
        graph.setDim(150, 150);
        graph.setBoxBgColor(0x3c4677);
        graph.setBoxLineColor(highlight);

        graph.setLogScale("x");
        graph.setInvertedXScale(true);

        graph.setPoints(points);
        graph.setPointColor(color(100, 100, 255, 50));
    }

    public boolean stuck(PVector position) {

        for (ElectricCharge e : charges){
            if (inCircle(position.x, position.y, e.x_pos, e.y_pos, e.c_radius)){
                return true;
            }
        }
        return false;
    }


    /* Timer control event functions **************************************************************************************/
    public void onTickEvent(CountdownTimer t, long timeLeftUntilFinish){


        /* check if new data is available from physical device */
        if (haply_board.data_available()) {

            PVector tempAngle = new PVector(angles.x, angles.y);


            /* GET END-EFFECTOR POSITION (TASK SPACE) */

            angles.set(haply_2DoF.get_device_angles()); //update device angles

            if (angles.x == tempAngle.x && angles.y == tempAngle.y) { //for dragging the body
                mouse_ctrl = true;
            } else {
                mouse_ctrl = false;
            }

            if (mouse_ctrl == false){
                pos_ee.set( haply_2DoF.get_device_position(angles.array()));
                pos_ee.set(device2graphics(pos_ee));

                current_charge.x_pos =  (int) ((int) pos_ee.x)*pixelsPerMeter+(int) offset.x; //which coord system are these in?
                current_charge.y_pos =  (int) ((int) pos_ee.y)*pixelsPerMeter+(int) offset.y;

                if (current_charge != null && (current_charge.x_pos < 0 || current_charge.x_pos > width || current_charge.y_pos < 0 || current_charge.y_pos > height)) {
                    //^if the charge happens to be off the screen at any time

                    current_charge.x_pos = (current_charge.x_pos + 10*width) % width; //then put it back somewhere on the screen.
                    current_charge.y_pos = (current_charge.y_pos + 10*height) % height;
                    //offset.set(offset.x % width, offset.y % height);
                    println("curr charge position: ", current_charge.x_pos, ", ", current_charge.y_pos);
                }

                //println("pos_ee: ", pos_ee.x*pixelsPerMeter, ", " ,pos_ee.y*pixelsPerMeter);

                ArrayList<PVector> v_list = computeEachForce();
                computeTotalForce(v_list);
            }

            if (haply_2DoF != null) {
                taus = haply_2DoF.mechanisms.get_torque();
                //println("taus: ", taus[0], ", ", taus[1]);
            }

        }

        haply_2DoF.set_device_torques(f_ee.array());
        torques.set(haply_2DoF.mechanisms.get_torque());
        haply_2DoF.device_write_torques();
        println("pos_ee: ", pos_ee.x*pixelsPerMeter, ", " ,pos_ee.y*pixelsPerMeter);

        lastpos_ee = pos_ee;
    }
    PVector device2graphics(PVector deviceFrame){

        deviceFrame.set(-deviceFrame.x, deviceFrame.y);
        return new PVector(deviceFrame.x, deviceFrame.y);

    }
    /**
     * haptic timer reset
     */
    void onFinishEvent(CountdownTimer t){
        println("Resetting timer...");
        haptic_timer.reset();
        haptic_timer = CountdownTimerService.getNewCountdownTimer(this).configure(SIMULATION_PERIOD, HOUR_IN_MILLIS).start();
    }

    public void keyPressed() {
        boolean found = false;
        int index = 0;

        if (key == BACKSPACE) { // when the mouse hovers over a charge and backspace is pressed, the charge is deleted.

            for (int i = 0; i < charges.size(); i++) { //find charge
                ElectricCharge temp = charges.get(i);
                if (found == false && hovered_charge.x_pos == temp.x_pos && hovered_charge.y_pos == temp.y_pos) {
                    index = i;
                    found = true;
                }
            }
            if (found == true) { //if found
                if (current_charge == hovered_charge) { // removing avatar charge does nothing. you can't do that, because then force rendering messes up
                    //if (charges.size() == 1) { //if there is only the avatar left, then remove that charge and the arrow too?
                    //    force_vector = null;
                    //    current_charge = null;
                    //}
                    //else { //2 or more charges
                    //current_charge = charges.get((index + 1) % charges.size());
                    //offset.set(current_charge.x_pos -(pos_ee.x)*pixelsPerMeter, current_charge.y_pos -(pos_ee.y)*pixelsPerMeter);
                    //}
                }

                else {

                    charges.remove(index);
                    ArrayList<PVector> v_list = computeEachForce();
                    computeTotalForce(v_list);

                }
            }
        }

        //println("hovered charge position: ", hovered_charge.x_pos, ", ", hovered_charge.y_pos);
    }


    /* Graph Stuff ************************************************************************************** Copy-paste this, and Dot.java into any file */

// Fields //

    int red = color(255, 0, 0);
    int green = color(0, 255, 0);
    int blue = color(0, 0, 255);
    int hotpink = color(255,182,193);
    int white = color(255, 255, 255);

    ArrayList<Dot> tau1List = new ArrayList(); // list of dots
    ArrayList<Dot> tau2List = new ArrayList();

    int graphwidth;
    int screenWidth; //input your initial screen width and height here
    int screenHeight;
    int centrex;
    int centrey;


    float[] taus = new float[2];
    int tauConst = 60; //constant factor to multiply the raw torques by, so that they are visible on the screen

    // Call this function in setup() //
    public void graphSetup() {

        if (haply_2DoF != null) {
            taus = haply_2DoF.mechanisms.get_torque();
        }
        //println("width: ", width);
        //println("height: ", height);
        //println("centrey: ", centrey);
    }
    // Call this function in draw(), right after world.draw() //
    public void graphDraw() {

        // intialising vars //
        graphwidth = 150; //change this as needed
        screenWidth = width-graphwidth;
        screenHeight = height-graphwidth;
        centrex = graphwidth/2;
        centrey = height-graphwidth/2;


        fill(0);
        stroke(0);
        rect(0, 0, graphwidth, height); // vertical graph
        rect(0, height-graphwidth, width, graphwidth); //horizontal graph

        stroke(red);
        line(graphwidth/2, 0, graphwidth/2, height);
        stroke(0);

        stroke(red);
        line(0, height-graphwidth/2, width, height-graphwidth/2);
        stroke(0);

        addText();

        //println("centre: ", centrex, ", ", centrey);

        // Drawing the dots //

        tau2List.add(new Dot(centrex, centrey + floor(taus[1]*tauConst), false)); //make a new dot for the current torque
        tau1List.add(new Dot(centrex + floor(taus[0]*tauConst), centrey, true)); //make a new dot for the current torque

        for (Dot dot : tau2List) { //update the two lists
            dot.update(width);
        }
        for (Dot dot : tau1List) {
            dot.update(width);
        }

        for (Dot each : tau1List) { //draw the dot in each list
            drawDot(each, white);
        }

        for (Dot each : tau2List) {
            drawDot(each, white);
        }

    }

    public void drawDot(Dot d, int c) {
        if (d.type && d.x <= graphwidth || !d.type && d.y >= height-graphwidth) {
            stroke(c);
            point(d.x, d.y);
            stroke(0);
        }
    }

    public void addText() {
        fill(white); // TODO the dimensions here aren't right
        textSize(24);
        text("Horizontal", 10, 30);
        text("Torque", 30, 62);
        fill(0);

        fill(white); // TODO the dimensions here aren't right
        textSize(32);
        text("Vertical Torque", height - 30, height-8*graphwidth/10);
        fill(0);
    }

    // These are the particles that create field lines for the charges
    public class Particle {

        float ll = 0.5f;
        float deltax, deltay;

        PVector loc;

        ArrayList<PVector> centers = new ArrayList <PVector> ();

        Particle() {
            loc = new PVector(random(width), random(height));

        }

        void run() {

            if (loc.x > width || loc.x < 0 || loc.y > height || loc.y < 0) {
                loc = new PVector(random(width), random(height));
            } else {
                if (centers.size() != 0){
                    loc.add(getDirection(loc));
                }
                point(loc.x, loc.y);
            }
        }

        void addCenter (float x, float y, int sign) {

            PVector new_center = new PVector(x,y, sign);
            centers.add(new_center);

        }

        PVector getDirection(PVector p) {

            ArrayList <PVector> effects = new ArrayList<PVector>();
            float total_ex = 0;
            float total_ey = 0;
            float total_e  = 0;

            for (PVector c : centers) {

                float dx = p.x - c.x;
                float dy = p.y - c.y;
                float d  = sqrt(dx*dx + dy*dy);

                //sign
                float E;
                if (c.z == 1) {
                    E = qp/(d*d);
                } else {
                    E = qn/(d*d);
                }

                float Ex = dx*E/d;
                float Ey = dy*E/d;

                PVector effect = new PVector(Ex, Ey);
                effects.add(effect);

            }

            for (PVector e : effects){

                total_ex += e.x;
                total_ey += e.y;

            }

            total_e = sqrt(total_ex * total_ex + total_ey * total_ey);

            deltax=ll * total_ex/total_e;
            deltay=ll * total_ey/total_e;

            return new PVector(deltax, deltay);
        }
    }

}
