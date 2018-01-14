<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ page import="novemberdobby.teamcity.autoProperties.common.AutoPropsConstants" %>

<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>

<c:set var="params_list" value="<%=AutoPropsConstants.SETTING_PARAMS%>"/>
<c:set var="trig_type" value="<%=AutoPropsConstants.SETTING_TYPE%>"/>
<c:set var="trig_variable" value="<%=AutoPropsConstants.SETTING_CUSTOM_VARIABLE%>"/>
<c:set var="trig_pattern" value="<%=AutoPropsConstants.SETTING_CUSTOM_PATTERN%>"/>
<c:set var="test_url" value="<%=AutoPropsConstants.TESTING_URL%>"/>
<c:set var="trigger_type_in" value="${empty propertiesBean.properties[trig_type] ? propertiesBean.defaultProperties[trig_type] : propertiesBean.properties[trig_type]}"/>

<%-- TODO this is probably provided in some other correct way without substringing --%>
<c:set var='buildTypeId' value='<%= request.getParameter("id") %>'/>

<tr class="noBorder">
  <th>Set on:</th>
  <td>
    <props:selectProperty name="${trig_type}" onchange="BS.AutoProps.onTriggerTypeChange()">
      <props:option value="auto" selected="${trigger_type_in eq 'auto'}">Automatic trigger</props:option>
      <props:option value="manual" selected="${trigger_type_in eq 'manual'}">Manual trigger</props:option>
      <props:option value="custom" selected="${trigger_type_in eq 'custom'}">Custom</props:option>
    </props:selectProperty>

    <span class="smallNote" id="autoprops.type.note.auto" style="${trigger_type_in eq 'auto' ? '' : 'display: none;'}">
      When build is triggered automatically
    </span>
    <span class="smallNote" id="autoprops.type.note.manual" style="${trigger_type_in eq 'manual' ? '' : 'display: none;'}">
      When build is triggered by a person
    </span>
    <span class="smallNote" id="autoprops.type.note.custom" style="${trigger_type_in eq 'custom' ? '' : 'display: none;'}">
      When a parameter's value matches a regular expression
    </span>
  </td>
</tr>

<tr class="noBorder" id="autoprops.type.custom.variable" style="display: none">
  <th>Variable:</th>
  <td>
    <props:textProperty name="${trig_variable}" className="disableBuildTypeParams" onkeyup="BS.AutoProps.onVariableChange()"/>
    <span class="smallNote" id="autoprops.type.custom.variable.bad" style="display: none; color:#ff0000"></span>
  </td>
</tr>


<tr class="noBorder" id="autoprops.type.custom.pattern" style="display: none">
  <th>Pattern:</th>
  <td>
    <props:textProperty name="${trig_pattern}" className="disableBuildTypeParams" onkeyup="BS.AutoProps.onRegexChange()"/>
    <span class="smallNote" >
      Match trigger text against this pattern (case insensitive, not anchored)
    </span>
    <span class="smallNote" id="autoprops.type.custom.badpattern" style="display: none; color:#ff0000; font-family: monospace; font-size: 14px; white-space: pre;" />
  </td>
</tr>

<tr class="noBorder">
  <th>Parameters to set:</th>
  <td>
    <c:set var="text">Newline delimited list of <strong>name => value</strong> parameters to set<br/></c:set>
    <props:multilineProperty name="${params_list}" rows="5" cols="70" linkTitle="Edit" note="${text}"/>
    <div id="autoprops.params.missing" style="display: none" class="headerNote">
      <span class="smallNote" >The following parameters don't exist in this build type:</span>
      <span class="smallNote" id="autoprops.params.missing.list" style="color:#ff0000; white-space: pre"></span>
      <span class="smallNote" >
        <i>Missing parameters will still be set, but don't currently appear to be referenced.
           Environment variables are not checked as they can be accessed without the need for parameter substitution.</i>
      </span>
    </div>
  </td>
</tr>

<script type="text/javascript">

  //TODO: validate pattern on save
  //TODO: warn of nonexistent variables?
  BS.AutoProps = {
    
    onTriggerTypeChange: function() {
      var typeElem = $('${trig_type}');
      var typeValue = typeElem.options[typeElem.selectedIndex].value;
      
      BS.Util.hide('autoprops.type.note.auto', 'autoprops.type.note.manual', 'autoprops.type.note.custom');
      BS.Util.show('autoprops.type.note.' + typeValue);
      
      if(typeValue == "custom")
      {
        BS.Util.show('autoprops.type.custom.variable', 'autoprops.type.custom.pattern');
      }
      else
      {
        BS.Util.hide('autoprops.type.custom.variable', 'autoprops.type.custom.pattern');
      }
      
      BS.MultilineProperties.updateVisible();
    },
    
    onRegexChange: function() {
      var patternElem = $('${trig_pattern}');
      var pattern = patternElem.value;
      var tgtElem = $('autoprops.type.custom.badpattern');
      
      if(pattern != null)
      {
        BS.ajaxRequest(window['base_uri'] + '${test_url}', {
          method: "GET",
          parameters: { 'action': 'checkPattern', 'pattern': pattern },
          onComplete: function(transport)
          {
            if(transport.responseText)
            {
              BS.Util.show(tgtElem.id);
              tgtElem.textContent = 'Error: ' + transport.responseText;
            }
            else
            {
              BS.Util.hide(tgtElem.id);
            }
          },
        });
      }
    },
    
    onParametersChange: function() {
      var parmElem = $('${params_list}');
      var props = parmElem.value;
      var tgtElem = $('autoprops.params.missing');
      var tgtElemText = $('autoprops.params.missing.list');
      
      if(props != null)
      {
        var transport = BS.AutoProps.testExistence(props);
        if(transport.responseText)
        {
          BS.Util.show(tgtElem.id);
          tgtElemText.textContent = transport.responseText;
        }
        else
        {
          BS.Util.hide(tgtElem.id);
        }
      }
    },
    
    onVariableChange: function() {
      var varElem = $('${trig_variable}');
      var varName = varElem.value;
      var bad = $('autoprops.type.custom.variable.bad');
      
      if(varName == null || varName.length == 0)
      {
        bad.textContent = "Please define a source parameter name";
        BS.Util.show(bad.id);
      }
      else if(varName.indexOf('%') >= 0) //they're trying to reference another value
      {
        bad.textContent = "To prevent incorrect resolution, parameter name should not include references";
        BS.Util.show(bad.id);
      }
      else
      {
        BS.Util.hide(bad.id);
      }
    },
    
    testExistence: function(props) {
      var t = null;
      BS.ajaxRequest(window['base_uri'] + '${test_url}', {
        method: "GET",
        asynchronous: false,
        parameters: { 'action': 'checkMissingProps', 'buildTypeId': '${buildTypeId}', 'props': props },
        onComplete: function(transport)
        {
          t = transport;
        }
      });
      
      return t;
    },
  };
  
  //multilineProperty only supports onkeydown, so set this directly on the textarea it hosts
  $('${params_list}')["onkeyup"] = BS.AutoProps.onParametersChange;
  
  BS.AutoProps.onTriggerTypeChange();
  BS.AutoProps.onVariableChange();
  BS.AutoProps.onRegexChange();
  BS.AutoProps.onParametersChange();
</script>