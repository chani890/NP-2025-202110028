package ch02_pit_01;

import javax.swing.SwingWorker;
import java.time.Duration;
import java.util.function.Consumer;

final class PlannerNetworkController {
    private final TcpPlanClient tcpClient;
    private final String serverHost;
    private final int serverPort;

    PlannerNetworkController(Duration timeout, String host, int port) {
        this.tcpClient = new TcpPlanClient(timeout);
        this.serverHost = host;
        this.serverPort = port;
    }

    void connectAsync(Runnable onSuccess, Consumer<Exception> onError, Runnable onFinally) {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                tcpClient.connect(serverHost, serverPort);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    if (onSuccess != null) {
                        onSuccess.run();
                    }
                } catch (Exception ex) {
                    if (onError != null) {
                        onError.accept(ex);
                    }
                } finally {
                    if (onFinally != null) {
                        onFinally.run();
                    }
                }
            }
        }.execute();
    }

    void requestPlan(
        PlanRequestPayload payload,
        Consumer<String> onSuccess,
        Consumer<Exception> onError,
        Runnable onFinally
    ) {
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return tcpClient.requestPlan(serverHost, serverPort, payload);
            }

            @Override
            protected void done() {
                try {
                    String response = get();
                    if (onSuccess != null) {
                        onSuccess.accept(response);
                    }
                } catch (Exception ex) {
                    if (onError != null) {
                        onError.accept(ex);
                    }
                } finally {
                    if (onFinally != null) {
                        onFinally.run();
                    }
                }
            }
        }.execute();
    }

    void requestPlanSelection(
        PlanSelectionPayload payload,
        Consumer<String> onSuccess,
        Consumer<Exception> onError,
        Runnable onFinally
    ) {
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return tcpClient.selectPlan(serverHost, serverPort, payload);
            }

            @Override
            protected void done() {
                try {
                    String response = get();
                    if (onSuccess != null) {
                        onSuccess.accept(response);
                    }
                } catch (Exception ex) {
                    if (onError != null) {
                        onError.accept(ex);
                    }
                } finally {
                    if (onFinally != null) {
                        onFinally.run();
                    }
                }
            }
        }.execute();
    }

    void close() {
        tcpClient.close();
    }
}

