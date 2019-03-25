package brave.webmvc;


import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import io.opentracing.Scope;
import okhttp3.OkHttpClient;


@EnableWebMvc
@RestController
@Configuration
@CrossOrigin // So that javascript can be hosted elsewhere
public class Frontend {
  @Autowired 
  @Qualifier("zipkinRest")
  RestTemplate restTemplate;
  
  @Autowired io.opentracing.Tracer jaegerTracing;
  
  
  @RequestMapping("/") public String callBackend(@RequestHeader(name = "user-name", required = false) String username) {
	  List<String> endPoints=new ArrayList<String>();
	  endPoints.add("http://localhost:9000/api");
	  endPoints.add("http://localhost:9000/querydb");
	  endPoints.add("http://localhost:9000/putmq");
	  
	  HttpHeaders headers = new HttpHeaders();
	  headers.set("user-name", username);
	  HttpEntity entity = new HttpEntity(headers);
	  
	  String result="";
	  for (int i=0;i<endPoints.size();i++) {
		  

		  ResponseEntity<String> response = restTemplate.exchange(
		      endPoints.get(i), HttpMethod.GET, entity, String.class);
		  result+=response.getBody();
	  }
	  
	  return result;
  }
  
  
  @RequestMapping("/j") public String callBackendJ(@RequestHeader(name = "user-name", required = false) String username) {
	 
	  OkHttpClient client=new OkHttpClient();
	  
	  String response="";
	  try (Scope scope = jaegerTracing.buildSpan("/j").startActive(true)) {
          response = TracingConfigurationJaeger.getHttp(9000, "apij", "user-name", username,jaegerTracing,client);
          response += TracingConfigurationJaeger.getHttp(9000, "querydbj", "user-name", username,jaegerTracing,client);
          response += TracingConfigurationJaeger.getHttp(9000, "putmqj", "user-name", username,jaegerTracing,client);
	  }catch (Exception e) {
		  e.printStackTrace();
	  }
	 
	  return response;
  }
  
}
