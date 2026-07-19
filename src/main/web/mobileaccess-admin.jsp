<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="java.io.PrintWriter" %>
<%@ page import="java.io.StringWriter" %>
<%@ page import="java.time.ZoneId" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%@ page import="java.util.Arrays" %>
<%@ page import="java.util.Collections" %>
<%@ page import="java.util.Enumeration" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.UUID" %>
<%@ page import="javax.servlet.http.Cookie" %>
<%@ page import="org.apache.commons.text.StringEscapeUtils" %>
<%@ page import="org.jivesoftware.util.WebManager" %>
<%@ page import="org.jivesoftware.util.CookieUtils" %>
<%@ page import="org.jivesoftware.util.ParamUtils" %>
<%@ page import="org.jivesoftware.util.StringUtils" %>
<%@ page import="org.slf4j.Logger" %>
<%@ page import="org.slf4j.LoggerFactory" %>
<%@ page import="org.slf4j.MDC" %>
<%@ page import="ru.krimm.openfire.mobileaccess.MobileAccessPlugin" %>
<%@ page import="ru.krimm.openfire.mobileaccess.admin.MobileAccessAdministrationService.ManagedMobileUser" %>

<%
    final Logger logger = LoggerFactory.getLogger("ru.krimm.openfire.mobileaccess.admin.jsp");
    final String requestId = UUID.randomUUID().toString().substring(0, 8);
    MDC.put("mobileAccessRequestId", requestId);

    final WebManager webManager = new WebManager();
    webManager.init(request, response, session, application, out);

    String message = null;
    String messageType = "success";
    String debugStage = "Page initialized";
    String debugStackTrace = null;
    final String requestMethod = request.getMethod();
    final boolean postRequest = "POST".equalsIgnoreCase(requestMethod);
    String operation = ParamUtils.getStringParameter(request, "operation", "");

    final StringBuilder parameterNames = new StringBuilder();
    final Enumeration<String> names = request.getParameterNames();
    while (names.hasMoreElements()) {
        if (parameterNames.length() > 0) parameterNames.append(", ");
        parameterNames.append(names.nextElement());
    }

    logger.info("[RID:{}] Admin request: method={}, operation={}, parameters=[{}]",
        requestId, requestMethod, operation, parameterNames);

    if (postRequest) {
        debugStage = "POST received";
        if (operation.isBlank()) {
            message = "POST request received, but operation is missing.";
            messageType = "error";
            debugStage = "POST received without operation";
        } else {
            final Cookie csrfCookie = CookieUtils.getCookie(request, "csrf");
            final String submittedCsrf = ParamUtils.getParameter(request, "csrf");
            if (csrfCookie == null || submittedCsrf == null || !csrfCookie.getValue().equals(submittedCsrf)) {
                message = "The request was rejected because CSRF validation failed.";
                messageType = "error";
                debugStage = "CSRF validation failed";
                logger.warn("[RID:{}] CSRF failed: cookiePresent={}, submittedPresent={}",
                    requestId, csrfCookie != null, submittedCsrf != null);
            } else {
                debugStage = "CSRF validation passed";
                final String actor = webManager.getUser().getUsername();
                final String username = ParamUtils.getStringParameter(request, "username", "");
                try {
                    switch (operation) {
                        case "setPassword":
                            debugStage = "Reading password fields";
                            final String passwordValue = ParamUtils.getStringParameter(request, "password", "");
                            final String confirmationValue = ParamUtils.getStringParameter(request, "passwordConfirmation", "");
                            final char[] password = passwordValue.toCharArray();
                            try {
                                if (!passwordValue.equals(confirmationValue)) {
                                    throw new IllegalArgumentException("Password confirmation does not match");
                                }
                                debugStage = "Calling administrationService.setPassword";
                                logger.info("[RID:{}] setPassword: actor={}, username={}, passwordLength={}",
                                    requestId, actor, username, password.length);
                                MobileAccessPlugin.administrationService().setPassword(actor, username, password);
                                debugStage = "Password saved successfully";
                                message = "The mobile password was created or replaced successfully.";
                            } finally {
                                Arrays.fill(password, '\0');
                            }
                            break;
                        case "block":
                            debugStage = "Calling administrationService.block";
                            MobileAccessPlugin.administrationService().block(actor, username);
                            debugStage = "Mobile access blocked";
                            message = "Mobile access was blocked.";
                            break;
                        case "enable":
                            debugStage = "Calling administrationService.enable";
                            MobileAccessPlugin.administrationService().enable(actor, username);
                            debugStage = "Mobile access enabled";
                            message = "Mobile access was enabled.";
                            break;
                        case "delete":
                            debugStage = "Calling administrationService.delete";
                            MobileAccessPlugin.administrationService().delete(actor, username);
                            debugStage = "Credential deleted";
                            message = "The mobile credential was deleted.";
                            break;
                        case "grantAdmin":
                            debugStage = "Calling administrationService.setAdministrator(true)";
                            MobileAccessPlugin.administrationService().setAdministrator(actor, username, true);
                            debugStage = "Administrator granted";
                            message = "Administrator access was granted.";
                            break;
                        case "revokeAdmin":
                            debugStage = "Calling administrationService.setAdministrator(false)";
                            MobileAccessPlugin.administrationService().setAdministrator(actor, username, false);
                            debugStage = "Administrator revoked";
                            message = "Administrator access was revoked.";
                            break;
                        default:
                            throw new IllegalArgumentException("Unknown administration operation: " + operation);
                    }
                } catch (final RuntimeException e) {
                    message = e.getClass().getSimpleName() + ": " + (e.getMessage() == null ? "Operation failed" : e.getMessage());
                    messageType = "error";
                    final StringWriter writer = new StringWriter();
                    e.printStackTrace(new PrintWriter(writer));
                    debugStackTrace = writer.toString();
                    logger.error("[RID:{}] Operation failed at stage '{}': operation={}, username={}",
                        requestId, debugStage, operation, username, e);
                }
            }
        }
    }

    final String csrf = StringUtils.randomString(15);
    CookieUtils.setCookie(request, response, "csrf", csrf, -1);

    List<ManagedMobileUser> users = Collections.emptyList();
    try {
        users = MobileAccessPlugin.administrationService().listUsers();
    } catch (final RuntimeException e) {
        message = e.getClass().getSimpleName() + ": " + (e.getMessage() == null ? "Unable to load users" : e.getMessage());
        messageType = "error";
        debugStage = "Loading managed users";
        final StringWriter writer = new StringWriter();
        e.printStackTrace(new PrintWriter(writer));
        debugStackTrace = writer.toString();
        logger.error("[RID:{}] Unable to load managed users", requestId, e);
    } finally {
        MDC.remove("mobileAccessRequestId");
    }

    final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault());
