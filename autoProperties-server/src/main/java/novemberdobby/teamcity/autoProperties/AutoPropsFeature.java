package novemberdobby.teamcity.autoProperties.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.springframework.web.util.HtmlUtils;

import jetbrains.buildServer.serverSide.BuildFeature;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.log.Loggers;

import novemberdobby.teamcity.autoProperties.common.AutoPropsConstants;
import novemberdobby.teamcity.autoProperties.common.AutoPropsUtil;

public class AutoPropsFeature extends BuildFeature {

    private String m_editUrl;
    
    public AutoPropsFeature(PluginDescriptor descriptor) {
        m_editUrl = descriptor.getPluginResourcesPath(AutoPropsConstants.FEATURE_SETTINGS_JSP);
    }
    
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
        String type = params.get(AutoPropsConstants.SETTING_TYPE);
        String typeStr = "unknown";
        
        switch(type) {
            
            case "auto":
                typeStr = "automatically triggered builds";
                break;
            
            case "manual":
                typeStr = "manually triggered builds";
                break;
                
            case "custom":
                String varName = params.get(AutoPropsConstants.SETTING_CUSTOM_VARIABLE);
                String varMatch = params.get(AutoPropsConstants.SETTING_CUSTOM_PATTERN);
                
                //let's not allow people to inject HTML =]
                typeStr = HtmlUtils.htmlEscape(String.format("builds when parameter \"%s\" matches: %s", varName, varMatch));
                break;
        }
        
        Map<String, String> toSet = AutoPropsUtil.getParameters(params);
        StringBuilder sb = new StringBuilder();
        for(Entry<String, String> var : toSet.entrySet()) {
            sb.append("<br>");
            sb.append(HtmlUtils.htmlEscape(var.getKey()));
            sb.append(HtmlUtils.htmlEscape(" => "));
            sb.append(HtmlUtils.htmlEscape(var.getValue()));
        }
        
        return String.format("Set %d parameter%s on %s%s", toSet.size(), toSet.size() == 1 ? "" : "s", typeStr, sb.toString());
    }
    
    @Override
    public Map<String, String> getDefaultParameters() {
        
        //all other props are empty strings by default
        HashMap<String, String> result = new HashMap<String, String>();
        result.put(AutoPropsConstants.SETTING_TYPE, AutoPropsConstants.SETTING_TYPE_DEFAULT);
        
        //this is the var we inspect for determining whether the build was automatically or manually triggered,
        //people might want to vary things based on it so use it as the default
        result.put(AutoPropsConstants.SETTING_CUSTOM_VARIABLE, "teamcity.build.triggeredBy");
        
        return result;
    }
    
    @Override
    public PropertiesProcessor getParametersProcessor() {
        return new AutoPropsFeatureValidator();
    }

    static class AutoPropsFeatureValidator implements PropertiesProcessor {
        
        @Override
        public Collection<InvalidProperty> process(Map<String, String> input) {
            
            ArrayList<InvalidProperty> result = new ArrayList<InvalidProperty>();
            
            String type = input.get(AutoPropsConstants.SETTING_TYPE);
            if(type.equals("custom")) {
                
                //custom var name
                String varName = input.get(AutoPropsConstants.SETTING_CUSTOM_VARIABLE);
                
                if(varName == null || varName.length() == 0) { //nothing there
                    result.add(new InvalidProperty(AutoPropsConstants.SETTING_CUSTOM_VARIABLE, "Please define a source parameter name"));
                    
                } else if(varName.contains("%")) { //they're trying to reference another value
                    result.add(new InvalidProperty(AutoPropsConstants.SETTING_CUSTOM_VARIABLE, "To prevent incorrect resolution, parameter name should not include references"));
                }
                //
                
                
                //custom var pattern
                String varPattern = input.get(AutoPropsConstants.SETTING_CUSTOM_PATTERN);
                
                if(varPattern == null || varPattern.length() == 0) { //nothing there
                    result.add(new InvalidProperty(AutoPropsConstants.SETTING_CUSTOM_PATTERN, "Please define a regex pattern to match"));
                    
                } else { //invalid regex
                    try {
                        Pattern compiled = Pattern.compile(varPattern);
                    }
                    catch(PatternSyntaxException ex) {
                        result.add(new InvalidProperty(AutoPropsConstants.SETTING_CUSTOM_PATTERN, ex.getMessage().toString()));
                    }
                }
                //
            }
            
            //actual params
            String params = input.get(AutoPropsConstants.SETTING_PARAMS);
            
            if(params == null || params.length() == 0) { //nothing there
                result.add(new InvalidProperty(AutoPropsConstants.SETTING_PARAMS, "Please define one or more parameters to set"));
                
            }
            //
            
            
            return result;
        }
    }
}