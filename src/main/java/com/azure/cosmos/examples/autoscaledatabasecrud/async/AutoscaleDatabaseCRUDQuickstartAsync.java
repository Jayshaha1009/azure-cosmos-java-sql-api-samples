// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.cosmos.examples.autoscaledatabasecrud.async;

import com.azure.cosmos.ConsistencyLevel;
import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosAsyncDatabase;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.examples.common.AccountSettings;
import com.azure.cosmos.models.CosmosDatabaseProperties;
import com.azure.cosmos.models.CosmosDatabaseRequestOptions;
import com.azure.cosmos.models.CosmosDatabaseResponse;
import com.azure.cosmos.models.ThroughputProperties;
import com.azure.cosmos.util.CosmosPagedFlux;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoscaleDatabaseCRUDQuickstartAsync {

    private CosmosAsyncClient client;

    private final String databaseName = "AzureSampleFamilyDB";

    private CosmosAsyncDatabase database;

    protected static Logger logger = LoggerFactory.getLogger(AutoscaleDatabaseCRUDQuickstartAsync.class);

    public void close() {
        client.close();
    }

    /**
     * Sample to demonstrate the following AUTOSCALE database CRUD operations:
     * -Create
     * -Read by ID
     * -Read all
     * -Delete
     */
    public static void main(String[] args) {
        AutoscaleDatabaseCRUDQuickstartAsync p = new AutoscaleDatabaseCRUDQuickstartAsync();

        try {
            logger.info("Starting ASYNC main");
            p.autoscaleDatabaseCRUDDemo();
            logger.info("Demo complete, please hold while resources are released");
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(String.format("Cosmos getStarted failed with %s", e));
        } finally {
            logger.info("Closing the client");
            p.shutdown();
        }
    }

    private void autoscaleDatabaseCRUDDemo() throws Exception {

        logger.info("Using Azure Cosmos DB endpoint: " + AccountSettings.HOST);

        //  Create sync client
        client = new CosmosClientBuilder()
                .endpoint(AccountSettings.HOST)
                .key(AccountSettings.MASTER_KEY)
                .consistencyLevel(ConsistencyLevel.EVENTUAL)
                .contentResponseOnWriteEnabled(true)
                .buildAsyncClient();


        createDatabaseIfNotExists();
        readDatabaseById();
        readAllDatabases();
        // deleteADatabase() is called at shutdown()

    }

    // Database Create
    private void createDatabaseIfNotExists() throws Exception {
        logger.info("Create database " + databaseName + " if not exists...");

        // Autoscale throughput settings
        ThroughputProperties autoscaleThroughputProperties = ThroughputProperties.createAutoscaledThroughput(4000); //Set autoscale max RU/s

        //Create the database with autoscale enabled
        Mono<CosmosDatabaseResponse> databaseResponse = client.createDatabaseIfNotExists(databaseName, autoscaleThroughputProperties);
        database = client.getDatabase(databaseResponse.block().getProperties().getId());

        logger.info("Done.");
    }

    // Database read
    private void readDatabaseById() throws Exception {
        logger.info("Read database " + databaseName + " by ID.");

        //  Read database by ID
        database = client.getDatabase(databaseName);

        logger.info("Done.");
    }

    // Database read all
    private void readAllDatabases() throws Exception {
        logger.info("Read all databases in the account.");

        //  Read all databases in the account
        CosmosPagedFlux<CosmosDatabaseProperties> databases = client.readAllDatabases();

        // Print
        String msg="Listing databases in account:\n";
        databases.byPage(100).flatMap(readAllDatabasesResponse -> {
            logger.info("read " +
            readAllDatabasesResponse.getResults().size() + " database(s)"
            + " with request charge of " + readAllDatabasesResponse.getRequestCharge());

            for (CosmosDatabaseProperties response : readAllDatabasesResponse.getResults()) {
                logger.info("database id: "+response.getId());
                //Got a page of query result with
            }
            return Flux.empty();
        }).blockLast();
        logger.info(msg + "\n");

        logger.info("Done.");
    }

    // Database delete
    private void deleteADatabase() throws Exception {
        logger.info("Last step: delete database " + databaseName + " by ID.");

        // Delete database
        Mono<CosmosDatabaseResponse> dbResp = client.getDatabase(databaseName).delete(new CosmosDatabaseRequestOptions());
        logger.info("Status code for database delete: {}",dbResp.block().getStatusCode());

        logger.info("Done.");
    }

    // Cleanup before close
    private void shutdown() {
        try {
            //Clean shutdown
            deleteADatabase();
        } catch (Exception err) {
            logger.error("Deleting Cosmos DB resources failed, will still attempt to close the client. See stack trace below.");
            err.printStackTrace();
        }
        client.close();
        logger.info("Done with sample.");
    }

}
