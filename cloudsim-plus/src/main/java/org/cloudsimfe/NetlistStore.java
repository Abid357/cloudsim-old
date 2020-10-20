package org.cloudsimfe;

import java.util.ArrayList;
import java.util.List;

public class NetlistStore {

    private List<Netlist> netlists;

    public NetlistStore() {
        this.netlists = new ArrayList<>();
    }

    public void addNetlist(Netlist netlist){
        netlists.add(netlist);
    }

    public Netlist getNetlist(int id){
        return netlists.stream()
                .filter(netlist -> netlist.getAccelerator().getAcceleratorId() == id)
                .findFirst()
                .get()
                .copy();
    }

    public int hasAcceleratorType(int type){
        Netlist found =  netlists.stream()
                .filter(netlist -> netlist.getAccelerator().getType() == type)
                .findFirst()
                .orElse(null);
        return found == null ? -1 : found.getAccelerator().getAcceleratorId();
    }
}