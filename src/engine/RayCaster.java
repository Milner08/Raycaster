package engine;

import java.awt.Color;
import java.awt.Event;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;

public class RayCaster {

	// size of tile (wall height)
	static final int TILE_SIZE = 64;
	static final int WALL_HEIGHT = 64;
	static final int WIDTH = 640;
	static final int HEIGHT = 400;
	static final int ANGLE60 = WIDTH;
	static final int ANGLE30 = (ANGLE60/2);
	static final int ANGLE15 = (ANGLE30/2);
	static final int ANGLE90 = (ANGLE30*3);
	static final int ANGLE180 = (ANGLE90*2);
	static final int ANGLE270 = (ANGLE90*3);
	static final int ANGLE360 = (ANGLE60*6);
	static final int ANGLE0 = 0;
	static final int ANGLE5 = (ANGLE30/6);
	static final int ANGLE10 = (ANGLE5*2);

	// trigonometric tables
	float sinTable[];
	float iSinTable[];
	float cosTable[];
	float iCosTable[];
	float tanTable[];
	float iTanTable[];
	float fishTable[];
	float xStepTable[];
	float yStepTable[];

	// offscreen buffer
	Image offscreenImage;
	Graphics offscreenGraphics;
	
	Map map;
	Player player;
	private int PlayerMapX;
	private int PlayerMapY;
	private int MinimapWidth;
	
	public RayCaster(Map m, Player p){
		map = m;
		player = p;
		
		offscreenImage = new BufferedImage(WIDTH+160, HEIGHT, BufferedImage.TYPE_INT_RGB);
		offscreenGraphics = offscreenImage.getGraphics();
		createTables();
		render();
	}

	//*******************************************************************//
	//* Convert arc to radian
	//*******************************************************************//
	float arcToRad(float arcAngle)
	{
		return ((float)(arcAngle*Math.PI)/(float)ANGLE180);    
	}

	//*******************************************************************//
	//* Create tigonometric values to make the program runs faster.
	//*******************************************************************//
	public void createTables()
	{
		int i;
		float radian;
		sinTable = new float[ANGLE360+1];
		iSinTable = new float[ANGLE360+1];
		cosTable = new float[ANGLE360+1];
		iCosTable = new float[ANGLE360+1];
		tanTable = new float[ANGLE360+1];
		iTanTable = new float[ANGLE360+1];
		fishTable = new float[ANGLE60+1];
		xStepTable = new float[ANGLE360+1];
		yStepTable = new float[ANGLE360+1];

		for (i=0; i<=ANGLE360;i++)
		{
			// get the radian value (the last addition is to avoid division by 0, try removing
			// that and you'll see a hole in the wall when a ray is at 0, 90, 180, or 270 degree)
			radian = arcToRad(i) + (float)(0.0001);
			sinTable[i]=(float)Math.sin(radian);
			iSinTable[i]=(1.0F/(sinTable[i]));
			cosTable[i]=(float)Math.cos(radian);
			iCosTable[i]=(1.0F/(cosTable[i]));
			tanTable[i]=(float)Math.tan(radian);
			iTanTable[i]=(1.0F/tanTable[i]);

			//  you can see that the distance between xi is the same
			//  if we know the angle
			//  _____|_/next xi______________
			//       |
			//  ____/|next xi_________   slope = tan = height / dist between xi's
			//     / |
			//  __/__|_________  dist between xi = height/tan where height=tile size
			// old xi|
			//                  distance between xi = x_step[view_angle];
			//
			//
			// facine left
			// facing left
			if (i>=ANGLE90 && i<ANGLE270)
			{
				xStepTable[i] = (float)(TILE_SIZE/tanTable[i]);
				if (xStepTable[i]>0)
					xStepTable[i]=-xStepTable[i];
			}
			// facing right
			else
			{
				xStepTable[i] = (float)(TILE_SIZE/tanTable[i]);
				if (xStepTable[i]<0)
					xStepTable[i]=-xStepTable[i];
			}

			// FACING DOWN
			if (i>=ANGLE0 && i<ANGLE180)
			{
				yStepTable[i] = (float)(TILE_SIZE*tanTable[i]);
				if (yStepTable[i]<0)
					yStepTable[i]=-yStepTable[i];
			}
			// FACING UP
			else
			{
				yStepTable[i] = (float)(TILE_SIZE*tanTable[i]);
				if (yStepTable[i]>0)
					yStepTable[i]=-yStepTable[i];
			}
		}

		for (i=-ANGLE30; i<=ANGLE30; i++)
		{
			radian = arcToRad(i);
			// we don't have negative angle, so make it start at 0
			// this will give range 0 to 320
			fishTable[i+ANGLE30] = (float)(1.0F/Math.cos(radian));
		}
	}

