package org.jenkinsci.plugins.iosdevlist;

import hudson.Extension;
import hudson.model.Computer;
import hudson.model.TaskListener;
import hudson.slaves.ComputerListener;

import javax.inject.Inject;
import java.io.IOException;

/**
 * @author Kohsuke Kawaguchi
 */
@Extension
public class ComputerListenerImpl extends ComputerListener {
    @Inject
    private iOSDeviceList deviceList;

    @Override
    public void onOnline(Computer c, TaskListener listener) throws IOException, InterruptedException {
        deviceList.update(c,listener);
    }

    @Override
    public void onOffline(Computer c) {
        deviceList.remove(c);
    }
}
