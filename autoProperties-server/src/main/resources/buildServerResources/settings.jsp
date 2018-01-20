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
    <props:textProperty name="${trig_variable}" className="disableBuildTypeParams"/>
    <span class="error" id="error_${trig_variable}"></span>
  </td>
</tr>


<tr class="noBorder" id="autoprops.type.custom.pattern" style="display: none">
  <th>Pattern:</th>
  <td>
    <props:textProperty name="${trig_pattern}" className="disableBuildTypeParams"/>
    <span class="smallNote" >
      Match trigger text against this pattern (case insensitive, not anchored)
    </span>
    <span class="error" id="error_${trig_pattern}" style="font-family: monospace; font-size: 14px; white-space: pre;" />
  </td>
</tr>

<tr class="noBorder">
  <th>Parameters to set:</th>
  <td>
    <c:set var="text">Newline delimited list of <strong>name => value</strong> parameters to set<br/></c:set>
    <props:multilineProperty name="${params_list}" rows="5" cols="70" linkTitle="" expanded="true" note="${text}"/>
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
#testOnBuildResults tbody td {
  border: 1px solid #ccc;
  padding: 3px;
}

.qualify {
  color: #2dc300;
  font-weight: bold;
}

.noqualify {
  color:#bbbbbb;
}
</style>

<tr class="noBorder">
  <td colspan="2">
    <div><forms:button id="openTestDialogBtn" onclick="BS.AutoProps.openTestDialog()" className="btn">Test on previous builds</forms:button></div>
    <div><forms:saving savingTitle="Getting builds..." id="getBuildsProgress"/></div>
    
    <bs:dialog dialogId="testOnBuildDialog" title="Previous builds" closeCommand="BS.AutoProps.TestOnBuildDialog.close()">
      <div id="testOnBuildResultsDiv">
        <div>Showing last 50 builds. Key:</div>
        <div class="qualify">Build qualifies</div>
        <div class="noqualify">Build does not qualify</div>
        <table id="testOnBuildResults">
          <thead>
            <tr>
              <th>Number</th>
              <%-- <th>ID</th> --%>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
          
          </tbody>
        </table>
      </div>
      <div class="popupSaveButtonsBlock">
        <forms:cancel label="Close" onclick="BS.AutoProps.TestOnBuildDialog.close()"/>
        <forms:saving id="testProgress"/>
      </div>
    </bs:dialog>
  </td>
</tr>

<script type="text/javascript">

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
            if(transport && transport.responseText)
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
    
    TestOnBuildDialog: OO.extend(BS.AbstractModalDialog, {
      getContainer: function () {
        return $('testOnBuildDialog');
      },
      
      init: function(transport) {
        var builds = transport.responseXML.firstChild.getElementsByTagName("build");
        if (builds && builds.length > 0) {
          
          var body = $('testOnBuildResults').lastChild;
          var newBody = document.createElement('tbody');
          
          for (var i = 0; i < builds.length; i++) {
            var row = document.createElement("tr");
            newBody.appendChild(row);
            
            var bId = builds[i].getAttribute("id");
            var bNumber = builds[i].getAttribute("number");
            var dNumber = document.createElement("td");
            dNumber.innerHTML = "<a href='/viewLog.html?buildId=" + bId + "'>" + bNumber + "</a>";
            row.appendChild(dNumber);
            
            <%--  //don't really need this
            var dId = document.createElement("td");
            dId.appendChild(document.createTextNode(bId));
            row.appendChild(dId);
            --%>
            
            var bStatus = builds[i].getAttribute("status");
            var dStatus = document.createElement("td");
            dStatus.appendChild(document.createTextNode(bStatus));
            row.appendChild(dStatus);
            
            if(builds[i].getAttribute("set") == "false")
            {
              row.className = "noqualify";
            }
            else
            {
              row.className = "qualify";
            }
          }
          
          body.parentNode.replaceChild(newBody, body);
        }
      },
      
      beforeShow: function() {
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
            '${trig_pattern}': $('${trig_pattern}').value
          },
          onComplete: function(transport) {
            BS.AutoProps.TestOnBuildDialog.init(transport);
            BS.AutoProps.TestOnBuildDialog.showCentered();
            BS.Util.hide('getBuildsProgress');
          }
      });
    }
  };
  
  //multilineProperty only supports onkeydown, so set this directly on the textarea it hosts
  $('${params_list}')["onkeyup"] = BS.AutoProps.onParametersChange;
  
  BS.AutoProps.onTriggerTypeChange();
  BS.AutoProps.onParametersChange();
</script>