package io.github.parkiyong.quran.data.driver

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import okio.Path

interface QuranDatabaseDriverFactory {
    fun createDriver(schema: SqlSchema<QueryResult.Value<Unit>>, name: String): SqlDriver
    fun createDynamicDriver(path: Path): SqlDriver
}
