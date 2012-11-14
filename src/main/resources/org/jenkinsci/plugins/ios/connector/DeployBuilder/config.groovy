package org.jenkinsci.plugins.ios.connector.DeployBuilder

def f = namespace(lib.FormTagLib)

f.entry(title:"Path to .ipa/app file(s)", field:"path") {
    f.textbox()
}
f.entry(title:"Device", field:"udid") {
    f.textbox()
}
f.advanced() {
    f.entry(title:"Fruitstrap args", field:"cmdLineArgs",
            description:"Command line arguments which will be passed to the <tt>fruitstrap</tt> command") {
        f.textbox()
    }
}
