package recogwithhaptics;

import exceptions.BoardException;
import fisica.*;
import haptics.tech.Board;
import haptics.tech.Device;
import haptics.tech.DeviceType;
import haptics.tech.HVirtualCoupling;
import processing.core.*;
import processing.data.FloatList;
import processing.data.StringList;
import processing.data.Table;
import processing.data.TableRow;
import processing.serial.Serial;
import recog.OneDollar;
import recog.SpringRecog;
import com.dhchoi.CountdownTimer;
import com.dhchoi.CountdownTimerService;

import java.util.ArrayList;

import static haptics.tech.DeviceType.HaplyTwoDOF;
import static processing.core.PApplet.println;

public class SpringRecogHaptic extends PApplet{

    public static void main(String[] args) {

        PApplet.main(new String[]{"recogwithhaptics.SpringRecogHaptic"});

    }


    /* Device block definitions ********************************************************************************************/
    Device            haply_2DoF;
    byte              deviceID                   = 5;
    Board             haply_board;
    DeviceType        degreesOfFreedom;
    boolean           rendering_force                 = false;


    /* Simulation Speed Parameters ****************************************************************************************/
    final long        SIMULATION_PERIOD          = 1; //ms
    final long        HOUR_IN_MILLIS             = 36000000;
    CountdownTimer    haptic_timer;
    float             dt                        = (float) (SIMULATION_PERIOD/1000.0);


    /* generic data for a 2DOF device */
    /* joint space */
    PVector           angles                    = new PVector(0, 0);
    PVector           torques                   = new PVector(0, 0);

    /* task space */
    PVector           pos_ee                    = new PVector(0, 0);
    PVector           pos_ee_last               = new PVector(0, 0);
    PVector           f_ee                      = new PVector(0, 0);
    float             offsetX                   =0;
    float             offsetY                   =0;
    //*******************************************************************************************************
    FWorld world;
    FPoly poly;
    FBody ava;
    FBlob blob;
    PImage spring;
    HVirtualCoupling s;
    FloatList sppointsx;
    FloatList         sppointsy;
    ArrayList<FBody>  TouchBody;
    ArrayList<FJoint> Joints;
    //*****
    float            frequency                  = 5;
    float            damping                    = 10;
    float            puenteY;
    int              boxWidth                   =4;
    float edgeTopLeftX = (float) 0.0;
    float edgeTopLeftY = (float) 0.0;
    float edgeBottomRightX =width;
    float edgeBottomRightY = height;
    //********************************************************************************************************
    OneDollar one;
    // Training setup:
    ArrayList <PVector> Pointlist;
    ArrayList <PVector> Pointlists;
    StringList       xPoints;
    StringList       yPoints;
    PVector          Po;
    String           pointSave;
    Table            table;
    Table            train;
    char             lable='N';
    PVector          po;
    Boolean          trFlag;
    int[]            candidate;
    /**********************************************************************************************************************/
    /*Definfing the sketching Env*/
    PGraphics pgDrawing;
    PShape SIM;
    ArrayList <PShape> bg;
    PShape tst;
    //RShape grp;
    int NP=1;
    float x=width/2;
    float y=height/2;
    boolean flag= true;
    int selected;
    boolean avatar=false;
    private ArrayList<PVector> listOfPoints;

    /**********************************************************************************************************************/


