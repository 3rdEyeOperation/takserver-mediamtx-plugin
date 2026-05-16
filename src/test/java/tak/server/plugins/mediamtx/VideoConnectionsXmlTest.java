/*
 * Copyright 2025 the takserver-mediamtx-plugin contributors.
 * Licensed under the MIT License.
 */
package tak.server.plugins.mediamtx;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;

import tak.server.plugins.mediamtx.model.Feed;
import tak.server.plugins.mediamtx.model.VideoConnections;

class VideoConnectionsXmlTest {

    @Test
    void marshalsExpectedSchema() throws Exception {
        Feed f = new Feed();
        f.setUuid("uid-1");
        f.setAlias("camera1");
        f.setProtocol("rtsp");
        f.setAddress("video.example.com");
        f.setPort("8554");
        f.setPath("/camera1");
        f.setActive(true);

        VideoConnections vc = new VideoConnections(Arrays.asList(f));

        JAXBContext ctx = JAXBContext.newInstance(VideoConnections.class);
        Marshaller m = ctx.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        m.marshal(vc, out);
        String xml = out.toString("UTF-8");

        // Element names must match what TAK Server's com.bbn.marti.video.Feed
        // / VideoConnections produce, otherwise ATAK clients will reject them.
        assertTrue(xml.contains("<videoConnections>"), xml);
        assertTrue(xml.contains("<feed>"), xml);
        assertTrue(xml.contains("<uid>uid-1</uid>"), xml);
        assertTrue(xml.contains("<alias>camera1</alias>"), xml);
        assertTrue(xml.contains("<protocol>rtsp</protocol>"), xml);
        assertTrue(xml.contains("<address>video.example.com</address>"), xml);
        assertTrue(xml.contains("<port>8554</port>"), xml);
        assertTrue(xml.contains("<path>/camera1</path>"), xml);
        assertTrue(xml.contains("<active>true</active>"), xml);
    }
}