%>
<html>
<head>
    <title>Mobile Access <%= MobileAccessPlugin.VERSION %></title>
    <meta name="pageID" content="mobileaccess-admin"/>
    <script>
        function mobileAccessPost(values) {
            const body = new URLSearchParams();
            Object.keys(values).forEach(function (key) { body.append(key, values[key]); });
            document.getElementById('mobileaccess-submit-state').textContent = 'Sending POST request...';
            fetch(window.location.pathname, {
                method: 'POST',
                credentials: 'same-origin',
                redirect: 'follow',
                headers: {'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'},
                body: body.toString()
            }).then(function (response) {
                return response.text();
            }).then(function (html) {
                document.open();
                document.write(html);
                document.close();
            }).catch(function (error) {
                document.getElementById('mobileaccess-submit-state').textContent = 'POST failed in browser: ' + error;
            });
        }

        function submitPassword() {
            mobileAccessPost({
                csrf: document.getElementById('mobileaccess-csrf').value,
                operation: 'setPassword',
                username: document.getElementById('username').value,
                password: document.getElementById('password').value,
                passwordConfirmation: document.getElementById('passwordConfirmation').value
            });
            return false;
        }

        function submitUserOperation(operation, username) {
            if (operation === 'delete' && !confirm('Delete the mobile credential for this user?')) return false;
            mobileAccessPost({csrf: document.getElementById('mobileaccess-csrf').value, operation: operation, username: username});
            return false;
        }
    </script>
