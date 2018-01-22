

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
/**
 * ��Ϣ�ֳ�����
 * 1.�����Ự���ӵ���Ϣ
 * 2.�رջỰ���ӵ���Ϣ
 * 3.�Ի���Ϣ
 *
 *ǰ�������ƶ������ݣ�����Ҫ���й���
 *��������Ҫ����js�����ͻ���ַ�ת��һ�£�������ν�Ĺ���
 *
 */

/**
 * tomcat�ڱ������е�����
 * tomcat���������
 * һ�����¼�������ִ����Ӧ�ķ���
 * 
 * �������µ��û���������
 * tomcat��ִ�������@OnOpen����
 *
 */


@ServerEndpoint(value = "/websocket/{userid}")

public class ChatAnnotation {
    private static final String GUEST_PREFIX = "�û�";  
    /** 
     * һ���ṩԭ�Ӳ�����Integer���ࡣ��Java�����У�++i��i++�����������̰߳�ȫ�ģ� 
     * ��ʹ�õ�ʱ�򣬲��ɱ���Ļ��õ�synchronized�ؼ��֡� ��AtomicInteger��ͨ��һ���̰߳�ȫ�ļӼ������ӿڡ� 
     */  
    private static final AtomicInteger connectionIds = new AtomicInteger(0);  
    private static final Set<ChatAnnotation> connections = new CopyOnWriteArraySet<>();  
  
    private  String nickname;  
    private Session session;  
  
    public ChatAnnotation() {  
        nickname = GUEST_PREFIX + connectionIds.getAndIncrement();  
    }  
  
    /** 
     * ��������ʱ����õķ��� 
     *  
     * @param session 
     */  
    @OnOpen  
    public void start(@PathParam("userid") String userid,Session session) { 
    	nickname=userid;
        this.session = session;  
        connections.add(this);  
        String message = String.format("* %s %s", nickname, "����������");  
        //����֪ͨ  
        broadcast(message);  
        try {  
            //ϵͳ�ʺ���  
            SendHello(this.nickname);  
            //���������û�  
            onlineList();
        } catch (IOException e) {  
            e.printStackTrace();  
        }  
          
    }  
  
    /** 
     * ���ӹر�ʱ���÷��� 
     * @throws IOException 
     */  
    @OnClose  
    public void end(@PathParam("userid") String userid) throws IOException {  
    	nickname=userid;
        connections.remove(this);  
        String message = String.format("* %s %s", nickname, "�˳�������");  
        broadcast(message);
        onlineList();
    }  
    /** 
     * ������Ϣ�����е��÷��� 
     * @param message 
     */  
    @OnMessage  
    public void incoming(@PathParam("userid") String userid,String message) {  
        // Never trust the client  
        // TODO: �������������  
    	nickname=userid;
        String m = String.format("* %s %s", nickname, message);  
        if(m.contains("to")){  
            //��Ե㷢��  
            broadcastOneToOne(m,nickname);  
        }else{  
            //Ⱥ��  
            broadcast(m);  
        }  
    }  
    /** 
     * ���������ǵ��÷��� 
     * @param t 
     * @throws Throwable 
     */  
    @OnError  
    public void onError(Throwable t) throws Throwable {  
        System.out.println("����: " + t.toString());  
    }  
    /** 
     * ��Ϣ�㲥 
     * ͨ��connections�������������û�������Ϣ�ķ��� 
     * @param msg 
     */  
    private static void broadcast(String msg) {  
        for (ChatAnnotation client : connections) {  
            try {  
                synchronized (client) {  
                    client.session.getBasicRemote().sendText(msg);  
                }  
            } catch (IOException e) {  
                System.out.println("����:��ͻ��˷�����Ϣʧ��");  
                connections.remove(client);  
                try {  
                    client.session.close();  
                } catch (IOException e1) {  
                    e1.printStackTrace();  
                }  
                String message = String.format("* %s %s", client.nickname,"�˳�������");  
                broadcast(message);  
            }  
        }  
    }  
    /** 
     * һ��һ������Ϣ
     * ������broadcast��
     * �ȱ���connections��������������˷�����Ϣ
     * @param msg 
     */  
    private static void broadcastOneToOne(String msg, String nickName) {  
        String[] arr = msg.split("to");  
        for (ChatAnnotation client : connections) {  
            try {  
                if(arr[1].equals(client.nickname) || nickName.equals(client.nickname)){  
                    synchronized (client) {  
                        client.session.getBasicRemote().sendText(arr[0]+"[˽����Ϣ]");  
                    }  
                }  
            } catch (IOException e) {  
                System.out.println("����:��ͻ��˷�����Ϣʧ��");  
                connections.remove(client);  
                try {  
                    client.session.close();  
                } catch (IOException e1) {  
                    e1.printStackTrace();  
                }
                String message = String.format("* %s %s", client.nickname,"�˳�������");  
                broadcast(message);  
            }  
        }  
    }  
    //ϵͳ�ʺ���  
    private static void SendHello(String nickName) throws IOException{  
        String m = String.format("* %s %s", nickName, "���");  
        for (ChatAnnotation client : connections) {  
            if(client.nickname.equals(nickName)){  
                client.session.getBasicRemote().sendText(m);  
            }  
        }  
    }  
    //�����û�  
    private static void onlineList() throws IOException{  
        String online = "";  
        for (ChatAnnotation client : connections) {  
            if(online.equals("")){  
                online = client.nickname;  
            }else{  
                online += ","+client.nickname;  
            }  
        }  
        String m = String.format("* %s,%s", "��ǰ�����û�", online);  
        for (ChatAnnotation client : connections) {  
            client.session.getBasicRemote().sendText(m);  
        }  
    }
}