	//*******************************************************************//
	//* Draw background image
	//*******************************************************************//
	public void drawBackground()
	{
		// sky
		int c=25;
		int r;
		for (r=0; r<(HEIGHT/5)*3; r+=10)
		{
			if(c > 255) c = 255;
			offscreenGraphics.setColor(new Color(c, 125, 225));
			offscreenGraphics.fillRect(0, r, WIDTH, 10);
			c+=5;
		}
		// ground
		c=22;
		for (; r<HEIGHT; r+=5)
		{
			if(c > 255) c = 255;
			offscreenGraphics.setColor(new Color(20, c, 20));
			offscreenGraphics.fillRect(0, r, WIDTH, 15);
			c+=10;
		}
	}

	//*******************************************************************//
	//* Draw map on the right side
	//*******************************************************************//
	public void drawOverheadMap()
	{
		MinimapWidth=5;
		for (int u=0; u<map.getWidth(); u++)
		{
			for (int v=0; v<map.getHeight(); v++)
			{
				if (map.getMap()[v][u] >= 1)
				{
					offscreenGraphics.setColor(Color.gray);
				}
				else
				{
					offscreenGraphics.setColor(Color.black);
				}
				offscreenGraphics.fillRect(WIDTH+(u*MinimapWidth),
						(v*MinimapWidth), MinimapWidth, MinimapWidth);
			}
		}
		PlayerMapX=WIDTH+(int)(((float)player.getX()/(float)TILE_SIZE) * MinimapWidth);
		PlayerMapY=(int)(((float)player.getY()/(float)TILE_SIZE) * MinimapWidth);
	}

	//*******************************************************************//
	//* Draw ray on the overhead map (for illustartion purpose)
	//* This is not part of the ray-casting process
	//*******************************************************************//
	public void drawRayOnOverheadMap(float x, float y)
	{
		offscreenGraphics.setColor(Color.yellow);
		// draw line from the player position to the position where the ray
		// intersect with wall
		offscreenGraphics.drawLine(PlayerMapX, PlayerMapY, 
				(int)(WIDTH+((float)(x*MinimapWidth)/(float)TILE_SIZE)),
				(int)(((float)(y*MinimapWidth)/(float)TILE_SIZE)));
		// draw a red line indication the player's direction
		offscreenGraphics.setColor(Color.red);
		offscreenGraphics.drawLine(PlayerMapX, PlayerMapY, 
				(int)(PlayerMapX+cosTable[player.getArc()]*10),
				(int)(PlayerMapY+sinTable[player.getArc()]*10));
	}

