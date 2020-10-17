/*
 * CloudSim Plus: A modern, highly-extensible and easier-to-use Framework for
 * Modeling and Simulation of Cloud Computing Infrastructures and Services.
 * http://cloudsimplus.org
 *
 *     Copyright (C) 2015-2018 Universidade da Beira Interior (UBI, Portugal) and
 *     the Instituto Federal de Educação Ciência e Tecnologia do Tocantins (IFTO, Brazil).
 *
 *     This file is part of CloudSim Plus.
 *
 *     CloudSim Plus is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     CloudSim Plus is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with CloudSim Plus. If not, see <http://www.gnu.org/licenses/>.
 */
package org.cloudsimfe;

import org.cloudbus.cloudsim.core.Identifiable;
import org.cloudsimplus.builders.tables.Table;
import org.cloudsimplus.builders.tables.TableBuilderAbstract;
import org.cloudsimplus.builders.tables.TableColumn;
import org.cloudsimplus.builders.tables.TextTable;

import java.util.Comparator;
import java.util.List;

/**
 * Builds a table for printing simulation results from a list of AccelerableCloudlets.
 * It defines a set of default columns but new ones can be added
 * dynamically using the {@code addColumn()} methods.
 *
 * <p>The basic usage of the class is by calling its constructor,
 * giving a list of AccelerableCloudlets to be printed, and then
 * calling the {@link #build()} method.</p>
 *
 * @author Manoel Campos da Silva Filho
 * @since CloudSim Plus 1.0
 */
public class AccelerableCloudletsTableBuilder extends TableBuilderAbstract<AccelerableCloudlet> {
    private static final String TIME_FORMAT = "%.5f";
    private static final String SECONDS = "Seconds";

    /**
     * Instantiates a builder to print the list of AccelerableCloudlets using the a
     * default {@link TextTable}.
     * To use a different {@link Table}, check the alternative constructors.
     *
     * @param list the list of AccelerableCloudlets to print
     */
    public AccelerableCloudletsTableBuilder(final List<? extends AccelerableCloudlet> list) {
        super(list);
    }

    /**
     * Instantiates a builder to print the list of AccelerableCloudlets using the a
     * given {@link Table}.
     *
     * @param list  the list of AccelerableCloudlets to print
     * @param table the {@link Table} used to build the table with the AccelerableCloudlets data
     */
    public AccelerableCloudletsTableBuilder(final List<? extends AccelerableCloudlet> list, final Table table) {
        super(list, table);
    }

    @Override
    protected void createTableColumns() {
        final String ID = "ID";
        addColumnDataFunction(getTable().addColumn("Cloudlet", ID), Identifiable::getId);
        addColumnDataFunction(getTable().addColumn("Acc.Segments", "Count"), cloudlet -> cloudlet.getSegments().size());
        addColumnDataFunction(getTable().addColumn("Acc.Length", "MI"),
                cloudlet -> cloudlet.getSegments().stream().mapToLong(AccelerableSegment::getLength).sum());
        addColumnDataFunction(getTable().addColumn("NAcc.Length", "MI"),
                cloudlet -> cloudlet.getLength() - cloudlet.getSegments().stream().mapToLong(AccelerableSegment::getLength).sum());

        addColumnDataFunction(getTable().addColumn("Accelerated?", "Yes/No"),
                cloudlet -> cloudlet.getSegments().stream().allMatch(segment -> segment.getExecution() == null) ?
                        "No" :
                        "Yes");

        Comparator<AccelerableSegment> startTimeComparator =
                Comparator.comparingDouble(s -> s.getExecution().getSegmentArrivalTime());
        Comparator<AccelerableSegment> finishTimeComparator =
                Comparator.comparingDouble(s -> s.getExecution().getFinishTime());

        TableColumn col = getTable().addColumn("Acc.ExecTime", SECONDS).setFormat(TIME_FORMAT);
        addColumnDataFunction(col,
                cloudlet -> cloudlet.getSegments().stream().anyMatch(segment -> segment.getExecution() == null) ? 0 :
                        cloudlet.getSegments().stream().max(finishTimeComparator).get().getExecution().getFinishTime() -
                                cloudlet.getSegments().stream().min(startTimeComparator).get().getExecution().getSegmentArrivalTime());

        col = getTable().addColumn("NAcc.ExecTime", SECONDS).setFormat(TIME_FORMAT);
        addColumnDataFunction(col,
                cloudlet -> cloudlet.getSegments().stream().anyMatch(segment -> segment.getExecution() == null) ? 0 :
                        cloudlet.getNonAccelerableSegments().getActualCpuTime());

        col = getTable().addColumn("TotalExecTime", SECONDS).setFormat(TIME_FORMAT);
        addColumnDataFunction(col,
                cloudlet -> cloudlet.getSegments().stream().anyMatch(segment -> segment.getExecution() == null) ?
                        cloudlet.getActualCpuTime() :
                        cloudlet.getSegments().stream().max(finishTimeComparator).get().getExecution().getFinishTime() -
                        cloudlet.getSegments().stream().min(startTimeComparator).get().getExecution().getSegmentArrivalTime() + cloudlet.getNonAccelerableSegments().getActualCpuTime());
    }

    /**
     * Rounds a given time so that decimal places are ignored.
     * Sometimes a AccelerableCloudlet start at time 0.1 and finish at time 10.1.
     * Previously, in such a situation, the finish time was rounded to 11 (Math.ceil),
     * giving the wrong idea that the AccelerableCloudlet took 11 seconds to finish.
     * This method makes some little adjustments to avoid such a precision issue.
     *
     * @param AccelerableCloudlet the AccelerableCloudlet being printed
     * @param time                the time to round
     * @return
     */
    private double roundTime(final AccelerableCloudlet AccelerableCloudlet, final double time) {
        final double fraction = AccelerableCloudlet.getExecStartTime() - (int) AccelerableCloudlet.getExecStartTime();
        return Math.round(time - fraction);
    }
}