    public void setup(){
        size(1200, 700);
        background(255);
        trFlag=true;
        table=loadTable("table.csv","header");

// Training setup:
        train=new Table();
        Pointlists=new ArrayList();
        Po=new PVector();
        yPoints= new StringList();
        xPoints= new StringList();
        println(table.getRowCount() + " total rows in table");
        if (table.getRowCount()==0){
            table= new Table();
            table.addColumn("X/Y");
            table.addColumn("Lable");
            candidate= new int[3];}
// *************************
        /*Making the drawing objects*/
        //pgDrawing = createGraphics(1057, 1057, SVG, "test1.svg");
        //pgDrawing.beginDraw();
        //pgDrawing.beginShape();
        listOfPoints = new ArrayList<>();
        bg=new ArrayList<PShape>();
        //bg.add(new PShape());
        //bg.get(0)=createShape();
        SIM=createShape(GROUP);

//*************************** making the phyiscal word
        Fisica.init(this);
        FBody ava;
        s= new HVirtualCoupling((1));
        //s.h_avatar.setDensity(2);
        //s.h_avatar.setFill(255,0,0);
        world = new FWorld();


        TouchBody=new ArrayList();
        world.setGravity(0, 50);
        world.setEdgesRestitution((float) 0.01);
        world.setEdges();
        //world.remove(world.left);
        //world.remove(world.right);
        //world.remove(world.top);

//***************************
        /* Initialization of the Board, Device, and Device Components */

        /* BOARD */
        /* BOARD */
        haply_board = new Board(this, "COM3", 115200); //Put your COM# port here

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

//***************************
        po= new PVector();
        Pointlist=new ArrayList();
        one = new OneDollar(this);
        one.setVerbose(false);
        println(one);


        one.setMinSimilarity(65);

        // Data-Pre-Processing:
        one.setMinDistance(100).enableMinDistance();
        one.setMaxTime(600000).enableMaxTime();
        //one.setMinSpeed(3).enableMinSpeed();

        // Algorithm settings:
        one.setFragmentationRate(100);
        one.setRotationStep(3);
        one.setRotationAngle(20);
        one.setBoundingBox(200);
        println(one);
        // All templates:
        //one.learn("triangle", new int[] {137,139,135,141,133,144,132,146,130,149,128,151,126,155,123,160,120,166,116,171,112,177,107,183,102,188,100,191,95,195,90,199,86,203,82,206,80,209,75,213,73,213,70,216,67,219,64,221,61,223,60,225,62,226,65,225,67,226,74,226,77,227,85,229,91,230,99,231,108,232,116,233,125,233,134,234,145,233,153,232,160,233,170,234,177,235,179,236,186,237,193,238,198,239,200,237,202,239,204,238,206,234,205,230,202,222,197,216,192,207,186,198,179,189,174,183,170,178,164,171,161,168,154,160,148,155,143,150,138,148,136,148} );
        //one.learn("c", new int[] {127,141,124,140,120,139,118,139,116,139,111,140,109,141,104,144,100,147,96,152,93,157,90,163,87,169,85,175,83,181,82,190,82,195,83,200,84,205,88,213,91,216,96,219,103,222,108,224,111,224,120,224,133,223,142,222,152,218,160,214,167,210,173,204,178,198,179,196,182,188,182,177,178,167,170,150,163,138,152,130,143,129,140,131,129,136,126,139} );
        one.learn("r", new int[] {78,149,78,153,78,157,78,160,79,162,79,164,79,167,79,169,79,173,79,178,79,183,80,189,80,193,80,198,80,202,81,208,81,210,81,216,82,222,82,224,82,227,83,229,83,231,85,230,88,232,90,233,92,232,94,233,99,232,102,233,106,233,109,234,117,235,123,236,126,236,135,237,142,238,145,238,152,238,154,239,165,238,174,237,179,236,186,235,191,235,195,233,197,233,200,233,201,235,201,233,199,231,198,226,198,220,196,207,195,195,195,181,195,173,195,163,194,155,192,145,192,143,192,138,191,135,191,133,191,130,190,128,188,129,186,129,181,132,173,131,162,131,151,132,149,132,138,132,136,132,122,131,120,131,109,130,107,130,90,132,81,133,76,133} );
        //one.learn("x", new int[] {87,142,89,145,91,148,93,151,96,155,98,157,100,160,102,162,106,167,108,169,110,171,115,177,119,183,123,189,127,193,129,196,133,200,137,206,140,209,143,212,146,215,151,220,153,222,155,223,157,225,158,223,157,218,155,211,154,208,152,200,150,189,148,179,147,170,147,158,147,148,147,141,147,136,144,135,142,137,140,139,135,145,131,152,124,163,116,177,108,191,100,206,94,217,91,222,89,225,87,226,87,224} );
        //one.learn("check", new int[] {91,185,93,185,95,185,97,185,100,188,102,189,104,190,106,193,108,195,110,198,112,201,114,204,115,207,117,210,118,212,120,214,121,217,122,219,123,222,124,224,126,226,127,229,129,231,130,233,129,231,129,228,129,226,129,224,129,221,129,218,129,212,129,208,130,198,132,189,134,182,137,173,143,164,147,157,151,151,155,144,161,137,165,131,171,122,174,118,176,114,177,112,177,114,175,116,173,118} );
        //one.learn("caret", new int[] {79,245,79,242,79,239,80,237,80,234,81,232,82,230,84,224,86,220,86,218,87,216,88,213,90,207,91,202,92,200,93,194,94,192,96,189,97,186,100,179,102,173,105,165,107,160,109,158,112,151,115,144,117,139,119,136,119,134,120,132,121,129,122,127,124,125,126,124,129,125,131,127,132,130,136,139,141,154,145,166,151,182,156,193,157,196,161,209,162,211,167,223,169,229,170,231,173,237,176,242,177,244,179,250,181,255,182,257} );
        one.learn("s", new int[] {307,216,333,186,356,215,375,186,399,216,418,186} );
        //one.learn("arrow", new int[] {68,222,70,220,73,218,75,217,77,215,80,213,82,212,84,210,87,209,89,208,92,206,95,204,101,201,106,198,112,194,118,191,124,187,127,186,132,183,138,181,141,180,146,178,154,173,159,171,161,170,166,167,168,167,171,166,174,164,177,162,180,160,182,158,183,156,181,154,178,153,171,153,164,153,160,153,150,154,147,155,141,157,137,158,135,158,137,158,140,157,143,156,151,154,160,152,170,149,179,147,185,145,192,144,196,144,198,144,200,144,201,147,199,149,194,157,191,160,186,167,180,176,177,179,171,187,169,189,165,194,164,196} );
        //one.learn("leftsquarebracket", new int[] {140,124,138,123,135,122,133,123,130,123,128,124,125,125,122,124,120,124,118,124,116,125,113,125,111,125,108,124,106,125,104,125,102,124,100,123,98,123,95,124,93,123,90,124,88,124,85,125,83,126,81,127,81,129,82,131,82,134,83,138,84,141,84,144,85,148,85,151,86,156,86,160,86,164,86,168,87,171,87,175,87,179,87,182,87,186,88,188,88,195,88,198,88,201,88,207,89,211,89,213,89,217,89,222,88,225,88,229,88,231,88,233,88,235,89,237,89,240,89,242,91,241,94,241,96,240,98,239,105,240,109,240,113,239,116,240,121,239,130,240,136,237,139,237,144,238,151,237,157,236,159,237} );
        //one.learn("rightsquarebracket", new int[] {112,138,112,136,115,136,118,137,120,136,123,136,125,136,128,136,131,136,134,135,137,135,140,134,143,133,145,132,147,132,149,132,152,132,153,134,154,137,155,141,156,144,157,152,158,161,160,170,162,182,164,192,166,200,167,209,168,214,168,216,169,221,169,223,169,228,169,231,166,233,164,234,161,235,155,236,147,235,140,233,131,233,124,233,117,235,114,238,112,238} );
        //one.learn("v", new int[] {89,164,90,162,92,162,94,164,95,166,96,169,97,171,99,175,101,178,103,182,106,189,108,194,111,199,114,204,117,209,119,214,122,218,124,222,126,225,128,228,130,229,133,233,134,236,136,239,138,240,139,242,140,244,142,242,142,240,142,237,143,235,143,233,145,229,146,226,148,217,149,208,149,205,151,196,151,193,153,182,155,172,157,165,159,160,162,155,164,150,165,148,166,146} );
        //one.learn("delete", new int[] {123,129,123,131,124,133,125,136,127,140,129,142,133,148,137,154,143,158,145,161,148,164,153,170,158,176,160,178,164,183,168,188,171,191,175,196,178,200,180,202,181,205,184,208,186,210,187,213,188,215,186,212,183,211,177,208,169,206,162,205,154,207,145,209,137,210,129,214,122,217,118,218,111,221,109,222,110,219,112,217,118,209,120,207,128,196,135,187,138,183,148,167,157,153,163,145,165,142,172,133,177,127,179,127,180,125} );
        //one.learn("leftcurlybrace", new int[] {150,116,147,117,145,116,142,116,139,117,136,117,133,118,129,121,126,122,123,123,120,125,118,127,115,128,113,129,112,131,113,134,115,134,117,135,120,135,123,137,126,138,129,140,135,143,137,144,139,147,141,149,140,152,139,155,134,159,131,161,124,166,121,166,117,166,114,167,112,166,114,164,116,163,118,163,120,162,122,163,125,164,127,165,129,166,130,168,129,171,127,175,125,179,123,184,121,190,120,194,119,199,120,202,123,207,127,211,133,215,142,219,148,220,151,221} );
        //one.learn("rightcurlybrace", new int[] {117,132,115,132,115,129,117,129,119,128,122,127,125,127,127,127,130,127,133,129,136,129,138,130,140,131,143,134,144,136,145,139,145,142,145,145,145,147,145,149,144,152,142,157,141,160,139,163,137,166,135,167,133,169,131,172,128,173,126,176,125,178,125,180,125,182,126,184,128,187,130,187,132,188,135,189,140,189,145,189,150,187,155,186,157,185,159,184,156,185,154,185,149,185,145,187,141,188,136,191,134,191,131,192,129,193,129,195,129,197,131,200,133,202,136,206,139,211,142,215,145,220,147,225,148,231,147,239,144,244,139,248,134,250,126,253,119,253,115,253} );
        //one.learn("star", new int[] {75,250,75,247,77,244,78,242,79,239,80,237,82,234,82,232,84,229,85,225,87,222,88,219,89,216,91,212,92,208,94,204,95,201,96,196,97,194,98,191,100,185,102,178,104,173,104,171,105,164,106,158,107,156,107,152,108,145,109,141,110,139,112,133,113,131,116,127,117,125,119,122,121,121,123,120,125,122,125,125,127,130,128,133,131,143,136,153,140,163,144,172,145,175,151,189,156,201,161,213,166,225,169,233,171,236,174,243,177,247,178,249,179,251,180,253,180,255,179,257,177,257,174,255,169,250,164,247,160,245,149,238,138,230,127,221,124,220,112,212,110,210,96,201,84,195,74,190,64,182,55,175,51,172,49,170,51,169,56,169,66,169,78,168,92,166,107,164,123,161,140,162,156,162,171,160,173,160,186,160,195,160,198,161,203,163,208,163,206,164,200,167,187,172,174,179,172,181,153,192,137,201,123,211,112,220,99,229,90,237,80,244,73,250,69,254,69,252} );
        //one.learn("pigtail", new int[] {81,219,84,218,86,220,88,220,90,220,92,219,95,220,97,219,99,220,102,218,105,217,107,216,110,216,113,214,116,212,118,210,121,208,124,205,126,202,129,199,132,196,136,191,139,187,142,182,144,179,146,174,148,170,149,168,151,162,152,160,152,157,152,155,152,151,152,149,152,146,149,142,148,139,145,137,141,135,139,135,134,136,130,140,128,142,126,145,122,150,119,158,117,163,115,170,114,175,117,184,120,190,125,199,129,203,133,208,138,213,145,215,155,218,164,219,166,219,177,219,182,218,192,216,196,213,199,212,201,211} );
        // http://depts.washington.edu/aimgroup/proj/dollar/unistrokes.gif

        one.bind("triangle circle rectangle x check zigzag arrow leftsquarebracket rightsquarebracket v delete leftcurlybrace righttcurlybrace star pigtail", "detected");
    }

