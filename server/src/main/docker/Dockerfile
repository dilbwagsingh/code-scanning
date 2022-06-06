FROM 578061096415.dkr.ecr.us-east-1.amazonaws.com/config-base:22.30.0-prod-java-wkhtmltopdf

#PROD 
RUN sed -i "s/jdbc:mysql:\/\/db.kaar.biz:3306\/SecureDBProd/jdbc:mysql:\/\/db.kaar.biz:3306\/SecureDBProd?serverTimezone=PST/g" /etc/kaarmaconfig-prod/securedb.xml

#PROD-DE 
RUN sed -i "s/jdbc:mysql:\/\/mykaardbprod-rds01.mykaarma.de\/SecureDBProd/jdbc:mysql:\/\/mykaardbprod-rds01.mykaarma.de\/SecureDBProd?serverTimezone=UTC/g" /etc/kaarmaconfig-prod-de/securedb.xml

#QA02
RUN sed -i "s/jdbc:mysql:\/\/db.kaar.biz:3306\/SecureDBQA/jdbc:mysql:\/\/db.kaar.biz:3306\/SecureDBQA?serverTimezone=PST/g" /etc/kaarmaconfig-qa02/securedb.xml

#QA-AWS
RUN sed -i "s/jdbc:mysql:\/\/db.kaar.biz:3306\/SecureDBQA/jdbc:mysql:\/\/db.kaar.biz:3306\/SecureDBQA?serverTimezone=PST/g" /etc/kaarmaconfig-qa-aws/securedb.xml

#DEVVM
RUN sed -i "s/jdbc:mysql:\/\/securedb:3306\/SecureDBQA/jdbc:mysql:\/\/securedb:3306\/SecureDBQA?serverTimezone=PST/g" /etc/kaarmaconfig-devvm/securedb.xml

#DEVVM-DE
RUN sed -i "s/jdbc:mysql:\/\/securedb:3306\/SecureDBQA/jdbc:mysql:\/\/securedb:3306\/SecureDBQA?serverTimezone=PST/g" /etc/kaarmaconfig-devvm-de/securedb.xml


ADD kcommunications-server*.jar app.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]
