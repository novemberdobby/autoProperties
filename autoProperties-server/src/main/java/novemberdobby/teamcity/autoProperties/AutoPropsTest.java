package novemberdobby.teamcity.autoProperties.server;

import java.util.List;
import java.util.Map;
import java.net.URLDecoder;

import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.servlet.ModelAndView;

import jetbrains.buildServer.serverSide.auth.Permission;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.SProject;

import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import jetbrains.buildServer.web.util.SessionUser;
import org.jetbrains.annotations.NotNull;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import jetbrains.buildServer.agent.Constants;

import novemberdobby.teamcity.autoProperties.common.AutoPropsConstants;
import novemberdobby.teamcity.autoProperties.common.AutoPropsUtil;

public class AutoPropsTest extends BaseController {
    
    SBuildServer m_server;
    PluginDescriptor m_descriptor;
    
    public AutoPropsTest(PluginDescriptor descriptor, SBuildServer server, WebControllerManager web) {
        m_server = server;
        m_descriptor = descriptor;
        
        web.registerController(AutoPropsConstants.TESTING_URL, this);
    }
    
    //TODO: test all types on finished builds
    //TODO: check inherited build feature
    
    @Override
    protected ModelAndView doHandle(@NotNull final HttpServletRequest request, @NotNull final HttpServletResponse response) throws Exception {
        
        //response page is just a single string
        ModelAndView mv = new ModelAndView(m_descriptor.getPluginResourcesPath(AutoPropsConstants.TESTING_RESPONSE));
        Map<String, Object> model = mv.getModel();
        
        StringBuffer url = request.getRequestURL();
        String query = request.getQueryString();
        if(query != null) {
            url.append("?");
            url.append(query);
        }
        
        SUser user = SessionUser.getUser(request);
        if(user == null) {
            return null;
        }
        
        MultiValueMap<String, String> params = UriComponentsBuilder.fromUriString(url.toString()).build().getQueryParams();
        
        String operation = getSingle(params, "action");
        
        String result = "";
        switch(operation)
        {
            case "checkMissingProps":
                String props = getSingle(params, "props");
                
                if(props.length() > 0) {
                    String buildTypeId = getSingle(params, "buildTypeId");
                    final String prefix = "buildType:";
                    if(buildTypeId.startsWith(prefix)) {
                        buildTypeId = buildTypeId.substring(prefix.length());
                    }
                    
                    SBuildType buildType = getWithPermission(user, buildTypeId);
                    if(buildType == null) {
                        return null;
                    }
                    
                    Map<String, String> btParams = buildType.getParameters();
                    List<String> missing = AutoPropsUtil.getMissingParameters(btParams, props, Constants.ENV_PREFIX);
                    
                    //parameter names can't contain newlines, so use them as a delimiter
                    result = String.join("\n", missing);
                }
                
                break;
        }
        
        model.put("result", result);
        return mv;
    }
    
    private String getSingle(MultiValueMap<String, String> map, String name) {
        List<String> val = map.get(name);
        if(val != null && val.size() == 1) {
            return URLDecoder.decode(val.get(0)); //js sent it encoded
        }
        
        return "";
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