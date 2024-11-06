package socket;

import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    public static final int PORT = 4000;
    private ServerSocket serverSocket;
    private volatile boolean running = true;
    private Map<String, Socket> authenticatedClients;

    public Server() {
        this.authenticatedClients = new HashMap<>();
    }

    public void start() throws IOException {
        System.out.println("Servidor de Chat iniciado na porta: " + PORT);
        serverSocket = new ServerSocket(PORT);

        new Thread(this::clientConnectionLoop).start();
        new Thread(this::consoleInputLoop).start();
    }

    private void clientConnectionLoop() {
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Novo cliente conectado: " + clientSocket.getRemoteSocketAddress());

                new Thread(new ClientHandler(clientSocket)).start();
            } catch (IOException e) {
                if (running) {
                    System.out.println("Erro ao aceitar conexão: " + e.getMessage());
                }
            }
        }
    }

    private void consoleInputLoop() {
        Scanner scanner = new Scanner(System.in);
        while (running) {
            if (scanner.hasNextLine()) {
                String input = scanner.nextLine();
                if ("sair".equalsIgnoreCase(input)) {
                    running = false;
                    closeServer();
                }
            }
        }
    }

    private void closeServer() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            System.out.println("\nServidor finalizado");
        } catch (IOException e) {
            System.out.println("Erro ao fechar o servidor: " + e.getMessage());
        }
    }

    private class ClientHandler implements Runnable {
        private Socket clientSocket;
        private String token;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try (
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            ) {
                String inputLine;
                while ((inputLine = in.readLine()) != null && running) {
                    if (token == null) {
                        // Primeira mensagem deve ser para autenticação
                        if (inputLine.startsWith("AUTH ")) {
                            token = UUID.randomUUID().toString();
                            authenticatedClients.put(token, clientSocket);
                            out.println("AUTH_SUCCESS " + token);
                            System.out.println("Cliente autenticado com token: " + token);
                        } else {
                            out.println("ERROR Autenticação necessária. Use AUTH <seu_nome>");
                        }
                    } else {
                        processarMensagem(inputLine);
                    }
                    
                    if ("sair".equalsIgnoreCase(inputLine)) {
                        break;
                    }
                }
            } catch (IOException e) {
                System.out.println("Erro ao comunicar com o cliente: " + e.getMessage());
            } finally {
                try {
                    if (token != null) authenticatedClients.remove(token);
                    clientSocket.close();
                } catch (IOException e) {
                    System.out.println("Erro ao fechar socket do cliente: " + e.getMessage());
                }
            }
        }
    }

    private void processarMensagem(String mensagem) {
        for (Socket client : authenticatedClients.values()) {
            try {
                PrintWriter out = new PrintWriter(client.getOutputStream(), true);
                out.println(mensagem);
            } catch (IOException e) {
                System.out.println("Erro ao enviar mensagem para um cliente: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        try {
            Server server = new Server();
            server.start();
            System.out.println("Servidor de chat está ativo!");
        } catch (IOException e) {
            System.out.println("Erro ao iniciar o servidor! Erro: " + e.getMessage());
        }
    }
}
