package novemberdobby.teamcity.autoProperties.server;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.net.URLDecoder;

import org.springframework.web.servlet.ModelAndView;

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
                String result = "";
                
                if(props.length() > 0) {
                    if(buildType != null) {
                        Map<String, String> btParams = buildType.getParameters();
                        List<String> missing = AutoPropsUtil.getMissingParameters(btParams, props, Constants.ENV_PREFIX);
                        
                        //parameter names can't contain newlines, so use them as a delimiter
                        result = String.join("\n", missing);
                    }
                }
                
                stream.print(result);
                break;
                
            case "checkBuilds":
                
                response.setContentType("application/xml");
                DocumentBuilder bob = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                Document doc = bob.newDocument();
                
                Element root = doc.createElement("root");
                doc.appendChild(root);
                
                if(buildType != null) {
                    
                    List<SFinishedBuild> history = buildType.getHistory();
                    int numBuilds = Math.min(history.size() - 1, 30);
                    for(int i = numBuilds; i >= 0; i--) {
                        
                        SFinishedBuild build = history.get(i);
                        
                        Map<String, String> buildParams = build.getParametersProvider().getAll();
                        SetDecision decision = AutoPropsUtil.testOnBuild(
                            request.getParameter(AutoPropsConstants.SETTING_TYPE),
                            request.getParameter(AutoPropsConstants.SETTING_CUSTOM_VARIABLE),
                            request.getParameter(AutoPropsConstants.SETTING_CUSTOM_PATTERN),
                            buildParams);
                        
                        Element eBuild = doc.createElement("build");
                        root.appendChild(eBuild);
                        
                        eBuild.setAttribute("number", build.getBuildNumber());
                        eBuild.setAttribute("id", Long.toString(build.getBuildId()));
                        eBuild.setAttribute("status", build.getStatusDescriptor().getText());
                        eBuild.setAttribute("set", Boolean.toString(decision.getSet()));
                        eBuild.setAttribute("var", decision.getMatchedVar() == null ? "" : decision.getMatchedVar());
                    }
                }
                
                Transformer tf = TransformerFactory.newInstance().newTransformer();
                Writer out = new StringWriter();
                tf.transform(new DOMSource(doc), new StreamResult(out));
                stream.print(out.toString());

                
                break;
        }
        
        return null;
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