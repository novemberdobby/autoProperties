package novemberdobby.teamcity.autoProperties.common;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.regex.Matcher;
import java.util.stream.Stream;

public class AutoPropsUtil {
    
    public static Map<String, String> getParameters(Map<String, String> featureParams) {
        return getParametersFromString(featureParams.get(AutoPropsConstants.SETTING_PARAMS));
    }
    
    //transform the feature's parameters into a list of params to set for a build
    private static Map<String, String> getParametersFromString(String input) {
        Map<String, String> result = new LinkedHashMap<String, String>();
        
        if(input != null) {
            List<String> params = Arrays.asList(input.split("[\n\r]"));
            
            for(String param : params) {
                Matcher mtch = AutoPropsConstants.PROP_MATCH.matcher(param);
                if(mtch.matches()) {
                    result.put(mtch.group(1), mtch.group(2));
                }
            }
        }
        
        return result;
    }
    
    //should we update any parameters?
    public static boolean shouldSet(Map<String, String> featureParams, Map<String, String> buildParams) {
        String trigType = featureParams.get(AutoPropsConstants.SETTING_TYPE);
        String customPattern = featureParams.get(AutoPropsConstants.SETTING_CUSTOM_PATTERN);
        
        String param = buildParams.get(featureParams.get(AutoPropsConstants.SETTING_CUSTOM_VARIABLE));
        boolean byUser = buildParams.get("teamcity.build.triggeredBy.username") != null;
        
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
                
                if(param == null) {
                    return false;
                }
                
                Pattern ptn = Pattern.compile(customPattern, AutoPropsConstants.CUSTOM_PATTERN_OPTIONS);
                Matcher mtch = ptn.matcher(param);
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
    
    public static List<String> getMissingParameters(Map<String, String> buildTypeParams, String input, String... excludePrefixes) {
        List<String> result = new ArrayList<String>();
        Map<String, String> params = getParametersFromString(input);
        
        for(Map.Entry<String, String> testParam : params.entrySet()) {
            
            String key = testParam.getKey();
            if(!Stream.of(excludePrefixes).anyMatch(ex -> key.startsWith(ex)) && !buildTypeParams.containsKey(key)) {
                result.add(key);
            }
        }
        
        return result;
    }
    
    public static boolean testOnBuild(Map<String, String> buildParams, String pattern) {
        
        //mock up a build feature's options
        Map<String, String> featureParams = new LinkedHashMap<String, String>();
        featureParams.put(AutoPropsConstants.SETTING_TYPE, "custom");
        featureParams.put(AutoPropsConstants.SETTING_CUSTOM_PATTERN, pattern);
        
        return shouldSet(featureParams, buildParams);
    }
}