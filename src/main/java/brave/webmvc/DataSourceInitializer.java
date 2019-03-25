package brave.webmvc;

import java.net.Socket;
import java.util.Properties;

import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import ch.vorburger.mariadb4j.DB;
import ch.vorburger.mariadb4j.DBConfigurationBuilder;
import com.mysql.cj.jdbc.MysqlDataSource;

@Configuration
public class DataSourceInitializer {

	DB db;
	private static int dbPort=3306;
	private static String dbName="tracetest";
	private static String dbHost="localhost";
	
	public DataSourceInitializer() {
		//User MariaDB4j to create a managed MySQL db process
		DBConfigurationBuilder configBuilder = DBConfigurationBuilder.newBuilder();
		configBuilder.setPort(dbPort);
		Socket s=null;
		try {
			s = new Socket(dbHost, dbPort);
		}catch (Exception e) {}
		
		try {
			if (s==null) {
				db = DB.newEmbeddedDB(configBuilder.build());
				db.start();
				db.createDB(dbName);	
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
    @Bean (name="zipkin")
    public DataSource getMysqlDataSource() {
        //Create a datasource that will be instrumented with brave.mysql interceptors
    	MysqlDataSource dataSource = new MysqlDataSource();

        // Set dataSource Properties
        dataSource.setServerName(dbHost);
        dataSource.setPortNumber(dbPort);
        dataSource.setDatabaseName(dbName);
        dataSource.setUser("root");
        dataSource.setPassword("");
        
        try {
        	dataSource.setServerTimezone("UTC");
        	dataSource.setQueryInterceptors("brave.mysql8.TracingQueryInterceptor");
			dataSource.setExceptionInterceptors("brave.mysql8.TracingExceptionInterceptor");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return dataSource;
      }
    
    @Bean (name="jaeger")
    public DataSource getMysqlDataSourceJaeger() {
        //Create a datasource that will be instrumented with instrumented Jaeger Opentracing drivers
        
    	DriverManagerDataSource dsMgr = new DriverManagerDataSource();
        try {
        	
            //Set the sql driver which will instrument SQL calls
        	dsMgr.setDriverClassName("io.opentracing.contrib.jdbc.TracingDriver");
            dsMgr.setUsername("root");
            dsMgr.setPassword("");
            dsMgr.setUrl("jdbc:tracing:mysql://"+dbHost+":"+dbPort+"/"+dbName+"?traceWithActiveSpanOnly=true");
            Properties props=new Properties();
            props.put("serverTimezone", "UTC");
        	dsMgr.setConnectionProperties(props);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return dsMgr;
      }
}