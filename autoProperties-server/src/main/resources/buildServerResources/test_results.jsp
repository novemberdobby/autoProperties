<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>

<style type="text/css">
#resultsTable th {
  border: 1px solid #ccc;
  padding-right: 10px;
  background: #f6f6f6;
}

.qualify {
  background: #ceffbf;
}

.noqualify {
  
}
</style>

<div>${info}</div>
<table id="resultsTable" cellpadding="4">
    <tr>
      <td class="qualify">Qualifies</td>
      <td class="noqualify">Doesn't qualify</td>
    </tr>
</table>

<br>

<table id="resultsTable" cellpadding="4">
  <thead>
    <tr>
      <th>Build</th>
      <th>Status</th>
      <th>Changes</th>
      <th>Started</th>
      <th>Agent</th>
      <c:if test='${not empty extra_column_name}'>
        <th>${extra_column_name}</th>
      </c:if>
    </tr>
  </thead>
  <c:forEach items="${builds}" var="build">
    <tr>
      <bs:buildRow
          build="${build.getBuild()}"
          showBuildNumber="true"
          addLinkToBuildNumber="true"
          showStatus="true"
          showChanges="true"
          showStartDate="true"
          showAgent="true"
          rowClass="${build.getSet() ? 'qualify' : 'noqualify'}"
      />
      
      <c:if test='${not empty extra_column_name}'>
        <td class="${build.getSet() ? 'qualify' : 'noqualify'}">${build.getVarName()}</td>
      </c:if>
    </tr>
    
  </c:forEach>
</table>