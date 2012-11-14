package org.jenkinsci.plugins.ios.connector;

import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
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
import java.util.Map;

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

        // Expand matrix and build variables in the device ID and command line args
        final String device = expandVariables(build, listener, udid);

        iOSDevice dev = devices.getDevice(device);
        if (dev == null) {
            throw new AbortException("No such device: "+device);
        }

        FilePath ws = build.getWorkspace();
        FilePath[] files = ws.child(path).exists() ? new FilePath[]{ws.child(path)} : ws.list(path);
        if (files.length == 0) {
            listener.getLogger().println("No iOS apps found to deploy!");
            return false;
        }

        for (FilePath bundle : files) {
            // Make sure we're being passed an iOS app, in some form
            String name = bundle.getName();
            int idx = name.lastIndexOf('.');
            if (idx < 0) {
                listener.getLogger().printf("Ignoring '%s'; expected either a .app or .ipa bundle\n", name);
                continue;
            }

            listener.getLogger().printf("Deploying iOS app: %s\n", name);
            dev.deploy(new File(bundle.getRemote()), listener);
        }
        return true;
    }

    private static String expandVariables(AbstractBuild<?, ?> build, BuildListener listener, String token) {
        Map<String, String> vars = build.getBuildVariables();
        try {
            vars.putAll(build.getEnvironment(listener));
        } catch (IOException e) {
            return null;
        } catch (InterruptedException e) {
            return null;
        }

        String result = Util.fixEmptyAndTrim(token);
        if (result != null) {
            result = Util.replaceMacro(result, vars);
        }
        return result;
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
