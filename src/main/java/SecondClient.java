import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
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

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * @author Suren Kalaychyan
 */
public class SecondClient {

    private static ObjectMapper objectMapper = new ObjectMapper();
    private static BlockingQueue<List<LinkedHashMap>> blockingQueue = blockingQueue = new ArrayBlockingQueue<>(3);

    public static void main(String[] args) throws Exception {

        final WebSocketStompClient stompClient = new WebSocketStompClient(new SockJsClient(List.of(new WebSocketTransport(new StandardWebSocketClient()))));
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        final StompSession stompSession = stompClient
            .connect(String.format("ws://localhost:%d/ws", 8080), new StompSessionHandlerAdapter() {
            })
            .get(10, TimeUnit.SECONDS);

        StompHeaders stompHeaders = new StompHeaders();
        stompHeaders.setDestination(String.format("/app/id/%d", 12));
        stompHeaders.setId(String.valueOf(1));

        stompSession.subscribe(String.format("/topic/id/%d", 12), prepareTestStompFrameHandler());
        stompSession.send(String.format("/app/id/%d", 12), 12);

        stompHeaders.setDestination(String.format("/app/id/%d", 12));
        stompSession.send(stompHeaders, 12);

        final List<LinkedHashMap> trackingObjectState = blockingQueue.poll(10, SECONDS);
        System.out.println(trackingObjectState);

        while (true) {
            final int trackingObjectId = new Scanner(System.in).nextInt();
            stompHeaders.setDestination(String.format("/app/id/%d", trackingObjectId));
            stompHeaders.setId(String.valueOf(trackingObjectId));
            stompSession.subscribe(stompHeaders, prepareTestStompFrameHandler());
        }
    }

    private static StompFrameHandler prepareTestStompFrameHandler() {
        return new StompFrameHandler() {

            @Override
            public Type getPayloadType(final StompHeaders headers) {
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
    }
}
