package novemberdobby.teamcity.autoProperties.server;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.net.URLDecoder;

import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.servlet.ModelAndView;

import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jetbrains.annotations.NotNull;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import novemberdobby.teamcity.autoProperties.common.AutoPropsConstants;

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
        
        MultiValueMap<String, String> params = UriComponentsBuilder.fromUriString(url.toString()).build().getQueryParams();
        List<String> ops = params.get("action");
        List<String> patterns = params.get("pattern");
        
        String op = (ops != null && ops.size() == 1) ? ops.get(0) : "";
        String pattern = (patterns != null && patterns.size() == 1) ? patterns.get(0) : "";
        pattern = URLDecoder.decode(pattern); //js sent it encoded
        
        switch(op)
        {
            case "checkPattern":
                try {
                    Pattern p = Pattern.compile(pattern);
                    model.put("result", "");
                }
                catch(PatternSyntaxException ex) {
                    model.put("result", ex.getMessage().toString());
                }
                break;
        }
        
        return mv;
    }
}