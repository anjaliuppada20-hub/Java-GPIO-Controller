import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.io.gpio.digital.DigitalState;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class GpioController {

    private static final int HTTP_PORT = 8080;

    private static Context pi4j;
    private static DigitalOutput led;

    public static void main(String[] args) throws Exception {

        // Initialize Pi4J
        pi4j = Pi4J.newAutoContext();

        // Configure GPIO18 as digital output
        led = pi4j.dout()
                .create(18);

        led.off();

        // Create HTTP server
        HttpServer server = HttpServer.create(
                new InetSocketAddress(HTTP_PORT),
                0
        );

        server.createContext("/", GpioController::handleHome);
        server.createContext("/on", GpioController::handleOn);
        server.createContext("/off", GpioController::handleOff);
        server.createContext("/status", GpioController::handleStatus);

        server.setExecutor(null);
        server.start();

        System.out.println(
                "Java GPIO Controller running on port "
                        + HTTP_PORT
        );

        System.out.println(
                "Open: http://<RASPBERRY_PI_IP>:"
                        + HTTP_PORT
        );

        Runtime.getRuntime().addShutdownHook(
                new Thread(() -> {

                    if (led != null) {
                        led.off();
                    }

                    if (pi4j != null) {
                        pi4j.shutdown();
                    }

                    server.stop(0);

                    System.out.println(
                            "GPIO controller stopped."
                    );
                })
        );
    }

    private static void handleHome(
            HttpExchange exchange
    ) throws IOException {

        String html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport"
                          content="width=device-width,
                                   initial-scale=1.0">

                    <title>Java GPIO Controller</title>

                    <style>
                        body {
                            font-family: Arial;
                            text-align: center;
                            background: #111827;
                            color: white;
                            padding: 40px;
                        }

                        .box {
                            max-width: 500px;
                            margin: auto;
                            background: #1f2937;
                            padding: 35px;
                            border-radius: 15px;
                        }

                        button {
                            border: none;
                            padding: 15px 35px;
                            margin: 10px;
                            font-size: 20px;
                            border-radius: 8px;
                            cursor: pointer;
                        }

                        .on {
                            background: #16a34a;
                            color: white;
                        }

                        .off {
                            background: #dc2626;
                            color: white;
                        }

                        #status {
                            font-size: 28px;
                            margin: 25px;
                        }
                    </style>
                </head>

                <body>

                    <div class="box">

                        <h1>Java GPIO Controller</h1>

                        <p>Raspberry Pi GPIO18</p>

                        <div id="status">
                            Loading...
                        </div>

                        <button class="on"
                                onclick="setGPIO('/on')">
                            Turn ON
                        </button>

                        <button class="off"
                                onclick="setGPIO('/off')">
                            Turn OFF
                        </button>

                    </div>

                    <script>

                        async function setGPIO(url) {

                            await fetch(url);

                            updateStatus();
                        }

                        async function updateStatus() {

                            const response =
                                await fetch('/status');

                            const data =
                                await response.text();

                            document.getElementById(
                                'status'
                            ).innerText =
                                'LED: ' + data;
                        }

                        updateStatus();

                    </script>

                </body>
                </html>
                """;

        sendResponse(
                exchange,
                200,
                "text/html",
                html
        );
    }

    private static void handleOn(
            HttpExchange exchange
    ) throws IOException {

        led.high();

        sendResponse(
                exchange,
                200,
                "text/plain",
                "ON"
        );
    }

    private static void handleOff(
            HttpExchange exchange
    ) throws IOException {

        led.low();

        sendResponse(
                exchange,
                200,
                "text/plain",
                "OFF"
        );
    }

    private static void handleStatus(
            HttpExchange exchange
    ) throws IOException {

        String status =
                led.state() == DigitalState.HIGH
                        ? "ON"
                        : "OFF";

        sendResponse(
                exchange,
                200,
                "text/plain",
                status
        );
    }

    private static void sendResponse(
            HttpExchange exchange,
            int statusCode,
            String contentType,
            String response
    ) throws IOException {

        byte[] bytes =
                response.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders()
                .set(
                        "Content-Type",
                        contentType + "; charset=UTF-8"
                );

        exchange.sendResponseHeaders(
                statusCode,
                bytes.length
        );

        try (OutputStream output =
                     exchange.getResponseBody()) {

            output.write(bytes);
        }
    }
}
