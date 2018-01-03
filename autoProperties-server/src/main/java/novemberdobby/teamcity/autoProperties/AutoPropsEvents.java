package novemberdobby.teamcity.autoProperties;

import java.util.Map;

import jetbrains.buildServer.serverSide.BuildServerAdapter;
import jetbrains.buildServer.serverSide.BuildServerListener;
import jetbrains.buildServer.serverSide.SRunningBuild;
import jetbrains.buildServer.util.EventDispatcher;

import org.jetbrains.annotations.NotNull;

public class AutoPropsEvents extends BuildServerAdapter {

    public AutoPropsEvents(EventDispatcher<BuildServerListener> dispatcher) {
        dispatcher.addListener(this);
    }
    
    @Override
    public void buildStarted(@NotNull SRunningBuild build) {
        
    }
}