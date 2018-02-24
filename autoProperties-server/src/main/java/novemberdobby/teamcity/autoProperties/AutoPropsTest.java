package novemberdobby.teamcity.autoProperties.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.TreeMap;
import java.util.TreeSet;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.HtmlUtils;

import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.auth.Permission;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.SFinishedBuild;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.TriggeredBy;

import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.util.ItemProcessor;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import jetbrains.buildServer.web.util.SessionUser;
import org.jetbrains.annotations.NotNull;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletOutputStream;
import jetbrains.buildServer.agent.Constants;

import java.io.StringWriter;
import java.io.Writer;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import novemberdobby.teamcity.autoProperties.common.AutoPropsConstants;
import novemberdobby.teamcity.autoProperties.common.AutoPropsUtil;
import novemberdobby.teamcity.autoProperties.common.SetDecision;

public class AutoPropsTest extends BaseController {
    
    SBuildServer m_server;
    PluginDescriptor m_descriptor;
    
    public AutoPropsTest(PluginDescriptor descriptor, SBuildServer server, WebControllerManager web) {
        m_server = server;
        m_descriptor = descriptor;
        
        web.registerController(AutoPropsConstants.TESTING_URL, this);
    }
    
    @Override
    protected ModelAndView doHandle(@NotNull final HttpServletRequest request, @NotNull final HttpServletResponse response) throws Exception {
        
        String operation = request.getParameter("action");
        SUser user = SessionUser.getUser(request);
        if(user == null || operation == null) {
            return null;
        }
        
        String buildTypeId = request.getParameter("buildTypeId");
        SBuildType buildType = null;
        
        final String prefix = "buildType:";
        if(buildTypeId != null && buildTypeId.startsWith(prefix)) {
            buildTypeId = buildTypeId.substring(prefix.length());
            buildType = getWithPermission(user, buildTypeId);
        }
        
        switch(operation)
        {
            case "checkMissingProps":
                String props = request.getParameter("props");
                
                if(props != null && props.length() > 0) {
                    if(buildType != null) {
                        Map<String, String> btParams = buildType.getParameters();
                        List<String> missing = AutoPropsUtil.getMissingParameters(btParams, props, Constants.ENV_PREFIX);
                        
                        //parameter names can't contain newlines, so use them as a delimiter
                        ServletOutputStream stream = response.getOutputStream();
                        stream.print(String.join("\n", missing));
                    }
                }
                
                break;
                
            case "checkBuilds":
            {
                ModelAndView mv = new ModelAndView(m_descriptor.getPluginResourcesPath(AutoPropsConstants.FEATURE_TEST_JSP));
                Map<String, Object> model = mv.getModel();
                List<AutoPropsBuildHelper> testResults = new ArrayList<AutoPropsBuildHelper>();
                int qual = 0;
                
                if(buildType != null) {
                    
                    HistoryProcessor history = new HistoryProcessor(AutoPropsConstants.CHECK_HISTORY_COUNT);
                    m_server.getHistory().processEntries(buildType.getInternalId(), null, true, false, true, history);
                    
                    Map<String, String> requestParams = request.getParameterMap().entrySet().stream().collect(Collectors.toMap(p -> p.getKey(), p -> p.getValue()[0]));
                    
                    String name_column = AutoPropsUtil.getCheckAgainst(requestParams);
                    if(name_column != null) {
                        model.put("extra_column_name", name_column);
                    }
                    
                    for(SFinishedBuild build : history.getBuilds()) {
                        
                        Map<String, String> buildParams = build.getParametersProvider().getAll();
                        Map<String, String> triggeredByParams = build.getTriggeredBy().getParameters();
                        SetDecision decision = AutoPropsUtil.makeDecision(requestParams, buildParams, triggeredByParams);
                        
                        if(!decision.isValid()) {
                            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid parameters");
                            return null;
                        }
                        
                        testResults.add(new AutoPropsBuildHelper(build, decision.getSet(), decision.getMatchedVar()));
                        if(decision.getSet()) {
                            qual++;
                        }
                    }
                }
                
                
                model.put("info", String.format("%d of the last %d builds %s. Key:", qual, testResults.size(), qual == 1 ? "qualifies" : "qualify"));
                model.put("builds", testResults);
                return mv;
            }
                
            case "autoCompleteVar":
            {
                Document doc = makeBaseXml();
                String varName = request.getParameter("name");
                
                if(varName != null && buildType != null) {
                    
                    //resulting properties from the last build
                    Map<String, String> lbParms = null;
                    
                    SFinishedBuild last = buildType.getLastChangesFinished();
                    if(last != null) {
                        lbParms = last.getParametersProvider().getAll();
                    }
                    
                    TreeMap<String, String> found = getMatchingProps(varName, buildType.getParameters(), lbParms);
                    
                    for(Entry<String, String> kvp : found.entrySet()) {
                        Element eVar = doc.createElement("var");
                        doc.getFirstChild().appendChild(eVar);
                        eVar.setAttribute("name", kvp.getKey());
                        eVar.setAttribute("display", kvp.getValue());
                    }
                }
                
                writeXmlToResponse(doc, response);
                break;
            }
            
            case "listTriggerTypes":
            {
                Document doc = makeBaseXml();
                if(buildType != null) {
                    
                    //trigger type names for the last builds
                    TreeSet<String> types = new TreeSet<String>();
                    types.add("user");
                    
                    HistoryProcessor history = new HistoryProcessor(AutoPropsConstants.CHECK_HISTORY_COUNT);
                    m_server.getHistory().processEntries(buildType.getInternalId(), null, true, true, true, history);
                    
                    for(SFinishedBuild build : history.getBuilds()) {
                    
                        TriggeredBy tb = build.getTriggeredBy();
                        Map<String, String> params = tb.getParameters();
                        String triggerType = params.get(AutoPropsConstants.AGENT_FLAG_KEY);
                        
                        if(triggerType != null) {
                            types.add(triggerType);
                        }
                    }
                    
                    for(String type : types) {
                        Element eVar = doc.createElement("var");
                        doc.getFirstChild().appendChild(eVar);
                        eVar.setAttribute("name", type);
                    }
                }
                
                writeXmlToResponse(doc, response);
                break;
            }
        }
        
        return null;
    }
    
