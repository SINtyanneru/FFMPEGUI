package su.rumishistem.ffmpegui.Module;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class JobWorker {
	private ThreadPoolExecutor job_worker;
	private List<Runnable> update_event_listener = new ArrayList<>();

	public JobWorker(int process) {
		job_worker = (ThreadPoolExecutor) Executors.newFixedThreadPool(process);
	}

	public int get_active_job() {
		return job_worker.getActiveCount();
	}

	public int get_queue_job() {
		return job_worker.getQueue().size();
	}

	public int get_end_job() {
		return (int) job_worker.getCompletedTaskCount();
	}

	public void shutdown() {
		job_worker.shutdownNow();
	}

	public void add_event_listener(Runnable listener) {
		update_event_listener.add(listener);
	}

	public void add_job(Runnable task) {
		job_worker.execute(new Runnable() {
			@Override
			public void run() {
				task.run();

				call_update_event_listener();
			}
		});
	}

	private void call_update_event_listener() {
		for (Runnable listener:update_event_listener) {
			listener.run();
		}
	}
}
