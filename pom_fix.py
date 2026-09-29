with open("pom.xml", "r") as f:
    pom = f.read()

pom = pom.replace(
    "<artifactId>h2</artifactId>",
    "<artifactId>h2</artifactId>\n            <version>2.2.224</version>"
)
with open("pom.xml", "w") as f:
    f.write(pom)
