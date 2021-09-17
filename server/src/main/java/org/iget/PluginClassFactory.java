package org.iget;

import org.elasticsearch.plugins.Plugin;
import org.iget.org.elasticsearch.searchPlugin.FunctionFilterPlugin;

import java.util.ArrayList;
import java.util.Collection;

public class PluginClassFactory {
    public static Collection<Class<? extends Plugin>> createPlugin(String ... pluginName) {
        Collection<Class<? extends Plugin>> result = new ArrayList<>();
        for(int i = 0; i < pluginName.length; i++) {
            switch (pluginName[i]) {
                case "test" :
                    // 继承了Plugin的插件类
                    result.add(FunctionFilterPlugin.class);
                    break;
                default:
                    break;
            }
        }
        return result;
    }
}