    void detected(String gesture, float percent, int startX, int startY, int centroidX, int centroidY, int endX, int endY){
        //println("Gesture: "+gesture+", "+startX+"/"+startY+", "+centroidX+"/"+centroidY+", "+endX+"/"+endY);
    }

    public void draw(){

        if(!rendering_force){
            background(255);
            world.step(1.0f/25.0f);
            world.draw(this);

            if ( ava != null  ) {
                //println("force in x",ava.getX(),"Force in Y direction",ava.getY());
            }
        }
    }

    public void mouseDragged(){
        FBody hovered = world.getBody(mouseX, mouseY);
        if ( hovered == null  ) {
            Pointlist.add(new PVector(mouseX, mouseY));
            Pointlists.add(new PVector(mouseX,mouseY));
            xPoints.append(str(mouseX));
            xPoints.append(str(mouseY));
            stroke(126);

            listOfPoints.add(new PVector(mouseX, mouseY));
        }
    }
    public void mouseReleased(){
        if (trFlag==false){
            FBody hovered = world.getBody(mouseX, mouseY);
            if ( hovered == null  ) {

                if (Pointlist.size()>=40){
                    for (int i=1;i<Pointlist.size();i++){
                        po=Pointlist.get(i);
                        one.track(po.x,po.y);
                    }
                }
            }
            Pointlist.clear();
            String res=one.checkGlobalCallbacks();

            if(res!=""){
                switch (res.charAt(0)){
                    case 'r':
                        println("A Mass is detected");
                        addMass();
                        //drawElement();
                        break;
                    case 'c':
                        println("A Charge is detected");
                        addcharge();
                        break;
                    case 's':
                        println("A Spring is detected");
                        addSpring();
                        break;
                    default:
                        listOfPoints = new ArrayList<>();
                        break;
                }
            }else{
                listOfPoints = new ArrayList<>();
            };
        }else{
            String[] ResultX=xPoints.array();
            xPoints.clear();
            pointSave=join(ResultX,",");
            String[] list = split(pointSave, ' ');
            TableRow row = table.addRow();
            row.setString("X/Y", pointSave);
            row.setString("Lable", str(lable));
            saveTable(table,"table.csv");
        }

    }

