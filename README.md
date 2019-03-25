# tracelab

Java Spring Web MVC app instrumented with Zipkin and Jaeger. 

## Details about the app

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. See deployment for notes on how to deploy the project on a live system. The project contains a managed MySQL (MariaDB) instance running on port 3306, an embedded AMQP broker running on port 8993, a frontend spring web instance running on port 8081, and a backend spring web instance on port 9000.

There are two sets of backend services instrumented with Zipkin and Jaeger each:
1. /api,/querydb, /putmq are instrumented with Zipkin
2. /apij,/querydbj,/putmqj are instrumented with Jaeger

The frontends call the respective set of backends and the endpoints are as follow:
1. / for Zipkin
2. /j for Jaeger

Traces will get sent locally so you will need to have zipkin and/or jaeger running:
1. To start zipkin
curl -sSL https://zipkin.io/quickstart.sh | bash -s
java -jar zipkin.jar

2. To start jaeger
docker pull jaegertracing/all-in-one:1.10
docker run   --rm   --name jaeger   -p6831:6831/udp   -p16686:16686   jaegertracing/all-in-one:1.10

## Building and Running the app

Once you have mvn installed run the following in the same order:
1. mvn jetty:run -Pbackend
2. mvn jetty:run -Pfrontend

This will start the frontend and backend webapps, the embedded QPid java AMQP broker, and the managed MariaDB SQL datasource.

Generate a few requests and check out the trace server UI
1. For zipkin
curl -s localhost:8081/ -H'user-name:bproverb'
2. For jaeger
curl -s localhost:8081/j -H'user-name:bproverb'

Once you generate traces you can access them at:
1. Zipkin UI
http://localhost:9411
2. Jaeger UI
http://localhost:16686

## Built With

* [Maven](https://maven.apache.org/) - Dependency Management

## Authors

* **Peco Karayanev** - *@bproverb* - 
```
Project is borrowing code heavily from https://github.com/openzipkin/brave-webmvc-example and https://github.com/yurishkuro/opentracing-tutorial

## License

This project is licensed under the Apache 2.0 License 

## Acknowledgments

* Hat tip to anyone whose code was used
* Inspiration
* etc

