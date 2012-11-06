package org.jenkinsci.plugins.ios.connector;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.sun.jna.Platform;
import hudson.Extension;
import hudson.Launcher.LocalLauncher;
import hudson.model.Computer;
import hudson.model.ModelObject;
import hudson.model.RootAction;
import hudson.model.TaskListener;
import hudson.remoting.Callable;
import hudson.security.Permission;
import hudson.security.PermissionGroup;
import hudson.util.IOException2;
import hudson.util.StreamTaskListener;
import jenkins.model.Jenkins;
import org.apache.commons.io.FileUtils;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.interceptor.RequirePOST;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.Future;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static hudson.util.jna.GNUCLibrary.*;

/**
 * Maintains the list of iOS Devices connected to all the slaves.
 *
 * @author Kohsuke Kawaguchi
 */
@Extension
public class iOSDeviceList implements RootAction, ModelObject {
    private volatile Multimap<Computer,iOSDevice> devices = LinkedHashMultimap.create();

    /**
     * List of all the devices.
     */
    public Multimap<Computer,iOSDevice> getDevices() {
        return Multimaps.unmodifiableMultimap(devices);
    }

    /**
     * Refresh all slaves in concurrently.
     */
    public void updateAll(TaskListener listener) {
        Jenkins.getInstance().checkPermission(Jenkins.ADMINISTER);

        Map<Future<List<iOSDevice>>,Computer> futures = newHashMap();

        for (Computer c : Jenkins.getInstance().getComputers()) {
            try {
                futures.put(c.getChannel().callAsync(new FetchTask(listener)), c);
            } catch (Exception e) {
                e.printStackTrace(listener.error("Failed to list up iOS devices on"+c.getName()));
            }
        }

        Multimap<Computer,iOSDevice> devices = LinkedHashMultimap.create();
        for (Entry<Future<List<iOSDevice>>, Computer> e : futures.entrySet()) {
            Computer c = e.getValue();
            try {
                List<iOSDevice> devs = e.getKey().get();
                for (iOSDevice d : devs)
                    d.computer = c;
                devices.putAll(c, devs);
            } catch (Exception x) {
                x.printStackTrace(listener.error("Failed to list up iOS devices on "+c.getName()));
            }
        }

        this.devices = devices;
    }

    public void update(Computer c, TaskListener listener) {
        Jenkins.getInstance().checkPermission(Jenkins.ADMINISTER);

        List<iOSDevice> r = Collections.emptyList();
        if (c.isOnline()) {// ignore disabled slaves
            try {
                r = c.getChannel().call(new FetchTask(listener));
                for (iOSDevice dev : r) dev.computer = c;
            } catch (Exception e) {
                e.printStackTrace(listener.error("Failed to list up iOS devices"));
            }
        }

        synchronized (this) {
            Multimap<Computer,iOSDevice> clone = LinkedHashMultimap.create(devices);
            clone.removeAll(c);
            clone.putAll(c, r);
            devices = clone;
        }
    }

    public synchronized void remove(Computer c) {
        Multimap<Computer,iOSDevice> clone = LinkedHashMultimap.create(devices);
        clone.removeAll(c);
        devices = clone;
    }

    public String getIconFileName() {
        return "/plugin/ios-device-connector/icons/24x24/iphone.png";
    }

    public String getDisplayName() {
        return "Connected iOS Devices";
    }

    public String getUrlName() {
        if (Jenkins.getInstance().hasPermission(READ))
            return "ios-devices";
        else
            return null;
    }

    @RequirePOST
    public HttpResponse doRefresh() {
        updateAll(StreamTaskListener.NULL);
        return HttpResponses.redirectToDot();
    }

    /**
     * Maps {@link iOSDevice} to URL space.
     */
    public iOSDevice getDynamic(String token) {
        return getDevice(token);
    }

    public iOSDevice getDevice(String udid) {
        for (iOSDevice dev : devices.values())
            if (udid.equalsIgnoreCase(dev.getUniqueDeviceId()) || udid.equals(dev.getDeviceName()))
                return dev;
        return null;
    }

    /**
     * Retrieves {@link iOSDevice}s connected to a machine.
     */
    private static class FetchTask implements Callable<List<iOSDevice>,IOException> {
        private final TaskListener listener;

        private FetchTask(TaskListener listener) {
            this.listener = listener;
        }

        public List<iOSDevice> call() throws IOException {
            if (!Platform.isMac())
                return Collections.emptyList();

            File exe = File.createTempFile("ios","list");
            try {
                PrintStream logger = listener.getLogger();
                logger.println("Listing up iOS Devices");

                FileUtils.copyURLToFile(getClass().getResource("list"),exe);
                LIBC.chmod(exe.getAbsolutePath(),0755);

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                int exit = new LocalLauncher(listener).launch().cmds(exe).stdout(out).stderr(logger).join();
                if (exit!=0) {
                    logger.println(exe + " failed to execute:" + exit);
                    logger.write(out.toByteArray());
                    logger.println();
                    return Collections.emptyList();
                }

                return parseOutput(logger, out);
            } catch (InterruptedException e) {
                throw new IOException2("Interrupted while listing up devices",e);
            } finally {
                exe.delete();
            }
        }

        private List<iOSDevice> parseOutput(PrintStream logger, ByteArrayOutputStream out) throws IOException {
            List<iOSDevice> r = newArrayList();

            {// parse the output
                Properties p = new Properties();
                String line;
                BufferedReader in = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(out.toByteArray()),"UTF-8"));
                while ((line=in.readLine())!=null) {
                    if (line.length()==0) {
                        r.add(new iOSDevice(p));
                        p.clear();
                        continue;
                    }
                    int idx = line.indexOf('=');
                    if (idx<0) {
                        logger.println("Invalid output line:"+line);
                        logger.write(out.toByteArray());
                        logger.println();
                        return Collections.emptyList();
                    }
                    p.put(line.substring(0,idx),line.substring(idx+1));
                }
            }

            return r;
        }
    }

    public static final PermissionGroup GROUP = new PermissionGroup(iOSDeviceList.class,Messages._iOSDeviceList_PermissionGroup_Title());
    public static final Permission READ = new Permission(GROUP,"Read",Messages._iOSDeviceList_ReadPermission(),Jenkins.READ);
    public static final Permission DEPLOY = new Permission(GROUP,"Deploy",Messages._iOSDeviceList_DeployPermission(),Jenkins.ADMINISTER);
}
