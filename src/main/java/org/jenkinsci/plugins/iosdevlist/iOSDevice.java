package org.jenkinsci.plugins.iosdevlist;

import hudson.model.Computer;
import hudson.model.ModelObject;

import java.io.Serializable;
import java.util.Properties;

/**
 * @author Kohsuke Kawaguchi
 */
public class iOSDevice implements Serializable, ModelObject {
    /**
     * Which computer is this connected to?
     */
    /*package*/ Computer computer;

    private final Properties props = new Properties();

    public iOSDevice(Properties props) {
        this.computer = computer;
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
