package org.jenkinsci.plugins.ios.connector;

import hudson.FilePath;
import hudson.Launcher.LocalLauncher;
import hudson.Launcher.ProcStarter;
import hudson.Util;
import hudson.model.TaskListener;
import hudson.remoting.Callable;
import hudson.util.ArgumentListBuilder;

import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.List;

/**
 * Deploys *.ipa to the device.
 *
 * @author Kohsuke Kawaguchi
 */
class DeployTask implements Callable<Void, IOException> {

    private final FilePath bundle;
    private final String cmdLineArgs;
    private final TaskListener listener;
    private final String deviceId;
    private final FilePath rootPath;

    DeployTask(iOSDevice device, File bundle, String cmdLineArgs, TaskListener listener) {
        this.bundle = new FilePath(bundle);
        this.cmdLineArgs = cmdLineArgs;
        this.listener = listener;
        this.deviceId = device.getUniqueDeviceId();
        this.rootPath = device.getComputer().getNode().getRootPath();
    }

    public Void call() throws IOException {
        File t = Util.createTempDir();
        try {
            FilePath fruitstrap = rootPath.child("fruitstrap");
            if (!fruitstrap.exists() || !fruitstrap.digest().equals(FRUITSTRAP_DIGEST)) {
                listener.getLogger().println("Extracting fruitstrap to "+fruitstrap);
                fruitstrap.copyFrom(DeployTask.class.getResource("fruitstrap"));
                fruitstrap.chmod(0755);
            }

            listener.getLogger().println("Copying "+ bundle +" to "+ t);

            // Determine what type of file was passed
            FilePath appDir;
            FilePath tmpDir = new FilePath(t);
            final String filename = bundle.getName();
            if (filename.toLowerCase().endsWith(".ipa")) {
                listener.getLogger().println("Extracting .app from .ipa file...");
                bundle.unzip(tmpDir);
                FilePath payloadDir = tmpDir.child("Payload");
                List<FilePath> payload = payloadDir.listDirectories();
                if (payload==null || payload.isEmpty())
                    throw new IOException("Malformed IPA file: "+bundle);
                appDir = payload.get(0);
            } else if (filename.toLowerCase().endsWith(".app")) {
                appDir = tmpDir.child(filename);
                bundle.copyRecursiveTo(appDir);
            } else {
                throw new IOException("Expected either a .app or .ipa bundle!");
            }

            ArgumentListBuilder arguments = new ArgumentListBuilder(fruitstrap.getRemote());
            arguments.add("--id", deviceId, "--bundle", appDir.getName());
            arguments.addTokenized(cmdLineArgs);

            ProcStarter proc = new LocalLauncher(listener).launch()
                    .cmds(arguments)
                    .stdout(listener)
                    .pwd(appDir.getParent());
            int exit = proc.join();
            if (exit!=0)
                throw new IOException("Deployment of "+bundle+" failed: "+exit);

            return null;
        } catch (InterruptedException e) {
            throw (IOException)new InterruptedIOException().initCause(e);
        } finally {
            Util.deleteRecursive(t);
            listener.getLogger().flush();
        }
    }

    private static final String FRUITSTRAP_DIGEST;

    static {
        try {
            FRUITSTRAP_DIGEST = Util.getDigestOf(DeployTask.class.getResourceAsStream("fruitstrap"));
        } catch (IOException e) {
            throw new Error("Failed to compute the digest of fruitstrap",e);
        }
    }
}
