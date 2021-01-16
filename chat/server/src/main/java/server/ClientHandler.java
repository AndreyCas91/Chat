package server;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientHandler {

    Server server = null;
    Socket socket = null;
    DataInputStream in;
    DataOutputStream out;
    private String nickname;
    private String login;
    private FileOutputStream fileOS;
    private BufferedReader fileBR;
    private ExecutorService service;

    public ClientHandler(Server server, Socket socket) {
        try {
            this.server = server;
            this.socket = socket;
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
/* Выбрал CachedThreadPool. чат у нас заточен под групповое общение, считаю что так будет эффективно. Под каждого
пользователя свой поток. Отключился, за ним и поток отключится.

Считаю что использовать ExecutorService нужно, если кол-во пользователей будет большое, но не уверен что правильно выбрал
CachedThreadPool.

 */

            service = Executors.newCachedThreadPool();

            service.execute(()-> {
                    try {
                        // цикл аутентификации
                        while (true){
                            socket.setSoTimeout(12000);
                            String str = in.readUTF();

                            if (str.startsWith("/auth")){
                                String[] token = str.split("\\s");
                                String newNick = server.getAuthService().getNicknameByLoginAndPassword(token[1], token[2]);
                                login = token[1];

                                if (newNick != null){
                                    if(!server.isLoginAuthenticated(token[1])) {
                                        nickname = newNick;
                                        sendMsg("/authok " + nickname);
                                        server.subscribe(this);
                                        System.out.println("Клиент " + nickname + " подключился");
                                        server.log("Клиент " + nickname + " подключился");
                                        socket.setSoTimeout(0);
                                        fileOS = new FileOutputStream("chat/" +
                                                "client/src/main/java/client/" +
                                                "historyChat/history_" + login + ".txt", true);
                                        break;
                                    }else{
                                        sendMsg("С данной учетной записью уже зашли");
                                    }
                                }else {
                                    sendMsg("Неверный логин / пароль");
                                }
                            }

                            if (str.startsWith("/reg")){
                                String[] token = str.split("\\s");
                                if(token.length < 4){
                                    continue;
                                }
                                boolean isRegistration = server.getAuthService()
                                        .registration(token[1], token[2], token[3]);
                                if(isRegistration){
                                    sendMsg("/regok");
                                } else {
                                    sendMsg("/regno");
                                }
                            }
                        }

                        // цикл работы
                        fileBR = new BufferedReader(new FileReader("chat/" +
                                "client/src/main/java/client/" +
                                "historyChat/history_" + login + ".txt"));

                        String strChat;
                        while ((strChat = fileBR.readLine()) != null){
                            sendMsg(strChat + "\n");
                        }

                        while (true) {
                            String str = in.readUTF();

                            if(str.startsWith("/")) {

                                if (str.equals("/end")) {
                                    out.writeUTF("/end");
                                    break;
                                }

                                if (str.startsWith("/w")) {
                                    String[] token = str.split("\\s+", 3);
                                    if (token.length <3){
                                        continue;
                                    }
                                    server.privateCastMsg(this, token[1], token[2]);
                                }

                            }else {
                                server.broadCastMsg(this, str);
                            }
                        }
                    } catch (IOException e) {
                        server.log("Ошибка со стороны клиента: " + e.toString());
                        e.printStackTrace();
                    }finally {
                        System.out.println("Клиент отключился");
                        server.log("Клиент отключился");
                        server.unsubscribe(this);
                        try {
                            fileOS.close();
                            fileBR.close();
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
            });
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            service.shutdown();
        }
    }

    void sendMsg(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getNickname(){
        return nickname;
    }

    public String getLogin() {
        return login;
    }

    public void historyChat(String str) throws IOException {
        fileOS.write((str + "\n").getBytes());
    }
}
