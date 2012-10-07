package org.jenkinsci.plugins.ios.connector;

import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;

public class DeployBuilder extends Builder {
    public final String udid;
    public final String path;

    @Inject
    private transient iOSDeviceList devices;

    @DataBoundConstructor
    public DeployBuilder(String path,String udid) {
        this.path = path;
        this.udid = udid;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        Jenkins.getInstance().getInjector().injectMembers(this);

        iOSDevice dev = devices.getDevice(udid);
        if (dev==null)
            throw new AbortException("No such device: "+udid);

        FilePath ws = build.getWorkspace();
        FilePath[] files = ws.child(path).exists() ? new FilePath[]{ws.child(path)} : ws.list(path);
        for (FilePath ipa : files) {
            listener.getLogger().printf("Deploying %s", ipa);

            File t = File.createTempFile("jenkins", "ipa");
            try {
                ipa.copyTo(new FilePath(t));
                dev.deploy(t,listener);
            } finally {
                t.delete();
            }
        }
        return true;
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Builder> {
        @Override
        public String getDisplayName() {
            return "Deploy iOS App to device";
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }
    }
}
