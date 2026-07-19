<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="java.time.ZoneId" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%@ page import="java.util.Arrays" %>
<%@ page import="java.util.Collections" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="javax.servlet.http.Cookie" %>
<%@ page import="org.jivesoftware.util.CookieUtils" %>
<%@ page import="org.jivesoftware.util.ParamUtils" %>
<%@ page import="org.jivesoftware.util.StringUtils" %>
<%@ page import="org.slf4j.LoggerFactory" %>
<%@ page import="ru.krimm.openfire.mobileaccess.MobileAccessPlugin" %>
<%@ page import="ru.krimm.openfire.mobileaccess.admin.MobileAccessAdministrationService.ManagedMobileUser" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="admin" uri="admin" %>

<jsp:useBean id="webManager" class="org.jivesoftware.util.WebManager"/>
<% webManager.init(request, response, session, application, out); %>

<%
    boolean create = request.getParameter("create") != null;
    boolean block = request.getParameter("block") != null;
    boolean enable = request.getParameter("enable") != null;
    boolean delete = request.getParameter("delete") != null;
    boolean grantAdmin = request.getParameter("grantAdmin") != null;
    boolean revokeAdmin = request.getParameter("revokeAdmin") != null;
    boolean actionRequested = create || block || enable || delete || grantAdmin || revokeAdmin;

    String username = ParamUtils.getParameter(request, "username");
    String password = ParamUtils.getParameter(request, "password");
    String passwordConfirm = ParamUtils.getParameter(request, "passwordConfirm");

    Cookie csrfCookie = CookieUtils.getCookie(request, "csrf");
    String csrfParam = ParamUtils.getParameter(request, "csrf");

    Map<String, String> errors = new HashMap<>();

    if (actionRequested) {
        if (csrfCookie == null || csrfParam == null || !csrfCookie.getValue().equals(csrfParam)) {
            actionRequested = false;
            create = false;
            block = false;
            enable = false;
            delete = false;
            grantAdmin = false;
            revokeAdmin = false;
            errors.put("csrf", "CSRF Failure!");
        }
    }

    csrfParam = StringUtils.randomString(15);
    CookieUtils.setCookie(request, response, "csrf", csrfParam, -1);
    pageContext.setAttribute("csrf", csrfParam);

    if (create) {
        if (username == null || username.trim().isEmpty()) {
            errors.put("username", "");
        } else {
            username = username.trim().toLowerCase();
        }

        if (password == null || password.trim().isEmpty()) {
            errors.put("password", "");
        }
        if (passwordConfirm == null) {
            errors.put("passwordConfirm", "");
        }
        if (password != null && passwordConfirm != null && !password.equals(passwordConfirm)) {
            errors.put("passwordMatch", "");
        }

        if (errors.isEmpty()) {
            final char[] passwordChars = password.toCharArray();
            try {
                MobileAccessPlugin.administrationService().setPassword(
                    webManager.getUser().getUsername(),
                    username,
                    passwordChars
                );
                webManager.logEvent("created mobile access credential for " + username, null);
                response.sendRedirect("mobileaccess-admin.jsp?success=true");
                return;
            } catch (final Exception e) {
                errors.put("general", e.getMessage() == null ? "Unable to create mobile credential" : e.getMessage());
                LoggerFactory.getLogger("mobileaccess-admin.jsp")
                    .error("Unexpected error while creating mobile credential for '{}'", username, e);
            } finally {
                Arrays.fill(passwordChars, '\0');
            }
        }
    } else if (actionRequested && errors.isEmpty()) {
        try {
            final String actor = webManager.getUser().getUsername();
            if (block) {
                MobileAccessPlugin.administrationService().block(actor, username);
            } else if (enable) {
                MobileAccessPlugin.administrationService().enable(actor, username);
            } else if (delete) {
                MobileAccessPlugin.administrationService().delete(actor, username);
            } else if (grantAdmin) {
                MobileAccessPlugin.administrationService().setAdministrator(actor, username, true);
            } else if (revokeAdmin) {
                MobileAccessPlugin.administrationService().setAdministrator(actor, username, false);
            }
            response.sendRedirect("mobileaccess-admin.jsp?success=true");
            return;
        } catch (final Exception e) {
            errors.put("general", e.getMessage() == null ? "Operation failed" : e.getMessage());
            LoggerFactory.getLogger("mobileaccess-admin.jsp")
                .error("Unexpected error while managing mobile credential for '{}'", username, e);
        }
    }

    List<ManagedMobileUser> users = Collections.emptyList();
    try {
        users = MobileAccessPlugin.administrationService().listUsers();
    } catch (final Exception e) {
        errors.put("list", e.getMessage() == null ? "Unable to load mobile users" : e.getMessage());
        LoggerFactory.getLogger("mobileaccess-admin.jsp").error("Unable to load managed mobile users", e);
    }

    pageContext.setAttribute("errors", errors);
    pageContext.setAttribute("success", request.getParameter("success") != null);

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

