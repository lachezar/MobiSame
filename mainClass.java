import javax.microedition.midlet.MIDlet;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Command;


public class mainClass extends MIDlet implements CommandListener {
	
	canvasClass gCanvas;

	public mainClass() {
        gCanvas = new canvasClass();
	}

	protected void destroyApp(boolean unconditional) {
	}

	protected void pauseApp() {
		gCanvas.pause = true;
	}

	protected void startApp() {
		if (!gCanvas.pause) {
	        gCanvas.setCommandListener(this);
	        gCanvas.startApplication(this);
	        Display.getDisplay(this).setCurrent(gCanvas);
		} else {
			synchronized (gCanvas) {
				gCanvas.notify();
			}
			gCanvas.pause = false;
		}
	}
	
    public void commandAction(Command command, Displayable displayable) {
        if (command == gCanvas.ExitCommand) {
            notifyDestroyed();
        } else if (command == gCanvas.UndoCommand) {
            gCanvas.undoIt();
        } else if (command == gCanvas.MenuCommand) {
        	gCanvas.menu();
        } else if (command == gCanvas.Back2GameCommand) {
        	gCanvas.back2game();
        } else if (command == gCanvas.Back2MenuCommand) {
        	gCanvas.back2menu();
        } else if (command == gCanvas.DeleteChar) {
        	gCanvas.deleteChar();
        } else if (command == gCanvas.Ok) {
        	gCanvas.okNewHighscore();
        }
    }

}
