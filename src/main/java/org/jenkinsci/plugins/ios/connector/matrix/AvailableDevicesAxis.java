// Copyright © 2012 iosphere GmbH
package org.jenkinsci.plugins.ios.connector.matrix;

import org.jenkinsci.plugins.ios.connector.Messages;
import org.jenkinsci.plugins.ios.connector.iOSDevice;
import org.jenkinsci.plugins.ios.connector.iOSDeviceList;
import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.matrix.Axis;
import hudson.matrix.AxisDescriptor;
import hudson.matrix.MatrixBuild.MatrixBuildExecution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import jenkins.model.Jenkins;

/** Dynamic matrix axis which expands to the UDID of every connected iOS device at build time. */
public class AvailableDevicesAxis extends Axis {

    /** Variable name under which each device UDID will be made available. */
    private static final String AXIS_NAME = "UDID";

    @Inject
    private transient iOSDeviceList deviceList;

    private List<String> axisValues;

    @DataBoundConstructor
    public AvailableDevicesAxis() {
        super(AXIS_NAME, AXIS_NAME);
    }

    @Override
    public List<String> getValues() {
        if (axisValues == null || axisValues.isEmpty()) {
            return Collections.singletonList("default");
        }
        return axisValues;
    }

    @Override
    public List<String> rebuild(MatrixBuildExecution context) {
        Jenkins.getInstance().getInjector().injectMembers(this);

        List<String> udids = new ArrayList<String>();
        if (deviceList != null) {
            for (iOSDevice device : deviceList.getDevices().values()) {
                udids.add(device.getUniqueDeviceId());
            }
        }

        axisValues = udids;
        return udids;
    }

    @Extension
    public static class DescriptorImpl extends AxisDescriptor {
        @Override
        public String getDisplayName() {
            return Messages.AvailableDevicesAxis_Name();
        }
    }

}