</head>
<body>
<input id="mobileaccess-csrf" type="hidden" value="<%= csrf %>"/>
<p><strong>Plugin version:</strong> <%= MobileAccessPlugin.VERSION %></p>
<% if (message != null) { %>
<div class="jive-contentBox"><strong><%= "error".equals(messageType) ? "Error" : "Success" %>:</strong> <%= StringEscapeUtils.escapeHtml4(message) %></div>
<% } %>
<div class="jive-contentBoxHeader">Debug diagnostics</div>
<div class="jive-contentBox">
    <p><strong>Request ID:</strong> <%= StringEscapeUtils.escapeHtml4(requestId) %></p>
    <p><strong>Request method:</strong> <%= StringEscapeUtils.escapeHtml4(requestMethod) %></p>
    <p><strong>Operation:</strong> <%= StringEscapeUtils.escapeHtml4(operation.isBlank() ? "(empty)" : operation) %></p>
    <p><strong>Parameters:</strong> <%= StringEscapeUtils.escapeHtml4(parameterNames.toString()) %></p>
    <p><strong>Last stage:</strong> <%= StringEscapeUtils.escapeHtml4(debugStage) %></p>
    <p><strong>Browser submit state:</strong> <span id="mobileaccess-submit-state">Idle</span></p>
    <% if (debugStackTrace != null) { %><pre style="white-space:pre-wrap;max-height:420px;overflow:auto"><%= StringEscapeUtils.escapeHtml4(debugStackTrace) %></pre><% } %>
</div>

<p>Manage separate mobile credentials for LDAP-backed Openfire users.</p>
<div class="jive-contentBoxHeader">Create or replace mobile password</div>
<div class="jive-contentBox">
    <table cellspacing="0" border="0">
        <tr><td><label for="username">Username</label></td><td><input id="username" type="text" maxlength="64" required/></td></tr>
        <tr><td><label for="password">New password</label></td><td><input id="password" type="password" minlength="12" maxlength="256" required autocomplete="new-password"/></td></tr>
        <tr><td><label for="passwordConfirmation">Confirm password</label></td><td><input id="passwordConfirmation" type="password" minlength="12" maxlength="256" required autocomplete="new-password"/></td></tr>
    </table>
    <button type="button" onclick="return submitPassword();">Create or replace password</button>
</div>

<br/>
<div class="jive-contentBoxHeader">Managed mobile users (<%= users.size() %>)</div>
<div class="jive-contentBox">
<% if (users.isEmpty()) { %>
    <p>No mobile credentials have been created.</p>
<% } else { %>
    <table class="jive-table" cellspacing="0" cellpadding="3" width="100%">
        <thead><tr><th>Username</th><th>Status</th><th>Administrator</th><th>Updated</th><th>Actions</th></tr></thead>
        <tbody>
        <% for (final ManagedMobileUser user : users) { final String escapedUsername = StringEscapeUtils.escapeEcmaScript(user.username()); %>
        <tr>
            <td><strong><%= StringEscapeUtils.escapeHtml4(user.username()) %></strong></td>
            <td><%= user.enabled() ? "Enabled" : "Blocked" %></td>
            <td><%= user.administrator() ? "Yes" : "No" %></td>
            <td><%= user.updatedAt() == null ? "—" : dateFormatter.format(user.updatedAt()) %></td>
            <td>
                <% if (user.enabled()) { %><button type="button" onclick="return submitUserOperation('block','<%= escapedUsername %>');">Block</button><% } else { %><button type="button" onclick="return submitUserOperation('enable','<%= escapedUsername %>');">Enable</button><% } %>
                <% if (user.administrator()) { %><button type="button" onclick="return submitUserOperation('revokeAdmin','<%= escapedUsername %>');">Remove administrator</button><% } else { %><button type="button" onclick="return submitUserOperation('grantAdmin','<%= escapedUsername %>');">Make administrator</button><% } %>
                <button type="button" onclick="return submitUserOperation('delete','<%= escapedUsername %>');">Delete</button>
            </td>
        </tr>
        <% } %>
        </tbody>
    </table>
<% } %>
</div>
</body>
</html>
