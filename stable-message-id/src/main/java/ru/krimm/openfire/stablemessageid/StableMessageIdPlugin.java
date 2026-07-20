package ru.krimm.openfire.stablemessageid;

import java.io.File;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.openfire.interceptor.InterceptorManager;
import org.jivesoftware.openfire.interceptor.PacketInterceptor;
import org.jivesoftware.openfire.interceptor.PacketRejectedException;
import org.jivesoftware.openfire.session.Session;
import org.jivesoftware.openfire.stanzaid.StanzaIDUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;

/** Adds a server-generated XEP-0359 stanza-id before direct messages are routed. */
public final class StableMessageIdPlugin implements Plugin, PacketInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(StableMessageIdPlugin.class);

    private InterceptorManager interceptorManager;
    private JID serverDomain;

    @Override
    public void initializePlugin(final PluginManager pluginManager, final File pluginDirectory) {
        serverDomain = new JID(XMPPServer.getInstance().getServerInfo().getXMPPDomain());
        interceptorManager = InterceptorManager.getInstance();
        interceptorManager.addInterceptor(0, this);
        LOGGER.info("Stable Message ID plugin initialized for domain {}", serverDomain);
    }

    @Override
    public void destroyPlugin() {
        if (interceptorManager != null) {
            interceptorManager.removeInterceptor(this);
        }
        LOGGER.info("Stable Message ID plugin destroyed");
        interceptorManager = null;
        serverDomain = null;
    }

    @Override
    public void interceptPacket(
        final Packet packet,
        final Session session,
        final boolean incoming,
        final boolean processed
    ) throws PacketRejectedException {
        if (!incoming || processed || !(packet instanceof Message message)) {
            return;
        }
        if (!isDirectUserMessage(message)) {
            return;
        }
        if (StanzaIDUtil.findFirstUniqueAndStableStanzaID(message, serverDomain.toString()) == null) {
            StanzaIDUtil.ensureUniqueAndStableStanzaID(message, serverDomain);
            LOGGER.debug("Added stable stanza-id to direct message {} from {} to {}",
                message.getID(), message.getFrom(), message.getTo());
        }
    }

    static boolean isDirectUserMessage(final Message message) {
        if (message.getBody() == null || message.getTo() == null) {
            return false;
        }
        final Message.Type type = message.getType();
        if (type != Message.Type.chat && type != Message.Type.normal) {
            return false;
        }
        return message.getTo().getNode() != null;
    }
}
