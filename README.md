# sp_health

To run dev: sbt run

To run production at NÄL: sbt runProdNal

The pub-sub subscriptions that are used are defined in application.conf (dev version) and prod_nal.conf(prod version). These files also shows which power-bi-endpoints the system uses.

The system uses a local SQL-database (Microsoft SQL server) to calculate frequencies and the database can also be used to build reports in Power BI. The database-settings are specified in application.conf. The steps to connect a database to the system are as follows (windows).

1. Download the [local sql-server](https://www.microsoft.com/sql-server/sql-server-downloads)
2. Create a database in the sql server. 
3. Add the following environemnt properites
    SQLSERVER_HOST (The name of the SQL-SERVER)
    SQLSERVER_PORT (The port the server is running on, probably 1433)
    SQLSERVER_INTELLIGENT_AKUT_MOTTAGNING_DB_NAME (The name of the database you created in step 2)
4. Run this project (The tables will be created automatically)



