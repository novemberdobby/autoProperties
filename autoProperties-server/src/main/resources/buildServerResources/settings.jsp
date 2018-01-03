<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>

<c:set var="trigger_type_in" value="${empty propertiesBean.properties['trigger.type'] ? 'auto' : propertiesBean.properties['trigger.type']}"/>

<tr class="noBorder">
  <th>Set properties:</th>
  <td>
    <props:selectProperty name="trigger.type" onchange="BS.AutoProps.onTriggerTypeChange()">
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


<script type="text/javascript">

  BS.AutoProps = {
    onTriggerTypeChange: function() {
      var typeElem = $('trigger.type');
      var typeValue = typeElem.options[typeElem.selectedIndex].value;
      
      BS.Util.hide('autoprops.type.note.auto');
      BS.Util.hide('autoprops.type.note.manual');
      BS.Util.hide('autoprops.type.note.custom');
      
      switch(typeValue)
      {
        default:
        case "auto":
          BS.Util.show('autoprops.type.note.auto');
          break;
        case "manual":
          BS.Util.show('autoprops.type.note.manual');
          break;
        case "custom":
          BS.Util.show('autoprops.type.note.custom');
          break;
      }
      
      BS.MultilineProperties.updateVisible();
    }
  };
</script>