package org.cloudsimfe;/*
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

import org.cloudsimplus.builders.tables.Table;
import org.cloudsimplus.builders.tables.TableBuilderAbstract;
import org.cloudsimplus.builders.tables.TextTable;

import java.util.List;

/**
 * Builds a table for printing simulation results from a list of Cloudlets.
 * It defines a set of default columns but new ones can be added
 * dynamically using the {@code addColumn()} methods.
 *
 * <p>The basic usage of the class is by calling its constructor,
 * giving a list of Cloudlets to be printed, and then
 * calling the {@link #build()} method.</p>
 *
 * @author Manoel Campos da Silva Filho
 * @since CloudSim Plus 1.0
 */
public class ResourceUtilizationTableBuilder extends TableBuilderAbstract<VFpga> {
    private static final String TIME_FORMAT = "%.5f";
    private static final String PERCENT_FORMAT = "%d(%.2f%%)";
    private static final String SECONDS = "Seconds";

    /**
     * Instantiates a builder to print the list of Cloudlets using the a
     * default {@link TextTable}.
     * To use a different {@link Table}, check the alternative constructors.
     *
     * @param list the list of Cloudlets to print
     */
    public ResourceUtilizationTableBuilder(final List<? extends VFpga> list) {
        super(list);
    }

    /**
     * Instantiates a builder to print the list of Cloudlets using the a
     * given {@link Table}.
     *
     * @param list  the list of Cloudlets to print
     * @param table the {@link Table} used to build the table with the Cloudlets data
     */
    public ResourceUtilizationTableBuilder(final List<? extends VFpga> list, final Table table) {
        super(list, table);
    }

    @Override
    protected void createTableColumns() {
        final String ID = "ID";
        addColumnDataFunction(getTable().addColumn("vFPGA", "ID"),
                vFpga -> vFpga.getId());
        addColumnDataFunction(getTable().addColumn("Segment", "UID"),
                vFpga -> vFpga.getSegmentExecutionList().size() > 1 ? "(many)" :
                        vFpga.getSegmentExecutionList().size() == 0 ? "" :
                                vFpga.getSegmentExecutionList().get(0).getSegment().getUniqueId());
        addColumnDataFunction(getTable().addColumn("  SegmentType   "),
                vFpga -> vFpga.getSegmentExecutionList().size() > 1 ? "(many)" :
                        vFpga.getSegmentExecutionList().size() == 0 ? "" :
                                vFpga.getSegmentExecutionList().get(0).getSegment().getAccelerator().getTypeInString());

        addColumnDataFunction(getTable().addColumn("Regions", "Count"),
                vFpga -> vFpga.getRegions().size());

        addColumnDataFunction(getTable().addColumn("FPGA", ID),
                vFpga -> vFpga.getManager().getFpga().getId());

        addColumnDataFunction(getTable().addColumn(" LogicElements "),
                vFpga -> String.format(PERCENT_FORMAT,
                        vFpga.getRegions().stream().mapToLong(Region::getUtilizedLogicElements).sum(),
                        100.0 * vFpga.getRegions().stream().mapToLong(Region::getUtilizedLogicElements).sum() / vFpga.getManager().getFpga().getLogicElements()));

        addColumnDataFunction(getTable().addColumn("MemoryRegisters"),
                vFpga -> String.format(PERCENT_FORMAT,
                        vFpga.getRegions().stream().mapToLong(Region::getUtilizedMemoryRegisters).sum(),
                        100.0 * vFpga.getRegions().stream().mapToLong(Region::getUtilizedMemoryRegisters).sum() / vFpga.getManager().getFpga().getMemory()));

        addColumnDataFunction(getTable().addColumn("     DSPs    "),
                vFpga -> String.format(PERCENT_FORMAT,
                        vFpga.getRegions().stream().mapToLong(Region::getUtilizedDspSlices).sum(),
                        100.0 * vFpga.getRegions().stream().mapToLong(Region::getUtilizedDspSlices).sum() / vFpga.getManager().getFpga().getDspSlices()));

        addColumnDataFunction(getTable().addColumn(" BlockedRAMs "),
                vFpga -> String.format(PERCENT_FORMAT,
                        vFpga.getRegions().stream().mapToLong(Region::getUtilizedBlockedRams).sum(),
                        100.0 * vFpga.getRegions().stream().mapToLong(Region::getUtilizedBlockedRams).sum() / vFpga.getManager().getFpga().getBlockedRams()));

        addColumnDataFunction(getTable().addColumn("   I/OPins   "),
                vFpga -> String.format(PERCENT_FORMAT,
                        vFpga.getRegions().stream().mapToLong(Region::getUtilizedIoPins).sum(),
                        100.0 * vFpga.getRegions().stream().mapToLong(Region::getUtilizedIoPins).sum() / vFpga.getManager().getFpga().getIo()));

        addColumnDataFunction(getTable().addColumn("Transceivers"),
                vFpga -> String.format(PERCENT_FORMAT,
                        vFpga.getRegions().stream().mapToLong(Region::getUtilizedTransceivers).sum(),
                        100.0 * vFpga.getRegions().stream().mapToLong(Region::getUtilizedTransceivers).sum() / vFpga.getManager().getFpga().getTransceivers()));

        addColumnDataFunction(getTable().addColumn(" PLL? ", "Yes/No"),
                vFpga -> vFpga.getManager().getFpga().getClock() == vFpga.getAccelerator().getClockValue() ? "Yes" :
                        "No");
    }
}
