package brave.webmvc;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;


import brave.ScopedSpan;
import brave.Span;
import brave.Tracing;
import brave.jms.JmsTracing;
import io.opentracing.Scope;
import io.opentracing.util.GlobalTracer;


@Configuration
@EnableWebMvc
@RestController
public class Backend {

	//Zipkin instrumentation
	@Autowired Tracing tracing;
	@Autowired 
	@Qualifier("zipkin")
	DataSource ds;
	@Autowired JmsTracing jmsTracing;
  
	//Jaeger instrumentation
	@Autowired io.opentracing.Tracer jaegerTracing;
	@Autowired 
	@Qualifier("jaeger")
	DataSource dsj;
 
  
	
  @RequestMapping("/api")
  public String printDate(@RequestHeader(name = "user-name", required = false) String username) {
    if (username != null) {
    	
    	ScopedSpan ss=tracing.tracer().startScopedSpan("root");
    	
    	//Example new long running Span
    	Span longSpan=tracing.tracer().nextSpan().name("Taking a nap");
		  try {
			  longSpan.start();
			  Thread.sleep(ThreadLocalRandom.current().nextLong(3000));
			  
		  } catch (Exception e) {
			longSpan.error(e);
		  }finally {
			longSpan.finish();
		  }
      
    	//Example Error Span
		  Span errorSpan=tracing.tracer().nextSpan().name("Something is very wrong");
		  try {
			  errorSpan.start();
			  errorSpan.tag("receiptNo", "ABCD1234");
			  
			  Thread.sleep(ThreadLocalRandom.current().nextLong(2000));
			  throw new Exception("Kaboom!");
			  
		  } catch (Exception e) {
			  errorSpan.error(e);
		  }finally {
			  errorSpan.finish();
		  }  
    	
    	ss.finish();
    	return new Date().toString() + " " + username;
    }
    return new Date().toString();
  }
  
  @RequestMapping("/querydb")
  public String queryMySQL(@RequestHeader(name = "user-name", required = false) String username) {
    if (username != null) {
    	
    	//Example SQL query tracing
		try {
			
			Connection conn = ds.getConnection();		
			String query="show databases;";
			Statement st = conn.createStatement();
			st.execute(query);
            
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	return "Query MySQL: "+username;
    }
    return new Date().toString();
  }
  
  @RequestMapping("/putmq")
  public String saveUsernameMQ(@RequestHeader(name = "user-name", required = false) String username) {
    if (username != null) {
    	
    	MQInitializer.handleMessage(username,jmsTracing);
    	
    	return "Put in MQ: "+username;
    }
    return new Date().toString();
  }
  
	@RequestMapping("/apij")
	public String printDateJ(@RequestHeader(name = "user-name", required = false) String username,@RequestHeader HttpHeaders headers) {
		if (username != null) {
			
			//For web tracing
			Scope scope = TracingConfigurationJaeger.startServerSpan(GlobalTracer.get(), headers, "/apij");
			
			//Scope scope = GlobalTracer.get().buildSpan("/apij").startActive(true);
			
			io.opentracing.Span longSpan=GlobalTracer.get().buildSpan("Taking a nap").asChildOf(scope.span()).start();
			try  {
				
				Thread.sleep(ThreadLocalRandom.current().nextLong(2000));
				
			} catch (Exception e) {
				
				e.printStackTrace();

			}finally {
				longSpan.finish();
			}

			// Example Span with exception code
			io.opentracing.Span span2=GlobalTracer.get().buildSpan("Process receipt").asChildOf(scope.span()).start();
			try {
				span2.setTag("receiptNo", "1234");
				Thread.sleep(ThreadLocalRandom.current().nextLong(2000));
				throw new Exception("Kaboom!");
				
			} catch (Exception e) {
				
				errorHandler(e.getMessage());
			} finally {
				span2.finish();
			}

			scope.close();
			return new Date().toString() + " " + username;
		}
		return new Date().toString();
	}
	
	private void errorHandler(String error) {
		try (Scope scope = GlobalTracer.get().buildSpan("Something went wrong").startActive(true)){
			Thread.sleep(ThreadLocalRandom.current().nextLong(2000));
			scope.span().log(error);
		}catch (Exception e) {
			
		}
	}
	
	@RequestMapping("/querydbj")
	  public String queryMySQLJ(@RequestHeader(name = "user-name", required = false) String username,@RequestHeader HttpHeaders headers) {
	    if (username != null) {
	    	
	    	
	    	Scope scope = TracingConfigurationJaeger.startServerSpan(GlobalTracer.get(), headers, "/querydbj");
	    	//Scope scope = GlobalTracer.get().buildSpan("/querydbj").startActive(true);
			
	    	//Example SQL query tracing
	    	io.opentracing.Span span = GlobalTracer.get().buildSpan("SQL Query").asChildOf(scope.span()).start();
			try {
				
				Connection conn = dsj.getConnection();	
				String query="SELECT 1;";
				Statement st = conn.createStatement();
				st.execute(query);
	            
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    	
			span.finish();
			scope.close();
	    	return "Query MySQL: "+username;
	    }
	    return new Date().toString();
	  }

	@RequestMapping("/putmqj")
	  public String saveUsernameMQJ(@RequestHeader(name = "user-name", required = false) String username,@RequestHeader HttpHeaders headers) {
	    if (username != null) {
	    	
	    	Scope scope = TracingConfigurationJaeger.startServerSpan(GlobalTracer.get(), headers, "/putmqj");
	    	//Scope scope = GlobalTracer.get().buildSpan("/putmqj").startActive(true);
		
	    	io.opentracing.Span span = GlobalTracer.get().buildSpan("JMS operation").asChildOf(scope.span()).start();
	    	try {
	    		MQInitializer.handleMessageJaeger(username,GlobalTracer.get());	
	    	}catch (Exception e) {
	    		
	    	}
	    	span.finish();
	    	
	    	scope.close();
	    	return "Put in MQ: "+username;
	    }
	    return new Date().toString();
	  }
}
  
