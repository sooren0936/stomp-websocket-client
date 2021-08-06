import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author Suren Kalaychyan
 */
public class FirstClient {

    final static ObjectMapper objectMapper = new ObjectMapper();

    private static BlockingQueue<List<LinkedHashMap>> blockingQueue = blockingQueue = new ArrayBlockingQueue<>(20);

    public static void main(String[] args) throws Exception {

        final WebSocketStompClient stompClient = new WebSocketStompClient(new SockJsClient(List.of(new WebSocketTransport(new StandardWebSocketClient()))));
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        StompSessionHandler stompSessionHandler = new StompSessionHandler() {
            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                session.subscribe(String.format("/topic/id/%d", 10), this);
                session.send(String.format("/app/id/%d", 10), 10);
            }

            @Override
            public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
                exception.printStackTrace();
            }

            @Override
            public void handleTransportError(StompSession session, Throwable exception) {
                exception.printStackTrace();
            }

            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Object.class;
            }

            @Override
            public void handleFrame(final StompHeaders headers, final Object payload) {
                try {
                    final List<LinkedHashMap> trackingObjectStates = objectMapper.readValue((byte[]) payload, new TypeReference<>() {
                    });
                    System.out.println(trackingObjectStates);
                    blockingQueue.add(trackingObjectStates);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        final StompSession stompSession = stompClient.connect(String.format("ws://localhost:%d/ws", 8080), stompSessionHandler).get(10, TimeUnit.SECONDS);

        while (true) {
            final int trackingObjectId = new Scanner(System.in).nextInt();
            stompSession.subscribe(String.format("/topic/id/%d", trackingObjectId), stompSessionHandler);
        }
    }
}

