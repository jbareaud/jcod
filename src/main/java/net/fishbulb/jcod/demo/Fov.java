package net.fishbulb.jcod.demo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;

import net.fishbulb.jcod.Console;
import net.fishbulb.jcod.fov.BasicRadiusStrategy;
import net.fishbulb.jcod.fov.FOVSolver;
import net.fishbulb.jcod.fov.RadiusStrategy;
import net.fishbulb.jcod.fov.RayCastingFOV;
import net.fishbulb.jcod.fov.RippleFOV;
import net.fishbulb.jcod.fov.ShadowFOV;
import net.fishbulb.jcod.fov.SpreadFOV;
import net.fishbulb.jcod.fov.TranslucenceWrapperFOV;
import net.fishbulb.jcod.util.BlendMode;
import net.fishbulb.jcod.util.CharCodes;
import net.fishbulb.jcod.util.extra.ExtraUtils;

public class Fov extends DemoApplet {
    String[] map = new String[]{
            "##############################################",
            "#######################      #################",
            "#####################    #     ###############",
            "######################  ###        ###########",
            "##################      #####             ####",
            "################       ########    ###### ####",
            "###############      #################### ####",
            "################    ######                  ##",
            "########   #######  ######   #     #     #  ##",
            "########   ######      ###                  ##",
            "########                                    ##",
            "####       ######      ###   #     #     #  ##",
            "#### ###   ########## ####                  ##",
            "#### ###   ##########   ###########=##########",
            "#### ##################   #####          #####",
            "#### ###             #### #####          #####",
            "####           #     ####                #####",
            "########       #     #### #####          #####",
            "########       #####      ####################",
            "##############################################",
    };

  	float[][] lightMap = new float[width][height];
	float [][] resistanceMap = new float[width][height];
    
    int torchRadius = 10;
    boolean flicker = false;
    boolean recompute = true;
    boolean lightWalls = true;

    Color darkWall = new Color(0, 0, 0.35f, 1f);
    Color lightWall = new Color(0.5f, 0.4f, 0.2f, 1f);
    Color darkGround = new Color(0.2f, 0.2f, 0.6f, 1);
    Color lightGround = new Color(0.8f, 0.7f, 0.2f, 1f);

    int px = 20;
    int py = 10;

    private long lastUpdate;

    int radius = 10;

    FOVSolver activeAlgo;
    
    List<FOVSolver> allAlgos = new ArrayList<FOVSolver>() {{
        add(new RayCastingFOV());
        add(new ShadowFOV());
        add(new RippleFOV());
        add(new SpreadFOV());
        add(new TranslucenceWrapperFOV());
    }};
    
    Iterator<FOVSolver> algos = ExtraUtils.cycle(allAlgos);
    
    RadiusStrategy activeRadiusStrategy;
    
    List<RadiusStrategy> allRadiuses = new ArrayList<RadiusStrategy>() {{
        add(BasicRadiusStrategy.CIRCLE);
        add(BasicRadiusStrategy.DIAMOND);
        add(BasicRadiusStrategy.SQUARE);
    }};
    
    Iterator<RadiusStrategy> radiuses = ExtraUtils.cycle(allRadiuses);
    
    public Fov(Console parent) {
        super(parent);
        initFov();
        cycleAlgos();
        cycleRadiuses();
    }

    private void initFov() {
    	for (int y = 0; y < height; y++) {
    		for (int x = 0; x < width; x++) {
    			char c = map[y].charAt(x);
	            switch (c) {
	                    case '=':
	                    case '#':
	                    	resistanceMap[x][y] = 1; // opaque
	                        break;
	                    case ' ':
	                    default:
	                    	resistanceMap[x][y] = 0; // see through
	            }
    		}
    	}
    }
    
    private void cycleAlgos() {
    	activeAlgo = algos.next();
    }

    private void cycleRadiuses() {
    	activeRadiusStrategy = radiuses.next();
    }
    
    @Override public void update() {
        long now = System.currentTimeMillis();
        long updateMillis = 30;
        if ((now - lastUpdate) < updateMillis) return;
        lastUpdate = now;

        int coffy = 0; 
        console.clear();
        console.setDefaultForeground(Color.WHITE);
        console.print(1, coffy++, "WASD: Move around");
        console.print(1, coffy++, "L: Light Walls");
        console.print(1, coffy++, "T: Torch (not yet)");
        console.print(1, coffy++, "X: Algorithm");
        console.print(1, coffy++, "+/-: radius");
        console.print(1, coffy++, activeAlgo.getClass().getSimpleName());

        console.setDefaultForeground(Color.BLACK);

        console.putChar(px, py, '@', BlendMode.None);
        computeFov(px, py, 0, lightWalls);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                char c = map[y].charAt(x);
                boolean lit = isFov(x, y);
                switch (c) {
                    case '=':
                        console.putChar(x, y, CharCodes.OEM.DHLINE, BlendMode.None);
                    case ' ':
                        console.setCharBackground(x, y, lit ? lightGround : darkGround);
                        break;
                    case '#':
                        console.setCharBackground(x, y, lit ? lightWall : darkWall);
                        break;
                }
            }
        }
    }

    private void moveTo(int x, int y) {
        if (map[y].charAt(x) == ' ') {
            console.putChar(px, py, ' ', BlendMode.None);
            px = x;
            py = y;
            console.putChar(px, py, '@', BlendMode.None);
        }
    }

    @Override public boolean keyDown(int keyCode) {
        switch (keyCode) {
            case Input.Keys.Z:
                moveTo(px, py - 1);
                return true;
            case Input.Keys.Q:
                moveTo(px - 1, py);
                return true;
            case Input.Keys.S:
                moveTo(px, py + 1);
                return true;
            case Input.Keys.D:
                moveTo(px + 1, py);
                return true;
            case Input.Keys.T:
                // TODO Torch effect
                return true;
            case Input.Keys.L:
                lightWalls = !lightWalls;
                return true;
            case Input.Keys.X:
            	cycleAlgos();
                return true;                
            case Input.Keys.MINUS:
            	radius = Math.max(1, radius-1);
            	return true;
            case Input.Keys.PLUS:
            	radius = Math.min(20, radius+1);
            	return true;
            case Input.Keys.R:
            	cycleRadiuses();
            	return true;
        }
        return false;
    }
	  
    private void computeFov(int px, int py, int i, boolean lightWalls) {
		float force = 1f;
		float decay = 1f / radius;
		lightMap = activeAlgo.calculateFOV(resistanceMap, px, py, force, decay, activeRadiusStrategy);
	}
	
    private boolean isFov(int x, int y) {
		return lightMap[x][y] > 0;
	}
}
