package org.cloudsimfe;


public class ConfigurationTask {

    public static int TASK_ID = 1;

    /*
     * task ID: starts from 1 to K where K is total number of tasks
     * This becomes vFPGA ID during vFPGA creation
     */
    private int id;

    /**
     * Unique ID of the accelerator that owns this configuration task instance
     */
    private int acceleratorId;

    private int fpgaId;

    /*
     * tiles: number of required configurable blocks of the task
     */
    private int tile;

    /*
     * task execution time: number of time units required by the task
     */
    private int executionTime;

    /*
     * deadline: time unit constraint by which the task should finish execution
     */
    private int deadline;

    public ConfigurationTask(int id, int acceleratorId, int tile, int executionTime, int deadline) {
        super();
        this.id = id;
        this.acceleratorId = acceleratorId;
        this.tile = tile;
        this.executionTime = executionTime;
        this.deadline = deadline;
    }

    public int getAcceleratorId() {
        return acceleratorId;
    }

    public void setAcceleratorId(int acceleratorId) {
        this.acceleratorId = acceleratorId;
    }

    public int getFpgaId() {
        return fpgaId;
    }

    public void setFpgaId(int fpgaId) {
        this.fpgaId = fpgaId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(int executionTime) {
        this.executionTime = executionTime;
    }

    public int getTile() {
        return tile;
    }

    public void setTile(int tile) {
        this.tile = tile;
    }

    public int getDeadline() {
        return deadline;
    }

    public void setDeadline(int deadline) {
        this.deadline = deadline;
    }

    @Override
    public String toString() {
        return "<" + id + ", " + tile + ", " + executionTime + ", " + deadline + ">";
    }
}

