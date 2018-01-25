package novemberdobby.teamcity.autoProperties.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.HashMap;
import java.util.TreeMap;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.HtmlUtils;

import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.auth.Permission;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.SFinishedBuild;
import jetbrains.buildServer.serverSide.SProject;

import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.controllers.BaseController;
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
        
        ServletOutputStream stream = response.getOutputStream();
        
        SUser user = SessionUser.getUser(request);
        if(user == null) {
            return null;
        }
        
        String operation = request.getParameter("action");
        String buildTypeId = request.getParameter("buildTypeId");

        SBuildType buildType = null;

        final String prefix = "buildType:";
        if(buildTypeId.startsWith(prefix)) {
            buildTypeId = buildTypeId.substring(prefix.length());
            buildType = getWithPermission(user, buildTypeId);
        }
        
        switch(operation)
        {
            case "checkMissingProps":
                String props = request.getParameter("props");
                
                if(props.length() > 0) {
                    if(buildType != null) {
                        Map<String, String> btParams = buildType.getParameters();
                        List<String> missing = AutoPropsUtil.getMissingParameters(btParams, props, Constants.ENV_PREFIX);
                        
                        //parameter names can't contain newlines, so use them as a delimiter
                        stream.print(String.join("\n", missing));
                    }
                }
                
                break;
                
            case "checkBuilds":
            {
                Document doc = makeBaseXml();
                
                if(buildType != null) {
                    
                    List<SFinishedBuild> history = buildType.getHistory();
                    int numBuilds = Math.min(history.size() - 1, 30);
                    for(int i = 0; i < numBuilds; i++) {
                        
                        SFinishedBuild build = history.get(i);
                        
                        Map<String, String> buildParams = build.getParametersProvider().getAll();
                        SetDecision decision = AutoPropsUtil.testOnBuild(
                            request.getParameter(AutoPropsConstants.SETTING_TYPE),
                            request.getParameter(AutoPropsConstants.SETTING_CUSTOM_VARIABLE),
                            request.getParameter(AutoPropsConstants.SETTING_CUSTOM_PATTERN),
                            buildParams);
                        
                        Element eBuild = doc.createElement("build");
                        doc.getFirstChild().appendChild(eBuild);
                        
                        eBuild.setAttribute("number", build.getBuildNumber());
                        eBuild.setAttribute("id", Long.toString(build.getBuildId()));
                        eBuild.setAttribute("status", build.getStatusDescriptor().getText());
                        eBuild.setAttribute("set", Boolean.toString(decision.getSet()));
                        eBuild.setAttribute("var", decision.getMatchedVar() == null ? "" : decision.getMatchedVar());
                    }
                }
                
                writeXmlToResponse(doc, response);
                break;
            }
                
            case "autoCompleteVar":
            {
                Document doc = makeBaseXml();
                String varName = request.getParameter("name");
                
                if(varName != null && buildType != null) {
                    
                    //resulting properties from the last build
                    Map<String, String> lbParms = null;
                    List<SFinishedBuild> history = buildType.getHistory();
                    if(history.size() > 0) {
                        lbParms = history.get(0).getParametersProvider().getAll();
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
}