    public void keyPressed() {
        lable=key;
        switch(key){
            case 32:
                trFlag=true;
                train=loadTable("table.csv","header");
                table=train;
                for (int i=0;i<(table.getRowCount());i++){
                    TableRow row = train.getRow(i);
                    String ps=(row.getString("X/Y"));
                    String la=row.getString("Lable");

                    //println("Value is",int(la.charAt(0)));
                    if ((la.charAt(0) =='N')||( la.charAt(0) ==32)){
                        table.removeRow(i);
                        saveTable(table,"table.csv");
                    }else{
                        String[] pts=split(ps,',');
                        int[] points = parseInt(pts);
                        if (points.length <= 5) {
                            table.removeRow(i);
                            saveTable(table, "table.csv");
                        }else{
                            one.addGesture(la,points);
                            println("LERANING NEW STROKES...");
                            println(la);
                            one.bind(la,la);
                        }
                    }
                }
                break;
            case 113:
                println("******************************************************************");
                println("SYSTEM IS READY");
                trFlag=false;
                break;
            case 't':
                println("Mass");
                lable=key;
                trFlag=true;
                break;
            case 's':
                println("spring");
                lable=key;
                trFlag=true;
                break;
            case 'r':
                println("Mass");
                lable=key;
                trFlag=true;
                break;
            case (BACKSPACE):
                println("Backspace");
                FBody hovered = world.getBody(mouseX, mouseY);
                if ( hovered != null  ) {
                    //int gind;
                    //gind=hovered.getGroupIndex();
                    //if (gind==1){
                    //      println("Spring is removed");
                    //  }else{
                    world.remove(hovered);
                    //}
                }
                break;
            default:
                lable=key;
                trFlag=false;

                //trFlag=true;
                break;
        }
    }


