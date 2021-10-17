package donmez.yusuf.shortener.services;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Types;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.object.SqlUpdate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class MySQLService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MySQLService.class);
    
    private static String CLOUD_SQL_CONNECTION_NAME;
    private static String DB_USER;
    private static String DB_PASS;
    private static String DB_NAME;
    private static final String DB_PROP = "/opt/data/db.properties"; //Google Secret Manager used.
    // private static final String DB_PROP = "db.properties";

	private static MySQLService service;
	private JdbcTemplate templateDyn;
	private DataSource dynDataSource;
	
	public static void load()
	{
		service = new MySQLService();
	}
	
	private MySQLService() {
		Properties props = new Properties();
		FileInputStream fis = null;
		
		try {
			fis = new FileInputStream(DB_PROP);
			props.load(fis);

			CLOUD_SQL_CONNECTION_NAME = props.getProperty("DYN_CONNECTION_NAME");
			DB_USER = props.getProperty("DYN_USER");
			DB_PASS = props.getProperty("DYN_PASS");
			DB_NAME = props.getProperty("DYN_NAME");
		} catch (IOException e) {
			e.printStackTrace();
			LOGGER.error("DB_PROP read error", e.fillInStackTrace());
		}
        
        dynDataSource = createConnectionPool();

		templateDyn = new JdbcTemplate( dynDataSource );
		System.out.println("DB connection is done");
	}
	
	public static MySQLService getInstance()
	{
		return service;
	}
	
    
    /*  @SuppressFBWarnings(
        value = "USBR_UNNECESSARY_STORE_BEFORE_RETURN",
        justification = "Necessary for sample region tag.") */
    private DataSource createConnectionPool() {
        System.out.println("creating connection Pool..");
        // [START cloud_sql_mysql_servlet_create]
        // Note: For Java users, the Cloud SQL JDBC Socket Factory can provide authenticated connections
        // which is preferred to using the Cloud SQL Proxy with Unix sockets.
        // See https://github.com/GoogleCloudPlatform/cloud-sql-jdbc-socket-factory for details.

        // The configuration object specifies behaviors for the connection pool.
        HikariConfig config = new HikariConfig();

        // The following URL is equivalent to setting the config options below:
        // jdbc:mysql:///<DB_NAME>?cloudSqlInstance=<CLOUD_SQL_CONNECTION_NAME>&
        // socketFactory=com.google.cloud.sql.mysql.SocketFactory&user=<DB_USER>&password=<DB_PASS>
        // See the link below for more info on building a JDBC URL for the Cloud SQL JDBC Socket Factory
        // https://github.com/GoogleCloudPlatform/cloud-sql-jdbc-socket-factory#creating-the-jdbc-url

        // Configure which instance and what database user to connect with.
        config.setJdbcUrl(String.format("jdbc:mysql:///%s", DB_NAME));
        config.setUsername(DB_USER); // e.g. "root", "mysql"
        config.setPassword(DB_PASS); // e.g. "my-password"

        config.addDataSourceProperty("socketFactory", "com.google.cloud.sql.mysql.SocketFactory");
        config.addDataSourceProperty("cloudSqlInstance", CLOUD_SQL_CONNECTION_NAME);

        // The ipTypes argument can be used to specify a comma delimited list of preferred IP types 
        // for connecting to a Cloud SQL instance. The argument ipTypes=PRIVATE will force the 
        // SocketFactory to connect with an instance's associated private IP. 
        config.addDataSourceProperty("ipTypes", "PUBLIC,PRIVATE");

        // ... Specify additional connection properties here.
        // [START_EXCLUDE]

        // [START cloud_sql_mysql_servlet_limit]
        // maximumPoolSize limits the total number of concurrent connections this pool will keep. Ideal
        // values for this setting are highly variable on app design, infrastructure, and database.
        config.setMaximumPoolSize(5);
        // minimumIdle is the minimum number of idle connections Hikari maintains in the pool.
        // Additional connections will be established to meet this value unless the pool is full.
        config.setMinimumIdle(5);
        // [END cloud_sql_mysql_servlet_limit]

        // [START cloud_sql_mysql_servlet_timeout]
        // setConnectionTimeout is the maximum number of milliseconds to wait for a connection checkout.
        // Any attempt to retrieve a connection from this pool that exceeds the set limit will throw an
        // SQLException.
        config.setConnectionTimeout(10000); // 10 seconds
        // idleTimeout is the maximum amount of time a connection can sit in the pool. Connections that
        // sit idle for this many milliseconds are retried if minimumIdle is exceeded.
        config.setIdleTimeout(600000); // 10 minutes
        // [END cloud_sql_mysql_servlet_timeout]

        // [START cloud_sql_mysql_servlet_backoff]
        // Hikari automatically delays between failed connection attempts, eventually reaching a
        // maximum delay of `connectionTimeout / 2` between attempts.
        // [END cloud_sql_mysql_servlet_backoff]

        // [START cloud_sql_mysql_servlet_lifetime]
        // maxLifetime is the maximum possible lifetime of a connection in the pool. Connections that
        // live longer than this many milliseconds will be closed and reestablished between uses. This
        // value should be several minutes shorter than the database's timeout value to avoid unexpected
        // terminations.
        config.setMaxLifetime(1800000); // 30 minutes
        // [END cloud_sql_mysql_servlet_lifetime]

        // [END_EXCLUDE]

        // Initialize the connection pool using the configuration object.
        DataSource pool = new HikariDataSource(config);
        // [END cloud_sql_mysql_servlet_create]
        System.out.println("CREATED connection Pool");
        return pool;
    }


	public List executeDynSelectQuery( String sql )
	{
		return templateDyn.queryForList( sql );
	}
	
	public String createUpdateStatement( String tableName, MapSqlParameterSource params )
	{
		StringBuffer buffer = new StringBuffer();
		
		buffer.append("Update ");
		buffer.append(tableName);
		buffer.append(" Set ");
		appendUpdateFields(buffer, params );
		buffer.append(" Where id = :id");
		System.out.printf("update statement %s \n", buffer.toString());
		
		return buffer.toString();
	}
	
	private void appendUpdateFields(StringBuffer buffer, MapSqlParameterSource params ) {
		for (Object field : params.getValues().keySet()) {
			if( field.equals("id") )
				continue;
			buffer.append( field.toString() + " = :"  );
			buffer.append( field.toString() + "," );
		}
		buffer.setLength(buffer.length() - 1 );
	}

	public String createInsertStatement( String tableName, MapSqlParameterSource params )
	{
		StringBuffer buffer = new StringBuffer();
		
		buffer.append("INSERT INTO ");
		buffer.append(tableName);
		buffer.append(" (");
		appendInsertFields( buffer, params );
		buffer.append(" ) VALUES (");
		appendInsertValues( buffer, params );
		buffer.append(" ) ");
		System.out.printf("insert statement %s \n", buffer.toString());
		return buffer.toString();
	}
	
	private void appendInsertFields(StringBuffer buffer, MapSqlParameterSource params ) {
		for (Object field : params.getValues().keySet() ) {
			buffer.append( field.toString()  + "," );
		}
		buffer.setLength(buffer.length()-1);
	}

	private void appendInsertValues(StringBuffer buffer, MapSqlParameterSource params ) {
		for (Object field : params.getValues().keySet() ) {
			buffer.append( ":" + field.toString() + "," );
		}
		buffer.setLength(buffer.length() - 1 );
	}
	
	public void executeUpdateQuery( String sql, MapSqlParameterSource params )
	{
	    NamedParameterJdbcTemplate template = new NamedParameterJdbcTemplate(dynDataSource);
	    int i = template.update(sql, params);
		System.out.printf("update executed %s \n", i);
	}
	
	public long executeInsertQuery( String sql, MapSqlParameterSource params )
	{
		SqlUpdate insert = new SqlUpdate(dynDataSource,sql);
		
		insert.setReturnGeneratedKeys(true);
		insert.setGeneratedKeysColumnNames(new String[] {"id"}); 
		generateInsertObject(insert,params);
		insert.compile();
		GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
		
		int i = insert.updateByNamedParam(params.getValues(), keyHolder);
		System.out.printf("insert executed %s --  %s \n", i, keyHolder.getKey().longValue());
		
		return keyHolder.getKey().longValue();
	}
	
	private void generateInsertObject(SqlUpdate insert, MapSqlParameterSource params ) {
		for (Object fieldValue : params.getValues().keySet()) {
			if( params.getValues().get(fieldValue) == null )
			{
				insert.declareParameter(new SqlParameter(fieldValue.toString(),Types.NULL));
			}
			else if( params.getValues().get(fieldValue) instanceof Integer )
			{
				insert.declareParameter(new SqlParameter(fieldValue.toString(),Types.INTEGER));
			}
			else if( params.getValues().get(fieldValue) instanceof Date )
			{
				insert.declareParameter(new SqlParameter(fieldValue.toString(),Types.TIMESTAMP));
			}
			else
			{
				insert.declareParameter(new SqlParameter(fieldValue.toString(),Types.VARCHAR));
			}
		}
	}
	
	@Transactional(rollbackFor = Exception.class,propagation = Propagation.REQUIRES_NEW)
	public void executeUpdateSql(String sql)
	{
		if( sql.contains(";") )
		{
			String [] sqlList = sql.split(";");
			
			for (int i = 0; i < sqlList.length; i++) {
				String string = sqlList[i];
				if( string.trim().equals("") )
					continue;
				if( string.endsWith("\\"))
				{
					StringBuffer str = new StringBuffer(string);
					str.setLength(str.length()-1);
					str.append(";");
					i++;
					str.append(sqlList[i]);
					while( str.charAt(str.length()-1) == '\\' )
					{
						str.setLength(str.length()-1);
						str.append(";");
						i++;
						str.append(sqlList[i]);
					}
					string = str.toString();
				}
				templateDyn.execute(string + ";");
			}
		}
		else
		{
			templateDyn.execute(sql);
		}
	}

}
