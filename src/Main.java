import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;

public class Main {
    private static final ReentrantLock lock = new ReentrantLock();
    private static final ThreadMXBean MX_BEAN = ManagementFactory.getThreadMXBean();

    public static void main(String[] args) throws Exception {
        CountDownLatch serverReady = new CountDownLatch(1);

        Thread server = new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(5001)) {
                log("server listening on port 5001");
                serverReady.countDown(); // tell main the server is ready

                try (Socket client = serverSocket.accept()) {
                    log("server accepted connection");

                    OutputStream out = client.getOutputStream();
                    int counter=0;
                    for (int i = 1; i <= 50000000; i++) {
//                        Thread.sleep(3000); // simulate slow production of data
//                        out.write(("chunk-" + i + "\n").getBytes());
//                        out.flush();
                        counter++;
//                        log("server sent chunk " + i);
                    }
                    out.write(("chunk-" + counter + "\n").getBytes());
                    out.flush();
                }
            } catch (Exception e) {
                log("server error: " + e.getMessage());
            }
        }, "server-thread");

        server.start();
        serverReady.await(); // wait until server is actually listening

        try (Socket socket = new Socket("localhost", 5001)) {
            log("client connected");

            InputStream in = socket.getInputStream();
            byte[] buffer = new byte[32];

            for (int i = 1; i <= 5; i++) {
                long wallBefore = System.nanoTime();
                long cpuBefore = currentThreadCpuTime();

                log("client calling read()");
//                int bytesRead = in.read(buffer); // blocks until server sends data

                int counter=0;
                for (long j = 1; j <= 50000000000L; j++) {
//                        Thread.sleep(3000); // simulate slow production of data
//                        out.write(("chunk-" + i + "\n").getBytes());
//                        out.flush();
                    counter++;
//                        log("server sent chunk " + i);
                }
                long wallAfter = System.nanoTime();
                long cpuAfter = currentThreadCpuTime();

                log("read returned bytesRead=" + counter
                        + ", wall ns=" + (wallAfter - wallBefore)
                        + ", cpu ns=" + (cpuAfter - cpuBefore)
                        );
            }
        }

        server.join();
    }

    private static void log(String message){
        System.out.printf("[%s] [%-15s] %s%n",
                LocalTime.now(),
                Thread.currentThread().getName(),
                message);
    }
    private static void await(CountDownLatch latch){
        try{
            latch.await();
        }catch (InterruptedException e){
            Thread.currentThread().interrupt();
        }
    }

    private static long currentThreadCpuTime(){
        return MX_BEAN.isCurrentThreadCpuTimeSupported()?MX_BEAN.getCurrentThreadCpuTime():-1L;
    }
}