	//*******************************************************************//
	//* Renderer
	//*******************************************************************//
	public Image render()
	{
		drawBackground();
		drawOverheadMap();

		int verticalGrid;        // horizotal or vertical coordinate of intersection
		int horizontalGrid;      // theoritically, this will be multiple of TILE_SIZE
		// , but some trick did here might cause
		// the values off by 1
		int distToNextVerticalGrid; // how far to the next bound (this is multiple of
		int distToNextHorizontalGrid; // tile size)
		
		float xIntersection;  // x and y intersections
		float yIntersection;
		
		float distToNextXIntersection;
		float distToNextYIntersection;

		int xGridIndex = 0;        // the current cell that the ray is in
		int yGridIndex = 0;

		int horXGridIn = 0;
		int horYGridIn = 0;
		
		float distToVerticalGridBeingHit;      // the distance of the x and y ray intersections from
		float distToHorizontalGridBeingHit;      // the viewpoint

		int castArc, castColumn;

		castArc = player.getArc();
		
		// field of view is 60 degree with the point of view (player's direction in the middle)
		// 30  30
		//    ^
		//  \ | /
		//   \|/
		//    v
		// we will trace the rays starting from the leftmost ray
		
		castArc-=ANGLE30;
		
		// wrap around if necessary
		if (castArc < 0)
		{
			castArc=ANGLE360 + castArc;
		}

		for (castColumn=0; castColumn<WIDTH; castColumn+=5)
		{
			// ray is between 0 to 180 degree (1st and 2nd quadrant)
			// ray is facing down
			if (castArc > ANGLE0 && castArc < ANGLE180)
			{
				horizontalGrid = (player.getY()/TILE_SIZE)*TILE_SIZE  + TILE_SIZE;
				distToNextHorizontalGrid = TILE_SIZE;

				float xtemp = iTanTable[castArc]*(horizontalGrid-player.getY());
				xIntersection = xtemp + player.getX();
			}
			// else, the ray is facing up
			else
			{
				horizontalGrid = (player.getY()/TILE_SIZE)*TILE_SIZE;
				distToNextHorizontalGrid = -TILE_SIZE;

				float xtemp = iTanTable[castArc]*(horizontalGrid - player.getY());
				xIntersection = xtemp + player.getX();

				horizontalGrid--;
			}
			// LOOK FOR HORIZONTAL WALL
			if (castArc==ANGLE0 || castArc==ANGLE180)
			{
				distToHorizontalGridBeingHit=9999999F;//Float.MAX_VALUE;
			}
			// else, move the ray until it hits a horizontal wall
			else
			{
				distToNextXIntersection = xStepTable[castArc];
				while (true)
				{
					xGridIndex = (int)(xIntersection/TILE_SIZE);
					// in the picture, yGridIndex will be 1
					yGridIndex = (horizontalGrid/TILE_SIZE);

					if ((xGridIndex>=map.getWidth()) ||
							(yGridIndex>=map.getHeight()) ||
							xGridIndex<0 || yGridIndex<0){
						horXGridIn = xGridIndex;
						horYGridIn = yGridIndex;
						distToHorizontalGridBeingHit = Float.MAX_VALUE;
						break;
					}
					else if ((map.getMap()[yGridIndex][xGridIndex]) > 0 && (map.getMap()[yGridIndex][xGridIndex]) < 3)
					{
						horXGridIn = xGridIndex;
						horYGridIn = yGridIndex;
						distToHorizontalGridBeingHit  = (xIntersection-player.getX())*iCosTable[castArc];
						break;
					}
					else if ((map.getMap()[yGridIndex][xGridIndex]) > 3 && (map.getMap()[yGridIndex][xGridIndex]) < 4)
					{
						horXGridIn = xGridIndex;
						horYGridIn = yGridIndex;
						distToHorizontalGridBeingHit  = (xIntersection-player.getX())*iCosTable[castArc];
						break;
					}
					// else, the ray is not blocked, extend to the next block
					else
					{
						xIntersection += distToNextXIntersection;
						horizontalGrid += distToNextHorizontalGrid;
					}
				}
			}


			// FOLLOW X RAY
			if (castArc < ANGLE90 || castArc > ANGLE270)
			{
				verticalGrid = TILE_SIZE + (player.getX()/TILE_SIZE)*TILE_SIZE;
				distToNextVerticalGrid = TILE_SIZE;

				float ytemp = tanTable[castArc]*(verticalGrid - player.getX());
				yIntersection = ytemp + player.getY();
			}
			// RAY FACING LEFT
			else
			{
				verticalGrid = (player.getX()/TILE_SIZE)*TILE_SIZE;
				distToNextVerticalGrid = -TILE_SIZE;

				float ytemp = tanTable[castArc]*(verticalGrid - player.getX());
				yIntersection = ytemp + player.getY();

				verticalGrid--;
			}
			// LOOK FOR VERTICAL WALL
			if (castArc==ANGLE90||castArc==ANGLE270)
			{
				distToVerticalGridBeingHit = 9999999;//Float.MAX_VALUE;
			}
			else
			{
				distToNextYIntersection = yStepTable[castArc];
				while (true)
				{
					// compute current map position to inspect
					xGridIndex = (verticalGrid/TILE_SIZE);
					yGridIndex = (int)(yIntersection/TILE_SIZE);

					if ((xGridIndex>=map.getWidth()) ||
							(yGridIndex>=map.getHeight()) ||
							xGridIndex<0 || yGridIndex<0){
						
						distToVerticalGridBeingHit = Float.MAX_VALUE;
						break;
					}
					else if ((map.getMap()[yGridIndex][xGridIndex]) > 0 && (map.getMap()[yGridIndex][xGridIndex]) < 3)
					{
						distToVerticalGridBeingHit =(yIntersection-player.getY())*iSinTable[castArc];
						break;
					}
					else
					{
						yIntersection += distToNextYIntersection;
						verticalGrid += distToNextVerticalGrid;
					}
				}
			}

			// DRAW THE WALL SLICE
			float scaleFactor;
			boolean sprite = false;
			float dist;
			int topOfWall;   // used to compute the top and bottom of the sliver that
			int bottomOfWall;   // will be the staring point of floor and ceiling
			// determine which ray strikes a closer wall.
			// if yray distance to the wall is closer, the yDistance will be shorter than
			// the xDistance
			
			if (distToHorizontalGridBeingHit < distToVerticalGridBeingHit)
			{
				if (horXGridIn>=map.getWidth()){
					horXGridIn = map.getWidth()-1;
				}else if (horXGridIn<0){
					horXGridIn = 0;
				}
				if (horYGridIn>=map.getHeight()){
					horYGridIn = map.getHeight()-1;
				}else if (horYGridIn<0){
					horYGridIn = 0;
				}
				// the next function call (drawRayOnMap()) is not a part of raycating rendering part, 
				// it just draws the ray on the overhead map to illustrate the raycasting process
				drawRayOnOverheadMap(xIntersection, horizontalGrid);
				dist=distToHorizontalGridBeingHit;
				if((map.getMap()[horYGridIn][horXGridIn]) == 1) offscreenGraphics.setColor(Color.gray);
				else if((map.getMap()[horYGridIn][horXGridIn]) == 2) offscreenGraphics.setColor(Color.red);
				else if((map.getMap()[horYGridIn][horXGridIn]) == 3) offscreenGraphics.setColor(Color.yellow);
			}
			// else, we use xray instead (meaning the vertical wall is closer than
			//   the horizontal wall)
			else
			{
				if (xGridIndex>=map.getWidth()){
					xGridIndex = map.getWidth()-1;
				}else if (xGridIndex<0){
					xGridIndex = 0;
				}
				if (yGridIndex>=map.getHeight()){
					yGridIndex = map.getHeight()-1;
				}else if (yGridIndex<0){
					yGridIndex = 0;
				}
				// the next function call (drawRayOnMap()) is not a part of raycating rendering part, 
				// it just draws the ray on the overhead map to illustrate the raycasting process
				drawRayOnOverheadMap(verticalGrid, yIntersection);
				dist=distToVerticalGridBeingHit;
				if((map.getMap()[yGridIndex][xGridIndex]) == 1) offscreenGraphics.setColor(Color.darkGray);
				else if((map.getMap()[yGridIndex][xGridIndex]) == 2) offscreenGraphics.setColor(Color.red.darker());
				else if((map.getMap()[yGridIndex][xGridIndex]) == 3) {
					sprite = true;
					offscreenGraphics.setColor(Color.yellow);
				}
			}

			// correct distance (compensate for the fishbown effect)
			dist /= fishTable[castColumn];

			if(!sprite){
				int projectedWallHeight=(int)(WALL_HEIGHT*(float)player.getDist()/dist);
			
				bottomOfWall = player.getYCenter()+(int)(projectedWallHeight*0.5F);
				topOfWall = HEIGHT-bottomOfWall;
			
				if (bottomOfWall>=HEIGHT)
					bottomOfWall=HEIGHT-1;
				
				offscreenGraphics.fillRect(castColumn, topOfWall, 5, projectedWallHeight);
			}else{
				int projectedHeight=(int)(((WALL_HEIGHT/2)*1)*(float)player.getDist()/dist);
				
				bottomOfWall = player.getYCenter()+(int)(projectedHeight*0.5F);
				topOfWall = HEIGHT-bottomOfWall;
			
				if (bottomOfWall>=HEIGHT)
					bottomOfWall=HEIGHT-1;
				
				offscreenGraphics.fillRect(castColumn, topOfWall, 5, projectedHeight);
			}
			// TRACE THE NEXT RAY
			castArc+=5;
			if (castArc>=ANGLE360)
				castArc-=ANGLE360;
		}
		return offscreenImage;
	}
	
	public Image update(){
		return render();
	}

}
