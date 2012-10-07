package org.jenkinsci.plugins.iosdevlist.cli;

import hudson.Extension;
import hudson.cli.CLICommand;
import org.jenkinsci.plugins.iosdevlist.iOSDevice;
import org.jenkinsci.plugins.iosdevlist.iOSDeviceList;

import javax.inject.Inject;

@Extension
public class ListCommand extends CLICommand {
    @Inject
    iOSDeviceList devices;

    @Override
    public String getName() {
        return "ios-list-device";
    }

    @Override
    public String getShortDescription() {
        return "List iOS devices attached to this Jenkins cluster";
    }

    @Override
    protected int run() throws Exception {
        for (iOSDevice dev : devices.getDevices().values())
            stdout.printf("%s\t%s\n",dev.getUniqueDeviceId(),dev.getDeviceName());
        return 0;
    }
}
