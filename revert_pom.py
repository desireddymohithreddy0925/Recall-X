import re
with open("pom.xml", "r") as f:
    pom = f.read()

pom = pom.replace("""        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webflux</artifactId>
        </dependency>""", "")

with open("pom.xml", "w") as f:
    f.write(pom)