<c:choose>
    <c:when test="${not empty errors}">
        <c:forEach var="err" items="${errors}">
            <admin:infobox type="error">
                <c:choose>
                    <c:when test="${err.key eq 'csrf'}">CSRF validation failed.</c:when>
                    <c:when test="${err.key eq 'username'}">Enter a username.</c:when>
                    <c:when test="${err.key eq 'password'}">Enter a password.</c:when>
                    <c:when test="${err.key eq 'passwordConfirm'}">Confirm the password.</c:when>
                    <c:when test="${err.key eq 'passwordMatch'}">The passwords do not match.</c:when>
                    <c:otherwise><c:out value="${err.value}"/></c:otherwise>
                </c:choose>
            </admin:infobox>
        </c:forEach>
    </c:when>
    <c:when test="${success}">
        <admin:infobox type="success">The operation completed successfully.</admin:infobox>
    </c:when>
</c:choose>

<p>Manage separate mobile credentials for LDAP-backed Openfire users.</p>

<form name="f" action="mobileaccess-admin.jsp" method="get" autocomplete="off">
    <input type="hidden" name="csrf" value="${csrf}">

    <div class="jive-contentBoxHeader">Create or replace mobile password</div>
    <div class="jive-contentBox">
        <table>
            <tbody>
            <tr>
                <td style="width: 1%; white-space: nowrap"><label for="usernametf">Username:</label> *</td>
                <td><input type="text" name="username" size="30" maxlength="75"
                           value="<%= StringUtils.escapeForXML(username) %>" id="usernametf" autocomplete="off"></td>
            </tr>
            <tr>
                <td style="width: 1%; white-space: nowrap"><label for="passtf">Password:</label> *</td>
                <td><input type="password" name="password" value="" size="20" maxlength="256"
                           id="passtf" autocomplete="off"></td>
            </tr>
            <tr>
                <td style="width: 1%; white-space: nowrap"><label for="confpasstf">Confirm password:</label> *</td>
                <td><input type="password" name="passwordConfirm" value="" size="20" maxlength="256"
                           id="confpasstf" autocomplete="off"></td>
            </tr>
            </tbody>
        </table>
    </div>

    <input type="submit" name="create" value="Create or replace password">
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
            <td><strong><%= StringUtils.escapeHTMLTags(user.username()) %></strong></td>
            <td><%= user.enabled() ? "Enabled" : "Blocked" %></td>
            <td><%= user.administrator() ? "Yes" : "No" %></td>
            <td><%= user.updatedAt() == null ? "—" : dateFormatter.format(user.updatedAt()) %></td>
            <td>
                <form action="mobileaccess-admin.jsp" method="get" style="display:inline">
                    <input type="hidden" name="csrf" value="${csrf}">
                    <input type="hidden" name="username" value="<%= StringUtils.escapeForXML(user.username()) %>">
                    <% if (user.enabled()) { %>
                        <input type="submit" name="block" value="Block">
                    <% } else { %>
                        <input type="submit" name="enable" value="Enable">
                    <% } %>
                    <% if (user.administrator()) { %>
                        <input type="submit" name="revokeAdmin" value="Remove administrator">
                    <% } else { %>
                        <input type="submit" name="grantAdmin" value="Make administrator">
                    <% } %>
                    <input type="submit" name="delete" value="Delete">
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
