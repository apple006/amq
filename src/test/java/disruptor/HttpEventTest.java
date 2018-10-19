package disruptor;

import com.artlongs.amq.disruptor.RingBuffer;
import com.artlongs.amq.disruptor.dsl.Disruptor;
import com.artlongs.amq.net.http.HttpRequest;
import com.artlongs.amq.net.http.HttpResponse;
import com.artlongs.amq.net.http.HttpServerConfig;
import com.artlongs.amq.net.http.aio.AioHttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 *
 * Created by ${leeton} on 2018/9/27.
 */
public class HttpEventTest {
    private static Logger logger = LoggerFactory.getLogger(HttpEventTest.class);

    public static void handleEvent(HttpEvent httpEvent, long sequence, boolean endOfBatch) {
        HttpRequest req = httpEvent.req;
        HttpResponse resp = httpEvent.resp;
        resp.write(req.query().getBytes());
        System.out.println("收到 URL = " + req.query());
    }

    public static void translate(HttpEvent httpEvent, long sequence, HttpRequest req, HttpResponse resp) {
        httpEvent.setReq(req);
        httpEvent.setResp(resp);
    }

    public static class HttpEvent {
        private HttpRequest req;
        private HttpResponse resp;

        public void setReq(HttpRequest req) {
            this.req = req;
        }

        public void setResp(HttpResponse resp) {
            this.resp = resp;
        }
    }

    public static void main(String[] args) throws Exception {
        AioHttpServer httpServer = new AioHttpServer(new HttpServerConfig());

        // Executor that will be used to construct nvueew threads for consumers
        Executor executor = Executors.newCachedThreadPool();

        // Specify the size of the ring buffer, must be power of 2.
        int bufferSize = 1024;

        // Construct the Disruptor
        Disruptor<HttpEvent> disruptor = new Disruptor<>(HttpEvent::new, bufferSize, executor);

        // Connect the handler
        disruptor.handleEventsWith(HttpEventTest::handleEvent);

        // Start the Disruptor, starts all threads running
        disruptor.start();

        // Get the ring buffer from the Disruptor to be used for publishing.
        RingBuffer<HttpEvent> ringBuffer = disruptor.getRingBuffer();
        // Get the request then publish event and translate data
        httpServer.handler((HttpRequest req, HttpResponse resp) -> {
            ringBuffer.publishEvent(HttpEventTest::translate, req, resp);
        });
        //
        httpServer.start();

        while (true) {
            logger.debug(httpServer.getState().getInfo());
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
