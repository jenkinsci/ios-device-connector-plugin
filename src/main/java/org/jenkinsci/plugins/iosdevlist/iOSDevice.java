package org.jenkinsci.plugins.iosdevlist;

import hudson.model.Computer;
import hudson.model.ModelObject;
import hudson.model.TaskListener;
import hudson.util.StreamTaskListener;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.Properties;

/**
 * @author Kohsuke Kawaguchi
 */
public class iOSDevice implements Serializable, ModelObject {
    /**
     * Which computer is this connected to?
     */
    /*package*/ transient Computer computer;

    private final Properties props = new Properties();

    public iOSDevice(Properties props) {
        this.props.putAll(props);
    }

    public Computer getComputer() {
        return computer;
    }

    public String getDisplayName() {
        return getDeviceName();
    }

    /**
     * The name of the device that the user set when activating the device,
     * such as "Kohsuke's iPhone"
     */
    public String getDeviceName() {
        return props.getProperty("DeviceName");
    }

    public String getUniqueDeviceId() {
        return props.getProperty("UniqueDeviceID");
    }

    public Properties getProps() {
        return props;
    }

    /**
     * Returns a human-readable name of the product type,
     * such as "iPhone 3GS", etc.
     */
    public String getProductTypeDisplayName() {
        String name = props.getProperty("ProductType");
        for (int i=0; i<PRODUCT_TYPE_MAP.length; i+=2) {
            if (name.startsWith(PRODUCT_TYPE_MAP[i]))
                return PRODUCT_TYPE_MAP[i+1];
        }
        return name; // unmapped
    }

    /**
     * Deploys the *.ipa file to this device.
     */
    public void deploy(File ipa, TaskListener listener) throws IOException, InterruptedException {
        computer.getChannel().call(new DeployTask(this,ipa,listener));
        computer.getChannel().syncLocalIO();    // TODO: verify if needed
    }

    public HttpResponse doDoDeploy(StaplerRequest req) throws IOException {
        File f = File.createTempFile("jenkins","ipa");
        StringWriter w = new StringWriter();
        try {
            req.getFileItem("ipa").write(f);
            deploy(f,new StreamTaskListener(w));
            return HttpResponses.forwardToView(this,"ok").with("msg",w.toString());
        } catch (Exception e) {
            // failed to deploy
            throw HttpResponses.error(StaplerResponse.SC_INTERNAL_SERVER_ERROR,
                    new Error("Failed to deploy app: "+w,e));
        } finally {
            f.delete();
        }
    }

    /**
     * Human readable product name from internal code.
     */
    private static String[] PRODUCT_TYPE_MAP = {
            "iPhone1,",     "iPhone 3G",
            "iPhone2,",     "iPhone 3GS",
            "iPhone3,",     "iPhone 4",
            "iPhone4,",     "iPhone 4S",
            "iPhone5,",     "iPhone 5",

            "iPad1,",       "iPad 1",
            "iPad2,",       "iPad 2",
            "iPad3,",       "iPad 3"
    };

    private static final long serialVersionUID = 1L;
}
