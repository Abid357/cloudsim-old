package org.cloudsimfe;

public class Mapper {
    int vFpgaId;
    int blockInHypervisor;
    int blockInFabric;

    public Mapper(int vFpgaId, int blockInHypervisor) {
        this.vFpgaId = vFpgaId;
        this.blockInHypervisor = blockInHypervisor;
        this.blockInFabric = -1;
    }

    public int getvFpgaId() {
        return vFpgaId;
    }

    public void setvFpgaId(int vFpgaId) {
        this.vFpgaId = vFpgaId;
    }

    public int getBlockInHypervisor() {
        return blockInHypervisor;
    }

    public void setBlockInHypervisor(int blockInHypervisor) {
        this.blockInHypervisor = blockInHypervisor;
    }

    public int getBlockInFabric() {
        return blockInFabric;
    }

    public void setBlockInFabric(int blockInFabric) {
        this.blockInFabric = blockInFabric;
    }

    @Override
    public String toString() {
        return "Mapper{" +
                "vFpgaId=" + vFpgaId +
                ", blockInHypervisor=" + blockInHypervisor +
                ", blockInFabric=" + blockInFabric +
                '}';
    }
}
