package com.github.swim_developer.dnotam.provider.infrastructure.out.xml;

import com.github.swim_developer.dnotam.provider.application.port.out.AixmMessageAssemblerPort;
import com.github.swim_developer.dnotam.provider.domain.model.DnotamStoredEvent;
import com.github.swim_developer.framework.infrastructure.out.xml.SafeXmlFactory;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.Instant;
import java.util.List;

@ApplicationScoped
@Slf4j
public class DnotamAixmMessageAssembler implements AixmMessageAssemblerPort {

    private static final String AIXM_MESSAGE_NS = "http://www.aixm.aero/schema/5.1.1/message";
    private static final String AIXM_MESSAGE_PREFIX = "message";
    private static final String EMPTY_AIXM_MESSAGE = """
            <?xml version="1.0" encoding="UTF-8"?>
            <message:AIXMBasicMessage xmlns:message="http://www.aixm.aero/schema/5.1.1/message"
                                      xmlns:gml="http://www.opengis.net/gml/3.2"
                                      xmlns:aixm="http://www.aixm.aero/schema/5.1.1"
                                      xmlns:event="http://www.aixm.aero/schema/5.1.1/event"
                                      xmlns:xlink="http://www.w3.org/1999/xlink"
                                      gml:id="DNOTAM-QUERY-RESULT-EMPTY">
            </message:AIXMBasicMessage>
            """;

    @Override
    public String assemble(List<DnotamStoredEvent> events) {
        if (events.isEmpty()) {
            return EMPTY_AIXM_MESSAGE;
        }

        try {
            DocumentBuilder builder = SafeXmlFactory.documentBuilder();
            Document resultDoc = builder.newDocument();

            Element root = resultDoc.createElementNS(AIXM_MESSAGE_NS, AIXM_MESSAGE_PREFIX + ":AIXMBasicMessage");
            root.setAttribute("xmlns:message", AIXM_MESSAGE_NS);
            root.setAttribute("xmlns:gml", "http://www.opengis.net/gml/3.2");
            root.setAttribute("xmlns:aixm", "http://www.aixm.aero/schema/5.1.1");
            root.setAttribute("xmlns:event", "http://www.aixm.aero/schema/5.1.1/event");
            root.setAttribute("xmlns:xlink", "http://www.w3.org/1999/xlink");
            root.setAttribute("gml:id", "DNOTAM-QUERY-RESULT-" + Instant.now().toEpochMilli());
            resultDoc.appendChild(root);

            for (DnotamStoredEvent event : events) {
                appendHasMembersFromEvent(resultDoc, root, event.getAixmMessage(), builder);
            }

            return documentToString(resultDoc);

        } catch (Exception e) {
            log.error("Failed to build aggregated AIXM message", e);
            return buildErrorMessage("Failed to aggregate AIXM messages: " + e.getMessage());
        }
    }

    private void appendHasMembersFromEvent(Document resultDoc, Element root, String aixmMessage, DocumentBuilder builder) {
        try {
            Document eventDoc = builder.parse(new InputSource(new StringReader(aixmMessage)));
            NodeList hasMemberNodes = eventDoc.getElementsByTagNameNS(AIXM_MESSAGE_NS, "hasMember");

            for (int i = 0; i < hasMemberNodes.getLength(); i++) {
                Node hasMember = hasMemberNodes.item(i);
                Node importedNode = resultDoc.importNode(hasMember, true);
                root.appendChild(importedNode);
            }
        } catch (Exception e) {
            log.warn("Failed to parse AIXM message for aggregation, skipping: {}", e.getMessage());
        }
    }

    private String documentToString(Document doc) {
        try {
            Transformer transformer = SafeXmlFactory.transformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            return writer.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize XML document", e);
        }
    }

    private String buildErrorMessage(String message) {
        return String.format("""
                <?xml version="1.0" encoding="UTF-8"?>
                <error xmlns="http://www.opengis.net/ows/2.0">
                    <code>NoApplicableCode</code>
                    <message>%s</message>
                </error>
                """, message);
    }
}
