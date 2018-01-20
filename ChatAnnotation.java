

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
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


@ServerEndpoint(value = "/websocket/chat")

public class ChatAnnotation {

  
    private static final String GUEST_PREFIX = "Guest";//�û�����ǰ׺
    private static final AtomicInteger connectionIds = new AtomicInteger(0);//�����������������û����  atomicinteger�� �����������̰߳�ȫ�������ڸ߲���
    private static final Set<ChatAnnotation> connections =
            new CopyOnWriteArraySet<ChatAnnotation>();//���ӵ��û�   set����

    private final String nickname;//�û���
    private Session session;//�Ự

    /* �������������û�������ǰ�û���    Ĭ��ǰ׺Guest  +   �û��������*/
    public ChatAnnotation() {
        nickname = GUEST_PREFIX + connectionIds.getAndIncrement();
    }

    //����һ���µĻỰ
    @OnOpen
    public void start(Session session) {
        this.session = session;
        connections.add(this);//����һ�Ự���뵽���Ӽ���set��
        String message = String.format("* %s %s", nickname, "has joined.");
        broadcast(message);
    }

    //��ֹ�Ự
    @OnClose
    public void end() {
        connections.remove(this);
        String message = String.format("* %s %s",
                nickname, "has disconnected.");
        broadcast(message);
    }

    //������Ϣ���ȹ��ˣ������˺����Ϣ���ͳ�ȥ
    @OnMessage
    public void incoming(String message) {
        // Never trust the client
    	/* ���˹������Ϣ*/
        String filteredMessage = String.format("%s: %s",
                nickname, HTMLFilter.filter(message.toString()));
        broadcast(filteredMessage);
    }



    /* ������IO��������ɾ�� */
    @OnError
    public void onError(Throwable t) throws Throwable {
      
    }


    private static void broadcast(String msg) {
        for (ChatAnnotation client : connections) {
            try {
                synchronized (client) {
                    client.session.getBasicRemote().sendText(msg);
                }
            } catch (IOException e) {
              
                connections.remove(client);
                try {
                    client.session.close();
                } catch (IOException e1) {
                    // Ignore
                }
                String message = String.format("* %s %s",
                        client.nickname, "has been disconnected.");
                broadcast(message);
            }
        }
    }
}
