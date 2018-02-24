package novemberdobby.teamcity.autoProperties.server;

import jetbrains.buildServer.serverSide.SFinishedBuild;

//struct to pass build test info to JSP
public class AutoPropsBuildHelper {
    
    SFinishedBuild m_build;
    boolean m_set;
    String m_varName;
    
    public AutoPropsBuildHelper(SFinishedBuild build, boolean set, String varName) {
        m_build = build;
        m_set = set;
        m_varName = varName;
    }
    
    public SFinishedBuild getBuild() {
        return m_build;
    }
    
    public boolean getSet() {
        return m_set;
    }
    
    public String getVarName() {
        return m_varName;
    }
}