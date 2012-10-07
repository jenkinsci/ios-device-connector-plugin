package org.jenkinsci.plugins.ios.connector.cli;

import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.cli.CLICommand;
import hudson.model.TaskListener;
import hudson.util.StreamTaskListener;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.ios.connector.iOSDevice;
import org.jenkinsci.plugins.ios.connector.iOSDeviceList;
import org.kohsuke.args4j.Argument;

import javax.inject.Inject;
import java.io.File;
import java.util.List;

/**
 * CLI command to deploy IPA files.
 */
@Extension
public class DeployCommand extends CLICommand {
    @Argument(metaVar="DEVICEID",
        usage="Unique device ID or device name",required=true)
    public String device;

    @Argument(index=1,metaVar="IPA",usage="*.ipa files to deploy",required=true)
    public List<String> files;

    @Inject
    iOSDeviceList devices;

    @Override
    public String getName() {
        return "ios-deploy-ipa";
    }

    @Override
    public String getShortDescription() {
        return "Deploy IPA files to iOS devices connected to Jenkins";
    }

    @Override
    protected int run() throws Exception {
        Jenkins.getInstance().getInjector().injectMembers(this);

        iOSDevice dev = devices.getDevice(device);
        if (dev==null)
            throw new AbortException("No such device found: "+device);

        TaskListener listener = new StreamTaskListener(stdout,getClientCharset());
        for (String ipa : files) {
            FilePath p = new FilePath(checkChannel(),ipa);
            listener.getLogger().println("Deploying "+ipa);
            File t = File.createTempFile("jenkins","ipa");
            try {
                p.copyTo(new FilePath(t));
                dev.deploy(t,listener);
            } finally {
                t.delete();
            }
        }
        return 0;
    }
}
