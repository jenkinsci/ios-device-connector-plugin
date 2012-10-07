package org.jenkinsci.plugins.ios.connector.iOSDevice
import org.jenkinsci.plugins.ios.connector.iOSDeviceList

l=namespace(lib.LayoutTagLib)

l.header()
l.side_panel {
    l.tasks {
        l.task(icon:"images/24x24/up.png", href:'..', title:_("Back to Device List"))
        l.task(icon:"images/24x24/setting.png", href:"deploy", title:_("Deploy App"),
                permission: iOSDeviceList.DEPLOY, it:app)
    }
}
