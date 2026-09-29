import re
with open("pom.xml", "r") as f:
    pom = f.read()

h2_dep = """
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>runtime</scope>
        </dependency>
"""
if "h2database" not in pom:
    pom = pom.replace("</dependencies>", h2_dep + "</dependencies>")
    with open("pom.xml", "w") as f:
        f.write(pom)
