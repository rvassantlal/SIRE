package sire.extensions;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyShell;
import groovy.lang.Script;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ExtensionManager {
    Map<String, Script> extensions;
    GroovyShell sh;
    public ExtensionManager() {
        this.sh = new GroovyShell();
        this.extensions = new TreeMap<>();
    }

    public void addExtension(String appId, ExtensionType type, String key, String code) {
        this.extensions.put(appId + type.name() + key, sh.parse(code));

    }

    public Script getExtension(String appId, ExtensionType type, String key) {
        return extensions.get(appId + type.name() + key);
    }

    public Script getExtension(String appId, ExtensionType type) {
        return extensions.get(appId + type.name());
    }

    public void runExtension(String appId, ExtensionType type, String key) {
        String temp;
        /*System.out.println("Checking...");
        for(Map.Entry e : extensions.entrySet())
            System.out.println("Key " + e.getKey() + " Value " + e.getValue());*/
        if(extensions.containsKey(appId + type.name() + key))
            temp = appId + type.name() + key;
        else if(extensions.containsKey(appId + type.name()))
            temp = appId + type.name();
        else if (extensions.containsKey(appId))
            temp = appId;
        else {
            //System.out.println("Left! " + appId + type.name() + key);
            return;
        }
        System.out.println("Running extension...");
        extensions.get(temp).run();
        System.out.println("Extension ran!");
    }

    public void removeExtension(String appId, ExtensionType type, String key) {
        this.extensions.remove(appId + type + key);
    }
}