    void drawElement(){
        bg.add(tst);
        flag=false;
        SIM.addChild(tst);
        tst.endShape();
        //pgDrawing.shape(SIM);
        //pgDrawing.endShape();
        pgDrawing.endDraw();
        pgDrawing.dispose();
        pgDrawing.beginDraw();
        pgDrawing.beginShape();
        // shape(SIM);
        println(bg.size());
        for (int i=0;i<bg.size();i++)
        {   shape(SIM.getChild(i),0,0);
        }////
        //P++;
        PVector v=new PVector (0,0);
        for (int i = 0; i < SIM.getChild(0).getVertexCount(); i++) {
            v = SIM.getChild(0).getVertex(i);
            //println((v.x-(width/2))/4000-pos_ee.x, ((height/5)+v.y)/4000-pos_ee.y);
            //println(pos_ee.x*4000,pos_ee.y*4000);
        }
        listOfPoints = new ArrayList<>();
    }
    void contactEnded(FContact c) {
        FBody b = (FBody)c.getBody1();
        FBody a = (FBody)c.getBody2();
        //println(a.getName());
        //println(a.getName());
        if ((a.getName()=="EndF")&&(b.getName()=="Mass")){
            b.setName("Joint");
            //a.setName("Joint");
            FRevoluteJoint jp= new FRevoluteJoint(a, b);

            jp.setAnchor(mouseX,mouseY);
            jp.setFill(0);
            jp.setDrawable(false);;
            world.add(jp);

        }else if((a.getName()=="EndF")&&(b.getName()=="Joint")){
            b.setName("Joint");
            a.setName("Joint");
            FRevoluteJoint jp= new FRevoluteJoint(a, b);

            jp.setAnchor(mouseX,mouseY);
            jp.setFill(0);
            jp.setDrawable(false);;
            world.add(jp);
        }else if((a.getName()=="pin")&&(b.getName()=="EndF")){

            println("two joint");
            Joints=a.getJoints();
            println(Joints.get(1).getBody1().getName());
            println("spring",Joints.get(1).getBody2().getName());
            //if (a.isStatic()){
            world.remove(a);
            //}
            FRevoluteJoint jp= new FRevoluteJoint(Joints.get(1).getBody2(), b);

            jp.setAnchor(mouseX,mouseY);
            jp.setFill(0);
            jp.setDrawable(false);;
            world.add(jp);
        }else if((a.getName()=="pin")&&(b.getName()=="Joint")){

            println("two joint");
            //b.setName("MCenter");
            Joints=a.getJoints();
            a.setName("null");
            world.remove(a);
            // println(Joints.get(1).getBody1().getName());
            println("spring",Joints.get(1).getBody2().getName());
            //if (a.isStatic()){

            //}
            FRevoluteJoint jp= new FRevoluteJoint(Joints.get(1).getBody2(), b);

            jp.setAnchor(mouseX,mouseY);
            jp.setFill(0);
            jp.setDrawable(false);;
            world.add(jp);
        }
        //} else if((a.getName()=="pin")&&(b.getName()=="Joint")){
        //     a.setName("Joint");
        //     b.setName("Joint");
        //     a.setStatic(false);
        //     FRevoluteJoint jp= new FRevoluteJoint(a, b);

        //     jp.setAnchor(mouseX,mouseY);
        //     jp.setFill(0);
        //     jp.setDrawable(false);;
        //     world.add(jp);
        //}
    }
    public void mouseClicked(){
        if (mouseButton == RIGHT) {
            ava = world.getBody(mouseX, mouseY);
            if ( ava != null  ) {
                println("avatar is being selected");
                //s= new HVirtualCoupling(ava);
                //s.h_avatar.setDensity(2);
                //s.h_avatar.setFill(255,0,0);
                // haply_avatar = loadImage("../img/Haply_avatar.png");
                //haply_avatar.resize((int)(hAPI_Fisica.worldToScreen(1)), (int)(hAPI_Fisica.worldToScreen(1)));
                // s.h_avatar.attachImage(haply_avatar);

                if (haply_board.data_available()) {
                    //  /* GET END-EFFECTOR STATE (TASK SPACE) */

                    angles.set(haply_2DoF.get_device_angles());
                    pos_ee.set( haply_2DoF.get_device_position(angles.array()));
                    pos_ee.mult(100);
                    offsetX=-ava.getX()-((pos_ee.x*20));
                    offsetY=-ava.getY()+((pos_ee.y*20));

                }
            }
        }
    }


    /**********************************************************************************************************************
     * Haptics simulation event, engages state of physical mechanism, calculates and updates physics simulation conditions
     **********************************************************************************************************************/

    public void onTickEvent(CountdownTimer t, long timeLeftUntilFinish){

        println("ontick called");

        rendering_force = true;
        if (ava!=null){
            //  /* GET END-EFFECTOR STATE (TASK SPACE) */
            if (haply_board.data_available()) {
                /* GET END-EFFECTOR STATE (TASK SPACE) */

                angles.set(haply_2DoF.get_device_angles());
                pos_ee.set( haply_2DoF.get_device_position(angles.array()));
                pos_ee.mult(500);
                //println(ava.getX(),ava.getY());


                //s.updateCouplingForce();
            }
            f_ee.set(-(ava.getX()+(pos_ee.x*20)+offsetX)*1000, +(ava.getY()-(pos_ee.y*20)+offsetY)*1000);
            //f_ee.set(0,0);
            f_ee.div(200); //
            haply_2DoF.set_device_torques(f_ee.array());
            torques.set(haply_2DoF.mechanisms.get_torque());
            haply_2DoF.device_write_torques();
            ava.setPosition(-(pos_ee.x*20)-offsetX, (pos_ee.y*20)-offsetY);

        }
        else{
            //f_ee.set(0,0);
        }
        //world.step(1.0f/25.0f);
        rendering_force = false;

//world.step(1.0f/1000.0f);

/////  s.setToolPosition(edgeTopLeftX+width/2-(pos_ee).x+1.0, edgeTopLeftY+(pos_ee).y);
        //s.updateCouplingForce();

        //f_ee.set(-s.getVCforceX(), s.getVCforceY());

        //f_ee.div(100000); //
        //haply_2DoF.set_device_torques(f_ee.array());
        //torques.set(haply_2DoF.mechanisms.get_torque());
        //haply_2DoF.device_write_torques();
        //angles.set(haply_2DoF.get_device_angles());
        //pos_ee.set( haply_2DoF.get_device_position(angles.array()));
        //pos_ee.set(pos_ee.copy().mult(100));
        ////println(pos_ee.x,pos_ee.y);

//  }
//  //if ( ava != null  ) {





    }


    /* Timer control event functions **************************************************************************************/

    /**
     * haptic timer reset
     */
    public void onFinishEvent(CountdownTimer t){
        println("Resetting timer...");
        haptic_timer.reset();
        haptic_timer = CountdownTimerService.getNewCountdownTimer(this).configure(SIMULATION_PERIOD, HOUR_IN_MILLIS).start();
    }

