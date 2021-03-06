package novemberdobby.teamcity.autoProperties.agent;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import jetbrains.buildServer.agent.AgentBuildFeature;
import jetbrains.buildServer.agent.AgentLifeCycleAdapter;
import jetbrains.buildServer.agent.AgentLifeCycleListener;
import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.Constants;
import jetbrains.buildServer.util.EventDispatcher;

import org.jetbrains.annotations.NotNull;

import novemberdobby.teamcity.autoProperties.common.AutoPropsConstants;
import novemberdobby.teamcity.autoProperties.common.AutoPropsUtil;
import novemberdobby.teamcity.autoProperties.common.SetDecision;

public class AutoPropsAgentEvents extends AgentLifeCycleAdapter {
    
    public AutoPropsAgentEvents(EventDispatcher<AgentLifeCycleListener> events) {
        events.addListener(this);
    }
    
    @Override
    public void buildStarted(@NotNull AgentRunningBuild build) {
        
        BuildProgressLogger log = build.getBuildLogger();
        Collection<AgentBuildFeature> setters = build.getBuildFeaturesOfType(AutoPropsConstants.FEATURE_TYPE_ID);
        for(AgentBuildFeature setter : setters) {
            
            Map<String, String> buildParams = new HashMap<String, String>(build.getSharedBuildParameters().getAllParameters());
            buildParams.putAll(build.getSharedConfigParameters());
            
            //feature options
            Map<String, String> params = setter.getParameters();
            SetDecision decision = AutoPropsUtil.makeDecision(params, buildParams, null);
            
            if(decision.isValid() && decision.getSet()) {
                Map<String, String> toSet = AutoPropsUtil.getParameters(params);
                
                if(toSet != null && toSet.size() > 0) {
                    String blockMsg = String.format("AutoProperties: setting %d parameter%s", toSet.size(), toSet.size() > 1 ? "s" : "");
                    log.activityStarted(blockMsg, "agent");
                    
                    for(Entry<String, String> entry : toSet.entrySet()) {
                        String key = entry.getKey();
                        String value = entry.getValue();
                        log.message(String.format("%s => %s", key, value));
                        
                        if(key.startsWith(Constants.ENV_PREFIX)) {
                            build.addSharedEnvironmentVariable(key.substring(Constants.ENV_PREFIX.length()), value);
                        } else if(key.startsWith(Constants.SYSTEM_PREFIX)) {
                            build.addSharedSystemProperty(key.substring(Constants.SYSTEM_PREFIX.length()), value);
                        } else {
                            build.addSharedConfigParameter(key, value);
                        }
                        
                    }
                    
                    log.activityFinished(blockMsg, "agent");
                }
            }
        }
    }
}