<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="java.util.Arrays" %>
<%@ page import="org.apache.commons.text.StringEscapeUtils" %>
<%@ page import="org.jivesoftware.util.WebManager" %>
<%@ page import="org.jivesoftware.util.ParamUtils" %>
<%@ page import="org.slf4j.Logger" %>
<%@ page import="org.slf4j.LoggerFactory" %>
<%@ page import="ru.krimm.openfire.mobileaccess.MobileAccessPlugin" %>
<%@ taglib uri="admin" prefix="admin" %>

<%!
    private static final Logger LOGGER = LoggerFactory.getLogger("ru.krimm.openfire.mobileaccess.admin.jsp");
%>

<%
    final WebManager webManager = new WebManager();
    webManager.init(request, response, session, application, out);

    String message = null;
    String messageType = "success";
    final boolean changePassword = request.getParameter("changePassword") != null;
    final boolean revokePassword = request.getParameter("revokePassword") != null;

    LOGGER.info(
        "Mobile Access admin request received: method={}, changePassword={}, revokePassword={}",
        request.getMethod(),
        changePassword,
        revokePassword
    );

    if (changePassword || revokePassword) {
        final String actor = webManager.getUser().getUsername();
        final String username = ParamUtils.getStringParameter(request, "username", "");
        LOGGER.info(
            "Mobile Access admin operation accepted: actor={}, target={}, action={}",
            actor,
            username,
            changePassword ? "SET_PASSWORD" : "REVOKE_PASSWORD"
        );
        try {
            if (changePassword) {
                final String passwordValue = ParamUtils.getStringParameter(request, "password", "");
                final String confirmationValue = ParamUtils.getStringParameter(request, "passwordConfirmation", "");
                final char[] password = passwordValue.toCharArray();
                try {
                    LOGGER.info(
                        "Mobile Access password fields parsed: target={}, passwordPresent={}, confirmationPresent={}, lengthsMatch={}",
                        username,
                        !passwordValue.isBlank(),
                        !confirmationValue.isBlank(),
                        passwordValue.length() == confirmationValue.length()
                    );
                    if (!passwordValue.equals(confirmationValue)) {
                        throw new IllegalArgumentException("Password confirmation does not match");
                    }
                    LOGGER.info("Calling MobileAccessAdministrationService.setPassword for target={}", username);
                    MobileAccessPlugin.administrationService().setPassword(actor, username, password);
                    LOGGER.info("MobileAccessAdministrationService.setPassword completed for target={}", username);
                    message = "The mobile password was created or replaced successfully.";
                } finally {
                    Arrays.fill(password, '\0');
                }
            } else {
                LOGGER.info("Calling MobileAccessAdministrationService.revoke for target={}", username);
                MobileAccessPlugin.administrationService().revoke(actor, username);
                LOGGER.info("MobileAccessAdministrationService.revoke completed for target={}", username);
                message = "Mobile access was revoked successfully.";
            }
        } catch (final IllegalArgumentException e) {
            LOGGER.warn("Mobile Access admin operation rejected for target={}: {}", username, e.getMessage());
            message = e.getMessage();
            messageType = "error";
        } catch (final RuntimeException e) {
            LOGGER.error("Mobile Access admin operation failed for target=" + username, e);
            message = "The operation failed. Review the Openfire log and Mobile Access audit table.";
            messageType = "error";
        }
    }

    final Object csrfAttribute = request.getAttribute("csrf");
    final String csrf = csrfAttribute == null ? "" : csrfAttribute.toString();
    if (csrf.isEmpty()) {
        LOGGER.warn("Openfire did not provide a CSRF token for the Mobile Access admin page");
    }
%>
<html>
<head>
    <title>Mobile Access</title>
    <meta name="pageID" content="mobileaccess-admin"/>
</head>
<body>
<% if (message != null) { %>
    <div class="jive-contentBox">
        <strong><%= "error".equals(messageType) ? "Error" : "Success" %>:</strong>
        <%= StringEscapeUtils.escapeHtml4(message) %>
    </div>
<% } %>

<p>Manage a separate local password for an LDAP-backed Openfire user. The user must exist and belong to the configured allowed group.</p>

<div class="jive-contentBoxHeader">Create or replace mobile password</div>
<div class="jive-contentBox">
    <form action="mobileaccess-admin.jsp" method="post" autocomplete="off">
        <input type="hidden" name="csrf" value="<%= StringEscapeUtils.escapeHtml4(csrf) %>"/>
        <table cellspacing="0" border="0">
            <tr><td><label for="username">Username</label></td><td><input id="username" name="username" type="text" maxlength="64" required/></td></tr>
            <tr><td><label for="password">New password</label></td><td><input id="password" name="password" type="password" minlength="12" maxlength="256" required autocomplete="new-password"/></td></tr>
            <tr><td><label for="passwordConfirmation">Confirm password</label></td><td><input id="passwordConfirmation" name="passwordConfirmation" type="password" minlength="12" maxlength="256" required autocomplete="new-password"/></td></tr>
        </table>
        <button type="submit" name="changePassword">Create or replace password</button>
    </form>
</div>

<br/>
<div class="jive-contentBoxHeader">Revoke mobile access</div>
<div class="jive-contentBox">
    <form action="mobileaccess-admin.jsp" method="post">
        <input type="hidden" name="csrf" value="<%= StringEscapeUtils.escapeHtml4(csrf) %>"/>
        <label for="revokeUsername">Username</label>
        <input id="revokeUsername" name="username" type="text" maxlength="64" required/>
        <button type="submit" name="revokePassword">Revoke</button>
    </form>
</div>
</body>
</html>
