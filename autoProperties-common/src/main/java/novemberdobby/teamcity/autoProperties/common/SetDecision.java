package novemberdobby.teamcity.autoProperties.common;

public class SetDecision {
    
    private boolean m_set;
    private boolean m_valid = false;
    private String m_matchedVar;
    
    public SetDecision() {
        
    }
    
    public SetDecision(boolean set) {
        m_valid = true;
        m_set = set;
    }
    
    public SetDecision(boolean set, String matchedVar) {
        m_valid = true;
        m_set = set;
        m_matchedVar = matchedVar;
    }
    
    public boolean getSet() {
        return m_set;
    }
    
    public boolean isValid() {
        return m_valid;
    }
    
    public String getMatchedVar() {
        return m_matchedVar == null ? "" : m_matchedVar;
    }
}
