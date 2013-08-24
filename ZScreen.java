// Z Machine V3/V4/V5 Runtime 
//
// Copyright 2002, Brian J. Swetland <swetland@frotz.net>
// Available under a BSD-Style License.  Share and Enjoy.
//
// Front-end Interface.
// The back-end in ZMachine.java is platform-agnostic

package net.frotz.zruntime;

public class ZScreen 
{
	public ZScreen() {}

	public void NewLine() { }
	public void Print(char data[], int len) { }

	public int Read() { return ' '; }
	public int ReadLine(char buffer[]) { return 0; }

	public void exit() { for(;;) ; }
	public int Random(int limit) { return 0; }
	public void SetStatus(char line[], int len) {}

	public int GetWidth() { return 40; }
	public int GetHeight() { return 15; }
	
	public void Restart() {}
	public boolean Save(byte state[]) { return false; }
	public byte[] Restore() { return null; }

	public void SetWindow(int num) {}
	public void SplitWindow(int height) {}
	public void EraseWindow(int number) {}
	public void MoveCursor(int x, int y) {}
	
	public void PrintNumber(int num) {
		int i = 16;
		int j;
		
		do {
			nbuf[--i] = (char) ('0' + (num % 10));
			num = num / 10;
		} while(num > 0);
		
		for(j = 0; i < 16; j++){
			nbuf[j] = nbuf[i++];
		}
		Print(nbuf, j);
	}

	public void PrintChar(int ch) {
		nbuf[0] = (char) ch;
		Print(nbuf, 1);
	}	

	char nbuf[] = new char[16];
}
