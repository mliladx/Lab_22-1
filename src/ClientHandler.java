import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.sql.SQLException;

public class ClientHandler {
    private MyServer myServer;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String name;

    public String getName() {
        return name;
    }

    boolean checkBlackList(String _blacklist_nick) throws SQLException {
        int nickId = myServer.getAuthService().getIdByNick(_blacklist_nick);
        int blacklistId = myServer.getAuthService().getBlackListUserById(nickId);
        return blacklistId > 0;
    }

    public ClientHandler(MyServer myServer, Socket socket) {
        try {
            this.myServer = myServer;
            this.socket = socket;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            this.name = "";
            new Thread(() -> {
                try {
                    authentication();
                    readMessages();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                } finally {
                    closeConnection();
                }
            }).start();
        } catch (IOException e) {
            throw new RuntimeException("Проблемы при создании обработчика клиента");
        }
    }


    public void authentication() throws IOException, SQLException, ClassNotFoundException {
        while (true) {
            String str = in.readUTF();
            if (str.startsWith("/auth")) {

                String[] parts = str.split("\\s");
                String nick = myServer.getAuthService().getNickByLoginPassDB(parts[1], parts[2]);
                if (nick != null) {
                    if (!myServer.isNickBusy(nick)) {
                        name = nick;
                        myServer.broadcastMsg(nick + " зашел в чат");
                        sendMsg("Авторизация пройдена " + "Hello " + nick + " !");
                        myServer.subscribe(this);
                        return;
                    } else {
                        sendMsg("Учетная запись уже используется");
                    }
                }else {
                    sendMsg("Неверные логин/пароль");
                }
            }
        }
    }
    private void readMessages() throws IOException, SQLException, ClassNotFoundException {
        while (true) {
            String strFromClient = in.readUTF();
            System.out.println("от " + name + ": " + strFromClient);
            if (strFromClient.equals("/end")) {
                closeConnection();
            }
            if (strFromClient.startsWith("/adduser ")) {
                myServer.getAuthService().setNewUsers(1,2,3);
            }
            if (strFromClient.startsWith("/blacklist ")) {
                String[] tokens = strFromClient.split(" ");
                int nickId = myServer.getAuthService().getIdByNick(name);
                int nicknameId = myServer.getAuthService().getIdByNick(tokens[1]);
                myServer.getAuthService().addBlackListByNickAndNickName(nickId, nicknameId);
                sendMsg("Вы добавили пользователя " + tokens[1] + " в черный список");
            }
            if (strFromClient.startsWith("/changename ")){
                String newNickname = strFromClient.split("\\s", 2)[1];
                myServer.broadcastMsgToChangeName(name,newNickname);
                name = newNickname;
            }
            else {
                myServer.broadcastMsg(name + ": " + strFromClient);
            }
            if (strFromClient.startsWith("/w")) {
                String[] parts = strFromClient.split("\\s");
                if (myServer.isNickBusy(parts[1])) {
                    myServer.broadcastMsgToNick(name, parts[1], parts[2] );
                }
                else {
                    myServer.broadcastMsg(name + ": " + strFromClient);
                }
            }
        }
    }
    public void sendMsg(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void closeConnection() {
        myServer.unsubscribe(this);
        myServer.broadcastMsg(name + " вышел из чата");
        try {
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}