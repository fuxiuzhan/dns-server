FROM centos

MAINTAINER fuxiuzhan@163.com

RUN yum update -y && \
    yum install -y java

ADD ./target/*.jar /fxz/apps/

WORKDIR /fxz/apps/

ENV APPNAME="console"

ENV VERSION=0.0.1

ENV PATH=$PATH:/fxz/apps/

ENV SERVER="inner.lite.fuled.xyz:11800"

ENV JAVA_OPTS="-Denv=prd"

RUN 'wget https://archive.apache.org/dist/skywalking/8.7.0/apache-skywalking-apm-8.7.0.tar.gz && \
    gzip -d apache-skywalking-apm-8.7.0.tar.gz && \
    tar -xvf apache-skywalking-apm-8.7.0.tar && \
    rm  apache-skywalking-apm-8.7.0.tar'

ENV AGENTPATH="/fxz/apps/apache-skywalking-apm-bin/agent/skywalking-agent.jar";

EXPOSE 8080/tcp,53/udp

CMD ["mkdir /data"]

VOLUME ["/data"]

RUN echo "java -javaagent:$AGENTPATH -Dagent.service_name=$APPNAME,collector.backend_service=$SERVER $JAVA_OPTS  -jar $APPNAME$VERSION.jar" > start.sh

RUN chmod +x start.sh

ENTRYPOINT ["sh","-c","start.sh"]