package su.rumishistem.ffmpegui;

import org.eclipse.swt.widgets.Display;
import su.rumishistem.ffmpegui.Form.MainForm;
import su.rumishistem.ffmpegui.Module.JobWorker;

public class Main {
	public static JobWorker jw = new JobWorker(1);
	private static Display display;

	public static void main(String[] args) {
		display = new Display();
		System.out.println("FFMPEGUI (C)るみ");

		new MainForm(display);

		//イベントループ
		while (!display.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}

		display.dispose();
	}

}