    void addMass() {

        poly = new FPoly();
        poly.setStrokeWeight(3);
        poly.setFill(120, 30, 90);
        poly.setBullet(true);
        poly.setDensity((float) 0.005);
        poly.setRotatable(false);
        poly.setName("Mass");
        poly.setRestitution((float) 0.7);
        for (int i = 0; i < listOfPoints.size(); i++) {
            PVector v = listOfPoints.get(i);
            poly.vertex(v.x, v.y);
        }
        TouchBody = poly.getTouching();
        if (TouchBody != null) {
            println("Touching bodies", TouchBody);
        }
        //tst.endShape();
        //tst = createShape();
        //tst.beginShape();
        listOfPoints = new ArrayList<>();
        if (poly != null) {
            world.add(poly);
            poly = null;
        }
    }

    void addcharge() {

        poly = new FPoly();
        poly.setStrokeWeight(3);
        poly.setStaticBody(true);
        poly.setFill(255, 255, 255);
        poly.setDensity(1);
        poly.setRestitution((float) 0.5);
        for (int i = 0; i < listOfPoints.size(); i++) {
            PVector v = listOfPoints.get(i);
            poly.vertex(v.x, v.y);
        }
        //tst.endShape();
        //tst = createShape();
        //tst.beginShape();
        if (poly != null) {
            world.add(poly);
            poly = null;
        }
    }

    //**********************************************************************
    void addSpring1() {
        int verc = listOfPoints.size();
        FBody[] steps = new FBody[floor(verc / 10)];
//**************Making the parts of the spring*************
        for (int i = 0; i < steps.length; i++) {
            PVector v = listOfPoints.get(i * 10);
            steps[i] = new FBox(boxWidth, 2);
            steps[i].setPosition(v.x, v.y);
            steps[i].setNoStroke();
            steps[i].setGroupIndex(1);
            //steps[i].setStaticBody(true);
            steps[i].setFill(120, 200, 190);
            world.add(steps[i]);
        }
//**************Making the hanging point 1*************
        FCircle hang = new FCircle(10);
        hang.setStatic(true);
        hang.setPosition(listOfPoints.get(0).x, listOfPoints.get(0).y - 10);
        hang.setDrawable(true);
        hang.setGroupIndex(1);
        hang.setBullet(true);
        world.add(hang);
//**************connecting the first part of spring to the *************
        FDistanceJoint juntaPrincipio = new FDistanceJoint(steps[0], hang);
        juntaPrincipio.setAnchor1(-boxWidth / 2, 0);
        juntaPrincipio.setAnchor2(0, 0);
        juntaPrincipio.setFrequency(frequency);
        juntaPrincipio.setDamping(damping);
        juntaPrincipio.calculateLength();
        juntaPrincipio.setFill(0);
        juntaPrincipio.setStrokeWeight(5);
//juntaPrincipio.setGroupIndex(1);
        world.add(juntaPrincipio);

//**************connecting the the sequences of spring together *************

        for (int i = 1; i < steps.length; i++) {
            FDistanceJoint junta = new FDistanceJoint(steps[i - 1], steps[i]);
            junta.setAnchor1(boxWidth / 2, 0);
            junta.setAnchor2(-boxWidth / 2, 0);
            junta.setFrequency(frequency);
            junta.setDamping(damping);
            junta.setFill(1);
            junta.setStrokeWeight(5);
            junta.setNoFill();
            junta.calculateLength();
            //junta.setGroupIndex(1);
            world.add(junta);
        }
//**************connecting different points of spring to the hanger 1 *************

        for (int i = 0; i < steps.length; i++) {
            FDistanceJoint junta = new FDistanceJoint(hang, steps[i]);
            junta.setAnchor1(boxWidth / 2, 0);
            junta.setAnchor2(-boxWidth / 2, 0);
            junta.setFrequency(frequency);
            junta.setDamping(damping);
            junta.setFill(0);
            junta.setDrawable(false);
            junta.calculateLength();
            //junta.setGroupIndex(1);
            world.add(junta);
        }
        //**************Creating hanger 2 and connecting different points of spring to the hanger 1 *************

        FCircle hanginv = new FCircle(10);
        hanginv.setStatic(true);
        hanginv.setPosition(listOfPoints.get(0).x + 30, listOfPoints.get(0).y);
        hanginv.setDrawable(true);
        hanginv.setDensity(1);
        hanginv.setGroupIndex(1);

        world.add(hanginv);

        for (int i = 1; i < steps.length; i++) {
            FDistanceJoint junta = new FDistanceJoint(hanginv, steps[i - 1]);
            junta.setAnchor1(boxWidth / 2, 10);
            junta.setAnchor2(-boxWidth / 2, 10);
            junta.setFrequency(frequency);
            junta.setDamping(damping);
            junta.setFill(0);
            junta.setDrawable(false);
            junta.calculateLength();
            world.add(junta);
        }
        int endv = (floor(verc / 10)) * 10;

        FCircle endpoint = new FCircle(15);
        endpoint.setPosition(listOfPoints.get(verc - 1).x, listOfPoints.get(endv - 10).y + 20);
        endpoint.setDrawable(true);
        endpoint.setFill(120, 30, 0);
        endpoint.setDensity((float) .02);
        endpoint.setBullet(true);
//endpoint.setGroupIndex(1);
        endpoint.setName("EndF");
//endpoint.setStatic(true);
        world.add(endpoint);
        for (int i = 1; i < steps.length; i++) {
            FDistanceJoint junta = new FDistanceJoint(endpoint, steps[i - 1]);
            junta.setAnchor1(boxWidth / 2, 0);
            junta.setAnchor2(-boxWidth / 2, 0);
            junta.setFrequency(frequency);
            junta.setDamping(damping);
            junta.setFill(0);
            junta.setDrawable(false);
            junta.calculateLength();
            world.add(junta);
        }
        FDistanceJoint junta = new FDistanceJoint(endpoint, steps[steps.length - 1]);
        junta.setAnchor1(boxWidth / 2, 0);
        junta.setAnchor2(-boxWidth / 2, 0);
        junta.setFrequency(frequency);
        junta.setDamping(damping);
        junta.setFill(0);
        junta.setStrokeWeight(5);
        junta.setDrawable(true);
        junta.calculateLength();
        world.add(junta);

        //tst.endShape();
        //tst = createShape();
        //tst.beginShape();
        if (poly != null) {
            world.add(poly);
            poly = null;
        }
    }

