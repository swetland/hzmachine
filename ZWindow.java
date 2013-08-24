// Z Machine V3/V4/V5 Runtime
//
// Copyright 2002-2003, Brian Swetland <swetland@frotz.net>
// Available under a BSD-Style License.  Share and Enjoy.
//
// Hiptop Application Main Window

package net.frotz.zruntime;

import danger.app.Resource;
import danger.app.ResourceDatabase;
import danger.app.Event;
import danger.app.Registrar;
import danger.app.IPCMessage;

import danger.ui.ScreenWindow;
import danger.ui.Font;
import danger.ui.Pen;
import danger.ui.Color;
import danger.ui.Window;
import danger.ui.TextField;
import danger.ui.EditText;
import danger.ui.PopupMenu;

import danger.app.DataStore;

public class ZWindow extends ScreenWindow implements Resources, Events
{
	public ZWindow(ZRuntime app) {
		super("ZMachine");
		setFullScreen(true);
		font = Font.findFont("Fixed5x7");
		CH = font.getAscent() + font.getDescent();
		CA = font.getAscent();
		CW = font.charWidth('M');
		w = getWidth() / CW;
		h = getHeight() / CH;
		System.err.println("[ " + w + " x " + h + " ]");
		scr = new ZDanger(this, w, h);
		screen = scr.screen;
		this.app = app;
		rdb = app.getResources();
		rdb.addToMenuFromResource(getActionMenu(),MENU_MAIN,this,null);
	}

	byte[] GetGameFile() {
		int b;
		Resource rsrc;
		rsrc = app.getResource(1000,1000);

		if(rsrc != null) {
			b = rsrc.getSize();
			System.err.println("Gamefile: "+b+" bytes");
			byte[] data = new byte[b];
			rsrc.getBytes(data, 0, b);
			return data;
		} else {
			return null;
		}
	}

	void ScrollLine(byte data[], int off) {
	}

	Window dw;
	
	public void Email(String subject, String body) {
		IPCMessage msg = new IPCMessage();
		msg.addItem("action", "send");
		msg.addItem("subject", subject);
		msg.addItem("body", body);
		Registrar.sendMessage("Email", msg, null);
	}
	
	public boolean receiveEvent(Event e) {
		switch(e.type) {
		case DO_VIEW_NOTEPAD:
			if(notepad == null) {
				notepad = rdb.getScreen(SCREEN_NOTEPAD, this);
			}
			dw = notepad;
			dw.show();
			break;
		case DO_VIEW_TRANSCRIPT:
			dw = rdb.getScreen(SCREEN_TRANSCRIPT,this);
			((EditText)dw.getDescendantWithID(ID_TEXT_TRANSCRIPT)).setText(transcript.toString());
			dw.show();
			break;
		case NOTEPAD_DISMISS:
		case TRANSCRIPT_DISMISS:
			show();
			dw.hide();
			dw = null;
			break;
		case DO_EMAIL_NOTEPAD:
			Email("Game Notepad",((EditText)dw.getDescendantWithID(ID_TEXT_NOTEPAD)).toString());
			break;
		case DO_EMAIL_TRANSCRIPT:
			Email("Game Transcript",((EditText)dw.getDescendantWithID(ID_TEXT_TRANSCRIPT)).toString());
			break;
		case DO_CLEAR_TRANSCRIPT:
			transcript.setLength(0);
			((EditText)dw.getDescendantWithID(ID_TEXT_TRANSCRIPT)).setText("");
			break;
		case DO_ABOUT:
			(rdb.getDialog(DIALOG_ABOUT)).show();
			break;
		case DO_RESTORE:
			dw = rdb.getDialog(DIALOG_RESTORE,this);
			String[] gamelist = getSavedGames();
			if((gamelist == null) || (gamelist.length == 0)) {
				notifyEngine(false);
				break;
			} else {
				PopupMenu pm = (PopupMenu) dw.getDescendantWithID(ID_NAME_RESTORE);
		
				for(int i = 0; i < gamelist.length; i++) {
					if(gamelist[i] != null) {
						System.err.println("Game["+i+"] = '"+gamelist[i]+"'");
						pm.addItem(gamelist[i]);
					}
				}
			}
			dw.show();
			break;
		case DO_SAVE:
			dw = rdb.getDialog(DIALOG_SAVE, this);
			dw.show();
			break;
		case SAVE_CANCEL:
		case RESTORE_CANCEL:
			notifyEngine(false);
			break;
		case SAVE_OK:
			TextField tf = (TextField) dw.getDescendantWithID(ID_NAME_SAVE);
			savegame = tf.toString();
			notifyEngine(true);
			break;
		case RESTORE_OK:
			PopupMenu pm = ((PopupMenu) dw.getDescendantWithID(ID_NAME_RESTORE));
			selection = pm.getValue();
			notifyEngine(true);
			break;
		default:
			return super.receiveEvent(e);
		}
		return true;
	}
	
