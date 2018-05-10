package novemberdobby.teamcity.autoProperties.server;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.TriggeredBy;
import jetbrains.buildServer.serverSide.parameters.AbstractBuildParametersProvider;
import jetbrains.buildServer.serverSide.parameters.ParameterDescriptionProvider;

import novemberdobby.teamcity.autoProperties.common.AutoPropsConstants;

//pass the trigger type name to the agent for use in AutoPropsAgentEvents
public class AutoPropsParamProvider
    extends AbstractBuildParametersProvider
    implements ParameterDescriptionProvider {
    
    //AbstractBuildParametersProvider
    @Override
    public Map<String, String> getParameters(SBuild build, boolean emulationMode) {
        return Collections.singletonMap(AutoPropsConstants.AGENT_FLAG_VAR_NAME, getTriggerType(build));
    }
    
    @Override
    public Collection<String> getParametersAvailableOnAgent(SBuild build) {
        return Collections.singletonList(AutoPropsConstants.AGENT_FLAG_VAR_NAME);
    }
    
    String getTriggerType(SBuild build) {
        TriggeredBy tb = build.getTriggeredBy();
        if(tb != null && tb.getParameters().containsKey(AutoPropsConstants.AGENT_FLAG_KEY)) {
            return tb.getParameters().get(AutoPropsConstants.AGENT_FLAG_KEY);
        }
        
        return null;
    }
    
    //ParameterDescriptionProvider
    @Override
    public String describe(String paramName) {
        
        if(paramName.equals(AutoPropsConstants.AGENT_FLAG_VAR_NAME)) {
            return "Trigger type name passed to agent on build start";
        }
        
        return null;
    }
    
    @Override
    public boolean isVisible(String paramName) {
        return paramName.equals(AutoPropsConstants.AGENT_FLAG_VAR_NAME);
    }
}