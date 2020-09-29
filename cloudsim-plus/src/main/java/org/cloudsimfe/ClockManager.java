package org.cloudsimfe;

import java.util.*;
import java.util.stream.Collectors;

public class ClockManager {

    private long clock;
    private Map<Clockable, Long> pllList;
    private int pllCount;
    private int maxCount;
    private Fpga fpga;

    public ClockManager(long clock) {
        this(clock, 1, new HashMap<>());
    }

    public ClockManager(long clock, int maxCount) {
        this(clock, maxCount, new HashMap<>());
    }

    private ClockManager(long clock, int maxCount, Map<Clockable, Long> pllList) {
        this.clock = clock;
        this.maxCount = maxCount;
        this.pllList = pllList;
        this.pllCount = 0;
    }

    public boolean acquireClockFor(Clockable clockable) {
        if (pllList.containsKey(clockable) || (pllList.size() == maxCount && !pllList.containsValue(clockable.getClockValue())))
            return false;
        long requestedClockValue = clockable.getClockValue();
        if (!pllList.containsValue(requestedClockValue) && requestedClockValue != clock)
            pllCount++;
        pllList.put(clockable, requestedClockValue);
        return true;
    }

    public boolean releaseClockFor(Clockable clockable) {
        if (!pllList.containsKey(clockable))
            return false;
        pllList.remove(clockable);
        long requestedClockValue = clockable.getClockValue();
        if (!pllList.containsValue(requestedClockValue) && requestedClockValue != clock)
            pllCount--;
        return true;
    }

    public void setClock(long clock) {
        this.clock = clock;
    }

    public void setMaxCount(int maxCount) {
        this.maxCount = maxCount;
    }

    @Override
    public String toString() {
        String string =
                "FPGA " + fpga.getId() + " ClockManager with master clock " + clock + " MHz has " + pllCount + " out " +
                        "of " + maxCount + " PLLs " + "occupied\n";
        string += String.format("%10s  %s\n", "Clock(MHz)", "Components");
        string += "----------  ----------\n";

        Set<Long> uniqueValues = new LinkedHashSet<>();
        uniqueValues.addAll(pllList.values());

        for (Iterator<Long> iterator = uniqueValues.iterator(); iterator.hasNext(); ) {
            long clockValue = iterator.next();
            List<Clockable> components = pllList.entrySet()
                    .stream()
                    .filter(entry -> Objects.equals(entry.getValue(), clockValue))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
            String componentString = "";
            for (int i = 0; i < components.size(); i++)
                componentString += components.get(i).getComponentId() + ", ";
            componentString = componentString.substring(0, componentString.length() - 2);
            string += String.format("%10s  %s\n", clockValue, componentString);
        }
        return string;
    }

    public Fpga getFpga() {
        return fpga;
    }

    public void setFpga(Fpga fpga) {
        this.fpga = fpga;
    }
}
