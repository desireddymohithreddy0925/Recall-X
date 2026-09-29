import re
with open("pom.xml", "r") as f:
    pom = f.read()

# Remove pgvector dependency
pom = re.sub(r'<dependency>\s*<groupId>org.springframework.ai</groupId>\s*<artifactId>spring-ai-pgvector-store-spring-boot-starter</artifactId>[\s\S]*?</dependency>', '', pom)

with open("pom.xml", "w") as f:
    f.write(pom)
