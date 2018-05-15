<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>
<%@ page import="novemberdobby.teamcity.autoProperties.common.AutoPropsConstants" %>

<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>

<c:set var="params_list" value="<%=AutoPropsConstants.SETTING_PARAMS%>"/>
<c:set var="trig_type" value="<%=AutoPropsConstants.SETTING_TYPE%>"/>
<c:set var="trig_variable" value="<%=AutoPropsConstants.SETTING_CUSTOM_VARIABLE%>"/>
<c:set var="trig_pattern" value="<%=AutoPropsConstants.SETTING_CUSTOM_PATTERN%>"/>
<c:set var="trig_type_name" value="<%=AutoPropsConstants.SETTING_TRIGGER_TYPE_NAME%>"/>
<c:set var="test_url" value="<%=AutoPropsConstants.TESTING_URL%>"/>

<%-- TODO this is probably provided in some other correct way without substringing --%>
<c:set var='buildTypeId' value='<%= request.getParameter("id") %>'/>

<tr class="noBorder">
  <th>Set on:</th>
  <td>
    <props:selectProperty name="${trig_type}" onchange="BS.AutoProps.onTriggerTypeChange()">
      <props:option value="auto" >Automatic trigger</props:option>
      <props:option value="manual">Manual trigger</props:option>
      <props:option value="trigger_type">By trigger type</props:option>
      <props:option value="custom">Custom</props:option>
    </props:selectProperty>

    <span class="smallNote" id="autoprops.type.note.auto">When build is triggered automatically</span>
    <span class="smallNote" id="autoprops.type.note.manual">When build is triggered by a person</span>
    <span class="smallNote" id="autoprops.type.note.trigger_type">When build is started by a trigger of the specified type (VCS, schedule trigger etc)</span>
    <span class="smallNote" id="autoprops.type.note.custom">When a parameter's value matches a regular expression (on build start)</span>
  </td>
</tr>

<tr class="noBorder" id="autoprops.type.custom.variable" style="display: none">
  <th>Variable:</th>
  <td>
    <props:textProperty name="${trig_variable}" className="disableBuildTypeParams"/>
    <span class="error" id="error_${trig_variable}"></span>
    <ul id="ac_dropdown" style="display: none; overflow-y:auto; max-height:300px; padding-left: 0px" ></ul>
  </td>
</tr>

<tr class="noBorder" id="autoprops.type.trigger_type.name" style="display: none">
  <th>Trigger type name:</th>
  <td>
    <props:textProperty name="${trig_type_name}" className="disableBuildTypeParams"/>
    <span class="error" id="error_${trig_type_name}"></span>
    <ul id="tt_dropdown" style="display: none; overflow-y:auto; max-height:300px; padding-left: 0px" ></ul>
  </td>
</tr>


<tr class="noBorder" id="autoprops.type.custom.pattern" style="display: none">
  <th>Pattern:</th>
  <td>
    <props:textProperty name="${trig_pattern}" className="disableBuildTypeParams"/>
    <span class="smallNote" >
      Match trigger text against this pattern (case insensitive, not anchored). Leave empty to test for parameter existence.
    </span>
    <span class="error" id="error_${trig_pattern}" style="font-family: monospace; font-size: 14px; white-space: pre; " /></span>
  </td>
</tr>

<tr class="noBorder">
  <th>Parameters to set:</th>
  <td>
    <c:set var="text">Newline delimited list of <strong>name => value</strong> parameters to set<br/></c:set>
    <props:multilineProperty name="${params_list}" rows="5" cols="70" linkTitle="" expanded="true" note="${text}"/>
    <br>
    <div id="autoprops.params.missing" style="display: none" class="headerNote">
      <span class="smallNote" >The following target parameters don't exist in this build type:</span>
      <span class="smallNote" id="autoprops.params.missing.list" style="color:#ff0000; white-space: pre"></span>
      <span class="smallNote" >
        <i>Missing parameters will still be set, but don't currently appear to be referenced.
           Environment variables are not checked as they can be accessed without the need for parameter substitution.</i>
      </span>
    </div>
  </td>
</tr>

<style type="text/css">
#testOnBuildResults tbody tr td, #testOnBuildResultsKey tbody tr td {
  border: 1px solid #ccc;
  max-width: 900px;
  overflow-wrap: break-word;
}

.listVar {
  line-height: 200%;
  color: cornflowerblue;
  cursor: pointer;
}

.wideDialog {
  width: 60em;
}
</style>

<tr class="noBorder">
  <td colspan="2">
    <forms:button id="openTestDialogBtn" onclick="BS.AutoProps.openTestDialog()" className="btn">Test on previous builds</forms:button>
    <forms:saving id="getBuildsProgress"/>
    
    <bs:dialog dialogId="testOnBuildDialog" dialogClass="wideDialog" title="Previous builds" closeCommand="BS.AutoProps.TestOnBuildDialog.close()">
      <div id="testOnBuildResults" style="overflow-y:auto; height:400px"></div>
      <div class="popupSaveButtonsBlock">
        <forms:cancel label="Close" onclick="BS.AutoProps.TestOnBuildDialog.close()"/>
      </div>
    </bs:dialog>
  </td>
</tr>

