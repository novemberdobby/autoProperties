package novemberdobby.teamcity.autoProperties.common;

import java.util.regex.Pattern;

public class AutoPropsConstants {
    public static final String FEATURE_TYPE_ID = "auto_properties_set";
    public static final String FEATURE_DISPLAY_NAME = "Auto Properties";
    public static final String FEATURE_PARAMS_DESC = "PLACEHOLDER PLACEHOLDER";
    public static final String FEATURE_SETTINGS_JSP = "settings.jsp";
    
    public static final String SETTING_TYPE = "trigger.type";
    public static final String SETTING_PARAMS = "parameters.list";
    public static final String SETTING_CUSTOM_PATTERN = "trigger.pattern";
    public static final Pattern PROP_MATCH = Pattern.compile("(.*?)\\s*=>\\s*(.*)");
}
