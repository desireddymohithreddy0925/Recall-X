with open("src/main/resources/application.yml", "w") as f:
    f.write("""server:
  port: 8080

spring:
  application:
    name: recall-x
  datasource:
    url: jdbc:h2:mem:recallx;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MySQL
    username: sa
    password: 
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: none
    show-sql: ${JPA_SHOW_SQL:false}
    properties:
      hibernate:
        format_sql: true
  liquibase:
    change-log: classpath:/db/changelog/db.changelog-master.yaml
    enabled: true

hindsight:
  base-url: http://localhost:8888
  api-key: ""

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: always

springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
""")
