<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ page import="novemberdobby.teamcity.autoProperties.common.AutoPropsConstants" %>

<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>

<c:set var="params_list" value="<%=AutoPropsConstants.SETTING_PARAMS%>"/>
<c:set var="trig_type" value="<%=AutoPropsConstants.SETTING_TYPE%>"/>
<c:set var="trig_pattern" value="<%=AutoPropsConstants.SETTING_CUSTOM_PATTERN%>"/>
<c:set var="trigger_type_in" value="${empty propertiesBean.properties[trig_type] ? 'auto' : propertiesBean.properties[trig_type]}"/>

<tr class="noBorder">
  <th>Set properties:</th>
  <td>
    <props:selectProperty name="${trig_type}" onchange="BS.AutoProps.onTriggerTypeChange()">
      <props:option value="auto" selected="${trigger_type_in eq 'auto'}">Automatic trigger (default)</props:option>
      <props:option value="manual" selected="${trigger_type_in eq 'manual'}">Manual trigger</props:option>
      <props:option value="custom" selected="${trigger_type_in eq 'custom'}">Custom</props:option>
    </props:selectProperty>

    <span class="smallNote" id="autoprops.type.note.auto" style="${trigger_type_in eq 'auto' ? '' : 'display: none;'}">
      When build is triggered automatically
    </span>
    <span class="smallNote" id="autoprops.type.note.manual" style="${trigger_type_in eq 'manual' ? '' : 'display: none;'}">
      When build is triggered by a person
      <br/>
    </span>
    <span class="smallNote" id="autoprops.type.note.custom" style="${trigger_type_in eq 'custom' ? '' : 'display: none;'}">
      When trigger text matches a custom pattern (regex)
      <br/>
    </span>
  </td>
</tr>

<tr class="noBorder">
  <th>Parameters to set:</th>
  <td>
    <c:set var="text">Newline delimited list of <strong>name => value</strong> parameters to set<br/></c:set>
    <props:multilineProperty name="${params_list}" rows="5" cols="70" linkTitle="Edit parameters" note="${text}"/>
</tr>

<script type="text/javascript">

  //TODO: validate pattern on save
  //TODO: warn of nonexistent variables?
  BS.AutoProps = {
    onTriggerTypeChange: function() {
      var typeElem = $(${trig_type});
      var typeValue = typeElem.options[typeElem.selectedIndex].value;
      
      //TODO: BS.Util.hide('[id^="autoprops.type.note."]'); or iterate an enum
      BS.Util.hide('autoprops.type.note.auto');
      BS.Util.hide('autoprops.type.note.manual');
      BS.Util.hide('autoprops.type.note.custom');
      
      BS.Util.show('autoprops.type.note.' + typeValue);
      
      BS.MultilineProperties.updateVisible();
    }
  };
</script>