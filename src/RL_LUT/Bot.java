package RL_LUT;
import java.awt.*;
import java.awt.geom.*;
import java.io.*;
import robocode.*;


public class Bot extends AdvancedRobot{
    public static final double PI = Math.PI;
    private Enemy target;
    private static LUT table;
    private QLearning learner;

    //initializing robot battle parameters
    private double reward=0.0 ;
    private double accu_reward=0.0;
    private double firePower;
    private int isHitWall = 0;
    private int isHitByBullet = 0;


    private boolean policy =true;       //true for Off-Policy and false for On-Policy
    private double explorationRate = 0.0;   //probability of making random moves


    //============================================================
    //Starting the robot tank battle
    //============================================================
    public void run()
    {
        //Initializing the objects for implementing RL
        table = new LUT();
        loadData();
        learner = new QLearning(table);

        target = new Enemy();
        target.distance = 100000;


        //Initial Setting of Robot Parameters
        setColors(Color.red, Color.blue, Color.gray);
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);
        turnRadarRightRadians(2 * PI);

        //Into the Battle Loop
        while (true)
        {
            robotMovement();    //function for robot movement

            //setting firepower
            firePower = 400 / target.distance;
            if (firePower > 3)
            {
                firePower = 3;
            }
            radarMovement();    //function for radar movement
            gunMovement();      //function for gun movement
            if (getGunHeat() == 0)
            {
                setFire(firePower);     //setting firepower
            }
            execute();
        }
    }

    //------------------------------------------------
    //Functions for Robot Movement, Radar Movement, Gun Movement

    private void robotMovement()
    {

        int state = getState();

        //selecting the action according to the state and exploration
        int action=learner.selectAction(state, explorationRate);

        //learning according to the policy
        learner.Learn(state,action,reward,policy);
        accu_reward+=reward;
        reward = 0.0;
        isHitWall = 0;
        isHitByBullet = 0;

        //Action cases to be performed
        switch (action)
        {
            case RoboActions.RobotAhead:
                setAhead(RoboActions.RobotMoveDistance1);
                break;
            case RoboActions.RobotBack:
                setBack(RoboActions.RobotMoveDistance2);
                break;
            case RoboActions.RobotAheadTurnLeft:
                setAhead(RoboActions.RobotMoveDistance1);
                setTurnLeft(RoboActions.RobotTurnDegree);
                break;
            case RoboActions.RobotAheadTurnRight:
                setAhead(RoboActions.RobotMoveDistance1);
                setTurnRight(RoboActions.RobotTurnDegree);
                break;
            case RoboActions.RobotBackTurnLeft:
                setBack(RoboActions.RobotMoveDistance2);
                setTurnLeft(RoboActions.RobotTurnDegree);
                break;
            case RoboActions.RobotBackTurnRight:
                setBack(RoboActions.RobotMoveDistance2);
                setTurnRight(RoboActions.RobotTurnDegree);
                break;
        }
    }

    //function for movement of radar
    private void radarMovement()
    {
        setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
    }

    //function of movement of gun
    private void gunMovement()
    {


        long time;
        long nextTime;
        Point2D.Double p;
        p = new Point2D.Double(target.x, target.y);
        for (int i = 0; i < 20; i++)
        {
            nextTime = (int)Math.round((getrange(getX(),getY(),p.x,p.y)/(20-(3*firePower))));
            time = getTime() + nextTime - 10;
            p = target.guessPosition(time);
        }

        double gunOffset = getGunHeadingRadians() - (Math.PI/2 - Math.atan2(p.y - getY(),p.x -  getX()));

        setTurnGunLeftRadians(NormaliseBearing(gunOffset));


    }


    //function to retrieve the state of robot
    private int getState()
    {
        int heading = Robo_States.getHeading(getHeading());
        int targetDistance = Robo_States.getEnemyDistance(target.distance);
        int targetBearing = Robo_States.getEnemyBearing(target.bearing);

        int state = Robo_States.MapState[targetDistance][targetBearing][heading][isHitWall][isHitByBullet];

        return state;
    }

    //normalizing bearing between -pi to pi range
    double NormaliseBearing(double ang)
    {
        if (ang > PI)
            ang -= 2*PI;
        if (ang < -PI)
            ang += 2*PI;
        return ang;
    }

    //Distance between two points on the coordinate plane
    public double getrange(double x1, double y1, double x2, double y2)
    {
        double xo = x2 - x1;
        double yo = y2 - y1;
        double h = Math.sqrt(xo * xo + yo * yo);
        return h;
    }

    //Event Methods

    public void onBulletHit(BulletHitEvent e)
    {
        if (target.name == e.getName())
        {
            reward += 2;

        }
    }

    //When the bullet misses hit another robot
    public void onBulletMissed(BulletMissedEvent e)
    {
        reward += -1;

    }

    //When the robot is hit by enemy's bullet
    public void onHitByBullet(HitByBulletEvent e)
    {
        reward += -2;
        isHitByBullet = 1;
    }

    //When the robot hits enemy robot
    public void onHitRobot(HitRobotEvent e)
    {
        reward += -2;
    }

    //When the robot hits the wall
    public void onHitWall(HitWallEvent e)
    {
        reward += -1;
        isHitWall = 1;
    }


    //When robot scans the enemy robot
    public void onScannedRobot(ScannedRobotEvent e)
    {
        if ((e.getDistance() < target.distance)||(target.name == e.getName()))
        {
            //Gets the absolute bearing to the point of the robot
            double absbearing_rad = (getHeadingRadians() + e.getBearingRadians()) % (2 * PI);

            //Storing all the information about the target robot
            target.name = e.getName();
            double h = NormaliseBearing(e.getHeadingRadians() - target.heading);
            h = h / (getTime() - target.ctime);
            target.changeHeading = h;
            target.x = getX() + Math.sin(absbearing_rad) * e.getDistance();
            target.y = getY() + Math.cos(absbearing_rad) * e.getDistance();
            target.bearing = e.getBearingRadians();
            target.heading = e.getHeadingRadians();
            target.ctime = getTime(); //game time at which this scan was produced
            target.speed = e.getVelocity();
            target.distance = e.getDistance();
            target.energy = e.getEnergy();
        }
    }

    public void onRobotDeath(RobotDeathEvent e)
    {
        if (e.getName() == target.name)
            target.distance = 10000;
    }

    //When robot wins the battle
    public void onWin(WinEvent event)
    {
        reward += 10;
        saveData();

        //Saving Battle History
        int winningFlag=1;

        PrintStream w = null;
        try {
            w = new PrintStream(new RobocodeFileOutputStream(getDataFile("Q3_10.csv").getAbsolutePath(), true));
            w.println(reward+","+accu_reward+","+getRoundNum()+","+winningFlag);
            if (w.checkError())
                System.out.println("Could not save the data!");  //setTurnLeft(180 - (target.bearing + 90 - 30));
            w.close();
        }
        catch (IOException e) {
            System.out.println("IOException trying to write: " + e);
        }
        finally {
            try {
                if (w != null)
                    w.close();
            }
            catch (Exception e) {
                System.out.println("Exception trying to close writer: " + e);
            }
        }


    }

    //On losing the battle robot dies
    public void onDeath(DeathEvent event)
    {

        reward += -5;
        saveData();

        //Saving Battle History
        int losingFlag=0;
        PrintStream w = null;
        try {
            w = new PrintStream(new RobocodeFileOutputStream(getDataFile("Q3_10.csv").getAbsolutePath(), true));
            w.println(reward+","+accu_reward+","+getRoundNum()+","+losingFlag);
            if (w.checkError())
                System.out.println("Could not save the data!");
            w.close();
        }
        catch (IOException e) {
            System.out.println("IOException trying to write: " + e);
        }
        finally {
            try {
                if (w != null)
                    w.close();
            }
            catch (Exception e) {
                System.out.println("Exception trying to close writer: " + e);
            }
        }
    }


    //============================================================
    //Load and save the data for the LUT
    //------------------------------------------------------------
    public void loadData()
    {
        try
        {
            table.loadData(getDataFile("LUT.dat"));
        }
        catch (Exception e)
        {
        }
    }

    public void saveData()
    {
        try
        {
            table.saveData(getDataFile("LUT.dat"));
        }
        catch (Exception e)
        {
            out.println("Exception trying to write: " + e);
        }
    }
}
