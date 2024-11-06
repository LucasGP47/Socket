package socket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private final String serverEndereco = "127.0.0.1"; 
    private Socket clientSocket;
    private Scanner scanner;
    private String token;

    public Client() {
        scanner = new Scanner(System.in);
    }

    public void start() throws IOException {
        clientSocket = new Socket(serverEndereco, 4000);
        System.out.println("Cliente conectado ao servidor de chat: " + serverEndereco + " // Porta:" + 4000);
        autenticar();
        requisicaoLoop();
    }

    private void autenticar() {
        try (
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        ) {
            System.out.print("Digite seu nome para autenticação: ");
            String nome = scanner.nextLine();
            out.println("AUTH " + nome);

            String resposta = in.readLine();
            if (resposta.startsWith("AUTH_SUCCESS")) {
                token = resposta.split(" ")[1];
                System.out.println("Autenticação bem-sucedida! Token recebido: " + token);
            } else {
                System.out.println("Falha na autenticação: " + resposta);
                clientSocket.close();
            }
        } catch (IOException e) {
            System.out.println("Erro ao autenticar no servidor: " + e.getMessage());
        }
    }

    private void requisicaoLoop() {
        try (
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        ) {
            String mensagem;
            do {
                System.out.print("Digite sua mensagem (ou 'sair' para finalizar): ");
                mensagem = scanner.nextLine();
                if (token != null) {
                    out.println(token + " " + mensagem);
                }
                String resposta = in.readLine();
                if (!"sair".equalsIgnoreCase(mensagem)) {
                    System.out.println("Mensagem recebida: " + resposta);
                }
            } while (!mensagem.equalsIgnoreCase("sair"));
        } catch (IOException e) {
            System.out.println("Erro ao comunicar com o servidor: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.out.println("Erro ao fechar socket do cliente: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        try {
            Client client = new Client();
            client.start();
        } catch (IOException e) {
            System.out.println("Erro ao iniciar o cliente: " + e.getMessage());
        }
        System.out.println("Cliente finalizado!");
    }
}
