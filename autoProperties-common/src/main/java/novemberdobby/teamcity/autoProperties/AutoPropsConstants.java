package novemberdobby.teamcity.autoProperties.common;

import java.util.regex.Pattern;

public class AutoPropsConstants {
    public static final String FEATURE_TYPE_ID = "auto_properties_set";
    public static final String FEATURE_DISPLAY_NAME = "Auto Properties";
    public static final String FEATURE_SETTINGS_JSP = "settings.jsp";
    
    public static final String SETTING_TYPE = "trigger.type";
    public static final String SETTING_TYPE_DEFAULT = "auto";
    public static final String SETTING_PARAMS = "parameters.list";
    public static final String SETTING_CUSTOM_VARIABLE = "trigger.variable";
    public static final String SETTING_CUSTOM_PATTERN = "trigger.pattern";
    public static final String SETTING_TRIGGER_TYPE_NAME = "trigger.type.name";
    
    public static final int CUSTOM_PATTERN_OPTIONS = Pattern.CASE_INSENSITIVE;
    public static final int CHECK_HISTORY_COUNT = 30;
    
    public static final String AGENT_FLAG_VAR_NAME = "auto_properties_trigger_type_name";
    public static final String AGENT_FLAG_KEY = "type";
    
    public static final String TESTING_URL = "/auto_props_test.html";
}
