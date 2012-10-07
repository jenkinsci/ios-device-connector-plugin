package org.jenkinsci.plugins.iosdevlist;

import hudson.FilePath;
import hudson.Launcher.LocalLauncher;
import hudson.Util;
import hudson.model.TaskListener;
import hudson.remoting.Callable;

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
    private final FilePath ipa;
    private final TaskListener listener;
    private final String deviceId;
    private final FilePath rootPath;

    DeployTask(iOSDevice device, File ipa, TaskListener listener) {
        this.ipa = new FilePath(ipa);
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

            listener.getLogger().println("Extracting "+ipa+" to "+t);

            ipa.unzip(new FilePath(t));
            FilePath payloadDir = new FilePath(t).child("Payload");
            List<FilePath> payload = payloadDir.listDirectories();
            if (payload==null || payload.isEmpty())
                throw new IOException("Malformed IPA file: "+ipa);
            FilePath appDir = payload.get(0);

            int exit = new LocalLauncher(listener).launch().cmds(
                    fruitstrap.getRemote(), "-i", deviceId, "-b", appDir.getName()).stdout(listener).pwd(payloadDir).join();
            if (exit!=0)
                throw new IOException("Deployment of "+ipa+" failed: "+exit);

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