    private Document makeBaseXml() throws Exception {
        DocumentBuilder bob = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = bob.newDocument();
        
        Element root = doc.createElement("root");
        doc.appendChild(root);
        return doc;
    }
    
    private void writeXmlToResponse(Document doc, HttpServletResponse response) throws Exception {
        response.setContentType("application/xml");
        
        Transformer tf = TransformerFactory.newInstance().newTransformer();
        Writer out = new StringWriter();
        tf.transform(new DOMSource(doc), new StreamResult(out));
        
        response.getOutputStream().print(out.toString());
    }
    
    private TreeMap<String, String> getMatchingProps(String search, Map<String, String>... params) {
        
        TreeMap<String, String> result = new TreeMap<String, String>();
        String find = search.toLowerCase();
        
        for(Map<String, String> map : params) {
            if(map != null) {
                for(Entry<String, String> param : map.entrySet()) {
                    String key = param.getKey();
                    String keyLow = key.toLowerCase();
                    Integer len = find.length();
                    
                    Integer index = len == 0 ? 0 : keyLow.indexOf(find);
                    if(!result.containsKey(key) && index != -1) {
                        
                        String name   = HtmlUtils.htmlEscape(key);
                        String start  = HtmlUtils.htmlEscape(key.substring(0, index));
                        String middle = HtmlUtils.htmlEscape(key.substring(index, index + len));
                        String end    = HtmlUtils.htmlEscape(key.substring(index + len));
                        
                        result.put(name, start + "<b>" + middle + "</b>" + end);
                    }
                }
            }
        }
        
        return result;
    }
    
    //find & return a build config requested by a user, or null if it doesn't exist/they don't have permission
    private SBuildType getWithPermission(SUser user, String buildTypeId) {
        
        ProjectManager pm = m_server.getProjectManager();
        SBuildType buildType = pm.findBuildTypeByExternalId(buildTypeId);
        
        if(buildType != null) {
            SProject proj = buildType.getProject();
            if(user.isPermissionGrantedForProject(proj.getProjectId(), Permission.EDIT_PROJECT)) {
                return buildType;
            }
        }
        
        return null;
    }
    
    static class HistoryProcessor implements ItemProcessor<SFinishedBuild> {
        
        int m_maxCount;
        List<SFinishedBuild> m_builds = new ArrayList<SFinishedBuild>();
        
        public HistoryProcessor(int maxCount) {
            m_maxCount = maxCount;
        }
        
        @Override
        public boolean processItem(SFinishedBuild build) {
            m_builds.add(build);
            return m_builds.size() < m_maxCount;
        }
        
        public List<SFinishedBuild> getBuilds() {
            return m_builds;
        }
    }
}