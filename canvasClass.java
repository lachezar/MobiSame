
import javax.microedition.lcdui.game.GameCanvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import java.io.IOException;
import java.util.Date;
import java.util.Random;
import java.util.Vector;

import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import javax.microedition.rms.RecordStoreFullException;
import javax.microedition.rms.RecordStoreNotFoundException;
import javax.microedition.rms.RecordStoreNotOpenException;

public class canvasClass extends GameCanvas implements Runnable {

    public canvasClass() {
        super(false);
    }

    private Image charMap;
    private Graphics g;
    public final Command ExitCommand = new Command("Exit", Command.ITEM, 0);
    public final Command MenuCommand = new Command("Menu", Command.EXIT, 0);
    public final Command UndoCommand = new Command("Undo", Command.ITEM, 0);
    public final Command Back2GameCommand = new Command("Back", Command.EXIT, 0);
    public final Command Back2MenuCommand = new Command("Back", Command.EXIT, 0);
    public final Command DeleteChar = new Command("Delete", Command.EXIT, 0);
    public final Command Ok = new Command("Ok", Command.ITEM, 0);
    private mainClass midlet;
    public boolean pause = false;
    private int chrW = 8;
    private int chrH = 15;
    private Image splash;
    private int splashCntr = 0;
    private int areaWidth, areaHeight, squareWidth;
    private int[][] areaState;
    private int[][][] areaStateHistory;
    private int[] scoreHistory;
    private int undoCntr = 0;
    private int cursorX, cursorY;
    private int originalColor, colCntr;
    private int gameState = 0;
    private Command previousCommand1, previousCommand2;  
    private Command command1, command2;
    private int lastPressedKey = 0;
    private long kTime;
    private int score, nextPts;
    private int selectedOption = 0;
    private String[] highscores; 
    private String name = "";
    private int[] charKeysIndex = new int[2];
    private int cursorBlinker = 0;
    private String helpText = "The main goal of the game is to remove as many blocks on the screen as you can. You can terminate only groups of 2 or more blocks which share one side. If you clean the whole screen you will get bonus points. @Have a nice play...";
    private Vector helpRows;
    private int scrollPos;
    
    private int[] colors = {
    		0xFF0000, 0xFFFF00, 0x00FF00, 0x0000FF, 0xFF00FF, 0x000000    		
    };

    private void initCharMap() {
        if (charMap == null) {
            try {
                charMap = Image.createImage("/res/charMap.gif");
            } catch(IOException e) {
                System.out.println("Can't load charmap.gif");
            }
        }
    }

    private void drawString(String str, int x, int y, int anchor) throws RuntimeException {
        
        if ((Graphics.HCENTER & anchor) != 0) {
            x -= (str.length()*chrW)/2;
        } else if ((Graphics.RIGHT & anchor) != 0) {
            x -= str.length()*chrW;
        }  
        
        for (int i = 0; i < str.length(); i++) {
            g.drawRegion(charMap, chrW*((str.charAt(i)-32)%16), chrH*((str.charAt(i)-32)/16), chrW, chrH, 0, x+i*chrW, y, Graphics.TOP | Graphics.LEFT);
        }

    }

    private void loadRes() throws RuntimeException {
    	try {
            splash = Image.createImage("/res/mobisame.gif");
        } catch(IOException e) {
            System.out.println("Can't load mobisame.gif");
        }
    }
    
    private void splashScreen() {
    	if (splashCntr == 0) {
    		Random rand = new Random();
    		for (int i = 0; i <= getWidth(); i+=getWidth()/3) {
    			for (int j = getHeight()-getWidth()/3; j >= -getWidth()/3; j-=getWidth()/3) {
    				g.setColor(colors[Math.abs(rand.nextInt()%5)]);
    		    	g.fillRect(i, j, getWidth()/3, getWidth()/3);    				
    			}
    		}
	    	g.drawImage(splash, getWidth()/2, getHeight()/2-2*chrH, Graphics.VCENTER | Graphics.HCENTER);
	    	drawString("By Lucho Yankov", getWidth()/2, getHeight()-chrH-2, Graphics.TOP | Graphics.HCENTER);
	    	splashCntr++;
    	} else if (splashCntr < 50) {
    		splashCntr++;   		
    	} else {
    		newGame();
    	}
    }
    
