<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="java.util.Arrays" %>
<%@ page import="javax.servlet.http.Cookie" %>
<%@ page import="org.apache.commons.text.StringEscapeUtils" %>
<%@ page import="org.jivesoftware.admin.WebManager" %>
<%@ page import="org.jivesoftware.util.CookieUtils" %>
<%@ page import="org.jivesoftware.util.ParamUtils" %>
<%@ page import="org.jivesoftware.util.StringUtils" %>
<%@ page import="ru.krimm.openfire.mobileaccess.MobileAccessPlugin" %>
<%@ taglib uri="admin" prefix="admin" %>

<%
    final WebManager webManager = new WebManager();
    webManager.init(request, response, session, application, out);

    String message = null;
    String messageType = "success";
    final boolean changePassword = request.getParameter("changePassword") != null;
    final boolean revokePassword = request.getParameter("revokePassword") != null;

    if (changePassword || revokePassword) {
        final Cookie csrfCookie = CookieUtils.getCookie(request, "csrf");
        final String submittedCsrf = ParamUtils.getParameter(request, "csrf");
        if (csrfCookie == null || submittedCsrf == null || !csrfCookie.getValue().equals(submittedCsrf)) {
            message = "The request was rejected because CSRF validation failed.";
            messageType = "error";
        } else {
            final String actor = webManager.getUser().getUsername();
            final String username = ParamUtils.getStringParameter(request, "username", "");
            try {
                if (changePassword) {
                    final String passwordValue = ParamUtils.getStringParameter(request, "password", "");
                    final String confirmationValue = ParamUtils.getStringParameter(request, "passwordConfirmation", "");
                    final char[] password = passwordValue.toCharArray();
                    try {
                        if (!passwordValue.equals(confirmationValue)) {
                            throw new IllegalArgumentException("Password confirmation does not match");
                        }
                        MobileAccessPlugin.administrationService().setPassword(actor, username, password);
                        message = "The mobile password was created or replaced successfully.";
                    } finally {
                        Arrays.fill(password, '\0');
                    }
                } else {
                    MobileAccessPlugin.administrationService().revoke(actor, username);
                    message = "Mobile access was revoked successfully.";
                }
            } catch (final IllegalArgumentException e) {
                message = e.getMessage();
                messageType = "error";
            } catch (final RuntimeException e) {
                message = "The operation failed. Review the Openfire log and Mobile Access audit table.";
                messageType = "error";
            }
        }
    }

    final String csrf = StringUtils.randomString(15);
    CookieUtils.setCookie(request, response, "csrf", csrf, -1);
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
        <input type="hidden" name="csrf" value="<%= csrf %>"/>
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
        <input type="hidden" name="csrf" value="<%= csrf %>"/>
        <label for="revokeUsername">Username</label>
        <input id="revokeUsername" name="username" type="text" maxlength="64" required/>
        <button type="submit" name="revokePassword">Revoke</button>
    </form>
</div>
</body>
</html>
