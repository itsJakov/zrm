import hr.algebra.jgojevi.zrm.Database
import kotlinx.serialization.Serializable

@Serializable
data class Snapshot(val version: Int, val tables: List<Snapshot.Table>) {

    @Serializable
    data class Table(val name: String, val columns: List<Snapshot.Column>)

    @Serializable
    data class Column(val name: String)

}