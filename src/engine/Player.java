package engine;

public class Player {
	
	public int x = 100, y = 160, arc = 0, dist = 277, yCenter = 160, speed = 3;

	public Player(){
	}

	public int getYCenter() {
		return yCenter;
	}

	public void setYCenter(int yCenter) {
		this.yCenter = yCenter;
	}

	public int getX(){
		return x;
	}

	public int getY(){
		return y;
	}

	public int getDist(){
		return dist;
	}
	
	public int getArc() {
		return arc;
	}

	public void setArc(int arc) {
		this.arc = arc;
	}

	public int getyCenter() {
		return yCenter;
	}

	public void setyCenter(int yCenter) {
		this.yCenter = yCenter;
	}

	public int getSpeed() {
		return speed;
	}

	public void setSpeed(int speed) {
		this.speed = speed;
	}

	public void setX(int x) {
		this.x = x;
	}

	public void setY(int y) {
		this.y = y;
	}

	public void setDist(int dist) {
		this.dist = dist;
	}

}
