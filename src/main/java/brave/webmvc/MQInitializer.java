package brave.webmvc;


import java.net.Socket;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;


import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TemporaryQueue;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.NamingException;

import org.apache.qpid.server.SystemLauncher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import brave.jms.JmsTracing;
import io.opentracing.Tracer;
import io.opentracing.contrib.jms.TracingMessageProducer;
import io.opentracing.contrib.jms.common.TracingMessageConsumer;
import io.opentracing.util.GlobalTracer;
 

@Configuration
public class MQInitializer {
	
	private static final String INITIAL_CONFIGURATION = "qpid-config.json";
	private static final String INITIAL_SYSTEM_PROPERTIES = "system.properties";
	private static final String AUTH="admin";
	private static final int PORT=8993;
	
	
	public MQInitializer() {
		Socket s=null;
		try {
			s = new Socket("localhost", PORT);
		}catch (Exception e) {}
		
		//Start the embedded qpid broker
		if (s==null) {
		try {
			EmbeddedBroker broker=new EmbeddedBroker();
			broker.start();
			Thread.sleep(5000);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
		}
	}
	
	public static void handleMessage(String username, JmsTracing jmsTracing) {
		
		try {
			Hashtable<Object, Object> env = new Hashtable<Object, Object>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, "org.apache.qpid.jms.jndi.JmsInitialContextFactory");
			env.put("connectionfactory.myFactoryLookup", "amqp://localhost:8993?jms.clientID=testClient&amqp.saslMechanisms=PLAIN");
			
			javax.naming.Context context = new javax.naming.InitialContext(env);
			//tracing is configured at the connection level
			ConnectionFactory factory = 
					jmsTracing.connectionFactory((ConnectionFactory) context.lookup("myFactoryLookup"));
			
			Connection connection = factory.createConnection(AUTH,AUTH);
			connection.setExceptionListener(new MyExceptionListener());

			connection.start();
			Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			
			
			//Create a temporary queue and consumer to receive responses, and a producer to send requests.
			TemporaryQueue queue = session.createTemporaryQueue();
			MessageConsumer messageConsumer = session.createConsumer(queue);
			MessageProducer messageProducer = session.createProducer(queue);

			//Send some requests and receive the responses.
			String[] requests = new String[] { username+":I Fashion My Crown From Quetzalcoatl's Quills",
			                                   "Build My Palace In The Jungles Of Brazil"
			                                   };

			for (String request : requests) {
			    TextMessage requestMessage = session.createTextMessage(request);
			    requestMessage.setJMSReplyTo(queue);

			    messageProducer.send(requestMessage, DeliveryMode.NON_PERSISTENT, Message.DEFAULT_PRIORITY, Message.DEFAULT_TIME_TO_LIVE);

			    TextMessage responseMessage = (TextMessage) messageConsumer.receive(2000);
			    if (responseMessage != null) {
			        System.out.println("[CLIENT] " + request + " ---> " + responseMessage.getText());
			    } else {
			        System.out.println("[CLIENT] Response for '" + request +"' was not received within the timeout, exiting.");
			        break;
			    }
			}
			connection.close();
		} catch (NamingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
	}
	
	public static void handleMessageJaeger(String username, Tracer tracer) {
		
		try {
			
			GlobalTracer.register(tracer);
			
			Hashtable<Object, Object> env = new Hashtable<Object, Object>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, "org.apache.qpid.jms.jndi.JmsInitialContextFactory");
			env.put("connectionfactory.myFactoryLookup", "amqp://localhost:8993?jms.clientID=testClient&amqp.saslMechanisms=PLAIN");
			
			javax.naming.Context context = new javax.naming.InitialContext(env);
			//tracing is configured at the connection level
			ConnectionFactory factory = (ConnectionFactory) context.lookup("myFactoryLookup");
			
			Connection connection = factory.createConnection(AUTH,AUTH);
			connection.setExceptionListener(new MyExceptionListener());

			connection.start();
			Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			
			
			//Create a temporary queue and consumer to receive responses, and a producer to send requests.
			TemporaryQueue queue = session.createTemporaryQueue();
			MessageConsumer messageConsumer = session.createConsumer(queue);
			MessageProducer messageProducer = session.createProducer(queue);
			
			//Wrap the producer and consumer with tracing
			TracingMessageProducer producer = new TracingMessageProducer(messageProducer,tracer);
			TracingMessageConsumer consumer = new TracingMessageConsumer(messageConsumer,tracer);
			
			//Send some requests and receive the responses.
			String[] requests = new String[] { username+":I Fashion My Crown From Quetzalcoatl's Quills",
			                                   "Build My Palace In The Jungles Of Brazil"
			                                   };

			for (String request : requests) {
			    TextMessage requestMessage = session.createTextMessage(request);
			    requestMessage.setJMSReplyTo(queue);

			    producer.send(requestMessage, DeliveryMode.NON_PERSISTENT, Message.DEFAULT_PRIORITY, Message.DEFAULT_TIME_TO_LIVE);

			    TextMessage responseMessage = (TextMessage) consumer.receive(2000);
			    if (responseMessage != null) {
			        System.out.println("[CLIENT] " + request + " ---> " + responseMessage.getText());
			    } else {
			        System.out.println("[CLIENT] Response for '" + request +"' was not received within the timeout, exiting.");
			        break;
			    }
			}
			connection.close();
		} catch (NamingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
	}

	public class EmbeddedBroker extends Thread {

	    public void run(){
	    	final SystemLauncher systemLauncher = new SystemLauncher();
	        try {
	            systemLauncher.startup(createSystemConfig());
	            
	        } catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
	    }   
	 }
 
    private Map<String, Object> createSystemConfig() {
    	
    	ClassLoader classLoader = getClass().getClassLoader();
			String config =  classLoader.getResource(INITIAL_CONFIGURATION).getFile();
			String sysprops =  classLoader.getResource(INITIAL_SYSTEM_PROPERTIES).getFile();
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("type", "Memory");
            attributes.put("initialSystemPropertiesLocation", sysprops);
            attributes.put("initialConfigurationLocation", config);
            attributes.put("startupLoggedToSystemOut", true);
            return attributes;
                
    }
	
	@Bean Connection connectionFactory() {
		

		Connection connection=null;
		try {
			//Context context = new InitialContext();
			
			
			
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return connection;
	}
	
	private static class MyExceptionListener implements ExceptionListener {
        @Override
        public void onException(JMSException exception) {
            System.out.println("Connection ExceptionListener fired, exiting.");
            exception.printStackTrace(System.out);
            System.exit(1);
        }
    }

}