<script type="text/javascript">

  BS.AutoProps = {
    
    onTriggerTypeChange: function() {
      var typeElem = $('${trig_type}');
      var typeValue = typeElem.options[typeElem.selectedIndex].value;
      
      BS.Util.hide('autoprops.type.note.auto', 'autoprops.type.note.manual', 'autoprops.type.note.trigger_type', 'autoprops.type.note.custom');
      BS.Util.show('autoprops.type.note.' + typeValue);
      
      if(typeValue == "custom")
      {
        BS.Util.show('autoprops.type.custom.variable', 'autoprops.type.custom.pattern');
      }
      else
      {
        BS.Util.hide('autoprops.type.custom.variable', 'autoprops.type.custom.pattern');
      }
      
      if(typeValue == "trigger_type")
      {
        BS.Util.show('autoprops.type.trigger_type.name');
      }
      else
      {
        BS.Util.hide('autoprops.type.trigger_type.name');
      }
      
      BS.MultilineProperties.updateVisible();
    },
    
    onParametersChange: function() {
      //run our parameter checks in here, as it's only a warning and shouldn't fail validation in the PropertiesProcessor
      var parmElem = $('${params_list}');
      var props = parmElem.value;
      var tgtElem = $('autoprops.params.missing');
      var tgtElemText = $('autoprops.params.missing.list');
      
      if(props != null)
      {
        BS.ajaxRequest(window['base_uri'] + '${test_url}', {
          method: "GET",
          parameters: { 'action': 'checkMissingProps', 'buildTypeId': '${buildTypeId}', 'props': props },
          onComplete: function(transport)
          {
            if(transport && transport.status == 200 && transport.responseText)
            {
              BS.Util.show(tgtElem.id);
              tgtElemText.textContent = transport.responseText;
            }
            else
            {
              BS.Util.hide(tgtElem.id);
            }
          }
        });
      }
    },
    
    onTriggerVariableChange: function() {
      BS.AutoProps.onDropdownChange('${trig_variable}', 'ac_dropdown', 'autoCompleteVar');
    },
    
    onTriggerTypeNameChange: function() {
      BS.AutoProps.onDropdownChange('${trig_type_name}', 'tt_dropdown', 'listTriggerTypes');
    },
    
    onDropdownChange: function(sVarElemId, sTgtElemId, sAction, clickFunc) {
      var varElem = $(sVarElemId);
      var varValue = varElem.value;
      var tgtElem = $(sTgtElemId);
      
      BS.ajaxRequest(window['base_uri'] + '${test_url}', {
        method: "GET",
        parameters: { 'action': sAction, 'buildTypeId': '${buildTypeId}', 'name': varValue },
        onComplete: function(transport)
        {
          BS.Util.hide(tgtElem.id);
          if(transport && transport.status == 200 && transport.responseXML)
          {
            var props = transport.responseXML.firstChild.getElementsByTagName("var");
            if (props && props.length > 0)
            {
              tgtElem.innerHTML = "";
              for (var i = 0; i < props.length; i++)
              {
                var sName = props[i].getAttribute("name");
                var sDisplay = props[i].getAttribute("display");
                if(!sDisplay)
                {
                  sDisplay = sName;
                }
                tgtElem.innerHTML += '<li class="listVar" onclick="BS.AutoProps.setElem(\'' + sVarElemId + '\',\'' + sName + '\',\'' + sTgtElemId + '\')">' + sDisplay + '</li>';
              }
              
              //don't show the "dropdown" if the only item is what's already in the box
              if(!(props.length == 1 && props[0].getAttribute("name") == varElem.value))
              {
                BS.Util.show(tgtElem.id);
              }
            }
          }
        }
      });
    },
    
    setElem: function(sElemId, sVal, sHide) {
      $(sElemId).value = sVal;
      BS.Util.hide(sHide);
    },
    
    TestOnBuildDialog: OO.extend(BS.AbstractModalDialog, {
      getContainer: function () {
        return $('testOnBuildDialog');
      },
      
      init: function(transport) {
        $('testOnBuildResults').innerHTML = transport.responseText;
      },
    }),
    
    openTestDialog: function() {
      $('openTestDialogBtn').disabled = 'disabled';
      BS.Util.show('getBuildsProgress');
    
      BS.ajaxRequest(window['base_uri'] + '${test_url}', {
          method: "GET",
          parameters: {
            'action': 'checkBuilds',
            'buildTypeId': '${buildTypeId}',
            '${trig_type}': $('${trig_type}').value,
            '${trig_variable}': $('${trig_variable}').value,
            '${trig_pattern}': $('${trig_pattern}').value,
            '${trig_type_name}': $('${trig_type_name}').value
          },
          onComplete: function(transport) {
            BS.Util.hide('getBuildsProgress');
            if(transport)
            {
              if(transport.status == 200)
              {
                BS.AutoProps.TestOnBuildDialog.init(transport);
                BS.AutoProps.TestOnBuildDialog.showCentered();
              }
              else if(transport.status == 400) //SC_BAD_REQUEST
              {
                //prompt a save so the error is shown, there's probably a better way ¯\_(ツ)_/¯
                $('submitBuildFeatureId').click();
              }
            }
          }
      });
    }
  };
  
  //multilineProperty only supports onkeydown, so set this directly on the textarea it hosts
  $('${params_list}')["onkeyup"] = BS.AutoProps.onParametersChange;
  $('${trig_variable}')["onkeyup"] = BS.AutoProps.onTriggerVariableChange;
  $('${trig_variable}')["onclick"] = BS.AutoProps.onTriggerVariableChange;
  $('${trig_type_name}')["onkeyup"] = BS.AutoProps.onTriggerTypeNameChange;
  $('${trig_type_name}')["onclick"] = BS.AutoProps.onTriggerTypeNameChange;
  
  BS.AutoProps.onTriggerTypeChange();
  BS.AutoProps.onParametersChange();
</script>