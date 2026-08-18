package io.github.parkiyong.quran.data.driver

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import okio.Path

class NativeQuranDatabaseDriverFactory : QuranDatabaseDriverFactory {

    override fun createDriver(schema: SqlSchema<QueryResult.Value<Unit>>, name: String): SqlDriver {
        return NativeSqliteDriver(schema, name)
    }

    override fun createDynamicDriver(path: Path): SqlDriver {
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
        return NativeSqliteDriver(
            schema = dummySchema,
            name = path.name
        )
    }
}
