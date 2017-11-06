package engine;

import java.awt.Event;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

public class Window extends JFrame implements ActionListener, KeyListener{
	static final int WIDTH = 450;
	static final int HEIGHT = 200;
	
	private boolean upKey, downKey, rightKey, leftKey;
	private Map map;
	private Player player;
	private RayCaster screen;
	
	private JLabel screenImage;
	private Image image;
	private boolean keyPressed;
	private int key;
	
	public Window(){
		setLayout(new FlowLayout());
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		
		map = new Map();
		player = new Player();
		screen = new RayCaster(map, player);
		screenImage = new JLabel();
		update();
		
		addKeyListener(this);
		add(screenImage);	
		
		pack();
		setVisible(true);
		
		gameLoop();
	}

	public void gameLoop(){
		long totalTime = System.currentTimeMillis();
		int fps = 20;
		while (true) {
			long currentTime = System.currentTimeMillis();
			long timePassed = currentTime - totalTime;
			if(timePassed >= 100/fps){
				update();
				totalTime += timePassed;
			}
		}
	}
	public void actionPerformed(ActionEvent e) {
		update();
	}
	
	@Override
	public void keyPressed(KeyEvent e) {
		System.out.println("Bo");
		key = e.getKeyCode();
		keyPressed = true;
	
	}

	@Override
	public void keyReleased(KeyEvent e) {
		key = e.getKeyCode();
		keyPressed = false;	
	}

	@Override
	public void keyTyped(KeyEvent e) {
		// TODO Auto-generated method stub

	}

	
	
	public void update(){
		if(keyPressed)	{
			System.out.println(key);
			if (key == 37)
			{
				System.out.println("LEFT");
				if ((player.arc-=screen.ANGLE5/10)<0)
					player.arc+=screen.ANGLE360;
			}
			// rotate right
			else if (key == 39)
			{
				System.out.println("RIGHT");

				if ((player.arc+=screen.ANGLE5/10)>=screen.ANGLE360)
					player.arc-=screen.ANGLE360;
			}

			float playerXDir=screen.cosTable[player.arc];
			float playerYDir=screen.sinTable[player.arc];

			// move forward
			if (key == 38)
			{
				int tx = player.x+(int)(playerXDir*player.speed);
				int ty = player.y+(int)(playerYDir*player.speed);
				int tmx = (int)((float)tx/(float)screen.TILE_SIZE);
				int tmy = (int)((float)ty/(float)screen.TILE_SIZE);
				System.out.println(tmx + ", " + tmy + ".");
				if(map.getMap()[tmy][tmx] == 0){
					player.x += (int)(playerXDir*player.speed);
					player.y += (int)(playerYDir*player.speed);
				}
				
			}
			// move backward
			else if (key == 40)
			{
				int tx = player.x-(int)(playerXDir*player.speed);
				int ty = player.y-(int)(playerYDir*player.speed);
				int tmx = (int)((float)tx/(float)screen.TILE_SIZE);
				int tmy = (int)((float)ty/(float)screen.TILE_SIZE);
				if(map.getMap()[tmy][tmx] == 0){
					player.x-=(int)(playerXDir*player.speed);
					player.y-=(int)(playerYDir*player.speed);
				}
			}
		}
		image = screen.update();
		screenImage.setIcon(new ImageIcon(image));
		
		repaint();
	}
}
