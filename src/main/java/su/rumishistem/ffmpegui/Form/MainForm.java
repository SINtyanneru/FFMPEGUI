package su.rumishistem.ffmpegui.Form;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.util.*;
import java.util.List;
import java.util.regex.*;
import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

import su.rumishistem.ffmpegui.Main;
import su.rumishistem.ffmpegui.Module.FFProbe;
import su.rumishistem.ffmpegui.Type.VideoProbe;

public class MainForm {
	private Display display;
	private Table job_table;

	public MainForm(Display display) {
		this.display = display;

		Shell shell = new Shell(display);
		shell.setSize(600, 300);
		shell.setText("FFMPEGUI");

		GridLayout l = new GridLayout(2, false);
		shell.setLayout(l);

		shell.addShellListener(new ShellAdapter() {
			@Override
			public void shellClosed(ShellEvent e) {
				display.dispose();
			}
		});

		//ドラドロする場所
		int drdr_width = 300;
		int drdr_height = 300;
		Image drdr_image = new Image(display, drdr_width, drdr_height);
		GC gc = new GC(drdr_image);
		gc.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
		gc.fillRectangle(0, 0, 300, 300);
		gc.setForeground(display.getSystemColor(SWT.COLOR_BLACK));
		gc.setFont(new Font(display, "Noto Sans", 24, SWT.NORMAL));

		String[] drdr_text = new String[] {"ここに", "ドラドロするか", "", "クリックして", "ファイルを", "選択して"};
		int total_text_height = 0;
		for (String text : drdr_text) {
			Point size = gc.textExtent(text);
			total_text_height += size.y;
		}

		int y = (drdr_height - total_text_height) / 2;
		for (String text:drdr_text) {
			Point size = gc.textExtent(text);
			int x = (drdr_width - size.x) / 2;
			gc.drawText(text, x, y, true);
			y += size.y;
		}

		gc.dispose();

		Label drdr_label = new Label(shell, SWT.NONE);
		drdr_label.setImage(drdr_image);
		drdr_label.setLayoutData(new GridData(GridData.FILL_VERTICAL));

		//ドラドロするためのやつ
		DropTarget target = new DropTarget(drdr_label, DND.DROP_COPY | DND.DROP_MOVE);
		target.setTransfer(new Transfer[] {FileTransfer.getInstance()});
		target.addDropListener(new DropTargetAdapter() {
			@Override
			public void drop(DropTargetEvent e) {
				if (!FileTransfer.getInstance().isSupportedType(e.currentDataType)) return;

				List<File> list = new ArrayList<>();
				String[] file_list = (String[]) e.data;
				for (String file:file_list) {
					File f = new File(file);
					if (!f.exists()) throw new RuntimeException("は？");
					list.add(f);
				}

				file_load(list, shell);
			}
		});

		//クリックでダイアログ表示
		drdr_label.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				FileDialog dialog = new FileDialog(shell, SWT.MULTI);
				dialog.setText("入力ファイルを選択");
				dialog.setFilterExtensions(new String[] {"*.*"});

				String first = dialog.open();
				if (first == null) return;

				List<File> list = new ArrayList<>();
				String dir = dialog.getFilterPath();
				String[] files = dialog.getFileNames();
				for (String file:files) {
					File f = new File(dir + "/" + file);
					if (!f.exists()) throw new RuntimeException("は？");
					list.add(f);
				}

				file_load(list, shell);
			}
		});

		//ジョブ一覧
		job_table = new Table(shell, SWT.BORDER);
		job_table.setHeaderVisible(false);
		job_table.setLinesVisible(false);
		job_table.setLayoutData(new GridData(GridData.FILL_VERTICAL | GridData.FILL_HORIZONTAL));

		TableColumn col = new TableColumn(job_table, SWT.NONE);
		col.setWidth(200);

		shell.layout();
		shell.open();
	}

	private void file_load(List<File> file_list, Shell shell) {
		boolean single_setting = true;

		//複数個選択している？
		if (file_list.size() != 1) {
			MessageBox mb = new MessageBox(shell, SWT.ICON_QUESTION | SWT.YES | SWT.NO);
			mb.setText("複数ファイルの変換");
			mb.setMessage(file_list.size()+"個のファイルが選択されました。\nすべてのファイルに同じ設定の変換を行いますか？");
			int result = mb.open();
			if (result == SWT.NO) {
				single_setting = false;
			}
		}

		if (single_setting) {
			ConvertSettingForm form = new ConvertSettingForm(shell);
			new Thread(new Runnable() {
				@Override
				public void run() {
					String args = form.open();
					if (args == null) return;

					for (File file:file_list) {
						processing(file, args);
					}
				}
			}).start();
		} else {
			new Thread(new Runnable() {
				@Override
				public void run() {
					HashMap<File, String> list = new HashMap<>();

					for (File file:file_list) {
						ConvertSettingForm form = new ConvertSettingForm(shell);
						String args = form.open();
						if (args == null) return;
						list.put(file, args);
					}

					for (File file:list.keySet()) {
						processing(file, list.get(file));
					}
				}
			}).start();
		}
	}

	private void processing(File file, String args) {
		String tmp_file = "/tmp/" + UUID.randomUUID().toString();

		VideoProbe probe = FFProbe.show(file.getPath());

		//コマンドライン引数を処理
		List<String> cmd = new ArrayList<>();
		cmd.add("/usr/bin/ffmpeg");
		for (String arg:args.split(" ")) {
			if (arg.equals("%input%")) {
				cmd.add(file.getPath());
			} else if (arg.startsWith("%output%")) {
				cmd.add(arg.replaceFirst("%output%", tmp_file));
			} else {
				cmd.add(arg);
			}
		}

		display.asyncExec(new Runnable() {
			@Override
			public void run() {
				TableItem item = new TableItem(job_table, SWT.NONE);
				Composite composite = new Composite(job_table, SWT.NONE);

				GridLayout layout = new GridLayout(2, false);
				composite.setLayout(layout);

				Label status_label = new Label(composite, SWT.NONE);
				status_label.setText("待機中");
				GridData status_label_ld = new GridData();
				status_label_ld.verticalSpan = 2;
				status_label.setLayoutData(status_label_ld);

				Label name_label = new Label(composite, SWT.NONE);
				name_label.setText(file.getName());
				name_label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

				ProgressBar progressbar = new ProgressBar(composite, SWT.SMOOTH);
				progressbar.setMaximum(100);
				progressbar.setSelection(0);
				progressbar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

				TableEditor table_editor = new TableEditor(job_table);
				table_editor.grabHorizontal = true;
				table_editor.minimumHeight = composite.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
				table_editor.minimumWidth = composite.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
				table_editor.setEditor(composite, item, 0);

				job_table.layout(true, true);
				composite.layout(true);

				Main.jw.add_job(new Runnable() {
					@Override
					public void run() {
						try {
							display.asyncExec(new Runnable() {
								@Override
								public void run() {
									status_label.setText("処理中");
								}
							});

							//FFMPEG起動
							ProcessBuilder pb = new ProcessBuilder(cmd);
							pb.redirectErrorStream(true);
							Process p = pb.start();

							//標準出力/標準エラーを読む
							InputStream is = p.getInputStream();
							BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
							String line;
							while ((line = br.readLine()) != null) {
								if (line.startsWith("frame=")) {
									Matcher m = Pattern.compile("time=([0-9:.]+)").matcher(line);
									if (m.find()) {
										String time = m.group(1);
										String[] parts = time.split(":");
										if (parts.length != 3) return;

										//FFMPEGのtime=を解析
										int hour = Integer.parseInt(parts[0]);
										int minute = Integer.parseInt(parts[1]);
										double second = Double.parseDouble(parts[2]);
										double parsed_time = hour * 3600 + minute * 60 + second;

										//進捗
										int progress = (int)Math.floor((parsed_time / probe.duration) * 100);
										display.asyncExec(new Runnable() {
											@Override
											public void run() {
												progressbar.setSelection(progress);
											}
										});
									}
								}
							}

							int code = p.waitFor();

							//tmpファイルを取得
							File processed_file = get_processed_file(tmp_file);
							if (processed_file == null) return;

							display.asyncExec(new Runnable() {
								@Override
								public void run() {
									progressbar.setSelection(100);

									if (code == 0) {
										try {
											Path src = processed_file.toPath();
											Path dst = Paths.get(file.getParent(), file.getName()+"_Re");

											Files.copy(src, dst);
											Files.delete(src);

											status_label.setText("完了");
										} catch (IOException ex) {
											ex.printStackTrace();
											status_label.setText("成功したが失敗");
										}
									} else {
										try { Files.delete(processed_file.toPath()); } catch (IOException ex) {}
										status_label.setText("失敗！");
									}
								}
							});
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					}
				});
			}
		});
	}

	private File get_processed_file(String tmp_file) throws IOException {
		DirectoryStream<Path> stream = Files.newDirectoryStream(Path.of("/tmp"), new File(tmp_file).getName() + "*");
		try {
			for (Path entry:stream) {
				return entry.toFile();
			}

			return null;
		} finally {
			stream.close();
		}
	}
}