    private void addSpring() {
        FloatList sppointsx = new FloatList();
        FloatList sppointsy = new FloatList();
        for (int i = 0; i < listOfPoints.size(); i++) {
            PVector v = listOfPoints.get(i);
            float vx = (v.x);
            float vy = v.y;
            sppointsx.append(vx);
            sppointsy.append(vy);
        }

        float FLength = sppointsy.max() - sppointsy.min();
        float FWidth = sppointsx.max() - sppointsx.min();
        FBox hang = new FBox(FLength, FWidth);
        hang.setStatic(false);
        hang.setPosition(listOfPoints.get(0).x, listOfPoints.get(0).y + FLength / 2);
        hang.setDrawable(true);
        hang.setGroupIndex(1);
        hang.setDensity((float) .0005 * FWidth / FLength);
        spring = loadImage("zigzag.png");
        spring.resize(parseInt(FWidth), parseInt(FLength));
        hang.attachImage(spring);
        hang.setName("spring");
        world.add(hang);
        FCircle hanger = new FCircle(30);
        hanger.setStatic(true);
        hanger.setPosition(listOfPoints.get(0).x, listOfPoints.get(0).y - 5);
        hanger.setDrawable(true);
        hanger.setGroupIndex(1);
        hanger.setName("pin");
        hanger.setDensity((float) 0.001);
        hanger.setBullet(true);
        world.add(hanger);
//**************connecting the first part of spring to the *************
        FRevoluteJoint juntaPrincipio = new FRevoluteJoint(hanger, hang);

        juntaPrincipio.setAnchor(listOfPoints.get(0).x, listOfPoints.get(0).y);
        juntaPrincipio.setFill(0);
        juntaPrincipio.setDrawable(false);
        world.add(juntaPrincipio);
        int verc = listOfPoints.size();
        FCircle endpoint = new FCircle(30);
        endpoint.setPosition(hang.getX(), sppointsy.max());
        endpoint.setDrawable(true);
        endpoint.setFill(120, 30, 0);
        endpoint.setDensity((float) 0.05);
//endpoint.setGroupIndex(1);
        endpoint.setName("EndF");
//endpoint.setStatic(true);
        world.add(endpoint);
        //ava = endpoint;
        FRevoluteJoint jp = new FRevoluteJoint(endpoint, hang);

        jp.setAnchor(hang.getX(), sppointsy.max());
        jp.setFill(0);
        jp.setDrawable(false);
        ;
        world.add(jp);

        listOfPoints = new ArrayList<>();
    }

