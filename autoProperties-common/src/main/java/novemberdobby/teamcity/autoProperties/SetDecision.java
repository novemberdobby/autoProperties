package novemberdobby.teamcity.autoProperties.common;

public class SetDecision {
    
    private boolean m_set;
    private String m_matchedVar;
    
    public SetDecision(boolean set) {
        m_set = set;
    }
    
    public SetDecision(boolean set, String matchedVar) {
        m_set = set;
        m_matchedVar = matchedVar;
    }
    
    public boolean getSet() {
        return m_set;
    }
    
    public String getMatchedVar() {
        return m_matchedVar;
    }
}
