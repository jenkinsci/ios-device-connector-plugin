package org.jenkinsci.plugins.iosdevlist.iOSDevice

l=namespace(lib.LayoutTagLib)

l.header()
l.side_panel {
    l.tasks {
        l.task(icon:"images/24x24/up.png", href:'..', title:_("Back to Device List"))
        // TODO: permission check:  permission:app.ADMINISTER, it:app?
        l.task(icon:"images/24x24/setting.png", href:"deploy", title:_("Deploy App"))
    }
}
