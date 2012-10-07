package org.jenkinsci.plugins.ios.connector.iOSDevice

def l = namespace(lib.LayoutTagLib)
def f = namespace(lib.FormTagLib)

l.layout {
    def title = "App deployed"
    l.header(title:title)
    include(my,"sidepanel")
    l.main_panel {
        h1 {
            img(src:"${resURL}/plugin/ios-device-list/icons/48x48/iphone.png",alt:"[!]",height:48,width:48)
            text " ${title} (${my.displayName})"
        }

        pre msg
    }
}