package novemberdobby.teamcity.autoProperties.server;

import java.util.Map;
import java.util.HashMap;

import jetbrains.buildServer.serverSide.BuildFeature;
import jetbrains.buildServer.web.openapi.PluginDescriptor;

import novemberdobby.teamcity.autoProperties.common.AutoPropsConstants;

public class AutoPropsFeature extends BuildFeature {

    private String m_editUrl;
    
    public AutoPropsFeature(PluginDescriptor descriptor) {
        m_editUrl = descriptor.getPluginResourcesPath(AutoPropsConstants.FEATURE_SETTINGS_JSP);
    }
    
    //TODO: on manual, on auto, on regex
    //TODO: check against triggered build ('#x would not trigger this with the current settings' etc)
    //TODO: can probably be a server-side only plugin
    
    @Override
    public String getDisplayName() {
        return AutoPropsConstants.FEATURE_DISPLAY_NAME;
    }

    @Override
    public String getEditParametersUrl() {
        return m_editUrl;
    }

    @Override
    public String getType() {
        return AutoPropsConstants.FEATURE_TYPE_ID;
    }
    
    @Override
    public boolean isMultipleFeaturesPerBuildTypeAllowed() {
        return true;
    }
    
    @Override
    public String describeParameters(Map<java.lang.String,java.lang.String> params) {
        return AutoPropsConstants.FEATURE_PARAMS_DESC; //TODO AutoPropsUtil.getParameters
    }
    
    @Override
    public Map<String, String> getDefaultParameters() {
        
        //all other props are empty strings by default
        HashMap<String, String> result = new HashMap<String, String>();
        result.put(AutoPropsConstants.SETTING_TYPE, "auto");
        
        //this is the var we inspect for determining whether the build was automatically or manually triggered,
        //people might want to vary things based on it so use it as the default
        result.put(AutoPropsConstants.SETTING_CUSTOM_VARIABLE, "teamcity.build.triggeredBy");
        
        return result;
    }
}