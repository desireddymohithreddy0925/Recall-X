import re
with open("pom.xml", "r") as f:
    pom = f.read()

webflux_dep = """
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webflux</artifactId>
        </dependency>
"""
if "spring-boot-starter-webflux" not in pom:
    pom = pom.replace("</dependencies>", webflux_dep + "</dependencies>")
    with open("pom.xml", "w") as f:
        f.write(pom)
