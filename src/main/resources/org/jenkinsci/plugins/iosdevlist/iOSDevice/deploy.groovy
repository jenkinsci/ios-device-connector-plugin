package org.jenkinsci.plugins.iosdevlist.iOSDevice

def l = namespace(lib.LayoutTagLib)
def f = namespace(lib.FormTagLib)

l.layout {
    def title = "Deploy IPA to ${my.deviceName}"
    l.header(title:title)
    include(my,"sidepanel")
    l.main_panel {
        h1 {
            img(src:"${resURL}/plugin/ios-device-list/icons/48x48/iphone.png",alt:"[!]",height:48,width:48)
            text " ${title} (${my.productTypeDisplayName})"
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