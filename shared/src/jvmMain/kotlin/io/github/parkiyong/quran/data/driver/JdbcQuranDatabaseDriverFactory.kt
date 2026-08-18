package io.github.parkiyong.quran.data.driver

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import okio.Path

class JdbcQuranDatabaseDriverFactory(
    private val basePath: Path? = null
) : QuranDatabaseDriverFactory {

    override fun createDriver(schema: SqlSchema<QueryResult.Value<Unit>>, name: String): SqlDriver {
        val url = if (name == JdbcSqliteDriver.IN_MEMORY) {
            JdbcSqliteDriver.IN_MEMORY
        } else if (basePath != null) {
            "jdbc:sqlite:${(basePath / name)}"
        } else {
            "jdbc:sqlite:$name"
        }
        val driver = JdbcSqliteDriver(url)
        if (name == JdbcSqliteDriver.IN_MEMORY) {
            schema.create(driver)
        }
        return driver
    }

    override fun createDynamicDriver(path: Path): SqlDriver {
        return JdbcSqliteDriver("jdbc:sqlite:$path")
    }
}