    public void menu() {
    	gameState = 2;
    	addCmd(ExitCommand, Back2GameCommand);
    	selectedOption = 0;
    }
    
    private void help() {
    	helpRows = new Vector();
    	int charsInRow = (getWidth()-8)/chrW;
    	int whiteSpacePos = 0;
    	int lastPos = 0;
    	for (int i = 0; i < helpText.length(); i++) {
    		if (i > lastPos+charsInRow) {
    			helpRows.addElement(helpText.substring(lastPos, whiteSpacePos));
    			lastPos = whiteSpacePos+1;
    		} else if (i <= lastPos+charsInRow && (helpText.charAt(i) == ' ' || helpText.charAt(i) == '@')) {
    			whiteSpacePos = i;
    			if (helpText.charAt(i) == '@') {
    				helpRows.addElement(helpText.substring(lastPos, whiteSpacePos));
    				helpRows.addElement("");
    				lastPos = whiteSpacePos+1;
    			}
    		}
    	}
    	helpRows.addElement(helpText.substring(lastPos, helpText.length()));
    	
    	scrollPos = 0;    	
    	gameState = 6;
    	addCmd(Back2MenuCommand, null);
    }
    
    public void back2game() {
    	gameState = 1;
    	addCmd(MenuCommand, UndoCommand);
    }
    
    public void back2menu() {
    	gameState = 2;
    	addCmd(Back2GameCommand, ExitCommand);
    }
    
    private void checkHighScores() throws RecordStoreFullException, RecordStoreNotFoundException, RecordStoreException {
    	RecordStore rs = null;
    	rs = RecordStore.openRecordStore("highscores", true);

    	if (rs.getNextRecordID() == 1) {
    		String name = ".....";
    		String score = "0";
    		for (int i = 0; i < 5; i++) {
    			rs.addRecord(name.getBytes(), 0, name.getBytes().length);
    			rs.addRecord(score.getBytes(), 0, score.getBytes().length);
    		}
    	}
    	
    	rs.closeRecordStore();    	
    }
    
    private void addHighScore(String name, int highscore) throws RecordStoreNotOpenException, RecordStoreException {
    	if (name == "") name = " ";
    	RecordStore rs = null;
    	rs = RecordStore.openRecordStore("highscores", true);

    	checkHighScores();
    	
    	highscores = new String[10];
    	int pos = 123;
    	
    	for (int i = 0; i < 10; i+=2) {
    		highscores[i] = new String(rs.getRecord(i+1), 0, rs.getRecord(i+1).length);
    		highscores[i+1] = new String(rs.getRecord(i+2), 0, rs.getRecord(i+2).length);
    		if (highscore >= Integer.parseInt(highscores[i+1]) && pos == 123) {
    			pos = i/2;
    		}
    	}
    	
    	if (pos < 5) {
    		for (int i = 4; i > pos; i--) {
    			highscores[2*i] = highscores[2*(i-1)];
    			highscores[2*i+1] = highscores[2*(i-1)+1];
    		}
    		highscores[2*pos] = name;
    		highscores[2*pos+1] = Integer.toString(highscore);
    	}
    	
    	for (int i = 0; i < 10; i++) {
    		rs.setRecord(i+1, highscores[i].getBytes(), 0, highscores[i].getBytes().length);
    	}

    	rs.closeRecordStore();  	
    	
    }
    
    private boolean newHighScore(int highscore) throws RecordStoreFullException, RecordStoreNotFoundException, RecordStoreException {
    	boolean flag = false;
    	RecordStore rs = null;
    	rs = RecordStore.openRecordStore("highscores", true);
    	
    	checkHighScores();
    	
    	if (Integer.parseInt(new String(rs.getRecord(10), 0, rs.getRecord(10).length)) <= highscore) {
    		flag = true;
    	}

    	rs.closeRecordStore();
    	
    	return flag;
    }
    