    /*private float pixelsPerMeter = 10000;

    public static void main(String[] args) {
        PApplet.main(new String[]{"recogwithhaptics.SpringRecogHaptic"});
    }


    *//* Device block definitions ********************************************************************************************//*
    Device haply_2DoF;
    byte deviceID = 5;
    Board haply_board;
    DeviceType degreesOfFreedom;
    boolean rendering_force = false;
    HVirtualCoupling s;

    *//* Simulation Speed Parameters ****************************************************************************************//*
    final long        SIMULATION_PERIOD          = 1; //ms
    final long        HOUR_IN_MILLIS             = 36000000;
    CountdownTimer    haptic_timer;
    float             dt                        = SIMULATION_PERIOD/1000.0f;


    *//* generic data for a 2DOF device *//*

    *//* joint space *//*
    PVector angles                    = new PVector(0, 0);
    PVector           torques                   = new PVector(0, 0);

    *//* task space *//*
    PVector           pos_ee                    = new PVector(0, 0);
    PVector           pos_ee_last               = new PVector(0, 0);
    PVector           f_ee                      = new PVector(0, 0);
    float             offsetX                   =0;
    float             offsetY                   =0;

    *//* where the edges are *//*

    float edgeTopLeftX = 0.0f;
    float edgeTopLeftY = 0.0f;
    float edgeBottomRightX =width;
    float edgeBottomRightY = height;


    @Override
    public void setup(){
        super.setup();
        s = new HVirtualCoupling((1));
        //***************************
        *//* Initialization of the Board, Device, and Device Components *//*

        *//* BOARD *//*

        haply_board = new Board(this, "COM3", 115200); //Put your COM# port here

        *//* DEVICE *//*
        haply_2DoF = new Device(HaplyTwoDOF, deviceID, haply_board);

        *//* Initialize graphical simulation components *//*

        *//* set device in middle of frame on the x-axis and in the fifth on the y-axis *//*
        //device_origin.add((width/2), (height/5) );

        *//* create pantograph graphics *//*
        //createpantograph();

        *//* haptics event timer, create and start a timer that has been configured to trigger onTickEvents *//*
        *//* every TICK (1ms or 1kHz) and run for HOUR_IN_MILLIS (1hr), then resetting *//*
        haptic_timer = CountdownTimerService.getNewCountdownTimer(this).configure(SIMULATION_PERIOD, HOUR_IN_MILLIS).start();

    }

    @Override
    public void draw() {
        background(255);
        if (!rendering_force) {
            world.step();
            world.draw(this);
        }
        *//*if (ava != null) {
            println("ava.getX() = ", ava.getX(), "ava.getY() = ", ava.getY());
        }*//*



    }

    public void detected(String gesture, float percent, int startX, int startY, int centroidX, int centroidY, int endX, int endY){
        //println("Gesture: "+gesture+", "+startX+"/"+startY+", "+centroidX+"/"+centroidY+", "+endX+"/"+endY);
    }



    *//**********************************************************************************************************************
     * Haptics simulation event, engages state of physical mechanism, calculates and updates physics simulation conditions
     **********************************************************************************************************************//*

    public void onTickEvent(CountdownTimer t, long timeLeftUntilFinish) {
        //println("ontick called");

        *//* check if new data is available from physical device *//*
        if (haply_board.data_available()) {

            //println("board data available");

                angles.set(haply_2DoF.get_device_angles()); //update device angles
                pos_ee.set(haply_2DoF.get_device_position(angles.array()));
                pos_ee.set(device2graphics(pos_ee));



            // TODO update the pen's virtual position to that of the body after this

            //current_charge.x_pos =  int((pos_ee.x)*pixelsPerMeter)+int(offset.x); //which coord system are these in?
            //current_charge.y_pos = int((pos_ee.y )*pixelsPerMeter)+int(offset.y);


            //println("pos_ee: ", pos_ee.x * pixelsPerMeter, ", ", pos_ee.y * pixelsPerMeter);

            //set_f_ee(); //set f_ee to what it should be
        }

        if (haply_2DoF != null) {
            //taus = haply_2DoF.mechanisms.get_torque(); //this is for the graphing of the torques

        }


        haply_2DoF.set_device_torques(f_ee.array());
        torques.set(haply_2DoF.mechanisms.get_torque());
        haply_2DoF.device_write_torques();
        println("pos_ee: ", pos_ee.x * pixelsPerMeter, ", ", pos_ee.y * pixelsPerMeter);

        //lastpos_ee = pos_ee; //if you ever need to keep track of the position.
    }

    *//* this is what the onTick function was before (down there) *//*

       *//* rendering_force = true; //flag for if you should be rendering force (this is used in draw so that you're not drawing and rendering force at the same time)

        if (ava!=null){

//  //   GET END-EFFECTOR STATE (TASK SPACE)
            if (haply_board.data_available()) {

                println("haply data available");

                angles.set(haply_2DoF.get_device_angles());
                pos_ee.set( haply_2DoF.get_device_position(angles.array()));
                //pos_ee.set(device2graphics(pos_ee));
                pos_ee.mult(500);
            }
            f_ee.set(-(ava.getX()+(pos_ee.x*20)+offsetX)*1000, +(ava.getY()-(pos_ee.y*20)+offsetY)*1000);

            f_ee.div(200);
            haply_2DoF.set_device_torques(f_ee.array());
            torques.set(haply_2DoF.mechanisms.get_torque());
            haply_2DoF.device_write_torques();
            ava.setPosition(-(pos_ee.x*20)-offsetX, (pos_ee.y*20)-offsetY);

        }

        //println("ava null");

        //world.step(1.0f/25.0f);
        rendering_force = false;

        //world.step(1.0f/1000.0f);

        s.updateCouplingForce();

        f_ee.set(-s.getVCforceX(), s.getVCforceY());

        f_ee.div(100000);
        haply_2DoF.set_device_torques(f_ee.array());
        torques.set(haply_2DoF.mechanisms.get_torque());
        haply_2DoF.device_write_torques();
        //angles.set(haply_2DoF.get_device_angles());
        pos_ee.set( haply_2DoF.get_device_position(angles.array()));
        pos_ee.mult(100);

        //println("pos_ee: ", pos_ee);*//*

    //}


    *//* Timer control event functions **************************************************************************************//*

    *//**
     * haptic timer reset
     *//*
    public void onFinishEvent(CountdownTimer t){
        println("Resetting timer...");
        haptic_timer.reset(CountdownTimer.StopBehavior.STOP_IMMEDIATELY);
        haptic_timer = CountdownTimerService.getNewCountdownTimer(this).configure(SIMULATION_PERIOD, HOUR_IN_MILLIS).start();
    }

    public PVector device2graphics(PVector deviceFrame){

        deviceFrame.set(-deviceFrame.x, deviceFrame.y);
        return new PVector(deviceFrame.x, deviceFrame.y);
    }

*/

}
