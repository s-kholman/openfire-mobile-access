<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="java.time.ZoneId" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%@ page import="java.util.Arrays" %>
<%@ page import="java.util.List" %>
<%@ page import="javax.servlet.http.Cookie" %>
<%@ page import="org.apache.commons.text.StringEscapeUtils" %>
<%@ page import="org.jivesoftware.util.WebManager" %>
<%@ page import="org.jivesoftware.util.CookieUtils" %>
<%@ page import="org.jivesoftware.util.ParamUtils" %>
<%@ page import="org.jivesoftware.util.StringUtils" %>
<%@ page import="ru.krimm.openfire.mobileaccess.MobileAccessPlugin" %>
<%@ page import="ru.krimm.openfire.mobileaccess.admin.MobileAccessAdministrationService.ManagedMobileUser" %>
<%@ taglib uri="admin" prefix="admin" %>

<%
    final WebManager webManager = new WebManager();
    webManager.init(request, response, session, application, out);

    String message = null;
    String messageType = "success";
    final boolean changePassword = request.getParameter("changePassword") != null;
    final boolean blockAccess = request.getParameter("blockAccess") != null;
    final boolean enableAccess = request.getParameter("enableAccess") != null;
    final boolean deleteCredential = request.getParameter("deleteCredential") != null;
    final boolean grantAdmin = request.getParameter("grantAdmin") != null;
    final boolean revokeAdmin = request.getParameter("revokeAdmin") != null;
    final boolean hasOperation = changePassword || blockAccess || enableAccess || deleteCredential || grantAdmin || revokeAdmin;

    if (hasOperation) {
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
                } else if (blockAccess) {
                    MobileAccessPlugin.administrationService().block(actor, username);
                    message = "Mobile access was blocked.";
                } else if (enableAccess) {
                    MobileAccessPlugin.administrationService().enable(actor, username);
                    message = "Mobile access was enabled.";
                } else if (deleteCredential) {
                    MobileAccessPlugin.administrationService().delete(actor, username);
                    message = "The mobile credential was deleted.";
                } else if (grantAdmin) {
                    MobileAccessPlugin.administrationService().setAdministrator(actor, username, true);
                    message = "Administrator access was granted.";
                } else {
                    MobileAccessPlugin.administrationService().setAdministrator(actor, username, false);
                    message = "Administrator access was revoked.";
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
    final List<ManagedMobileUser> users = MobileAccessPlugin.administrationService().listUsers();
    final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault());
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

<p>Manage separate mobile credentials for LDAP-backed Openfire users. Blocking preserves the password; deletion removes the credential permanently.</p>

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
<div class="jive-contentBoxHeader">Managed mobile users (<%= users.size() %>)</div>
<div class="jive-contentBox">
<% if (users.isEmpty()) { %>
    <p>No mobile credentials have been created.</p>
<% } else { %>
    <table class="jive-table" cellspacing="0" cellpadding="3" width="100%">
        <thead>
        <tr>
            <th>Username</th>
            <th>Status</th>
            <th>Administrator</th>
            <th>Updated</th>
            <th>Actions</th>
        </tr>
        </thead>
        <tbody>
        <% for (final ManagedMobileUser user : users) { %>
        <tr>
            <td><strong><%= StringEscapeUtils.escapeHtml4(user.username()) %></strong></td>
            <td><%= user.enabled() ? "Enabled" : "Blocked" %></td>
            <td><%= user.administrator() ? "Yes" : "No" %></td>
            <td><%= user.updatedAt() == null ? "—" : dateFormatter.format(user.updatedAt()) %></td>
            <td>
                <form action="mobileaccess-admin.jsp" method="post" style="display:inline">
                    <input type="hidden" name="csrf" value="<%= StringEscapeUtils.escapeHtml4(csrf) %>"/>
                    <input type="hidden" name="username" value="<%= StringEscapeUtils.escapeHtml4(user.username()) %>"/>
                    <% if (user.enabled()) { %>
                        <button type="submit" name="blockAccess">Block</button>
                    <% } else { %>
                        <button type="submit" name="enableAccess">Enable</button>
                    <% } %>
                    <% if (user.administrator()) { %>
                        <button type="submit" name="revokeAdmin">Remove administrator</button>
                    <% } else { %>
                        <button type="submit" name="grantAdmin">Make administrator</button>
                    <% } %>
                    <button type="submit" name="deleteCredential" onclick="return confirm('Delete the mobile credential for this user?');">Delete</button>
                </form>
            </td>
        </tr>
        <% } %>
        </tbody>
    </table>
<% } %>
</div>
</body>
</html>
