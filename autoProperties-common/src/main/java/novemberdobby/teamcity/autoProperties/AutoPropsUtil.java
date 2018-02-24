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
                final String mark = "=>";
                int ind = param.indexOf(mark);
                if(ind > 0) {
                    String key = param.substring(0, ind).trim();
                    String value = param.substring(ind + mark.length(), param.length()).trim();
                    
                    if(key.length() > 0 && value.length() > 0) {
                        result.put(key, value);
                    }
                }
            }
        }
        
        return result;
    }
    
    //should we update any parameters?
    public static SetDecision makeDecision(Map<String, String> featureParams, Map<String, String> buildParams, Map<String, String> triggeredByParams) {
        String trigType = featureParams.get(AutoPropsConstants.SETTING_TYPE);
        
        String customPattern = featureParams.get(AutoPropsConstants.SETTING_CUSTOM_PATTERN);
        customPattern = customPattern == null ? "" : customPattern;
        boolean isEmptyPattern = customPattern.length() == 0;
        
        String param = buildParams.get(featureParams.get(AutoPropsConstants.SETTING_CUSTOM_VARIABLE));
        boolean byUser = buildParams.containsKey("teamcity.build.triggeredBy.username");
        
        switch(trigType) {
            
            case "auto":
                return new SetDecision(!byUser);
            
            case "manual":
                return new SetDecision(byUser);
                
            case "trigger_type":
                String checkAgainst = featureParams.get(AutoPropsConstants.SETTING_TRIGGER_TYPE_NAME);
                String toCheck;
                if(triggeredByParams != null) {
                    toCheck = triggeredByParams.get(AutoPropsConstants.AGENT_FLAG_KEY); //we were called from the server side
                } else {
                    toCheck = buildParams.get(AutoPropsConstants.AGENT_FLAG_VAR_NAME); //called from the agent side
                }
                
                return new SetDecision(toCheck != null && checkAgainst != null && toCheck.equalsIgnoreCase(checkAgainst), toCheck);
                
            case "custom":
                //check for parameter existence
                if(isEmptyPattern) {
                    return new SetDecision(param != null, param);
                }
                
                //we've checked it's valid on set, but just in case...
                if(param == null) {
                    return new SetDecision(false);
                }
                
                if(!isValidRegex(customPattern)) {
                    return new SetDecision();
                }
                
                Pattern ptn = Pattern.compile(customPattern, AutoPropsConstants.CUSTOM_PATTERN_OPTIONS);
                Matcher mtch = ptn.matcher(param);
                return new SetDecision(mtch.find(), param);
        }
        
        return new SetDecision(false);
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
    
    public static String getCheckAgainst(Map<String, String> featureParams) {
        
        String trigType = featureParams.get(AutoPropsConstants.SETTING_TYPE);
        String customVar = featureParams.get(AutoPropsConstants.SETTING_CUSTOM_VARIABLE);
        
        if(trigType != null) {
            if(trigType.equals("custom") && customVar != null) {
                return "Value of '" + customVar + "'";
            } else if(trigType.equals("trigger_type")) {
                return "Trigger type name";
            }
        }
        
        return null;
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
}