    private void getHighScoreData() throws RecordStoreFullException, RecordStoreNotFoundException, RecordStoreException {
    	RecordStore rs = null;
    	rs = RecordStore.openRecordStore("highscores", true);

    	checkHighScores();
    	
    	highscores = new String[10];
    	
    	for (int i = 0; i < 10; i+=2) {
    		highscores[i] = new String(rs.getRecord(i+1), 0, rs.getRecord(i+1).length);
    		highscores[i+1] = new String(rs.getRecord(i+2), 0, rs.getRecord(i+2).length);
    	}
    }
    
    private void showHighScores() {

    	try {
			getHighScoreData();
		} catch (RecordStoreFullException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RecordStoreNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RecordStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	gameState = 4;
    	addCmd(Back2MenuCommand, null);
    	
    }
    
    private void newGame() {
    	Random rand = new Random();
    	addCmd(MenuCommand, UndoCommand);
    	areaWidth = 10;
    	areaHeight = 16;
    	areaState = new int[areaWidth][areaHeight];
    	int pickColor = 0;
    	for (int i = 0; i < areaWidth; i++) {
        	for (int j = 0; j < areaHeight; j++) {
        		if (i > 0 && j > 0) {
        			pickColor = Math.abs(rand.nextInt())%7;
        			if (pickColor == 5) {
        				areaState[i][j] = areaState[i-1][j]; 
        			} else if (pickColor == 6) {
        				areaState[i][j] = areaState[i][j-1];
        			} else {
        				areaState[i][j] = colors[pickColor];
        			}
        		} else if (i > 0 && j == 0) {
        			pickColor = Math.abs(rand.nextInt())%6;
        			if (pickColor == 5) {
        				areaState[i][j] = areaState[i-1][j]; 
        			} else {
        				areaState[i][j] = colors[pickColor];
        			}
        		} else if (i == 0 && j > 0) {
        			pickColor = Math.abs(rand.nextInt())%6;
        			if (pickColor == 5) {
        				areaState[i][j] = areaState[i][j-1]; 
        			} else {
        				areaState[i][j] = colors[pickColor];
        			}
        		} else {
        			areaState[i][j] = colors[Math.abs(rand.nextInt())%5];
        		}
        	}
        }
/*    	
    	for (int i = 0; i < areaWidth; i++) {
        	for (int j = 0; j < areaHeight; j++) {
        		areaState[i][j] = 0x000000;
        	}
    	}
    	areaState[1][14] = colors[0];
    	areaState[0][15] = areaState[1][15] = areaState[0][14] = colors[1];
    	areaState[0][13] = colors[0];
*/  	
    	
    	undoCntr = 0;
    	areaStateHistory = new int[areaWidth*areaHeight/2+1][areaWidth][areaHeight];
    	scoreHistory = new int[areaWidth*areaHeight/2+1];
    	for (int i = 0; i < areaWidth; i++) {
    		for (int j = 0; j < areaHeight; j++) {
    			areaStateHistory[undoCntr][i][j] = areaState[i][j];
    		}
    	}
    	cursorX = cursorY = 0;
    	scoreHistory[undoCntr] = score = nextPts = 0;
    	squareWidth = (getWidth()-((getHeight()/areaHeight)*areaWidth) < 66 ? (getWidth()-66)/areaWidth : getHeight()/areaHeight);
    	gameState = 1;
    	originalColor = areaState[cursorX][cursorY];
        colCntr = 5;
        nextPts();
    }

    private void endGame() {
    	boolean empty = true, newHS = false;
    	for (int i = 0; i < areaWidth; i++) {
    		for (int j = 0; j < areaHeight; j++) {
    			if (areaState[i][j] != colors[5]) {
    				empty = false;
    				break;
    			}
    		}
    		if (!empty) {
    			break;
    		}
    	}
    	if (empty) {
    		score *= 5;
    	}
    	try {
			newHS = newHighScore(score);
			getHighScoreData();
		} catch (RecordStoreFullException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RecordStoreNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RecordStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (newHS) {
    		gameState = 5;
    		cursorBlinker = 0;
    		name = "";
    		addCmd(DeleteChar, Ok);
		} else {
			selectedOption = 0;
    		gameState = 3;
    		addCmd(null, null);
		}
    	
    }
    
    private void boom() {
    	boolean top = false;
    	int blockCount = flood(cursorX, cursorY, areaState[cursorX][cursorY], colors[5]);
    	score += (blockCount-1)*(blockCount-1);
    	for (int i = 0; i < areaWidth; i++) {
    		top = false;
    		for (int j = areaHeight-1; j > 0; j--) {
        		while (areaState[i][j] == colors[5] && !top) {
        			top = true;
        			for (int k = j; k > 0; k--) {
        				areaState[i][k] = areaState[i][k-1];
        				if (areaState[i][k] != colors[5]) top = false;
        			}
        			areaState[i][0] = colors[5];
        		}
        	}
    	}
    	for (int i = 0; i < areaWidth-1; i++) {
    		if (areaState[i][areaHeight-1] == colors[5]) {
    			top = true;
    			for (int j = i; j < areaWidth; j++) {
    				if (areaState[j][areaHeight-1] != colors[5]) {
    					top = false;
    				}
    			}
    			while (!top && areaState[i][areaHeight-1] == colors[5]) {
    				for (int k = i; k < areaWidth-1; k++) {
    					for (int l = 0; l < areaHeight; l++) {
    						areaState[k][l] = areaState[k+1][l];
    					}
    				}
    				for (int l = 0; l < areaHeight; l++) {
						areaState[areaWidth-1][l] = colors[5];
					}
    			}
    		}
    	}
    	undoCntr++;
    	for (int i = 0; i < areaWidth; i++) {
    		for (int j = 0; j < areaHeight; j++) {
    			areaStateHistory[undoCntr][i][j] = areaState[i][j];
    		}
    	}
    	scoreHistory[undoCntr] = score;
    	
    	boolean single = true;
    	for (int i = 0; i < areaWidth; i++) {
    		for (int j = 0; j < areaHeight; j++) {
    			if (areaState[i][j] != colors[5] && !single(i, j)) {
    				single = false;
    				break;
    			}
    		}
    		if (!single) {
    			break;
    		}
    	}
    	if (single) {
    		endGame();
    	}
    	originalColor = areaState[cursorX][cursorY];
		colCntr = 5;
		nextPts();
    }
    
    private void addCmd(Command cmd1, Command cmd2) {
    	previousCommand1 = command1;
    	previousCommand2 = command2;
    	command1 = cmd1;
    	command2 = cmd2;
    	if (previousCommand1 != null) removeCommand(previousCommand1);
    	if (previousCommand2 != null) removeCommand(previousCommand2);
    	if (cmd1 != null) addCommand(cmd1);
    	if (cmd2 != null) addCommand(cmd2);
    }
    
    private int flood(int x, int y, int color, int color2) {
    	if (color == colors[5] || color == color2) return 0;
    	int blocksCount = 1; 
    	areaState[x][y] = color2;
    	if (x < areaWidth-1 && areaState[x+1][y] == color) blocksCount += flood(x+1, y, color, color2);
    	if (x > 0 && areaState[x-1][y] == color) blocksCount += flood(x-1, y, color, color2);
    	if (y < areaHeight-1 && areaState[x][y+1] == color) blocksCount += flood(x, y+1, color, color2);
    	if (y > 0 && areaState[x][y-1] == color) blocksCount += flood(x, y-1, color, color2);
    	return blocksCount;
    }
    
    private boolean single(int x, int y) {
    	if ((x < areaWidth-1 && areaState[x+1][y] == areaState[x][y]) ||
    			(x > 0 && areaState[x-1][y] == areaState[x][y]) ||
    			(y < areaHeight-1 && areaState[x][y+1] == areaState[x][y]) ||
    			(y > 0 && areaState[x][y-1] == areaState[x][y])) { 
    		return false;
    	} else {
    		return true;
    	}
    }
    
    private void nextPts() {
    	int blocksCount; 
    	if (areaState[cursorX][cursorY] != colors[5]) {
    		blocksCount = flood(cursorX, cursorY, areaState[cursorX][cursorY], 0x12345678);
    		flood(cursorX, cursorY, 0x12345678, originalColor);
    		nextPts = (blocksCount-1)*(blocksCount-1);
		} else {
			nextPts = 0;
		}
    }
    
    public void undoIt() {
    	if (--undoCntr < 0) {
    		undoCntr = 0;
    	}
    	for (int i = 0; i < areaWidth; i++) {
    		for (int j = 0; j < areaHeight; j++) {
    			areaState[i][j] = areaStateHistory[undoCntr][i][j];
    		}
    	}
    	score = scoreHistory[undoCntr];
    	originalColor = areaState[cursorX][cursorY];
		colCntr = 5;
		nextPts();
    }
    
    private void typeText(int key) {
    	char[][] keyChars = {
    			{'A', 'B', 'C'},
    			{'D', 'E', 'F'},
    			{'G', 'H', 'I'},
    			{'J', 'K', 'L'},
    			{'M', 'N', 'O'},
    			{'P', 'Q', 'R', 'S'},
    			{'T', 'U', 'V'},
    			{'W', 'X', 'Y', 'Z'}
    	};

    	if (key == KEY_NUM0) {
    		charKeysIndex[0] = 100;
    	} else if (key >= KEY_NUM2 && key <= KEY_NUM9) {
			if (charKeysIndex[0] != key-KEY_NUM2) {
				charKeysIndex[1] = 0;
			} else {
				charKeysIndex[1] = (++charKeysIndex[1]) % keyChars[charKeysIndex[0]].length;
				if (name.length() > 0) name = name.substring(0, name.length()-1);
			}
			charKeysIndex[0] = key-KEY_NUM2;
			name += keyChars[charKeysIndex[0]][charKeysIndex[1]];
    	}
    }
    
    public void deleteChar() {
    	if (name.length() > 0) {
    		name = name.substring(0, name.length()-1);
    	}
    	charKeysIndex[0] = 100;
    }
    
    public void okNewHighscore() {
    	gameState = 10;
    	try {
			addHighScore(name, score);
		} catch (RecordStoreNotOpenException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RecordStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		newGame();
    }
    
    private void repaint(Graphics g) {
    	if (gameState == 0) {
    		splashScreen();
    	} else if (gameState == 1) {
	    	g.setColor(0xEEEEEE);
	        g.fillRect(0, 0, getWidth(), getHeight());
	        g.setColor(0x000000);
	        g.fillRect(0, 0, areaWidth*squareWidth, getHeight());
	        int offset = getHeight()-areaHeight*squareWidth;
	        for (int i = 0; i < areaWidth; i++) {
	        	for (int j = 0; j < areaHeight; j++) {
	        		g.setColor(areaState[i][j]);
	        		g.fillRect(i*squareWidth, j*squareWidth+offset, squareWidth, squareWidth);
	        	}
	        }
	        colCntr++;
	        if (colCntr < 0) colCntr = 0;
	        if (colCntr%25 == 12 && !single(cursorX, cursorY)) {
	            flood(cursorX, cursorY, areaState[cursorX][cursorY], (areaState[cursorX][cursorY]/5)*4);
	        } else if (colCntr%25 == 0 && !single(cursorX, cursorY)) {
	        	flood(cursorX, cursorY, areaState[cursorX][cursorY], originalColor);            	
	        }
	        g.setColor(areaState[cursorX][cursorY] != 0 ? 0x000000 : 0xFFFFFF);
	        g.drawRect(cursorX*squareWidth, cursorY*squareWidth+offset, squareWidth-1, squareWidth-1);
	        
	        drawString("Score:", getWidth()-(getWidth()-(areaWidth*squareWidth))/2, 2, Graphics.TOP | Graphics.HCENTER);
	        drawString(Integer.toString(score), getWidth()-(getWidth()-(areaWidth*squareWidth))/2, 2+chrH, Graphics.TOP | Graphics.HCENTER);
	        
	        drawString("Next:", getWidth()-(getWidth()-(areaWidth*squareWidth))/2, 2+chrH*2, Graphics.TOP | Graphics.HCENTER);
	        drawString(Integer.toString(nextPts), getWidth()-(getWidth()-(areaWidth*squareWidth))/2, 2+chrH*3, Graphics.TOP | Graphics.HCENTER);
	        
	        drawString("Bonus:", getWidth()-(getWidth()-(areaWidth*squareWidth))/2, 2+chrH*4, Graphics.TOP | Graphics.HCENTER);
	        drawString(Integer.toString(score*4), getWidth()-(getWidth()-(areaWidth*squareWidth))/2, 2+chrH*5, Graphics.TOP | Graphics.HCENTER);
	        
	    } else if (gameState == 2) {
	    	g.setColor(0xEEEEEE);
	    	g.fillRect(0, 0, getWidth(), getHeight());
	    	g.setColor(0xAAAAAA);
	    	g.fillRect((getWidth()-(10*chrW))/2-chrW, (2+selectedOption)*chrH+1, 10*chrW+chrW, chrH);
	    	drawString("< Menu >", getWidth()/2, 2, Graphics.TOP | Graphics.HCENTER);
	    	drawString("New Game", getWidth()/2-(11*chrW)/2, 2+2*chrH, Graphics.TOP | Graphics.LEFT);
	    	drawString("HighScores", getWidth()/2-(11*chrW)/2, 2+3*chrH, Graphics.TOP | Graphics.LEFT);
	    	drawString("Help", getWidth()/2-(11*chrW)/2, 2+4*chrH, Graphics.TOP | Graphics.LEFT);
	    	
	    } else if (gameState == 3) {
	    	g.setColor(0xEEEEEE);
	        g.fillRect(0, 0, getWidth(), getHeight());
	        drawString("Final Score:", getWidth()/2, 10, Graphics.TOP | Graphics.HCENTER);
	        drawString(Integer.toString(score), getWidth()/2, 10+chrH, Graphics.TOP | Graphics.HCENTER);
	        
	        g.setColor(0xAAAAAA);
	        g.fillRect(getWidth()/2-6*chrW, 9+chrH*(3+selectedOption), 13*chrW, chrH);
	        
	        drawString("1. New Game", getWidth()/2, 10+chrH*3, Graphics.TOP | Graphics.HCENTER);
	        drawString("2. Exit    ", getWidth()/2, 10+chrH*4, Graphics.TOP | Graphics.HCENTER);
	    } else if (gameState == 4) {
	    	g.setColor(0xEEEEEE);
	        g.fillRect(0, 0, getWidth(), getHeight());
	        drawString("< Highscores >", getWidth()/2, 0, Graphics.TOP | Graphics.HCENTER);
	        for (int i = 0; i < 5; i++) {
	        	drawString(Integer.toString(i+1)+". ", getWidth()/2-64+2, chrH*(i+1), Graphics.TOP | Graphics.LEFT);
        		drawString(highscores[2*i], getWidth()/2-40+2, chrH*(i+1), Graphics.TOP | Graphics.LEFT);
        		drawString(highscores[2*i+1], getWidth()/2+64-2, chrH*(i+1), Graphics.TOP | Graphics.RIGHT);
	        }
	    	
	    } else if (gameState == 5) {
	    	g.setColor(0xEEEEEE);
	        g.fillRect(0, 0, getWidth(), getHeight());
	        drawString("Enter your name:", getWidth()/2, 0, Graphics.TOP | Graphics.HCENTER);
	        int pos = 0;
	        
	        while (Integer.parseInt(highscores[2*pos+1]) > score) {
	        	drawString(Integer.toString(pos+1)+". ", getWidth()/2-64+2, chrH*(pos+1), Graphics.TOP | Graphics.LEFT);
        		drawString(highscores[2*pos], getWidth()/2-40+2, chrH*(pos+1), Graphics.TOP | Graphics.LEFT);
        		drawString(highscores[2*pos+1], getWidth()/2+64-2, chrH*(pos+1), Graphics.TOP | Graphics.RIGHT);
        		pos++;
	        }
   			cursorBlinker++;
    		if (name.length() < 6 && cursorBlinker%15 < 8) {
        		g.setColor(0xAAAAAA);
        		g.fillRect(getWidth()/2-40+2+name.length()*chrW, chrH*(pos+1), 2, 12);
    		}
	        drawString(Integer.toString(pos+1)+". ", getWidth()/2-64+2, chrH*(pos+1), Graphics.TOP | Graphics.LEFT);
    		drawString(name, getWidth()/2-40+2, chrH*(pos+1), Graphics.TOP | Graphics.LEFT);
    		drawString(Integer.toString(score), getWidth()/2+64-2, chrH*(pos+1), Graphics.TOP | Graphics.RIGHT);
    		pos++;
	        while (pos < 5) {
	        	drawString(Integer.toString(pos+1)+". ", getWidth()/2-64+2, chrH*(pos+1), Graphics.TOP | Graphics.LEFT);
	    		drawString(highscores[2*(pos-1)], getWidth()/2-40+2, chrH*(pos+1), Graphics.TOP | Graphics.LEFT);
	    		drawString(highscores[2*(pos-1)+1], getWidth()/2+64-2, chrH*(pos+1), Graphics.TOP | Graphics.RIGHT);
	    		pos++;
	        }
	    } else if (gameState == 6) {
	    	g.setColor(0xEEEEEE);
	        g.fillRect(0, 0, getWidth(), getHeight());
	        
	        for (int i = 0; i < (helpRows.size() < (getHeight()/chrH) ? helpRows.size() : (getHeight()/chrH)); i++) {
	        	drawString((String) helpRows.elementAt(i+scrollPos), 2, i*chrH, Graphics.TOP | Graphics.LEFT);
	        }
	        
	        if (getHeight()/chrH < helpRows.size()) {
	        	g.setColor(0x000000);
	        	if (scrollPos > 0) {
			        g.drawLine(getWidth()-3-2, 0, getWidth()-6-2, 12);
			        g.drawLine(getWidth()-3-2, 0, getWidth()-2, 12);
			        g.drawLine(getWidth()-6-2, 12, getWidth()-2, 12);
			        g.drawLine(getWidth()-3-2, 12, getWidth()-3-2, 24);
	        	}
	        	if (scrollPos < helpRows.size()-getHeight()/chrH) {
			        g.drawLine(getWidth()-3-2, getHeight(), getWidth()-6-2, getHeight()-12);
			        g.drawLine(getWidth()-3-2, getHeight(), getWidth()-2, getHeight()-12);
			        g.drawLine(getWidth()-6-2, getHeight()-12, getWidth()-2, getHeight()-12);
			        g.drawLine(getWidth()-3-2, getHeight()-12, getWidth()-3-2, getHeight()-24);
	        	}
	        }
	        	        
	        
	    } else if (gameState == 10) {
	    	g.setColor(0xEEEEEE);
	        g.fillRect(0, 0, getWidth(), getHeight());	    	
	    }
    }
    
    public void startApplication(mainClass midlet) {
        g = getGraphics();
        Thread runner = new Thread(this);
        runner.start();
        this.midlet = midlet;
    }
    
    private void exitGame() {
    	midlet.notifyDestroyed();
    }

    public void run() {
        init();
        long pTime;
        while (true) {
            try {

                pTime = new Date().getTime();

                if (lastPressedKey != 0 && getGameAction(lastPressedKey) != FIRE && (new Date().getTime() - kTime) % 200 <= 40 && new Date().getTime() - kTime > 40) {
                        keyPressed(lastPressedKey); 
                }

                repaint(g);

                flushGraphics();

                try {
                    Thread.sleep((40 - (new Date().getTime() - pTime) < 0) ? 0 : 40 - (new Date().getTime() - pTime));
                    if (pause) {
                            synchronized(this) {
                                    wait();
                            }
                    }
                } catch (InterruptedException e) {
                    //e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }

            } catch(Exception e) {
                e.printStackTrace();
            }
        }

    }
    
    protected void keyPressed(int key) {
    	super.keyPressed(key);
    	
    	if (gameState == 1) {
    		
	    	lastPressedKey = key;   
	    	kTime = new Date().getTime();
	    	int tempX = cursorX, tempY = cursorY;
	    	int action = getGameAction(key);
	    	
	    	switch(action) {
	    		case UP :
	    			if (cursorY > 0) {
	    				cursorY--;
	    			} else {
	    				cursorY = areaHeight-1;
	    			}
	    			break;
	    		case LEFT :
	    			if (cursorX > 0) {
	    				cursorX--;
	    			} else {
	    				cursorX = areaWidth-1;
	    			}
	    			break;
	    		case FIRE :
	    			if (!single(cursorX, cursorY) && areaState[cursorX][cursorY] != colors[5]) boom();    			
	    			break;
	    		case RIGHT :
	    			if (cursorX < areaWidth-1) {
	    				cursorX++;
	    			} else {
	    				cursorX = 0;
	    			}
	    			break;
	    		case DOWN :
	    			if (cursorY < areaHeight-1) {
	    				cursorY++;
	    			} else {
	    				cursorY = 0;
	    			}
	    			break;
	    	}
	    	
	    	if (((areaState[cursorX][cursorY] != originalColor && areaState[cursorX][cursorY] != (originalColor/5)*4) || (Math.abs(tempX-cursorX) > 1 || Math.abs(tempY-cursorY) > 1)) && action != FIRE) {
	    		flood(tempX, tempY, areaState[tempX][tempY], originalColor);
	    		originalColor = areaState[cursorX][cursorY];
	    		colCntr = 5;
	    		nextPts();
	    	}

    	} else if (gameState == 2) {
    		int action = getGameAction(key);
			if (action == UP) {
				if (selectedOption > 0) selectedOption--;
				else selectedOption = 2;
			} else if (action == DOWN) {
				if (selectedOption < 2) selectedOption++;
				else selectedOption = 0;
			} else if (action == FIRE) {
				if (selectedOption == 0) {
					newGame();
				} else if (selectedOption == 1) {
					showHighScores();
				} else if (selectedOption == 2) {
					help();
				}
			}
    	} else if (gameState == 3) {
    		
			int action = getGameAction(key);
			if (action == UP) {
				selectedOption = selectedOption == 0 ? 1 : 0; 
			} else if (action == DOWN) {
				selectedOption = selectedOption == 0 ? 1 : 0;
			} else if (action == FIRE) {
				if (selectedOption == 0) {
					newGame();
				} else if (selectedOption == 1) {
					exitGame();
				}
			}
    	} else if (gameState == 5) {
   			typeText(key);
   			if (name.length() > 6) {
   				name = name.substring(0, 6);
   			}
    	} else if (gameState == 6) {
    		int action = getGameAction(key);
    		lastPressedKey = key;   
	    	kTime = new Date().getTime();
    		if (getHeight()/chrH < helpRows.size()) {
	    		if (action == UP && scrollPos > 0) {
	    			scrollPos--;
	    		} else if (action == DOWN && scrollPos < helpRows.size()-getHeight()/chrH) {
	    			scrollPos++;
	    		}
    		}
    	}
    }
    
    protected void keyReleased(int key) {
    	lastPressedKey = 0;
    }
    
    protected void keyRepeated(int key) {
    	System.out.println("key repeated!!!");
    }

    private void init() {
        initCharMap();
        loadRes();
        splashScreen();
    }

}
