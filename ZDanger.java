// Z Machine V3/V4/V5 Runtime
//
// Copyright 2002-2003, Brian Swetland <swetland@frotz.net>
// Available under a BSD-Style License.  Share and Enjoy.
//
// glue between the runtime and the Danger UI

package net.frotz.zruntime;

import danger.app.DataStore;
import danger.app.Event;
import danger.ui.Menu;
import java.util.Random;


public class ZDanger extends ZScreen implements Runnable
{
	ZDanger(ZWindow win, int width, int height) {
		this.win = win;
		this.w = width;
		this.h = height;
		screen = new byte[width*height];
		buffer = new byte[width];
		r = new Random();
		Clear();
		buffered = true;
		win0h = height;
		win1h = 0;
		win1cx = 0;
		win1cy = 0;
		LF = new char[1];
		LF[0] = '\n';
	}

	public int GetWidth() { return w; }
	public int GetHeight() { return h; }
	
	void Clear() {
		for(int i = 0; i < w*h; i++) screen[i] = (byte) ' ';
		cx = 0;
		cy = h - 1;
	}

	public void SetStatus(char line[], int len) {
		int i;
		if(win1h > 0){
			int max = w-1;
			if(len >= max) len = max;
			for(i = 0; i < len; i++){
				screen[i+1] = (byte) line[i];
			}
			screen[0] = (byte) ' ';
			while(i < max) screen[1+i++] = (byte)' ';
			win.invalidate();
		}
	}

	public void run() {
		for(;;) {
			byte[] data = win.GetGameFile();
			if(data == null) {
				Clear();
				Print("no game file installed.");
				break;
			}
			try {
				zm = new ZMachine(this, data);
				Clear();
				zm.run();
			} catch (Throwable t) {
				System.err.println("ZMachine: exception "+t);
				t.printStackTrace();
			}
		}
	}

	public void SetWindow(int num) {
		selected = num;
		if(selected == 1){
			win1cx = 0;
			win1cy = 0;
		} else {
			win.invalidate();	
		}
	}
	public void SplitWindow(int height) {
		win1h = height;
		win0h = h - height;
	}
	public void EraseWindow(int num) {
		if(num == -1) {
			Clear();
			win1h = 0;
			win0h = h;
			cx = 0;
			cy = 0;
			win1cx = 0;
			win1cy = 0;
			selected = 0;
			count = 0;
		}
	}
	public void MoveCursor(int x, int y) {
			/* upper left is 1,1 */
		x--;
		y--;
		
			/* gracefully deal with bogus locations */
		if(x < 0) x = 0;
		if(y < 0) y = 0;
		
		if(selected == 0){
			cx = x;
			cy = win1h + y;
			if(cx >= w) cx = w-1;
			if(cy >= h) cy = h-1;
		} else {
			win1cx = x;
			win1cy = y;
			if(win1cx >= w) win1cx = w-1;
			if(win1cy >= win1h) win1cy = win1h - 1;
		}
	}

	public void Print(char data[], int len) {
		int ptr = 0;

		if(selected == 1){
			if(win1h == 0) return;
			
			while(ptr < len){
				char ch =  data[ptr++];
				
				if(ch == 13 || ch == 10){
					win1cy++;
					win1cx=0;
				} else {
					screen[win1cx + win1cy * w] = (byte) ch;
					win1cx++;
					if(win1cx == w){
						if(win1cy + 1 < win1h){
							win1cy++;
							win1cx = 0;
						} else {
							win1cx--;
						}
					}
				}
				if(win1cy >= win1h) win1cy = win1h - 1;
			}
			return;
		}

		if(!input_active) win.Transcript(data, 0, len);
		
		while(ptr < len){
			char c = data[ptr];
			if((c == 13) || (c == 10)) {
				DoNewLine();
			} else {
				if(buffered) {
					buffer[buflen++] = (byte) c;
					if(buflen == w){
						int i;
						for(i = buflen - 1; i > 0; i--){
							if(buffer[i] == ' ') {
								int t = buflen;
								buflen = i ;
								DoNewLine();
								i++;
								buflen = t - i;
								System.arraycopy(buffer, i, buffer, 0, buflen);
								i = -1;
								break;
							}
						}
						if(i != -1) DoNewLine();
					}
				} else {
					screen[cx + cy * w] = (byte) c;
					cx ++;
					if(cx == w) DoNewLine();
				}
			}
			ptr++;
		}
		win.invalidate();
	}

