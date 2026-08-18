package io.github.parkiyong.quran.data.driver

import android.content.Context
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import okio.Path
import java.io.File

class AndroidQuranDatabaseDriverFactory(
    private val context: Context
) : QuranDatabaseDriverFactory {

    override fun createDriver(schema: SqlSchema<QueryResult.Value<Unit>>, name: String): SqlDriver {
        return AndroidSqliteDriver(schema, context, name)
    }

    override fun createDynamicDriver(path: Path): SqlDriver {
        val file = File(path.toString())
        val dummySchema = object : SqlSchema<QueryResult.Value<Unit>> {
            override val version: Long = 1
            override fun create(driver: SqlDriver): QueryResult.Value<Unit> = QueryResult.Unit
            override fun migrate(
                driver: SqlDriver,
                oldVersion: Long,
                newVersion: Long,
                vararg callbacks: app.cash.sqldelight.db.AfterVersion
            ): QueryResult.Value<Unit> = QueryResult.Unit
        }
        return AndroidSqliteDriver(
            schema = dummySchema,
            context = context,
            name = file.name
        )
    }
}
