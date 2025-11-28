package su.rumishistem.ffmpegui.Form;

import java.util.concurrent.CountDownLatch;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class ConvertSettingForm {
	private Shell dialog;
	private CountDownLatch cdl = new CountDownLatch(1);
	private String cmd_args = null;

	public ConvertSettingForm(Shell owner) {
		dialog = new Shell(owner, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
		dialog.setSize(500, 300);
		dialog.setText("変換設定");
		dialog.setLayout(new GridLayout(2, false));

		//プリセット
		Combo preset_combo = new Combo(dialog, SWT.DROP_DOWN | SWT.READ_ONLY);
		preset_combo.setItems(new String[] {"動画を再エンコード", "動画→MP4"});
		preset_combo.select(0);
		preset_combo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		//プリセット選択
		Button preset_select = new Button(dialog, SWT.NONE);
		preset_select.setText("選択");
		preset_select.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int index = preset_combo.getSelectionIndex();
				System.out.println(index);
			}
		});
		preset_select.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		//コマンドライン引数
		Text cmd_args_text = new Text(dialog, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
		cmd_args_text.setFont(new Font(Display.getCurrent(), "Monospace", 12, SWT.NORMAL));
		cmd_args_text.setText("-i %input% %output%.mp4");
		cmd_args_text.setLayoutData(new GridData(GridData.FILL_BOTH));
		GridData cmd_args_lg = new GridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL);
		cmd_args_lg.horizontalSpan = 2;
		cmd_args_text.setLayoutData(cmd_args_lg);

		//OKボタン
		Button ok_button = new Button(dialog, SWT.NONE);
		ok_button.setText("OK");
		ok_button.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		ok_button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				cmd_args = cmd_args_text.getText();
				cdl.countDown();
				dialog.close();
			}
		});

		//キャンセルボタン
		Button cancel_button = new Button(dialog, SWT.NONE);
		cancel_button.setText("キャンセル");
		cancel_button.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		cancel_button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				cdl.countDown();
				dialog.close();
			}
		});

		dialog.open();
	}

	public String open() {
		try {
			cdl.await();
			return cmd_args;
		} catch (InterruptedException ex) {
			return null;
		}
	}
}
