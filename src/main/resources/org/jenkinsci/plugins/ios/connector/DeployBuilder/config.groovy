package org.jenkinsci.plugins.ios.connector.DeployBuilder

def f = namespace(lib.FormTagLib)

f.entry(title:"Path of IPA files", field:"path") {
    f.textbox()
}
f.entry(title:"Device", field:"udid") {
    f.textbox()
}