	public boolean eventKeyUp(char c, Event e) {
		scr.KeyPress(c);
		return true;
	}

	public boolean eventWidgetUp(int widget, Event event) {
		switch(widget) {
		case Event.DEVICE_BUTTON_BACK:
			app.returnToLauncher();
			return true;
		default:
			return super.eventWidgetUp(widget,event);
		}
	}
	public void paint(Pen p) {
		int i, off;
		p.setFont(font);
		int BG = Color.WHITE;
		int FG = Color.BLACK;
		int sh = scr.win1h;
		
		p.setColor(FG);
		p.fillRect(0,0,w * CW,sh*CH);

		p.setColor(BG);
		for(i = 0, off = 0; i < sh; i++){
			p.drawText(2, i * CH + CA, screen, off, w);
			off += w;
		}

		p.fillRect(0, sh*CH, w*CW, h*CH);
		p.setColor(FG);
		for(; i < h; i++){
			p.drawText(2, i * CH + CA, screen, off, w);
			off += w;
		}
		p.setColor(BG);
		p.fillRect(0, i*CH, getWidth(), getHeight());

		if(scr.input_active){
			p.setColor(Color.RED);
			int x = scr.cx * CW + 2;
			int y = scr.cy * CH;
			p.fillRect(x, y, x + CW, y + CH);
		}
	}

	public String[] getSavedGames() {
		DataStore ds = getDataStore();
		int i, j;
		int count = ds.getRecordCount();
		String[] out = new String[count];

		for(i = 0; i < count; i++) {
			byte[] data = ds.getRecordData(i);

			if(data[0] == 'Z' && data[1] == '!' && data[2] == '1' && data[3] == '0'){
				for(j = 32; j < 64; j++){
					if(data[j] == 0) break;
				}
				out[i] = new String(data, 32, j-32);
			} else {
				out[i] = null;
			}
		}
		return out;
	}

	public void saveGame(String gamename, byte[] data) {
		int i,j,count;

		DataStore ds = getDataStore();
		
		byte str[] = gamename.getBytes();
		
		data[0] = 'Z';
		data[1] = '!';
		data[2] = '1';
		data[3] = '0';
		
		for(i = 4; i < 64; i++) data[i] = 0;

		for(i = 0; i < str.length; i++){
			if(i == 32) break;
			data[32 + i] = str[i];
		}

		count = ds.getRecordCount();
		for(i = 0; i < count; i++){
			byte game[] = ds.getRecordData(i);
			for(j = 0; j < 64; j++){
				if(game[j] != data[j]) break;
			}
			if(j == 64){
				ds.setRecordData(i, data);
				return;
			}
		}

		ds.addRecord(data);
	}
		
	String usename = "SaveGame";
	int count = 0;

	DataStore getDataStore() {
		DataStore ds = DataStore.findDataStore("saved-games");
		if (ds == null) {
			return DataStore.createDataStore("saved-games", true);
		} else {
			return ds;
		}
	}

	boolean waitForUI() {
		System.err.println("WaitForUI");
		try {
			synchronized(notify) {
				success = false;
				notify.wait();
			}
		} catch(Throwable t) {
		}
		System.err.println("DoneWaiting: " +success);
		
		return success;
	}
	
	boolean success;
	String savegame;
	int selection;
	
	void notifyEngine(boolean status) {
		synchronized(notify){
			success = status;
			notify.notify();
		}
	}

	public boolean Save(byte[] state) {
		sendEvent(DO_SAVE, 0, 0, null);
		
		savegame = null;
		if(waitForUI() && (savegame != null) && (savegame.length() > 0)){
			saveGame(savegame, state);
			scr.Print("Saved game as '"+savegame+"'\n");
		}
		return true;
	}

	public byte[] Restore() {
		selection = -1;
		sendEvent(DO_RESTORE, 0, 0, null);
		if(waitForUI() && (selection != -1)){
			return getDataStore().getRecordData(selection);
		} else {
			return null;
		}
	}

	public void show() {
		super.show();
		if(!scr.running){
			scr.running = true;
			(new Thread(scr,"ZMachine")).start();
		}
	}

	public void Transcript(char data[], int off, int len) {
		transcript.append(data, off, len);
	}
	
	StringBuffer transcript = new StringBuffer(8192);
	
	final static int DO_RESTORE = 1;
	final static int DO_SAVE = 2;
	
	Object notify = new Object();	

	Font font;
	ZDanger scr;
	ZRuntime app;
	Window notepad;
	ResourceDatabase rdb;
	int w,h;
	int CW, CH, CA;
	byte screen[];

}

