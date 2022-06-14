
public class Task {

	/*
	 * task ID: starts from 1 to K where K is total number of tasks
	 */
	private int id;

	/*
	 * CLBs: number of required configurable logic blocks of the task
	 */
	private int clb;

	/*
	 * task execution time: number of time units required by the task
	 */
	private int execTime;

	/*
	 * deadline: time unit constraint by which the task should finish execution
	 */
	private int deadline;

	public Task(int id, int clb, int execTime, int deadline) {
		super();
		this.id = id;
		this.clb = clb;
		this.execTime = execTime;
		this.deadline = deadline;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getExecTime() {
		return execTime;
	}

	public void setExecTime(int execTime) {
		this.execTime = execTime;
	}

	public int getClb() {
		return clb;
	}

	public void setClb(int clb) {
		this.clb = clb;
	}

	public int getDeadline() {
		return deadline;
	}

	public void setDeadline(int deadline) {
		this.deadline = deadline;
	}

	@Override
	public String toString() {
		return "<" + id + ", " + clb + ", " + execTime + ", " + deadline + ">";
	}
}