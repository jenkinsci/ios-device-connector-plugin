package org.jenkinsci.plugins.ios.connector;

import junit.framework.TestCase;

import java.util.Properties;

public class iOSDeviceTest extends TestCase {

    public void testProductTypeMapping_iPhone() {
        assertProductTypeMapping("iPhone 3G", "iPhone1,2");
        assertProductTypeMapping("iPhone 5", "iPhone5,");
        assertProductTypeMapping("iPhone 5", "iPhone5,99");
    }

    public void testProductTypeMapping_iPad2() {
        // Only certain 2.x devices are the iPad 2
        assertProductTypeMapping("iPad 2", "iPad2,4");
        assertProductTypeMapping("iPad mini", "iPad2,6");
        assertProductTypeMapping("iPad 2", "iPad2,8");
    }

    public void testProductTypeMapping_iPad3() {
        // Only certain 3.x devices are the iPad 3
        assertProductTypeMapping("iPad 3", "iPad3,3");
        assertProductTypeMapping("iPad 4", "iPad3,5");
        assertProductTypeMapping("iPad 3", "iPad3,7");
    }

    public void testProductTypeMapping_unknown() {
        // If we don't have a mapping, return the original string
        assertProductTypeMapping("", "");
        assertProductTypeMapping("foo", "foo");
        assertProductTypeMapping("iDroid4,2", "iDroid4,2");
    }

    private static void assertProductTypeMapping(String expectedDisplayName, String productType) {
        assertNotNull(productType);
        assertNotNull(expectedDisplayName);

        Properties props = new Properties();
        props.put(iOSDevice.PROP_PRODUCT_TYPE, productType);
        iOSDevice device = new iOSDevice(props);
        assertEquals(expectedDisplayName, device.getProductTypeDisplayName());
    }

}
