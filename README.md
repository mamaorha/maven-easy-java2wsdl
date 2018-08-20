# maven-easy-java2wsdl
wrapping cxf-java2ws-plugin, supporting generation by classes / packages + merging ability.

when i used cxf-java2ws-plugin i had to add class by class which made my pom a bit messy + i was getting out of memory issues.
this plugin is a wrapper that will accept list of classes / packages and generate wsdls for the @Webservice classes + gives the user the ability to merge all of the generated wsdls into 1 big wsdl.