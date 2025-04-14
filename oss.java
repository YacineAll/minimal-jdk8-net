<dependency>
    <groupId>net.logstash.log4j</groupId>
    <artifactId>jsonevent-layout</artifactId>
    <version>1.7</version>
</dependency>

# Root logger option
log4j.rootLogger=INFO, file, console

# Configure JSON file appender
log4j.appender.file=org.apache.log4j.RollingFileAppender
# Use an absolute path to ensure it's written where you expect
log4j.appender.file.File=${hadoop.log.dir}/my-application.log
# Fallback to a local path if hadoop.log.dir is not defined
log4j.appender.file.File.fallback=./logs/my-application.log
log4j.appender.file.MaxFileSize=10MB
log4j.appender.file.MaxBackupIndex=10
log4j.appender.file.layout=net.logstash.log4j.JSONEventLayoutV1

# Console appender configuration
log4j.appender.console=org.apache.log4j.ConsoleAppender
log4j.appender.console.Target=System.out
log4j.appender.console.layout=net.logstash.log4j.JSONEventLayoutV1
