<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="java.time.ZoneId" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%@ page import="java.util.Arrays" %>
<%@ page import="java.util.Collections" %>
<%@ page import="java.util.List" %>
<%@ page import="javax.servlet.http.Cookie" %>
<%@ page import="org.apache.commons.text.StringEscapeUtils" %>
<%@ page import="org.jivesoftware.util.CookieUtils" %>
<%@ page import="org.jivesoftware.util.ParamUtils" %>
<%@ page import="org.jivesoftware.util.StringUtils" %>
<%@ page import="org.slf4j.Logger" %>
<%@ page import="org.slf4j.LoggerFactory" %>
<%@ page import="ru.krimm.openfire.mobileaccess.MobileAccessPlugin" %>
<%@ page import="ru.krimm.openfire.mobileaccess.admin.MobileAccessAdministrationService.ManagedMobileUser" %>

<jsp:useBean id="webManager" class="org.jivesoftware.util.WebManager"/>
<% webManager.init(request, response, session, application, out); %>

<%
    final Logger logger = LoggerFactory.getLogger("ru.krimm.openfire.mobileaccess.admin.jsp");

    boolean setPassword = request.getParameter("setPassword") != null;
    boolean block = request.getParameter("block") != null;
    boolean enable = request.getParameter("enable") != null;
    boolean delete = request.getParameter("delete") != null;
    boolean grantAdmin = request.getParameter("grantAdmin") != null;
    boolean revokeAdmin = request.getParameter("revokeAdmin") != null;
    boolean actionRequested = setPassword || block || enable || delete || grantAdmin || revokeAdmin;

    String errorMessage = null;
    String username = ParamUtils.getStringParameter(request, "username", "");

    final Cookie csrfCookie = CookieUtils.getCookie(request, "csrf");
    String csrfParam = ParamUtils.getParameter(request, "csrf");

    if (actionRequested && (csrfCookie == null || csrfParam == null || !csrfCookie.getValue().equals(csrfParam))) {
        actionRequested = false;
        errorMessage = "The request was rejected because CSRF validation failed.";
        logger.warn("Mobile Access administration request rejected by CSRF validation");
    }

    if (actionRequested) {
        final String actor = webManager.getUser().getUsername();
        String resultCode = null;

        try {
            if (setPassword) {
                final String passwordValue = ParamUtils.getStringParameter(request, "password", "");
                final String confirmationValue = ParamUtils.getStringParameter(request, "passwordConfirmation", "");
                final char[] password = passwordValue.toCharArray();
                try {
                    if (!passwordValue.equals(confirmationValue)) {
                        throw new IllegalArgumentException("Password confirmation does not match");
                    }
                    MobileAccessPlugin.administrationService().setPassword(actor, username, password);
                    resultCode = "passwordUpdated";
                } finally {
                    Arrays.fill(password, '\0');
                }
            } else if (block) {
                MobileAccessPlugin.administrationService().block(actor, username);
                resultCode = "blocked";
            } else if (enable) {
                MobileAccessPlugin.administrationService().enable(actor, username);
                resultCode = "enabled";
            } else if (delete) {
                MobileAccessPlugin.administrationService().delete(actor, username);
                resultCode = "deleted";
            } else if (grantAdmin) {
                MobileAccessPlugin.administrationService().setAdministrator(actor, username, true);
                resultCode = "adminGranted";
            } else if (revokeAdmin) {
                MobileAccessPlugin.administrationService().setAdministrator(actor, username, false);
                resultCode = "adminRevoked";
            }

            response.sendRedirect("mobileaccess-admin.jsp?result=" + resultCode);
            return;
        } catch (final RuntimeException e) {
            errorMessage = e.getMessage() == null ? "Operation failed" : e.getMessage();
            logger.error("Mobile Access administration operation failed: actor={}, username={}", actor, username, e);
        }
    }

    csrfParam = StringUtils.randomString(15);
    CookieUtils.setCookie(request, response, "csrf", csrfParam, -1);

    List<ManagedMobileUser> users = Collections.emptyList();
    try {
        users = MobileAccessPlugin.administrationService().listUsers();
    } catch (final RuntimeException e) {
        errorMessage = e.getMessage() == null ? "Unable to load mobile users" : e.getMessage();
        logger.error("Unable to load managed mobile users", e);
    }

    final String result = ParamUtils.getStringParameter(request, "result", "");
    String successMessage = null;
    switch (result) {
        case "passwordUpdated":
            successMessage = "The mobile password was created or replaced successfully.";
            break;
        case "blocked":
            successMessage = "Mobile access was blocked.";
            break;
        case "enabled":
            successMessage = "Mobile access was enabled.";
            break;
        case "deleted":
            successMessage = "The mobile credential was deleted.";
            break;
        case "adminGranted":
            successMessage = "Administrator access was granted.";
            break;
        case "adminRevoked":
            successMessage = "Administrator access was revoked.";
            break;
        default:
            break;
    }

    final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault());
%>

<html>
<head>
    <title>Mobile Access <%= MobileAccessPlugin.VERSION %></title>
    <meta name="pageID" content="mobileaccess-admin"/>
</head>
<body>

<p><strong>Plugin version:</strong> <%= MobileAccessPlugin.VERSION %></p>

<% if (errorMessage != null) { %>
<div class="jive-contentBox">
    <strong>Error:</strong> <%= StringEscapeUtils.escapeHtml4(errorMessage) %>
</div>
<br/>
<% } else if (successMessage != null) { %>
<div class="jive-contentBox">
    <strong>Success:</strong> <%= StringEscapeUtils.escapeHtml4(successMessage) %>
</div>
<br/>
<% } %>

<p>
    Manage separate mobile credentials for LDAP-backed Openfire users.
    Blocking preserves the password; deletion removes the credential permanently.
</p>

<form action="mobileaccess-admin.jsp" method="post" autocomplete="off">
    <input type="hidden" name="csrf" value="<%= StringEscapeUtils.escapeHtml4(csrfParam) %>"/>

    <fieldset>
        <legend>Create or replace mobile password</legend>
        <table>
            <tbody>
            <tr>
                <td><label for="username">Username</label></td>
                <td><input id="username" name="username" type="text" maxlength="64" required/></td>
            </tr>
            <tr>
                <td><label for="password">New password</label></td>
                <td><input id="password" name="password" type="password" minlength="12" maxlength="256" required autocomplete="new-password"/></td>
            </tr>
            <tr>
                <td><label for="passwordConfirmation">Confirm password</label></td>
                <td><input id="passwordConfirmation" name="passwordConfirmation" type="password" minlength="12" maxlength="256" required autocomplete="new-password"/></td>
            </tr>
            </tbody>
        </table>
    </fieldset>

    <br/>
    <input type="submit" name="setPassword" value="Create or replace password"/>
</form>

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
                    <input type="hidden" name="csrf" value="<%= StringEscapeUtils.escapeHtml4(csrfParam) %>"/>
                    <input type="hidden" name="username" value="<%= StringEscapeUtils.escapeHtml4(user.username()) %>"/>
                    <% if (user.enabled()) { %>
                        <input type="submit" name="block" value="Block"/>
                    <% } else { %>
                        <input type="submit" name="enable" value="Enable"/>
                    <% } %>
                    <% if (user.administrator()) { %>
                        <input type="submit" name="revokeAdmin" value="Remove administrator"/>
                    <% } else { %>
                        <input type="submit" name="grantAdmin" value="Make administrator"/>
                    <% } %>
                    <input type="submit" name="delete" value="Delete"/>
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