	void Flush() {
		if(selected == 0) {
			if(buffered) {
				System.arraycopy(buffer, 0, screen, cx + cy * w, buflen);
				cx += buflen;
				buflen = 0;
			}
		}
	}

	public void NewLine() {
		if(!input_active) win.Transcript(LF,0,1);
		DoNewLine();
	}
	
	public void DoNewLine() {
		Flush();

		cx = 0;
		cy++;
		if(cy == h){
			cy--;
			scrollup();
		}
		count++;
		if(count == (win0h - 1)){
			int p = w * (h - 1);
			screen[p+0] = (byte) '[';
			screen[p+1] = (byte) 'M';
			screen[p+2] = (byte) 'O';
			screen[p+3] = (byte) 'R';
			screen[p+4] = (byte) 'E';
			screen[p+5] = (byte) ']';
			win.invalidate();
			synchronized (zm) {
				paused = true;
				try {
					zm.wait();
				} catch (Throwable t) {
				}
			}
			screen[p+0] = (byte) ' ';
			screen[p+1] = (byte) ' ';
			screen[p+2] = (byte) ' ';
			screen[p+3] = (byte) ' ';
			screen[p+4] = (byte) ' ';
			screen[p+5] = (byte) ' ';
			count = 0;
		}
	}

	void scrollup() {
		int i,p;
		System.arraycopy(screen, (win1h+1) * w, screen, win1h*w, w * (win0h - 1));

		p = w * (h - 1);
		for(i = 0; i < w; i++){
			screen[p++] = (byte) ' ';
		}

	}

	public void Backspace() {
		cx--;
		if(cx < 0){
			cy--;
			cx = w - 1;
		}
		screen[cx + cy * w] = (byte) ' ';
		win.invalidate();
	}

	public void KeyPress(char key) {
		if(paused) {
			synchronized(zm) {
				paused = false;
				zm.notify();
			}
			return;
		}
		if(input_active) {
			if(readchar == -1){
				readchar = (int) key;
				synchronized(zm){
					input_active = false;
					zm.notify();
				}
				return;
			}
			switch(key){
			case 10:
			case 13:
				DoNewLine();
				synchronized(zm) {
					input_active = false;
					zm.notify();
				}
				break;
			case 8:
				if(line_sz > 0) {
					Backspace();
					line_sz--;
				}
				break;
			default:
				line[line_sz++] = key;
				PrintChar(key);
			}
		}
	}

	public int Read(){ 
		win.invalidate();
		try {
			synchronized(zm) {
				input_active = true;
				readchar = -1;
				zm.wait();
				return readchar;
			}
		} catch (Throwable t){
		}
		return 0;
	}

	public int ReadLine(char buffer[]) {
		Flush();
		win.invalidate();
		count = 0;
		try {
			synchronized(zm) {
				buffered = false;
				line = buffer;
				input_active = true;
				line_sz = 0;
				zm.wait();
				line = null;
				buffered = true;
				win.Transcript(buffer, 0, line_sz);
				win.Transcript(LF, 0, 1);
				return line_sz;
			}
		} catch (Throwable t) {
		}
		return 0;
	}

	public void Print(String s) {
		// there ought to be a better way to do this.
		char[] buf = new char[s.length()];
		s.getChars(0, s.length(), buf, 0);
		Print(buf, s.length());
	}

	public int Random(int limit) {
		if((limit & 0x8000) != 0) {
			r.setSeed(limit & 0x7fff);
			return 0;
		}
		if(limit == 0) {
			r = new Random();
			return 0;
		}
		return r.nextInt(limit) + 1;
	}

	public boolean Save(byte state[]) { 
		return win.Save(state);
	}
	public byte[] Restore() { 
		return win.Restore();
	}

	int selected;
	int readchar;
	
	int win0h;
	int win1h;
	int win1cx;
	int win1cy;
	
	Random r;
	byte screen[];
	byte buffer[];
	int buflen;

	char line[];
	int line_sz;
	ZWindow win;
	int w,h; /* screen dimensions */
	int cx,cy; /* cursor position */
	int count;

	boolean input_active;
	boolean buffered;
	boolean paused;
	boolean running;
	ZMachine zm;

	char[] LF;
	static final boolean TRACE = false;
	
}
