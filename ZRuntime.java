// Z Machine V3/V4/V5 Runtime
//
// Copyright 2002-2003, Brian Swetland <swetland@frotz.net>
// Available under a BSD-Style License.  Share and Enjoy.
//
// ZWindow - Danger Hiptop Application
// ZScreen - Interface between ZMachine and a front-end
// ZDanger - Danger Hiptop ZMachine front-end
// ZMachine - 'Z' Virtual Machine
// ZRuntime - Application main class
//

package net.frotz.zruntime;

import danger.app.Application;

public class ZRuntime extends Application
{
	public ZRuntime() { }

	public void resume() {
		if(win == null){
			win = new ZWindow(this);
			win.show();
		}
	}

	ZWindow win;
}
