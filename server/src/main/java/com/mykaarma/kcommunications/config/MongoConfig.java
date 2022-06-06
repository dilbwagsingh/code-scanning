package com.mykaarma.kcommunications.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ReadPreference;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;

@EnableMongoRepositories({"com.mykaarma.kcommunications.mongo.repository"})
@Configuration
public class MongoConfig {

	@Value("${mongo.userName}")
	private String mongoUserName;

	@Value("${mongo.credDb}")
	private String credentialDatabase;

	@Value("${mongo.dataDb}")
	private String database;

	@Value("${mongo.password}")
	private String password;

	@Value("${mongo.replicaHost1}")
	private String replicaHost1;

	@Value("${mongo.replicaHost2}")
	private String replicaHost2;

	@Value("${mongo.replicaPort1}")
	private Integer replicaPort1;

	@Value("${mongo.replicaPort2}")
	private int replicaPort2;

	@Value("${mongo.W}")
	private int W;

	@Value("${mongo.fsync}")
	private boolean fsync;

	@Value("${mongo.connectionsPerHost}")
	private int connectionPerHost;

	@Value("${mongo.connectTimeout}")
	private int connectTimeout;

	@Value("${mongo.maxWaitTime}")
	private int maxWaitTime;

	@Value("${mongo.socketKeepAlive}")
	private boolean socketKeepAlive;

	@Value("${mongo.socketTimeout}")
	private int socketTimeOut;

	@Value("${mongo.threadsAllowedToBlockForConnectionMultiplier}")
	private int threadsAllowedToBlockForConnectionMultiplier;

	@Value("${mongo.replicaHost3}")
	private String replicaHost3;

	@Value("${mongo.replicaPort3}")
	private int replicaPort3;

	@Bean
	public MongoDbFactory mongoDbFactory() throws Exception{
		MongoCredential credential = MongoCredential.createCredential(mongoUserName, credentialDatabase, password.toCharArray());
		List<ServerAddress> list = new ArrayList<ServerAddress>();
		list.add(new ServerAddress(replicaHost1,replicaPort1));
		list.add(new ServerAddress(replicaHost2,replicaPort2));
		list.add(new ServerAddress(replicaHost3,replicaPort3));

		WriteConcern writeConcern = new WriteConcern(1);
		writeConcern.withW(W);
		writeConcern.withJournal(fsync);
		ReadPreference readPreference = ReadPreference.primaryPreferred();
		MongoClientOptions options = MongoClientOptions.builder().
			connectionsPerHost(connectionPerHost).
			connectTimeout(connectTimeout).
			maxWaitTime(maxWaitTime).
			socketKeepAlive(socketKeepAlive).
			socketTimeout(socketTimeOut).
			threadsAllowedToBlockForConnectionMultiplier(threadsAllowedToBlockForConnectionMultiplier).
			writeConcern(writeConcern).
			readPreference(readPreference).build();
		MongoClient mongoClient = new MongoClient(list,Arrays.asList(credential),options);

		SimpleMongoDbFactory simpleMongoDbFactory = new SimpleMongoDbFactory(mongoClient, database);

		return simpleMongoDbFactory;
	}


	@Bean
	public MongoTemplate mongoTemplate() throws Exception {
		return new MongoTemplate(mongoDbFactory());
	}
}