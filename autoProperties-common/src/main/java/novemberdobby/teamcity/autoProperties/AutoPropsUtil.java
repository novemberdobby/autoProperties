package novemberdobby.teamcity.autoProperties.common;

import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.regex.Matcher;

public class AutoPropsUtil {
    
    //transform the feature's parameters into a list of params to set for a build
    public static Map<String, String> getParameters(Map<String, String> featureParams) {
        Map<String, String> result = new HashMap<String, String>();
        
        if(featureParams.containsKey(AutoPropsConstants.SETTING_PARAMS)) {
            String list = featureParams.get(AutoPropsConstants.SETTING_PARAMS);
            List<String> params = Arrays.asList(list.split("[\n\r]"));
            
            for(String param : params) {
                Matcher mtch = AutoPropsConstants.PROP_MATCH.matcher(param);
                if(mtch.matches()) {
                    result.put(mtch.group(1), mtch.group(2));
                }
            }
        }
        
        return result;
    }
    
    //should we update any parameters, based on how this build was triggered?
    public static boolean shouldSet(Map<String, String> featureParams, Map<String, String> buildParams) {
        String trigType = featureParams.get(AutoPropsConstants.SETTING_TYPE);
        String customPattern = featureParams.get(AutoPropsConstants.SETTING_CUSTOM_PATTERN);
        
        String triggeredBy = buildParams.get("teamcity.build.triggeredBy");
        String triggeredByUser = buildParams.get("teamcity.build.triggeredBy.username");
        boolean byUser = triggeredByUser != null && triggeredByUser.length() > 0;
        
        switch(trigType) {
            
            case "auto":
                return !byUser;
            
            case "manual":
                return byUser;
                
            case "custom":
                //we've checked it's valid on set, but just in case...
                if(!isValidRegex(customPattern)) {
                    return false;
                }
                
                Pattern ptn = Pattern.compile(customPattern, AutoPropsConstants.CUSTOM_PATTERN_OPTIONS);
                Matcher mtch = ptn.matcher(triggeredBy);
                return mtch.matches();
        }
        
        return false;
    }
    
    public static boolean isValidRegex(String pattern) {
        try {
            Pattern ptn = Pattern.compile(pattern, AutoPropsConstants.CUSTOM_PATTERN_OPTIONS);
            return true;
        }
        catch(PatternSyntaxException ex) {
            return false;
        }
    }
}