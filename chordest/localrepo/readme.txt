See http://stackoverflow.com/questions/2229757/maven-add-a-dependency-to-a-jar-by-relative-path

Add libraries to local repository using this maven command:

mvn org.apache.maven.plugins:maven-install-plugin:2.3.1:install-file \
                         -Dfile=<path-to-file> -DgroupId=<myGroup> \ 
                         -DartifactId=<myArtifactId> -Dversion=<myVersion> \
                         -Dpackaging=<myPackaging> -DlocalRepositoryPath=<path>

Sources jars were added manually after installing libraries to the repository.

Then declare it as any other dependency:

<dependency>
  <groupId>your.group.id</groupId>
  <artifactId>3rdparty</artifactId>
  <version>X.Y.Z</version>
</dependency>
