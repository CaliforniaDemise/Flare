package com.cleanroommc.flare.common.command.sub.component.tick;

import com.cleanroommc.flare.api.FlareAPI;
import com.cleanroommc.flare.common.command.sub.FlareSubCommand;
import com.cleanroommc.flare.common.component.tick.TickMonitor;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;

import java.util.Arrays;
import java.util.List;

public class TickMonitoringCommand extends FlareSubCommand {

    /** The tick hook instance currently running, if any */
    private TickMonitor activeTickMonitor = null;

    public TickMonitoringCommand(FlareAPI flare) {
        super(flare);
    }

    @Override
    public String getName() {
        return "tickmonitor";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("tickmonitoring", "tm");
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/flare tickmonitor [--percentage-threshold (integer)], [--tick-duration (integer)], [--without-gc]";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {

    }

}
