package org.jenkinsci.plugins.ios.connector.iOSDevice

def l = namespace(lib.LayoutTagLib)
def f = namespace(lib.FormTagLib)

l.layout {
    def title = "Deploy IPA to ${my.deviceName}"
    l.header(title:title)
    include(my,"sidepanel")
    l.main_panel {
        h1 {
            img(src:"${resURL}/plugin/ios-device-connector/icons/48x48/iphone.png",alt:"[!]",height:48,width:48)
            text " ${title} (${my.displayName})"
        }

        f.form(method:"POST",action:"doDeploy") {
            f.entry(title:"IPA file to deploy") {
                input(name:"ipa",type:"file")
            }
            f.block {
                    f.submit(value:"Deploy")
            }
        }
    }
}