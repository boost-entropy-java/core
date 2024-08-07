package com.dotmarketing.startup.runonce;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.google.common.collect.Lists;
import io.vavr.control.Try;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Task05200WorkflowTaskUniqueKey implements StartupTask {

   private final static String TABLE_NAME = "workflow_task";

   private final static String CONSTRAINT_NAME = "unique_workflow_task";

   private final static String ADD_CONSTRAINT_SQL = "ALTER TABLE workflow_task ADD CONSTRAINT unique_workflow_task UNIQUE (webasset,language_id)";

    @Override
    public boolean forceRun() {
        final DotDatabaseMetaData dotDatabaseMetaData = new DotDatabaseMetaData();
        try {
            final List<String> constraints = dotDatabaseMetaData.getConstraints(TABLE_NAME);
            if (DbConnectionFactory.isMsSql()) {
                //This is necessary since MS-SQL might store certain constraints as indices
                constraints.addAll(dotDatabaseMetaData.getIndices(TABLE_NAME));
            }
            return constraints.stream().map(String::toLowerCase).noneMatch(s -> s.equals(CONSTRAINT_NAME));
        } catch (DotDataException e) {
           throw  new DotRuntimeException(e);
        }
    }

    @Override
    public void executeUpgrade() throws DotDataException, DotRuntimeException {
        Logger.debug(this,
                String.format("Upgrading workflow_task table adding `%s` constraint",
                        CONSTRAINT_NAME));

        try {
            final Connection connection = DbConnectionFactory.getConnection();
            connection.setAutoCommit(true);

            final DotConnect dotConnect = new DotConnect();

            // 18524: set to default language records with null language id
            final Optional<Number> optionalLang = dotConnect
                    .setSQL("select id from language where language_code=?")
                    .addParam(Config.getStringProperty("DEFAULT_LANGUAGE_CODE", "en"))
                    .loadObjectResults(connection).stream().map(r -> (Number)r.get("id")).findFirst();

            if(optionalLang.isEmpty()){
               throw new DotDataException("I wasn't able to find a Language marked as default int the db.");
            }

            final Number defaultLangId = optionalLang.get();

            dotConnect
                    .setSQL("update workflow_task set language_id=?, mod_date=? where language_id is null")
                    .addParam(defaultLangId)
                    .addParam(new Date())
                    .loadResult(connection);

            final List<Map<String, Object>> results = dotConnect
                    .setSQL("select webasset,language_id from workflow_task group by webasset,language_id having count(*)>1")
                    .loadObjectResults(connection);

            for (final Map<String, Object> map : results) {
                final String webAsset = (String) map.get("webasset");
                //Number is the super class of Long, Integer and BidDecimal making it cross-driver.
                final Number lang = Try.of(() -> (Number) map.get("language_id")).getOrElse(0);

                final List<String> deleteMes = dotConnect
                        .setSQL("select id from workflow_task where webasset=? and language_id=? order by mod_date desc")
                        .addParam(webAsset)
                        .addParam(lang.longValue())
                        .setStartRow(1) //magic happens here
                        .loadObjectResults(connection)
                        .stream()
                        .map(r -> (String) r.get("id"))
                        .collect(Collectors.toList());

                final List<List<String>> partitionDeleteMes = Lists.partition(deleteMes, 100);

                for (final List<String> partition : partitionDeleteMes) {
                    final String subQuery = "'" + String.join("','", partition) + "'";

                    Logger.warn(this, "Found multiple workflow tasks for contentlet id:" + webAsset
                            + " language: " + lang + ". Keeping the most recent workflow task");

                    new DotConnect()
                            .setSQL("delete from workflow_comment where workflowtask_id in ("
                                    + subQuery
                                    + ")").loadResult(connection);

                    new DotConnect()
                            .setSQL("delete from workflow_history where workflowtask_id  in ("
                                    + subQuery + ")").loadResult(connection);

                    new DotConnect()
                            .setSQL("delete from workflowtask_files where workflowtask_id in ("
                                    + subQuery + ")").loadResult(connection);

                    new DotConnect()
                            .setSQL("delete from workflow_task where id in (" + subQuery + ")")
                            .loadResult(connection);
                }

            }

                dotConnect.executeStatement(ADD_CONSTRAINT_SQL, connection);

        } catch (SQLException e) {
            throw new DotDataException("Error occurred while applying wf constraint ", e.getMessage(), e);
        }
    }

}
